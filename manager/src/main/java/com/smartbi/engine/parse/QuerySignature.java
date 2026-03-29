package com.smartbi.engine.parse;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured summary extracted from SQL via Calcite (serializable to JSON).
 */
public class QuerySignature {

    private List<String> tables = new ArrayList<>();
    private List<String> groupBy = new ArrayList<>();
    private List<String> selectItems = new ArrayList<>();
    private List<String> aggregates = new ArrayList<>();
    private String rootKind;

    public List<String> getTables() {
        return tables;
    }

    public void setTables(List<String> tables) {
        this.tables = tables;
    }

    public List<String> getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(List<String> groupBy) {
        this.groupBy = groupBy;
    }

    public List<String> getSelectItems() {
        return selectItems;
    }

    public void setSelectItems(List<String> selectItems) {
        this.selectItems = selectItems;
    }

    public List<String> getAggregates() {
        return aggregates;
    }

    public void setAggregates(List<String> aggregates) {
        this.aggregates = aggregates;
    }

    public String getRootKind() {
        return rootKind;
    }

    public void setRootKind(String rootKind) {
        this.rootKind = rootKind;
    }
}
