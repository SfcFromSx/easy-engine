package com.smartbi.query.cache;

import com.smartbi.analyze.sql.ParsedSql;
import com.smartbi.query.config.QueryProperties;

import java.util.regex.Pattern;

public class CachePolicy {

    private static final Pattern VOLATILE_SQL_PATTERN = Pattern.compile(
            "\\b(current_date|current_time|current_timestamp|now\\s*\\(|rand\\s*\\(|random\\s*\\(|uuid\\s*\\()",
            Pattern.CASE_INSENSITIVE);

    private final QueryProperties.Cache cacheProperties;

    public CachePolicy(QueryProperties.Cache cacheProperties) {
        this.cacheProperties = cacheProperties;
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

    public boolean shouldBypassCacheBeforeLookup(ParsedSql parsed) {
        return cacheProperties.isSafeModeEnabled()
                && parsed != null
                && parsed.cleanSql != null
                && VOLATILE_SQL_PATTERN.matcher(parsed.cleanSql).find();
    }

    public boolean isFingerprintableParameterType(String className) {
        return className == null
                || "java.lang.String".equals(className)
                || "java.lang.Integer".equals(className)
                || "java.lang.Long".equals(className)
                || "java.lang.Short".equals(className)
                || "java.lang.Double".equals(className)
                || "java.lang.Float".equals(className)
                || "java.math.BigDecimal".equals(className)
                || "java.lang.Boolean".equals(className)
                || "java.sql.Date".equals(className)
                || "java.sql.Time".equals(className)
                || "java.sql.Timestamp".equals(className);
    }

    public String cacheModeKeyTag() {
        return "safeMode=" + cacheProperties.isSafeModeEnabled();
    }
}
