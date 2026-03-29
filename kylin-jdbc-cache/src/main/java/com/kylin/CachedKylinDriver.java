package com.kylin;

import com.kylin.cache.RedisCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Properties;

/**
 * Kylin Redis 缓存包装驱动程序的入口点。
 *
 * <p>接受的 URL 格式：
 * <pre>
 * jdbc:kylin-cached://&lt;host&gt;:7070/&lt;project&gt;?redis.host=localhost&amp;redis.port=6379
 *   &amp;redis.password=&lt;pass&gt;&amp;redis.db=0&amp;cache.ttl=300&amp;cache.prefix=kylin:
 * </pre>
 *
 * <p>拒绝所有其他 URL 格式（acceptsURL 返回 false）。
 *
 * <p>此驱动程序通过 JDBC ServiceLoader SPI
 * ({@code META-INF/services/java.sql.Driver}) 自动注册。
 */
public class CachedKylinDriver implements Driver {

    private static final Logger log =
            LoggerFactory.getLogger(CachedKylinDriver.class);

    /** Kylin 缓存 JDBC 驱动程序的主版本号。 */
    private static final int MAJOR_VERSION = 1;
    /** Kylin 缓存 JDBC 驱动程序的次版本号。 */
    private static final int MINOR_VERSION = 0;

    static {
        try {
            DriverManager.registerDriver(new CachedKylinDriver());
            log.info("CachedKylinDriver 已在 DriverManager 中注册");
        } catch (SQLException e) {
            throw new RuntimeException("无法注册 CachedKylinDriver", e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null; // JDBC 规范：对于不支持的 URL 返回 null
        }
        log.debug("正在使用 CachedKylinDriver 连接: {}", url);

        DriverConfig config;
        try {
            config = DriverConfig.parse(url, info);
        } catch (IllegalArgumentException e) {
            throw new SQLException("CachedKylinDriver 的 URL 无效: " + e.getMessage(), e);
        }

        // 附加 Redis 缓存管理器
        RedisCacheManager cacheManager = RedisCacheManager.getInstance(config);

        return new CachedKylinConnection(config, cacheManager);
    }

    @Override
    public boolean acceptsURL(String url) {
        return url != null && url.startsWith(DriverConfig.CACHE_SCHEME);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[]{
            prop("redis.host",     "Redis 服务器主机名",         "localhost", false),
            prop("redis.port",     "Redis 服务器端口",             "6379",      false),
            prop("redis.password", "Redis AUTH 密码",           "",          false),
            prop("redis.db",       "Redis 数据库索引",          "0",         false),
            prop("cache.ttl",      "默认缓存 TTL（秒）",  "300",       false),
            prop("cache.prefix",   "Redis 键前缀",              "kylin_cache:", false),
        };
    }

    @Override public int getMajorVersion() { return MAJOR_VERSION; }
    @Override public int getMinorVersion() { return MINOR_VERSION; }
    @Override public boolean jdbcCompliant() { return false; }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("不使用 java.util.logging");
    }

    // -------------------------------------------------------------------------
    // 工具方法
    // -------------------------------------------------------------------------

    private static DriverPropertyInfo prop(String name, String desc,
                                           String defaultVal, boolean required) {
        DriverPropertyInfo info = new DriverPropertyInfo(name, defaultVal);
        info.description = desc;
        info.required = required;
        return info;
    }
}
