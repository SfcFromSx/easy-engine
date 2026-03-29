package com.kylin.cache;

import com.kylin.DriverConfig;
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

/**
 * 集成测试：Kylin + Presto + Redis（与 docker-compose profile olap 一致：Kylin 17070、Presto 18081、Redis 6380）。
 */
public class KylinPrestoIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(KylinPrestoIntegrationTest.class);

    private static final String JDBC_URL =
            "jdbc:kylin-cached://localhost:17070/learn_kylin?redis.host=127.0.0.1&redis.port=6380";

    private static Properties connectProps() {
        Properties p = new Properties();
        p.setProperty("user", "ADMIN");
        p.setProperty("password", "KYLIN");
        return p;
    }

    private Connection conn;
    private RedisCacheManager redis;

    @Before
    public void setUp() throws Exception {
        log.info(">>> 开始 Kylin + Presto 集成测试 <<<");
        Class.forName("com.kylin.CachedKylinDriver");

        DriverConfig config = DriverConfig.parse(JDBC_URL, connectProps());
        redis = RedisCacheManager.getInstance(config);
        redis.flushAll();

        conn = DriverManager.getConnection(JDBC_URL, connectProps());
    }

    @Test
    public void testFullWorkflow() throws Exception {
        executeAndVerify("Kylin 默认", "SELECT count(*) FROM KYLIN_SALES", null);
        executeAndVerify("Kylin 缓存命中", "SELECT count(*) FROM KYLIN_SALES", null);

        executeAndVerify("Presto 路由", "SELECT 1", "presto_local");
        executeAndVerify("Presto 缓存命中", "SELECT 1", "presto_local");

        // 避免 SELECT 2：Kylin OLAP 易将字面量 2 误解析为 ordinal，引发 group by 相关错误
        String sharedSql = "SELECT 'shared-marker' AS v";
        executeAndVerify("Kylin Shared", sharedSql, "default");
        executeAndVerify("Presto Shared", sharedSql, "presto_local");
    }

    private void executeAndVerify(String tag, String sql, String engine) throws Exception {
        String finalSql = (engine != null) ? "-- engine=" + engine + "\n" + sql : sql;
        log.info("[{}] 执行 SQL: {}", tag, finalSql.replace("\n", " "));

        long start = System.currentTimeMillis();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(finalSql);
        long end = System.currentTimeMillis();

        assertTrue(tag + " 应该返回结果", rs.next());
        log.info("[{}] 耗时: {} ms, 结果: {}", tag, (end - start), rs.getObject(1));
        rs.close();
        stmt.close();
    }

    @After
    public void tearDown() throws Exception {
        if (conn != null) conn.close();
        log.info(">>> Kylin + Presto 集成测试结束 <<<");
    }
}
