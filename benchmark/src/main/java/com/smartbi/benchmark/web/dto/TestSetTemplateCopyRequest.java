package com.smartbi.benchmark.web.dto;

import java.util.List;

public class TestSetTemplateCopyRequest {

    private List<Long> sqlLibIds;
    private List<Long> templateIds;

    public List<Long> getSqlLibIds() {
        return sqlLibIds;
    }

    public void setSqlLibIds(List<Long> sqlLibIds) {
        this.sqlLibIds = sqlLibIds;
    }

    public List<Long> getTemplateIds() {
        return templateIds;
    }

    public void setTemplateIds(List<Long> templateIds) {
        this.templateIds = templateIds;
    }

    public List<Long> resolveSqlLibIds() {
        return sqlLibIds != null ? sqlLibIds : templateIds;
    }
}
