package com.kylin.cache;

import com.kylin.DriverConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 管理 Jedis 连接池并提供针对 Redis 的简单 get/set/delete 操作。
 *
 * <p>
 * 每个唯一的 Redis host:port:db 组合共享一个 {@link RedisCacheManager} 实例，
 * 缓存在静态映射中。
 */
public class RedisCacheManager implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheManager.class);

    /** 按 "host:port:db" 键控的连接池注册表。 */
    private static final ConcurrentHashMap<String, RedisCacheManager> INSTANCES = new ConcurrentHashMap<>();

    /** 共享的异步推送线程池，使用有界队列防止 OOM */
    private static final ExecutorService ASYNC_EXECUTOR = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(10000), // 最多积压 1w 条日志
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "kylin-cache-async-worker-" + counter.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.DiscardOldestPolicy() // 队列满时丢弃最老的日志
    );

    private final JedisPool pool;
    private final long maxCacheSizeBytes;

    private RedisCacheManager(JedisPool pool, long maxCacheSizeBytes) {
        this.pool = pool;
        this.maxCacheSizeBytes = maxCacheSizeBytes;
    }

    /**
     * 根据给定配置返回共享的 {@link RedisCacheManager}。
     * 连接池在第一次调用时延迟创建。
     */
    public static RedisCacheManager getInstance(DriverConfig config) {
        String instanceKey = buildInstanceKey(config);
        String displayKey = config.getRedisHost() + ":" + config.getRedisPort() + ":" + config.getRedisDb();
        return INSTANCES.computeIfAbsent(instanceKey, k -> {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(20);
            poolConfig.setMaxIdle(10);
            poolConfig.setMinIdle(2);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestWhileIdle(true);

            int timeout = config.getRedisTimeoutMs();
            JedisPool jedisPool;
            if (config.getRedisPassword() != null && !config.getRedisPassword().isEmpty()) {
                jedisPool = new JedisPool(poolConfig,
                        config.getRedisHost(),
                        config.getRedisPort(),
                        timeout,
                        config.getRedisPassword(),
                        config.getRedisDb());
            } else {
                jedisPool = new JedisPool(poolConfig,
                        config.getRedisHost(),
                        config.getRedisPort(),
                        timeout,
                        null,
                        config.getRedisDb());
            }
            log.info("已为 {} 创建 Jedis 连接池", displayKey);
            return new RedisCacheManager(jedisPool, config.getMaxCacheSizeBytes());
        });
    }

    /**
     * 通过键检索缓存值。
     *
     * @param key 缓存键
     * @return 原始字节，如果未找到或发生错误则为 {@code null}
     */
    public byte[] get(String key) {
        try (Jedis jedis = pool.getResource()) {
            byte[] value = jedis.get(key.getBytes());
            if (value == null) {
                log.debug("缓存未命中，键: {}", key);
            } else {
                log.debug("缓存命中，键: {}", key);
            }
            return value;
        } catch (Exception e) {
            log.warn("Redis GET 失败，键 '{}': {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 将值存储在 Redis 中，带有过期时间。
     *
     * @param key        缓存键
     * @param value      要存储的原始字节
     * @param ttlSeconds 过期时间（秒）
     */
    public void set(String key, byte[] value, int ttlSeconds) {
        if (value != null && value.length > maxCacheSizeBytes) {
            log.warn("缓存跳过：值大小 ({} bytes) 超过最大限制 ({} bytes)，键: {}",
                    value.length, maxCacheSizeBytes, key);
            return;
        }
        try (Jedis jedis = pool.getResource()) {
            jedis.setex(key.getBytes(), ttlSeconds, value);
            log.debug("缓存设置键: {}, ttl: {}s", key, ttlSeconds);
        } catch (Exception e) {
            log.warn("Redis SET 失败，键 '{}': {}", key, e.getMessage());
        }
    }

    /**
     * 从 Redis 中删除一个键。
     *
     * @param key 缓存键
     */
    public void delete(String key) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(key);
            log.debug("缓存删除键: {}", key);
        } catch (Exception e) {
            log.warn("Redis DEL 失败，键 '{}': {}", key, e.getMessage());
        }
    }

    /**
     * 清空当前数据库中的所有键。
     */
    public void flushAll() {
        try (Jedis jedis = pool.getResource()) {
            jedis.flushDB();
            log.info("已清空 Redis 缓存池");
        } catch (Exception e) {
            log.warn("Redis FLUSH 失败: {}", e.getMessage());
        }
    }

    /**
     * 异步将消息推送到 Redis List（用于不阻塞主线程地发送审计日志等）。
     *
     * @param listKey Redis List 的键名
     * @param message 要推送的字符串消息 (如 JSON)
     */
    public void pushToListAsync(String listKey, String message) {
        ASYNC_EXECUTOR.submit(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.lpush(listKey, message);
                // 限制队列长度，防止 Redis 内存因消费不及时而爆炸
                jedis.ltrim(listKey, 0, 50000); 
            } catch (Exception e) {
                log.warn("向 Redis List '{}' 推送消息失败: {}", listKey, e.getMessage());
            }
        });
    }

    @Override
    public void close() {
        pool.close();
    }

    private static String buildInstanceKey(DriverConfig config) {
        String passwordKey = config.getRedisPassword() == null
                ? "<none>"
                : Integer.toHexString(config.getRedisPassword().hashCode());
        return config.getRedisHost()
                + ":" + config.getRedisPort()
                + ":" + config.getRedisDb()
                + ":" + passwordKey
                + ":" + config.getRedisTimeoutMs()
                + ":" + config.getMaxCacheSizeBytes();
    }
}
