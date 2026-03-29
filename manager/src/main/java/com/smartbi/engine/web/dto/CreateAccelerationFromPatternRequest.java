package com.smartbi.engine.web.dto;

public class CreateAccelerationFromPatternRequest {

    private Long patternStatsId;
    private String tableName;
    private String schemaName;

    public Long getPatternStatsId() {
        return patternStatsId;
    }

    public void setPatternStatsId(Long patternStatsId) {
        this.patternStatsId = patternStatsId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }
}
