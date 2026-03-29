package com.kylin.cache;

import com.kylin.DriverConfig;
import com.kylin.SqlCommentParser;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * 集中管理缓存准入与禁缓存规则。
 */
public class CachePolicy {

    private static final Pattern VOLATILE_SQL_PATTERN = Pattern.compile(
            "\\b(current_date|current_time|current_timestamp|now\\s*\\(|rand\\s*\\(|random\\s*\\(|uuid\\s*\\()",
            Pattern.CASE_INSENSITIVE);

    private final DriverConfig config;

    public CachePolicy(DriverConfig config) {
        this.config = config;
    }

    public boolean isQuerySql(String sql) {
        if (sql == null) {
            return false;
        }
        String normalized = sql.trim().toLowerCase();
        return normalized.startsWith("select")
                || normalized.startsWith("with")
                || normalized.startsWith("show")
                || normalized.startsWith("describe")
                || normalized.startsWith("explain");
    }

    public boolean shouldBypassCacheBeforeLookup(SqlCommentParser.ParsedSql parsed) {
        return config.isConservativeCacheModeEnabled()
                && parsed != null
                && parsed.cleanSql != null
                && VOLATILE_SQL_PATTERN.matcher(parsed.cleanSql).find();
    }

    public boolean shouldCacheResultSet(ResultSet rs) {
        if (!config.isConservativeCacheModeEnabled()) {
            return true;
        }
        try {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                if (!isSupportedColumnType(meta.getColumnType(i))) {
                    return false;
                }
            }
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public String cacheModeKeyTag() {
        return "safeMode=" + config.isConservativeCacheModeEnabled();
    }

    /**
     * 判断通过 {@code setObject(...)} 绑定的参数是否适合参与缓存键构造。
     *
     * <p>这里采用保守白名单，只接受语义和字符串表示都足够稳定的标量类型。
     */
    public boolean isFingerprintableParameterValue(Object value) {
        return value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Date
                || value instanceof byte[];
    }

    private static boolean isSupportedColumnType(int sqlType) {
        switch (sqlType) {
            case Types.BOOLEAN:
            case Types.BIT:
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
            case Types.NUMERIC:
            case Types.DECIMAL:
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
                return true;
            default:
                return false;
        }
    }
}
