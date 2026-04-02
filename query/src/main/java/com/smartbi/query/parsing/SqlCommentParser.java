package com.smartbi.query.parsing;

public class SqlCommentParser {

    public static ParsedSql parse(String sql) {
        return wrap(com.smartbi.analyze.sql.SqlCommentParser.parse(sql));
    }

    public static ParsedSql safeParse(String sql) {
        return wrap(com.smartbi.analyze.sql.SqlCommentParser.safeParse(sql));
    }

    private static ParsedSql wrap(com.smartbi.analyze.sql.ParsedSql parsed) {
        if (parsed == null) {
            return new ParsedSql(null, null, new SqlMetadata.Builder().build());
        }
        return new ParsedSql(parsed.cleanSql, parsed.executionSql, new SqlMetadata(parsed.metadata));
    }

    public static final class ParsedSql extends com.smartbi.analyze.sql.ParsedSql {
        public final String cleanSql;
        public final String executionSql;
        public final SqlMetadata metadata;

        public ParsedSql(String cleanSql, String executionSql, SqlMetadata metadata) {
            super(cleanSql, executionSql, metadata);
            this.cleanSql = cleanSql;
            this.executionSql = executionSql;
            this.metadata = metadata;
        }
    }
}
