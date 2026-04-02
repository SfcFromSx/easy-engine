package com.smartbi.analyze.route;

public class RoutingDecision {
    private final String datasourceName;
    private final String datasourceType;
    private final String executionSql;
    private final String cacheTable;
    private final String accelerationRuleName;

    public RoutingDecision(String datasourceName,
                           String datasourceType,
                           String executionSql,
                           String cacheTable,
                           String accelerationRuleName) {
        this.datasourceName = datasourceName;
        this.datasourceType = datasourceType;
        this.executionSql = executionSql;
        this.cacheTable = cacheTable;
        this.accelerationRuleName = accelerationRuleName;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public String getDatasourceType() {
        return datasourceType;
    }

    public String getExecutionSql() {
        return executionSql;
    }

    public String getCacheTable() {
        return cacheTable;
    }

    public String getAccelerationRuleName() {
        return accelerationRuleName;
    }
}
