package com.kylin.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 基于 HikariCP 实现的池化 Connection 包装器。
 */
public class HikariConnectionProvider implements ConnectionProvider {

    private static final Logger log = LoggerFactory.getLogger(HikariConnectionProvider.class);

    private final DataSourceConfig config;
    private final HikariDataSource dataSource;

    public HikariConnectionProvider(DataSourceConfig config) {
        this.config = config;
        
        HikariConfig hc = new HikariConfig();
        hc.setPoolName("HikariPool-" + config.getName());
        hc.setDriverClassName(config.getDriverClass());
        hc.setJdbcUrl(config.getJdbcUrl());
        if (config.getUsername() != null && !config.getUsername().isEmpty()) {
            hc.setUsername(config.getUsername());
        }
        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            hc.setPassword(config.getPassword());
        }
        hc.setMaximumPoolSize(config.getPoolMaxSize());
        hc.setMinimumIdle(config.getPoolMinIdle());
        hc.setConnectionTimeout(config.getPoolConnectionTimeoutMs());

        this.dataSource = new HikariDataSource(hc);
        log.info("已针对 [{}] 完成 HikariCP 线程池的初始化。最大连接数={}", config.getName(), config.getPoolMaxSize());
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("内部数据源 [{}] 线程池已关闭。", config.getName());
        }
    }
}
