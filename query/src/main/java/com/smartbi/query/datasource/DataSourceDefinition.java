package com.smartbi.query.datasource;

public class DataSourceDefinition {

    private final String name;
    private final String type;
    private final String driverClass;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int maxPoolSize;
    private final int minIdle;
    private final long connectionTimeoutMs;

    public DataSourceDefinition(String name,
                                String type,
                                String driverClass,
                                String jdbcUrl,
                                String username,
                                String password,
                                int maxPoolSize,
                                int minIdle,
                                long connectionTimeoutMs) {
        this.name = name;
        this.type = type;
        this.driverClass = driverClass;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.maxPoolSize = maxPoolSize;
        this.minIdle = minIdle;
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public long getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }
}
