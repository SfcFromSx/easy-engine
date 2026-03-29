package com.smartbi.engine.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "acceleration_table")
public class AccelerationTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(nullable = false)
    private String name;

    @Column(name = "schema_name", nullable = false)
    private String schemaName = "public";

    @Column(name = "ddl_text", nullable = false, columnDefinition = "TEXT")
    private String ddlText;

    @Column(name = "refresh_sql", columnDefinition = "TEXT")
    private String refreshSql;

    @Column(name = "cron_expr")
    private String cronExpr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AccelerationStatus status = AccelerationStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AccelerationSource source = AccelerationSource.MANUAL;

    @Column(name = "recommendation_note", columnDefinition = "TEXT")
    private String recommendationNote;

    @PreUpdate
    public void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getDdlText() {
        return ddlText;
    }

    public void setDdlText(String ddlText) {
        this.ddlText = ddlText;
    }

    public String getRefreshSql() {
        return refreshSql;
    }

    public void setRefreshSql(String refreshSql) {
        this.refreshSql = refreshSql;
    }

    public String getCronExpr() {
        return cronExpr;
    }

    public void setCronExpr(String cronExpr) {
        this.cronExpr = cronExpr;
    }

    public AccelerationStatus getStatus() {
        return status;
    }

    public void setStatus(AccelerationStatus status) {
        this.status = status;
    }

    public AccelerationSource getSource() {
        return source;
    }

    public void setSource(AccelerationSource source) {
        this.source = source;
    }

    public String getRecommendationNote() {
        return recommendationNote;
    }

    public void setRecommendationNote(String recommendationNote) {
        this.recommendationNote = recommendationNote;
    }
}
