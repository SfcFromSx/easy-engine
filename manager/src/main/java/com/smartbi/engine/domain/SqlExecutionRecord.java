package com.smartbi.engine.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "manager_sql_execution_record")
public class SqlExecutionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    @Column(name = "raw_payload", columnDefinition = "MEDIUMTEXT")
    private String rawPayload;

    @Column(name = "datasource_name")
    private String datasourceName;

    @Column(name = "datasource_type")
    private String datasourceType;

    @Column(name = "original_sql", columnDefinition = "MEDIUMTEXT")
    private String originalSql;

    @Column(name = "clean_sql", columnDefinition = "MEDIUMTEXT")
    private String cleanSql;

    @Column(name = "param_fingerprint")
    private String paramFingerprint;

    @Column(name = "parameter_payload", columnDefinition = "MEDIUMTEXT")
    private String parameterPayload;

    @Column(name = "execution_mode", length = 32)
    private String executionMode;

    private Boolean success;

    @Column(name = "cache_hit")
    private Boolean cacheHit;

    @Column(name = "cache_key", length = 1024)
    private String cacheKey;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", columnDefinition = "MEDIUMTEXT")
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "parse_status", nullable = false, length = 32)
    private ParseStatus parseStatus;

    @Column(name = "parse_error", columnDefinition = "MEDIUMTEXT")
    private String parseError;

    @Column(name = "signature_json", columnDefinition = "MEDIUMTEXT")
    private String signatureJson;

    @Column(name = "sql_fingerprint")
    private String sqlFingerprint;

    public Long getId() {
        return id;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    public String getDatasourceType() {
        return datasourceType;
    }

    public void setDatasourceType(String datasourceType) {
        this.datasourceType = datasourceType;
    }

    public String getOriginalSql() {
        return originalSql;
    }

    public void setOriginalSql(String originalSql) {
        this.originalSql = originalSql;
    }

    public String getCleanSql() {
        return cleanSql;
    }

    public void setCleanSql(String cleanSql) {
        this.cleanSql = cleanSql;
    }

    public String getParamFingerprint() {
        return paramFingerprint;
    }

    public void setParamFingerprint(String paramFingerprint) {
        this.paramFingerprint = paramFingerprint;
    }

    public String getParameterPayload() {
        return parameterPayload;
    }

    public void setParameterPayload(String parameterPayload) {
        this.parameterPayload = parameterPayload;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Boolean getCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(Boolean cacheHit) {
        this.cacheHit = cacheHit;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ParseStatus getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(ParseStatus parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getParseError() {
        return parseError;
    }

    public void setParseError(String parseError) {
        this.parseError = parseError;
    }

    public String getSignatureJson() {
        return signatureJson;
    }

    public void setSignatureJson(String signatureJson) {
        this.signatureJson = signatureJson;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
    }
}
