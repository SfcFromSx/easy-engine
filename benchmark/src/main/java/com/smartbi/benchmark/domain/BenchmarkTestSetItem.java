package com.smartbi.benchmark.domain;

import javax.persistence.*;

@Entity
@Table(name = "benchmark_test_set_item")
public class BenchmarkTestSetItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_set_id", nullable = false)
    private Long testSetId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(length = 512)
    private String label;

    @Column(name = "sql_text", nullable = false, columnDefinition = "TEXT")
    private String sqlText;

    @Column(nullable = false)
    private int weight = 1;

    @Column(name = "execution_mode", nullable = false, length = 32)
    private String executionMode = "STATEMENT";

    @Column(name = "param_json", columnDefinition = "TEXT")
    private String paramJson;

    public Long getId() {
        return id;
    }

    public Long getTestSetId() {
        return testSetId;
    }

    public void setTestSetId(Long testSetId) {
        this.testSetId = testSetId;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }

    public String getParamJson() {
        return paramJson;
    }

    public void setParamJson(String paramJson) {
        this.paramJson = paramJson;
    }
}
