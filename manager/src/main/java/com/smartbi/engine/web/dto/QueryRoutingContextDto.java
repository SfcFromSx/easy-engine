package com.smartbi.engine.web.dto;

import com.smartbi.engine.datasource.QueryDatasourceConfig;

import java.util.ArrayList;
import java.util.List;

public class QueryRoutingContextDto {
    private List<QueryDatasourceConfig> datasources = new ArrayList<QueryDatasourceConfig>();
    private List<QueryRoutingAccelerationRuleDto> accelerationRules = new ArrayList<QueryRoutingAccelerationRuleDto>();

    public List<QueryDatasourceConfig> getDatasources() {
        return datasources;
    }

    public void setDatasources(List<QueryDatasourceConfig> datasources) {
        this.datasources = datasources;
    }

    public List<QueryRoutingAccelerationRuleDto> getAccelerationRules() {
        return accelerationRules;
    }

    public void setAccelerationRules(List<QueryRoutingAccelerationRuleDto> accelerationRules) {
        this.accelerationRules = accelerationRules;
    }
}
