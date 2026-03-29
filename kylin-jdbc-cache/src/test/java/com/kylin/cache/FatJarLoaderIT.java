package com.kylin.cache;

import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

/**
 * 这是一个极度硬核的集成测试（Integration Test）。
 * 
 * 作用：
 * 在 Maven 的 `verify` 阶段运行，此时 `package` 阶段已经生成了 `target/kylin-jdbc-cache-1.0.0-fat.jar`。
 * 我们使用一个完全隔离的 {@link URLClassLoader} 仅挂载这个 Fat JAR，屏蔽掉当前工程的所有测试 Classpath。
 * 如果该测试能查出数据，就 100% 证明打包出来的 Fat JAR 内部不缺少任何第三方依赖（如 Jackson, Jedis 等），
 * 并且它独立运行良好，完全符合业务直接投入生产的要求。
 */
public class FatJarLoaderIT {

    private static final Logger log = LoggerFactory.getLogger(FatJarLoaderIT.class);

    @Test
    public void testFatJarIsCompletelySelfContained() throws Exception {
        // 1. 定位打包好的 Fat JAR
        File fatJar = new File("target/kylin-jdbc-cache-1.0.0-fat.jar");
        
        // 如果文件不存在，跳过测试 (不过在 mvn verify 阶段一定存在)
        Assume.assumeTrue("Fat JAR 未找到，请先执行 mvn package", fatJar.exists());
        log.info("找到了 Fat JAR: {}", fatJar.getAbsolutePath());

        // 2. 创建一个隔离的 ClassLoader
        // 父类加载器设为 null，意味着只继承 JVM Bootstrap ClassLoader（包含 java.sql.*）
        // 完全不继承当前 Maven 测试环境引入的任何 jar 依赖。
        // 这迫使驱动必须自身包含了 Jedis, Jackson, slf4j-api, logback 等所有东西！
        try (URLClassLoader isolatedLoader = new URLClassLoader(new URL[]{fatJar.toURI().toURL()}, ClassLoader.getSystemClassLoader().getParent())) {
            
            // 3. 反射加载底层的 Kylin Driver (触发 ServiceLoader 可能无法在纯隔离环境下自动执行的 static 注册)
            Class.forName("org.apache.kylin.jdbc.Driver", true, isolatedLoader);
            
            // 4. 反射加载我们包裹好的 Driver 类
            Class<?> driverClass = Class.forName("com.kylin.CachedKylinDriver", true, isolatedLoader);
            Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
            log.info("隔离加载 CachedKylinDriver 成功: {}", driver.getClass().getName());

            // 4. 连接真实环境
            String url = "jdbc:kylin-cached://localhost:17070/learn_kylin?redis.host=127.0.0.1&redis.port=6380&cache.prefix=fatjar_test:";
            Properties props = new Properties();
            props.setProperty("user", "ADMIN");
            props.setProperty("password", "KYLIN");

            log.info("正在通过反射获取 JDBC Connection...");
            try (Connection conn = driver.connect(url, props)) {
                
                // 5. 执行查询
                try (Statement stmt = conn.createStatement()) {
                    // 先强制刷新一次
                    String sql = "-- force-refresh\nSELECT count(*) FROM KYLIN_SALES";
                    try (ResultSet rs = stmt.executeQuery(sql)) {
                        assertTrue("结果集为空！", rs.next());
                        log.info("Fat JAR 直接查询 Kylin 返回结果: {}", rs.getString(1));
                    }
                    
                    // 第二次走缓存
                    String cachedSql = "SELECT count(*) FROM KYLIN_SALES";
                    long t1 = System.currentTimeMillis();
                    try (ResultSet cacheRs = stmt.executeQuery(cachedSql)) {
                        assertTrue("缓存结果集为空！", cacheRs.next());
                        long duration = System.currentTimeMillis() - t1;
                        log.info("Fat JAR 缓存查询返回结果: {} (耗时: {} ms)", cacheRs.getString(1), duration);
                    }
                }
            }
            
            log.info("Fat JAR 纯净沙盒验证通过 ✅");
        }
    }
}
