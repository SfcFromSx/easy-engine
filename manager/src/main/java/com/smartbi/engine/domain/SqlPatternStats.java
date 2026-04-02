package com.smartbi.engine.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "manager_sql_pattern_stats")
public class SqlPatternStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sql_fingerprint", nullable = false, unique = true, length = 64)
    private String sqlFingerprint;

    @Column(name = "clean_sql_sample", columnDefinition = "TEXT")
    private String cleanSqlSample;

    @Column(name = "execution_count", nullable = false)
    private long executionCount = 0;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    @Column(name = "avg_duration_ms")
    private Double avgDurationMs;

    @Column(name = "signature_json", columnDefinition = "TEXT")
    private String signatureJson;

    public Long getId() {
        return id;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
    }

    public String getCleanSqlSample() {
        return cleanSqlSample;
    }

    public void setCleanSqlSample(String cleanSqlSample) {
        this.cleanSqlSample = cleanSqlSample;
    }

    public long getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(long executionCount) {
        this.executionCount = executionCount;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Double getAvgDurationMs() {
        return avgDurationMs;
    }

    public void setAvgDurationMs(Double avgDurationMs) {
        this.avgDurationMs = avgDurationMs;
    }

    public String getSignatureJson() {
        return signatureJson;
    }

    public void setSignatureJson(String signatureJson) {
        this.signatureJson = signatureJson;
    }
}
