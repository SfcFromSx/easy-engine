package com.smartbi.query.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.smartbi.query.parsing.SqlMetadata;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class TracePayload {

    public final String datasourceName;
    public final String datasourceType;
    public final String originalSql;
    public final String cleanSql;
    public final String paramFingerprint;
    public final String parameterPayload;
    public final String executionMode;
    public final boolean success;
    public final Boolean cacheHit;
    public final long durationMs;
    public final String errorMessage;
    public final SqlMetadata metadata;

    public TracePayload(String datasourceName,
                        String datasourceType,
                        String originalSql,
                        String cleanSql,
                        String paramFingerprint,
                        String parameterPayload,
                        String executionMode,
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
        this.parameterPayload = parameterPayload;
        this.executionMode = executionMode;
        this.success = success;
        this.cacheHit = cacheHit;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
        this.metadata = metadata;
    }
}
