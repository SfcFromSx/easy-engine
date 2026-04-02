package com.smartbi.analyze.sql;

public class ParsedSql {
    public final String cleanSql;
    public final String executionSql;
    public final SqlMetadata metadata;

    public ParsedSql(String cleanSql, String executionSql, SqlMetadata metadata) {
        this.cleanSql = cleanSql;
        this.executionSql = executionSql;
        this.metadata = metadata;
    }
}
