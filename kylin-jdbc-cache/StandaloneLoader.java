import java.sql.*;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandaloneLoader {
    private static final Logger log = LoggerFactory.getLogger(StandaloneLoader.class);

    public static void main(String[] args) throws Exception {
        log.info("Starting standalone JAR test...");
        
        // 1. 验证驱动是否可加载
        Class.forName("com.kylin.CachedKylinDriver");
        log.info("Driver class found and registered.");
        
        // 2. 尝试获取连接 (使用 IntegrationTest 同样的参数)
        String url = "jdbc:kylin-cached://localhost:17070/learn_kylin?redis.host=127.0.0.1&redis.port=6380";
        Properties props = new Properties();
        props.setProperty("user", "ADMIN");
        props.setProperty("password", "KYLIN");
        
        try (Connection conn = DriverManager.getConnection(url, props)) {
            log.info("Connection successful!");
            
            // 3. 执行一次查询
            String sql = "SELECT count(*) FROM KYLIN_SALES";
            try (ResultSet rs = conn.createStatement().executeQuery(sql)) {
                if (rs.next()) {
                    log.info("Query successful, result: {}", rs.getString(1));
                }
            }
            
            // 4. 第二次查询（应命中缓存）
            long start = System.currentTimeMillis();
            try (ResultSet rs = conn.createStatement().executeQuery(sql)) {
                if (rs.next()) {
                    long duration = System.currentTimeMillis() - start;
                    log.info("Second query (cached) took: {} ms", duration);
                }
            }
        }
        
        log.info("Standalone JAR test PASSED!");
    }
}
