package com.kylin.datasource;

import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.*;

/**
 * {@link DataSourceRegistry} 的单元测试。
 */
public class DataSourceRegistryTest {

    private DataSourceRegistry registry;

    @Before
    public void setUp() {
        registry = new DataSourceRegistry();
    }

    @Test
    public void testRegisterAndGetConfig() {
        DataSourceConfig config = new DataSourceConfig(
                "test_db", "kylin", "org.apache.kylin.jdbc.Driver",
                "jdbc:kylin://localhost/test", "ADMIN", "KYLIN", false, 0, 0, 0);
        registry.register(config);

        assertNotNull(registry.getConfig("test_db"));
        assertEquals("test_db", registry.getConfig("test_db").getName());
    }

    @Test
    public void testGetUnregisteredReturnsNull() {
        assertNull(registry.getConfig("missing"));
    }

    @Test
    public void testDefaultDataSource() {
        registry.setDefaultDataSourceName("kylin_local");
        assertEquals("kylin_local", registry.getDefaultDataSourceName());
    }

    @Test
    public void testCloseRegistryClosesAllPools() throws Exception {
        // 注册一个模拟配置
        DataSourceConfig config = new DataSourceConfig(
                "mock", "kylin", "org.apache.kylin.jdbc.Driver",
                "jdbc:kylin://localhost/mock", "ADMIN", "KYLIN", false, 0, 0, 0);
        registry.register(config);

        // 触发连接创建 (初始化 HikariCP)
        try {
            registry.getConnection("mock").close();
        } catch (SQLException ignored) {
        }

        // 关闭注册表
        registry.close();

        // 关闭后再获取连接应报错
        try {
            registry.getConnection("mock");
            fail("应抛出异常");
        } catch (SQLException e) {
            assertTrue(e.getMessage().contains("close") || e.getMessage().contains("not found"));
        }
    }
}
