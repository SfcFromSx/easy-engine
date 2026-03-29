package com.kylin.cache;

import com.kylin.DriverConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

/**
 * 专门验证本地 Redis 环境连通性的测试类。
 */
public class RedisConnectivityTest {

    private DriverConfig config;
    private RedisCacheManager cacheManager;

    @Before
    public void setUp() {
        // 与仓库 docker-compose 一致：Redis 映射到主机 6380
        config = DriverConfig.parse(
                "jdbc:kylin-cached://localhost:17070/test?redis.host=127.0.0.1&redis.port=6380",
                new Properties());
        cacheManager = RedisCacheManager.getInstance(config);
    }

    @Test
    public void testRedisPing() {
        String testKey = "connectivity_check_key";
        String testVal = "hello_redis_" + System.currentTimeMillis();
        
        // 1. 写入
        cacheManager.set(testKey, testVal.getBytes(), 10);
        
        // 2. 读取
        byte[] retrieved = cacheManager.get(testKey);
        assertNotNull("应该能从 Redis 读取到刚才写入的数据（请确保 Redis 已启动，compose 下为 localhost:6380）", retrieved);
        assertEquals(testVal, new String(retrieved));
        
        // 3. 删除
        cacheManager.delete(testKey);
        assertNull(cacheManager.get(testKey));
    }
}
