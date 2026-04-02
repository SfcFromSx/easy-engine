package com.smartbi.engine.web.dto;

public class QueryRoutingAccelerationRuleDto {
    private String name;
    private String schemaName;
    private String tableName;
    private String refreshSql;

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

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getRefreshSql() {
        return refreshSql;
    }

    public void setRefreshSql(String refreshSql) {
        this.refreshSql = refreshSql;
    }
}
