package com.kylin.record;

import com.kylin.SqlMetadata;

/**
 * 表示一次 SQL 执行上报的标准载荷（不可变值对象）。
 */
public class SqlExecutionTrace {

    public final String datasourceName;
    public final String datasourceType;
    public final String originalSql;
    public final String cleanSql;
    public final String paramFingerprint;
    public final boolean success;
    public final Boolean cacheHit;
    public final long durationMs;
    public final String errorMessage;
    public final SqlMetadata metadata;

    public SqlExecutionTrace(String datasourceName,
                             String datasourceType,
                             String originalSql,
                             String cleanSql,
                             String paramFingerprint,
                             boolean success,
                             Boolean cacheHit,
                             long durationMs,
                             String errorMessage,
                             SqlMetadata metadata) {
        this.datasourceName = datasourceName;
        this.datasourceType = datasourceType;
        this.originalSql = originalSql;
        this.cleanSql = cleanSql;
        this.paramFingerprint = paramFingerprint;
        this.success = success;
        this.cacheHit = cacheHit;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
        this.metadata = metadata;
    }
}
