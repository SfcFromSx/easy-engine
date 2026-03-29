package com.kylin;

import com.kylin.datasource.DataSourceConfig;
import com.kylin.datasource.DataSourceRegistry;

import java.util.Properties;

/**
 * 解析 JDBC URL 并提取 Redis、缓存和多数据源配置。
 */
public class DriverConfig {

    /** 此驱动程序接受的 JDBC URL 前缀。 */
    public static final String CACHE_SCHEME = "jdbc:kylin-cached://";

    private final String kylinUrl;
    private final String redisHost;
    private final int redisPort;
    private final String redisPassword;
    private final int redisDb;
    private final int defaultTtlSeconds;
    private final String cacheKeyPrefix;
    private final long maxCacheSizeBytes;
    private final int redisTimeoutMs;
    private final boolean conservativeCacheModeEnabled;
    private final boolean preparedStatementCacheEnabled;
    private final boolean sqlTraceEnabled;
    private final boolean sqlTraceRedisEnabled;
    private final int sqlTraceMaxSqlLength;
    private final boolean routingEnabled;
    private final boolean cacheDataSourceIsolationEnabled;
    private final Properties mergedProperties;
    private DataSourceRegistry dataSourceRegistry;

    private DriverConfig(String kylinUrl, String redisHost, int redisPort,
                         String redisPassword, int redisDb,
                         int defaultTtlSeconds, String cacheKeyPrefix,
                         long maxCacheSizeBytes, int redisTimeoutMs,
                         boolean conservativeCacheModeEnabled,
                         boolean preparedStatementCacheEnabled,
                         boolean sqlTraceEnabled,
                         boolean sqlTraceRedisEnabled,
                         int sqlTraceMaxSqlLength,
                         boolean routingEnabled,
                         boolean cacheDataSourceIsolationEnabled,
                         Properties mergedProperties,
                         DataSourceRegistry dataSourceRegistry) {
        this.kylinUrl = kylinUrl;
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.redisPassword = redisPassword;
        this.redisDb = redisDb;
        this.defaultTtlSeconds = defaultTtlSeconds;
        this.cacheKeyPrefix = cacheKeyPrefix;
        this.maxCacheSizeBytes = maxCacheSizeBytes;
        this.redisTimeoutMs = redisTimeoutMs;
        this.conservativeCacheModeEnabled = conservativeCacheModeEnabled;
        this.preparedStatementCacheEnabled = preparedStatementCacheEnabled;
        this.sqlTraceEnabled = sqlTraceEnabled;
        this.sqlTraceRedisEnabled = sqlTraceRedisEnabled;
        this.sqlTraceMaxSqlLength = sqlTraceMaxSqlLength;
        this.routingEnabled = routingEnabled;
        this.cacheDataSourceIsolationEnabled = cacheDataSourceIsolationEnabled;
        this.mergedProperties = mergedProperties;
        this.dataSourceRegistry = dataSourceRegistry;
    }

    public DataSourceRegistry getDataSourceRegistry() { return dataSourceRegistry; }

    /** 仅供测试注入 mock 使用。 */
    public void setDataSourceRegistry(DataSourceRegistry registry) { this.dataSourceRegistry = registry; }

    /**
     * 将包装后的 JDBC URL 和属性解析为 {@link DriverConfig}。
     * 支持标准格式：jdbc:kylin-cached://host:port/project?params
     */
    public static DriverConfig parse(String url, Properties info) {
        if (url == null || !url.startsWith(CACHE_SCHEME)) {
            throw new IllegalArgumentException("URL 必须以 " + CACHE_SCHEME + " 开头");
        }

        // 1. 加载默认配置文件
        Properties all = new Properties();
        try (java.io.InputStream is = DriverConfig.class.getResourceAsStream("/kylin-cache.properties")) {
            if (is != null) {
                all.load(is);
            }
        } catch (java.io.IOException ignored) {}

        // 2. 合并传入的 Properties (如用户传递的 user/password)
        if (info != null) {
            all.putAll(info);
        }

        // 3. 解析 URL
        int queryStart = url.indexOf('?');
        String mainPart = queryStart >= 0 ? url.substring(CACHE_SCHEME.length(), queryStart) : url.substring(CACHE_SCHEME.length());
        
        // 解析查询参数 (URL 优先级最高)
        if (queryStart >= 0) {
            for (String param : url.substring(queryStart + 1).split("&")) {
                int eq = param.indexOf('=');
                if (eq > 0) {
                    all.setProperty(param.substring(0, eq), param.substring(eq + 1));
                }
            }
        }

        // 4. 构造默认 Kylin URL (基于当前 JDBC URL 的主体部分)
        String kylinUrl = "jdbc:kylin://" + mainPart;

        // 5. 提取全局设置 (Redis 等)
        String redisHost = all.getProperty("redis.host", "localhost");
        int redisPort = Integer.parseInt(all.getProperty("redis.port", "6379"));
        String redisPassword = all.getProperty("redis.password", null);
        int redisDb = Integer.parseInt(all.getProperty("redis.db", "0"));
        int ttl = Integer.parseInt(all.getProperty("cache.ttl", "300"));
        String prefix = all.getProperty("cache.prefix", "kylin_cache:");
        long maxSize = Long.parseLong(all.getProperty("cache.max_size_bytes", "2097152"));
        int redisTimeout = Integer.parseInt(all.getProperty("redis.timeout_ms", "500"));
        boolean conservativeMode = Boolean.parseBoolean(all.getProperty("cache.safe_mode.enabled", "false"));
        boolean preparedCache = Boolean.parseBoolean(all.getProperty("cache.prepared_sql.enabled", "true"));
        boolean sqlTrace = Boolean.parseBoolean(all.getProperty("sql.trace.enabled", "false"));
        boolean sqlTraceRedis = Boolean.parseBoolean(all.getProperty("sql.trace.redis.enabled", "false"));
        int sqlTraceMaxLen = Integer.parseInt(all.getProperty("sql.trace.max_sql_length", "512"));
        boolean routingEnabled = Boolean.parseBoolean(all.getProperty("datasource.routing.enabled", "false"));
        boolean cacheDataSourceIsolationEnabled = Boolean.parseBoolean(all.getProperty("cache.datasource.isolation.enabled", "true"));

        // 6. 初始化注册表 (URL 产生的为 default, 配置文件里的为扩展)
        DataSourceRegistry registry = buildRegistry(all, kylinUrl);
        
        // 允许通过 URL 主体部分直接引用配置文件中定义的非 default 数据源
        if (!mainPart.isEmpty() && registry.getConfig(mainPart) != null 
                && !mainPart.equals("default") && !mainPart.contains("/")) {
            registry.setDefaultDataSourceName(mainPart);
        }

        return new DriverConfig(kylinUrl, redisHost, redisPort, redisPassword,
                redisDb, ttl, prefix, maxSize, redisTimeout,
                conservativeMode, preparedCache, sqlTrace, sqlTraceRedis, sqlTraceMaxLen, 
                routingEnabled, cacheDataSourceIsolationEnabled, all, registry);
    }

