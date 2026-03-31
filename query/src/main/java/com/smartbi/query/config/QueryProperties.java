package com.smartbi.query.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "engine.query")
public class QueryProperties {

    private final Auth auth = new Auth();
    private final Cache cache = new Cache();
    private final Trace trace = new Trace();
    private final Datasource datasource = new Datasource();
    private String managerUrl = "http://localhost:8090";

    public Auth getAuth() {
        return auth;
    }

    public Cache getCache() {
        return cache;
    }

    public Trace getTrace() {
        return trace;
    }

    public Datasource getDatasource() {
        return datasource;
    }

    public String getManagerUrl() {
        return managerUrl;
    }

    public void setManagerUrl(String managerUrl) {
        this.managerUrl = managerUrl;
    }

    public static class Auth {
        private String username = "ADMIN";
        private String password = "KYLIN";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Cache {
        private int defaultTtlSeconds = 300;
        private String keyPrefix = "kylin_cache:";
        private long maxCacheSizeBytes = 2 * 1024 * 1024L;
        private boolean safeModeEnabled = false;
        private boolean preparedSqlEnabled = true;
        private boolean datasourceIsolationEnabled = true;

        public int getDefaultTtlSeconds() {
            return defaultTtlSeconds;
        }

        public void setDefaultTtlSeconds(int defaultTtlSeconds) {
            this.defaultTtlSeconds = defaultTtlSeconds;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public long getMaxCacheSizeBytes() {
            return maxCacheSizeBytes;
        }

        public void setMaxCacheSizeBytes(long maxCacheSizeBytes) {
            this.maxCacheSizeBytes = maxCacheSizeBytes;
        }

        public boolean isSafeModeEnabled() {
            return safeModeEnabled;
        }

        public void setSafeModeEnabled(boolean safeModeEnabled) {
            this.safeModeEnabled = safeModeEnabled;
        }

        public boolean isPreparedSqlEnabled() {
            return preparedSqlEnabled;
        }

        public void setPreparedSqlEnabled(boolean preparedSqlEnabled) {
            this.preparedSqlEnabled = preparedSqlEnabled;
        }

        public boolean isDatasourceIsolationEnabled() {
            return datasourceIsolationEnabled;
        }

        public void setDatasourceIsolationEnabled(boolean datasourceIsolationEnabled) {
            this.datasourceIsolationEnabled = datasourceIsolationEnabled;
        }
    }

    public static class Trace {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Datasource {
        private NamedDatasource defaultDatasource = new NamedDatasource();
        private Map<String, NamedDatasource> named = new LinkedHashMap<String, NamedDatasource>();

        public NamedDatasource getDefault() {
            return defaultDatasource;
        }

        public void setDefault(NamedDatasource defaultDatasource) {
            this.defaultDatasource = defaultDatasource;
        }

        public Map<String, NamedDatasource> getNamed() {
            return named;
        }

        public void setNamed(Map<String, NamedDatasource> named) {
            this.named = named;
        }
    }

    public static class NamedDatasource {
        private String name;
        private String type = "unknown";
        private String driverClass;
        private String jdbcUrl;
        private String username;
        private String password;
        private int maxPoolSize = 4;
        private int minIdle = 1;
        private long connectionTimeoutMs = 10000L;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDriverClass() {
            return driverClass;
        }

        public void setDriverClass(String driverClass) {
            this.driverClass = driverClass;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getMinIdle() {
            return minIdle;
        }

        public void setMinIdle(int minIdle) {
            this.minIdle = minIdle;
        }

        public long getConnectionTimeoutMs() {
            return connectionTimeoutMs;
        }

        public void setConnectionTimeoutMs(long connectionTimeoutMs) {
            this.connectionTimeoutMs = connectionTimeoutMs;
        }
    }
}
