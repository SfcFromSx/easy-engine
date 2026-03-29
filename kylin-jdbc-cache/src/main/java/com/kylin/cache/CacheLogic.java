package com.kylin.cache;

import com.kylin.DriverConfig;
import com.kylin.SqlCommentParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 主流程类共享的缓存逻辑。
 */
public class CacheLogic {

    private static final Logger log = LoggerFactory.getLogger(CacheLogic.class);

    private final RedisCacheManager cache;
    private final DriverConfig config;
    private final CachePolicy cachePolicy;

    public CacheLogic(RedisCacheManager cache, DriverConfig config, CachePolicy cachePolicy) {
        this.cache = cache;
        this.config = config;
        this.cachePolicy = cachePolicy;
    }

    public String buildKey(SqlCommentParser.ParsedSql parsed, String paramFingerprint, String datasourceName) {
        String effectiveDsName = config.isCacheDataSourceIsolationEnabled() ? datasourceName : "shared";
        if (parsed.metadata.cacheKey != null) {
            return config.getCacheKeyPrefix() + effectiveDsName + ":" + parsed.metadata.cacheKey;
        }
        String raw = effectiveDsName
                + "|" + parsed.cleanSql
                + "|" + (paramFingerprint == null ? "" : paramFingerprint)
                + "|" + cachePolicy.cacheModeKeyTag();
        return config.getCacheKeyPrefix() + md5Hex(raw);
    }

    public CachedResultSet tryGet(String key) {
        byte[] bytes = cache.get(key);
        if (bytes == null) return null;
        try {
            return ResultSetSerializer.deserialize(bytes);
        } catch (Exception e) {
            log.warn("无法为键 '{}' 反序列化缓存的 ResultSet: {}", key, e.getMessage());
            return null;
        }
    }

    public CachedResultSet storeAndReturn(String key, ResultSet rs, int ttlSeconds) throws SQLException {
        byte[] bytes;
        try {
            bytes = ResultSetSerializer.serialize(rs);
        } catch (Exception e) {
            log.error("无法序列化 ResultSet: {}", e.getMessage());
            throw new SQLException("结果集序列化失败: " + e.getMessage(), e);
        }

        try {
            cache.set(key, bytes, ttlSeconds);
        } catch (Exception e) {
            log.warn("无法将 ResultSet 存储到 Redis (键 '{}'): {}", key, e.getMessage());
        }

        try {
            return ResultSetSerializer.deserialize(bytes);
        } catch (Exception e) {
            throw new SQLException("结果集反序列化失败: " + e.getMessage(), e);
        }
    }

    public int effectiveTtl(SqlCommentParser.ParsedSql parsed) {
        return (parsed.metadata.cacheTtl != null && parsed.metadata.cacheTtl > 0) 
            ? parsed.metadata.cacheTtl 
            : config.getDefaultTtlSeconds();
    }

    public DriverConfig getConfig() {
        return config;
    }

    public CachePolicy getCachePolicy() {
        return cachePolicy;
    }

    private static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    @FunctionalInterface
    public interface SqlExecutor {
        ResultSet execute() throws SQLException;
    }

    public static final class CacheExecutionResult {
        public final ResultSet resultSet;
        public final boolean cacheHit;
        public final String cacheKey;

        public CacheExecutionResult(ResultSet resultSet, boolean cacheHit, String cacheKey) {
            this.resultSet = resultSet;
            this.cacheHit = cacheHit;
            this.cacheKey = cacheKey;
        }
    }

    public ResultSet executeWithCache(SqlCommentParser.ParsedSql parsed, String paramFingerprint, SqlExecutor executor, String datasourceName) throws SQLException {
        return executeWithCacheDetailed(parsed, paramFingerprint, executor, datasourceName).resultSet;
    }

    public CacheExecutionResult executeWithCacheDetailed(SqlCommentParser.ParsedSql parsed, String paramFingerprint, SqlExecutor executor, String datasourceName) throws SQLException {
        String key = null;
        try {
            if (parsed.metadata.noCache) {
                return new CacheExecutionResult(executor.execute(), false, null);
            }
            if (cachePolicy.shouldBypassCacheBeforeLookup(parsed)) {
                log.debug("安全模式命中易变 SQL，跳过缓存: {}", parsed.cleanSql);
                return new CacheExecutionResult(executor.execute(), false, null);
            }
            key = buildKey(parsed, paramFingerprint, datasourceName);

            if (!parsed.metadata.cacheRefresh) {
                CachedResultSet cached = tryGet(key);
                if (cached != null) {
                    return new CacheExecutionResult(cached, true, key);
                }
            }
        } catch (Exception e) {
            log.warn("缓存读取/预处理异常，降级到透传执行: {}", e.getMessage());
            return new CacheExecutionResult(executor.execute(), false, key);
        }

        ResultSet rs = executor.execute();

        if (!cachePolicy.shouldCacheResultSet(rs)) {
            log.debug("安全模式命中非白名单结果集类型，跳过缓存: {}", parsed.cleanSql);
            return new CacheExecutionResult(rs, false, key);
        }

        try {
            return new CacheExecutionResult(storeAndReturn(key, rs, effectiveTtl(parsed)), false, key);
        } catch (Exception e) {
            log.error("查询成功但缓存化处理失败: {}", e.getMessage());
            throw new SQLException("Kylin 执行成功，但缓存处理异常: " + e.getMessage(), e);
        }
    }
}
