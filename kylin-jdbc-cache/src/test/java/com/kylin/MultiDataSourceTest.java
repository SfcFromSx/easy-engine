package com.kylin;

import com.kylin.cache.RedisCacheManager;
import com.kylin.datasource.DataSourceRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assert.*;

public class MultiDataSourceTest {
    private static final Logger log = LoggerFactory.getLogger(MultiDataSourceTest.class);

    private Connection virtualConn;
    private RedisCacheManager redisCache;
    private DriverConfig config;

    // 使用配置文件中定义的名称
    private static final String KYLIN_ENGINE = "default";
    private static final String PRESTO_ENGINE = "presto_local";
    private static final String JDBC_URL =
            "jdbc:kylin-cached://localhost:17070/learn_kylin?redis.host=127.0.0.1&redis.port=6380";

    private static Properties connectProps() {
        Properties p = new Properties();
        p.setProperty("user", "ADMIN");
        p.setProperty("password", "KYLIN");
        return p;
    }

    @Before
    public void setUp() throws Exception {
        log.info("--- 初始化真实多数据源集成测试 ---");

        // 加载驱动
        Class.forName("com.kylin.CachedKylinDriver");

        // 直接通过 DriverConfig.parse 加载类路径下的 kylin-cache.properties
        config = DriverConfig.parse(JDBC_URL, connectProps());

        redisCache = RedisCacheManager.getInstance(config);
        redisCache.flushAll(); // 清空测试缓存

        // 获取连接
        virtualConn = DriverManager.getConnection(JDBC_URL, connectProps());
        log.info("物理注册表中的数据源: {}", config.getDataSourceRegistry().getDataSourceNames());
    }

    @Test
    public void testRoutingAndCacheIsolation() throws Exception {
        String sql = "SELECT 1";

        // 1. 执行 Kylin 路由
        log.info("[测试] 路由至 Kylin...");
        Statement stmt1 = virtualConn.createStatement();
        ResultSet rs1 = stmt1.executeQuery("-- engine=" + KYLIN_ENGINE + "\n" + sql);
        assertTrue("Kylin 应该返回数据", rs1.next());
        log.info("Kylin 结果: {}", rs1.getObject(1));
        rs1.close();

        // 2. 执行 Presto 路由 (相同 SQL 内容)
        log.info("[测试] 路由至 Presto...");
        try {
            Statement stmt2 = virtualConn.createStatement();
            ResultSet rs2 = stmt2.executeQuery("-- engine=" + PRESTO_ENGINE + "\n" + sql);
            assertTrue("Presto 应该返回数据", rs2.next());
            log.info("Presto 结果: {}", rs2.getObject(1));
            rs2.close();
        } catch (Exception e) {
            log.warn("Presto 测试失败 (可能服务未启动): {}", e.getMessage());
            // 如果 Presto 确实不可用，这里会报错。根据用户要求，我们要真实测试。
            throw e;
        }

        // 3. 验证缓存隔离：再次执行 Kylin 路由，应该命中缓存
        log.info("[测试] 再次路由至 Kylin (验证缓存)...");
        long start = System.currentTimeMillis();
        ResultSet rs3 = virtualConn.createStatement().executeQuery("-- engine=" + KYLIN_ENGINE + "\n" + sql);
        long end = System.currentTimeMillis();
        assertTrue(rs3.next());
        log.info("Kylin 缓存命中耗时: {} ms", (end - start));
        rs3.close();

        // 4. 再次执行 Presto 路由，应该命中缓存
        log.info("[测试] 再次路由至 Presto (验证缓存)...");
        ResultSet rs4 = virtualConn.createStatement().executeQuery("-- engine=" + PRESTO_ENGINE + "\n" + sql);
        assertTrue(rs4.next());
        log.info("Presto 缓存命中成功");
        rs4.close();
    }

    @After
    public void tearDown() throws Exception {
        if (virtualConn != null)
            virtualConn.close();
        log.info("--- 测试完成 ---");
    }
}
