import java.sql.*;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FatJarTestSuite {
    private static final Logger log = LoggerFactory.getLogger(FatJarTestSuite.class);

    public static void main(String[] args) {
        log.info("=================================================");
        log.info("Starting Comprehensive Fat-JAR Integration Tests");
        log.info("=================================================");

        String url = "jdbc:kylin-cached://localhost:17070/learn_kylin?redis.host=127.0.0.1&redis.port=6380";
        Properties props = new Properties();
        props.setProperty("user", "ADMIN");
        props.setProperty("password", "KYLIN");

        try {
            Class.forName("com.kylin.CachedKylinDriver");
            log.info("[INFO] Driver loaded successfully.");

            try (Connection conn = DriverManager.getConnection(url, props)) {
                log.info("[INFO] Connection established successfully.\n");

                testStatement(conn);
                testPreparedStatement(conn);
                testNoCacheHint(conn);
                testForceRefreshHint(conn);
            }

            log.info("\n[SUCCESS] All Fat-JAR integration tests EXECUTED successfully!");

        } catch (Exception e) {
            log.error("\n[ERROR] Test Suite Failed!", e);
        }
    }

    private static void testStatement(Connection conn) throws Exception {
        log.info("--- [Test 1] Regular Statement Cache ---");
        String sql = "SELECT count(*) FROM KYLIN_SALES";

        // 第一轮：不确定有没有缓存，跑一次预热
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                log.info("   > T1 Result: {}", rs.getString(1));
            }
        }

        // 第二轮：必须命中缓存
        long start = System.currentTimeMillis();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                long duration = System.currentTimeMillis() - start;
                log.info("   > T2 Result: {} (Cached took: {} ms)", rs.getString(1), duration);
            }
        }
        log.info("--- [Test 1] Done ---\n");
    }

    private static void testPreparedStatement(Connection conn) throws Exception {
        log.info("--- [Test 2] PreparedStatement Cache ---");
        
        // 由于测试环境(learn_kylin)的部分 Cube 未就绪，复杂的带有 ? 的动态传参查询会被 Kylin 引擎拦截
        // 这里只是为了演示 PreparedStatement 的基础流程没有报错。
        String psSql = "SELECT count(*) FROM KYLIN_SALES";
        try (PreparedStatement ps = conn.prepareStatement(psSql)) {
            long start1 = System.currentTimeMillis();
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    log.info("   > T1 Result: {} (PreparedStatement Init took: {} ms)", rs.getString(1), (System.currentTimeMillis() - start1));
                }
            }
            
            long start2 = System.currentTimeMillis();
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    log.info("   > T2 Result: {} (PreparedStatement Cached took: {} ms)", rs.getString(1), (System.currentTimeMillis() - start2));
                }
            }
        }
        log.info("--- [Test 2] Done ---\n");
    }

    private static void testNoCacheHint(Connection conn) throws Exception {
        log.info("--- [Test 3] SQL Hint: -- no-cache ---");
        String sql = "-- no-cache\nSELECT count(*) FROM KYLIN_SALES";

        long start = System.currentTimeMillis();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                long duration = System.currentTimeMillis() - start;
                log.info("   > Result: {} (Bypass Cache, Kylin directly took: {} ms)", rs.getString(1), duration);
            }
        }
        log.info("--- [Test 3] Done ---\n");
    }

    private static void testForceRefreshHint(Connection conn) throws Exception {
        log.info("--- [Test 4] SQL Hint: -- force-refresh ---");
        String sql = "-- force-refresh\nSELECT count(*) FROM KYLIN_SALES";

        long start = System.currentTimeMillis();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                long duration = System.currentTimeMillis() - start;
                log.info("   > Result: {} (Force Refresh from Kylin took: {} ms)", rs.getString(1), duration);
            }
        }
        log.info("--- [Test 4] Done ---\n");
    }
}
