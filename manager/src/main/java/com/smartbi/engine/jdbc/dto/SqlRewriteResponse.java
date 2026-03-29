package com.smartbi.engine.jdbc.dto;

/**
 * Engine 对 JDBC 的改写建议。驱动负责将 {@link #hintCommentBlock} 与 {@link #executionSql} 合并后再走缓存键与路由逻辑。
 */
public class SqlRewriteResponse {

    /** 建议发往底层引擎执行的 SQL（未改则与请求中的有效 SQL 相同） */
    private String executionSql;
    /**
     * 建议置于语句前的注释块（多行 {@code -- }），例如 engine / cache 类 Hint；
     * 空表示无额外注释。
     */
    private String hintCommentBlock;
    private boolean modified;
    /** 给人读或日志用的说明，不参与执行 */
    private String advisoryMessage;

    public String getExecutionSql() {
        return executionSql;
    }

    public void setExecutionSql(String executionSql) {
        this.executionSql = executionSql;
    }

    public String getHintCommentBlock() {
        return hintCommentBlock;
    }

    public void setHintCommentBlock(String hintCommentBlock) {
        this.hintCommentBlock = hintCommentBlock;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public String getAdvisoryMessage() {
        return advisoryMessage;
    }

    public void setAdvisoryMessage(String advisoryMessage) {
        this.advisoryMessage = advisoryMessage;
    }
}
