package com.smartbi.engine.jdbc.dto;

/**
 * JDBC 驱动向 Engine 咨询改写时的入参（驱动仍自行执行查询，仅应用返回建议）。
 */
public class SqlRewriteRequest {

    /** 用户或 BI 发出的原始 SQL（可含注释 Hint） */
    private String originalSql;
    /** 驱动已剥离部分 Hint 后的文本；若为空则 Engine 仅用 originalSql */
    private String cleanSql;
    private String datasourceName;
    private String datasourceType;
    /** 可选：与轨迹对齐的指纹，便于 Engine 关联模式统计 */
    private String sqlFingerprint;

    public String getOriginalSql() {
        return originalSql;
    }

    public void setOriginalSql(String originalSql) {
        this.originalSql = originalSql;
    }

    public String getCleanSql() {
        return cleanSql;
    }

    public void setCleanSql(String cleanSql) {
        this.cleanSql = cleanSql;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    public String getDatasourceType() {
        return datasourceType;
    }

    public void setDatasourceType(String datasourceType) {
        this.datasourceType = datasourceType;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
    }
}
