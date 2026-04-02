package com.smartbi.benchmark.web.dto;

import java.time.Instant;

public class TestSetItemListVo {

    private final Long id;
    private final int sortOrder;
    private final Long sqlLibId;
    private final String name;
    private final String sqlText;
    private final int weight;
    private final String executionMode;
    private final String paramJson;
    private final String sourceFilename;
    private final Instant uploadedAt;

    public TestSetItemListVo(Long id,
                             int sortOrder,
                             Long sqlLibId,
                             String name,
                             String sqlText,
                             int weight,
                             String executionMode,
                             String paramJson,
                             String sourceFilename,
                             Instant uploadedAt) {
        this.id = id;
        this.sortOrder = sortOrder;
        this.sqlLibId = sqlLibId;
        this.name = name;
        this.sqlText = sqlText;
        this.weight = weight;
        this.executionMode = executionMode;
        this.paramJson = paramJson;
        this.sourceFilename = sourceFilename;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() {
        return id;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Long getSqlLibId() {
        return sqlLibId;
    }

    public String getName() {
        return name;
    }

    public String getSqlText() {
        return sqlText;
    }

    public int getWeight() {
        return weight;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public String getParamJson() {
        return paramJson;
    }

    public String getSourceFilename() {
        return sourceFilename;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
