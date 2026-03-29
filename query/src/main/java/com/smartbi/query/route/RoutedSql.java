package com.smartbi.query.route;

public final class RoutedSql {
    public final String datasourceName;
    public final String datasourceType;
    public final String originalSql;
    public final String executionSql;

    public RoutedSql(String datasourceName, String datasourceType, String originalSql, String executionSql) {
        this.datasourceName = datasourceName;
        this.datasourceType = datasourceType;
        this.originalSql = originalSql;
        this.executionSql = executionSql;
    }
}
