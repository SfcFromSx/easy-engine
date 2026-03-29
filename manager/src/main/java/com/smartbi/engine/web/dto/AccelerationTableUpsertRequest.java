package com.smartbi.engine.web.dto;

public class AccelerationTableUpsertRequest {

    private String name;
    private String schemaName;
    private String ddlText;
    private String refreshSql;
    private String cronExpr;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getDdlText() {
        return ddlText;
    }

    public void setDdlText(String ddlText) {
        this.ddlText = ddlText;
    }

    public String getRefreshSql() {
        return refreshSql;
    }

    public void setRefreshSql(String refreshSql) {
        this.refreshSql = refreshSql;
    }

    public String getCronExpr() {
        return cronExpr;
    }

    public void setCronExpr(String cronExpr) {
        this.cronExpr = cronExpr;
    }
}
