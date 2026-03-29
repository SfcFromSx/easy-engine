package com.kylin.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 每次获取都通过反射加载驱动类新建连接的单连（无池化）Provider。
 */
public class SingleConnectionProvider implements ConnectionProvider {

    private static final Logger log = LoggerFactory.getLogger(SingleConnectionProvider.class);

    private final DataSourceConfig config;
    private final Driver driver;

    public SingleConnectionProvider(DataSourceConfig config) {
        this.config = config;
        try {
            Class<?> driverClass = Class.forName(config.getDriverClass());
            this.driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("无法加载底层驱动程序类: " + config.getDriverClass(), e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        Properties props = new Properties();
        if (config.getUsername() != null && !config.getUsername().isEmpty()) {
            props.setProperty("user", config.getUsername());
        }
        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            props.setProperty("password", config.getPassword());
        }
        
        Connection conn = driver.connect(config.getJdbcUrl(), props);
        if (conn == null) {
            throw new SQLException("底层驱动程序拒绝了 URL: " + config.getJdbcUrl());
        }
        log.debug("为 [{}] 创建了新的非池化连接", config.getName());
        return conn;
    }

    @Override
    public void close() throws Exception {
        // 单连接模式不需要关闭任何长期持有的资源
    }
}
