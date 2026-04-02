package com.smartbi.analyze.route;

public class AccelerationRule {
    private final String name;
    private final String schemaName;
    private final String tableName;
    private final String refreshSql;

    public AccelerationRule(String name, String schemaName, String tableName, String refreshSql) {
        this.name = name;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.refreshSql = refreshSql;
    }

    public String getName() {
        return name;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getRefreshSql() {
        return refreshSql;
    }

    public String getQualifiedTableName() {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            return tableName;
        }
        return schemaName + "." + tableName;
    }
}
