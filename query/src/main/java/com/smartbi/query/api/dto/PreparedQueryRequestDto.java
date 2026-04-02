package com.smartbi.query.api.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PreparedQueryRequestDto {

    private String sql;
    private String project;
    private boolean acceptPartial;
    private List<StatementParameterDto> params;
    private Map<String, String> backdoorToggles;
    private final Map<String, Object> unsupportedProperties = new LinkedHashMap<String, Object>();

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

    @JsonAnySetter
    public void captureUnsupportedProperty(String name, Object value) {
        unsupportedProperties.put(name, value);
    }

    public boolean hasUnsupportedProperties() {
        return !unsupportedProperties.isEmpty();
    }

    public List<String> getUnsupportedPropertyNames() {
        return Collections.unmodifiableList(new ArrayList<String>(unsupportedProperties.keySet()));
    }
}
