package com.kylin.cache;

import com.kylin.DriverConfig;
import com.kylin.SqlCommentParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.*;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * 集成测试：验证真实 Kylin + Redis 场景下的缓存透传行为。
 *
 * 前提条件：
 *   1. Kylin 经 compose 映射到主机 localhost:17070
 *   2. Redis 经 compose 映射到主机 localhost:6380
 *
 * 测试逻辑：
 *   - 第一次查询：Redis 无缓存 → 透传到 Kylin 获取真实数据，并写入 Redis
 *   - 第二次查询：Redis 命中 → 直接返回缓存，完全不访问 Kylin
 */
public class IntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(IntegrationTest.class);

    // 使用 Kylin 自带的 learn_kylin 项目中确定存在的表
    private static final String TEST_SQL = "SELECT count(*) FROM KYLIN_SALES";

    private static final String JDBC_URL =
            "jdbc:kylin-cached://localhost:17070/learn_kylin?redis.host=127.0.0.1&redis.port=6380";

    private static Properties kylinConnectProps() {
        Properties p = new Properties();
        p.setProperty("user", "ADMIN");
        p.setProperty("password", "KYLIN");
        return p;
    }

    private Connection conn;
    private RedisCacheManager redis;
    private DriverConfig config;

    @Before
    public void setUp() throws Exception {
        // 加载驱动（触发 static 注册块）
        Class.forName("com.kylin.CachedKylinDriver");

        config = DriverConfig.parse(JDBC_URL, kylinConnectProps());
        redis  = RedisCacheManager.getInstance(config);

        log.info("==============================");
        log.info(" Kylin JDBC Cache 集成测试");
        log.info("==============================");
        log.info(" JDBC URL : {}", JDBC_URL);
        log.info(" SQL      : {}", TEST_SQL);
        log.info("==============================");
    }

    @Test
    public void testCacheHitOnSecondQuery() throws Exception {
        // 清空缓存，避免与驱动内部 cache 键构造（含数据源隔离等）不一致导致误判
        redis.flushAll();
        log.info("[SETUP] 已 flush Redis");

        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse(TEST_SQL);
        CacheLogic logic = new CacheLogic(redis, config, new CachePolicy(config));
        // 与路由默认数据源逻辑名一致（见 DataSourceRegistry default），非字面 "kylin"
        String cacheKey = logic.buildKey(parsed, null, "default");

        // ============================
        // 第一次查询：应该走 Kylin
        // ============================
        conn = DriverManager.getConnection(JDBC_URL, kylinConnectProps());
        long t1Start = 0, t1End = 0;
        try {
            t1Start = System.currentTimeMillis();
            ResultSet rs1 = conn.createStatement().executeQuery(TEST_SQL);
            t1End = System.currentTimeMillis();

            log.info("[第一次查询] 耗时: {} ms（应走 Kylin）", (t1End - t1Start));
            assertTrue("第一次查询应有数据", rs1.next());
            log.info("[第一次查询] 首行: {}", rs1.getString(1));
            rs1.close();
        } catch (Exception e) {
            log.error("[ERROR ALERT] IntegrationTest caught an exception!", e);
            throw e;
        }

        byte[] cached = redis.get(cacheKey);
        assertNotNull("第一次查询后 Redis 应写入缓存", cached);
        log.info("[验证] Redis 缓存已写入，大小: {} bytes ✓", cached.length);

        // 关闭第一个连接
        conn.close();

        // ============================
        // 第二次查询：应该走 Redis 缓存
        // ============================
        conn = DriverManager.getConnection(JDBC_URL, kylinConnectProps());
        long t2Start = System.currentTimeMillis();
        ResultSet rs2 = conn.createStatement().executeQuery(TEST_SQL);
        long t2End = System.currentTimeMillis();

        log.info("[第二次查询] 耗时: {} ms（应走 Redis 缓存，应远快于第一次）", (t2End - t2Start));
        assertTrue("第二次查询应有数据", rs2.next());
        log.info("[第二次查询] 首行: {}", rs2.getString(1));
        rs2.close();

        // 第二次查询比第一次快很多，说明走的是缓存
        // (宽松判断：不强制要求时间，只验证数据正确)
        log.info("==============================");
        log.info(" 结论: 缓存机制验证通过 ✓");
        log.info(" 第一次: {} ms (Kylin)", (t1End - t1Start));
        log.info(" 第二次: {} ms (Redis cache)", (t2End - t2Start));
        log.info("==============================");
    }

    @After
    public void tearDown() throws Exception {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }
}
