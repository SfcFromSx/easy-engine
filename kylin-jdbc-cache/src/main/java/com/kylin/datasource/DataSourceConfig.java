package com.kylin.datasource;

public class DataSourceConfig {
    private final String name;
    private final String type;
    private final String driverClass;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final boolean poolEnabled;
    private final int poolMaxSize;
    private final int poolMinIdle;
    private final long poolConnectionTimeoutMs;

    public DataSourceConfig(String name, String type, String driverClass, String jdbcUrl,
                            String username, String password, boolean poolEnabled,
                            int poolMaxSize, int poolMinIdle, long poolConnectionTimeoutMs) {
        this.name = name;
        this.type = type;
        this.driverClass = driverClass;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.poolEnabled = poolEnabled;
        this.poolMaxSize = poolMaxSize;
        this.poolMinIdle = poolMinIdle;
        this.poolConnectionTimeoutMs = poolConnectionTimeoutMs;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public String getDriverClass() { return driverClass; }
    public String getJdbcUrl() { return jdbcUrl; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public boolean isPoolEnabled() { return poolEnabled; }
    public int getPoolMaxSize() { return poolMaxSize; }
    public int getPoolMinIdle() { return poolMinIdle; }
    public long getPoolConnectionTimeoutMs() { return poolConnectionTimeoutMs; }
}