    private static DataSourceRegistry buildRegistry(Properties all, String kylinUrl) {
        DataSourceRegistry registry = new DataSourceRegistry();

        // A. 首先建立基础的 default 数据源 (源自当前连接 URL)
        DataSourceConfig urlDefault = new DataSourceConfig(
                "default", "kylin", "org.apache.kylin.jdbc.Driver",
                kylinUrl, all.getProperty("user"), all.getProperty("password"),
                false, 0, 0, 0);
        registry.register(urlDefault);
        registry.setDefaultDataSourceName("default");

        // B. 从配置文件加载额外的扩展数据源
        String namesProp = all.getProperty("datasource.names");
        if (namesProp != null && !namesProp.trim().isEmpty()) {
            for (String n : namesProp.split(",")) {
                String name = n.trim();
                if (name.isEmpty() || name.equals("default")) continue; // 避免冲突

                String pre = "datasource." + name + ".";
                DataSourceConfig dsConfig = new DataSourceConfig(
                        name,
                        all.getProperty(pre + "type", "unknown"),
                        all.getProperty(pre + "driver_class"),
                        all.getProperty(pre + "jdbc_url"),
                        all.getProperty(pre + "username", ""),
                        all.getProperty(pre + "password", ""),
                        Boolean.parseBoolean(all.getProperty(pre + "pool.enabled", "false")),
                        Integer.parseInt(all.getProperty(pre + "pool.max_size", "10")),
                        Integer.parseInt(all.getProperty(pre + "pool.min_idle", "1")),
                        Long.parseLong(all.getProperty(pre + "pool.connection_timeout_ms", "30000")));
                registry.register(dsConfig);
            }
        }
        
        // C. 如果配置文件指定了全局默认，则覆盖 A 的设置
        String explicitDefault = all.getProperty("datasource.default");
        if (explicitDefault != null && !explicitDefault.isEmpty()) {
            registry.setDefaultDataSourceName(explicitDefault);
        }
        
        return registry;
    }

    public String getKylinUrl() { return kylinUrl; }
    public String getRedisHost() { return redisHost; }
    public int getRedisPort() { return redisPort; }
    public String getRedisPassword() { return redisPassword; }
    public int getRedisDb() { return redisDb; }
    public int getDefaultTtlSeconds() { return defaultTtlSeconds; }
    public long getMaxCacheSizeBytes() { return maxCacheSizeBytes; }
    public int getRedisTimeoutMs() { return redisTimeoutMs; }
    public String getCacheKeyPrefix() { return cacheKeyPrefix; }
    public boolean isConservativeCacheModeEnabled() { return conservativeCacheModeEnabled; }
    public boolean isPreparedStatementCacheEnabled() { return preparedStatementCacheEnabled; }
    public boolean isSqlTraceEnabled() { return sqlTraceEnabled; }
    public boolean isSqlTraceRedisEnabled() { return sqlTraceRedisEnabled; }
    public int getSqlTraceMaxSqlLength() { return sqlTraceMaxSqlLength; }
    public boolean isRoutingEnabled() { return routingEnabled; }
    public boolean isCacheDataSourceIsolationEnabled() { return cacheDataSourceIsolationEnabled; }
    public Properties getMergedProperties() { return mergedProperties; }
}
