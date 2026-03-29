package com.smartbi.query.api.dto;

import java.util.List;
import java.util.Map;

public class PreparedQueryRequestDto {

    private String sql;
    private String project;
    private boolean acceptPartial;
    private List<StatementParameterDto> params;
    private Map<String, String> backdoorToggles;

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public boolean isAcceptPartial() {
        return acceptPartial;
    }

    public void setAcceptPartial(boolean acceptPartial) {
        this.acceptPartial = acceptPartial;
    }

    public List<StatementParameterDto> getParams() {
        return params;
    }

    public void setParams(List<StatementParameterDto> params) {
        this.params = params;
    }

    public Map<String, String> getBackdoorToggles() {
        return backdoorToggles;
    }

    public void setBackdoorToggles(Map<String, String> backdoorToggles) {
        this.backdoorToggles = backdoorToggles;
    }
}
