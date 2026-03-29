package com.smartbi.query.parsing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlCommentParserTest {

    @Test
    void shouldStripDriverHintsButKeepMetadataInExecutionSql() {
        String sql = "-- engine=presto_local\n" +
                "/* YH_QUERYID=abc123 */\n" +
                "-- cache-ttl=60\n" +
                "SELECT * FROM sales";

        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse(sql);

        assertTrue(normalize(parsed.cleanSql).contains("SELECT * FROM sales"));
        assertTrue(parsed.executionSql.contains("YH_QUERYID=abc123"));
        assertFalse(parsed.executionSql.contains("engine=presto_local"));
        assertEquals("presto_local", parsed.metadata.engine);
        assertEquals(Integer.valueOf(60), parsed.metadata.cacheTtl);
        assertEquals("abc123", parsed.metadata.queryId);
    }

    @Test
    void shouldParseNoCacheAndRefreshFlags() {
        String sql = "-- no-cache\n-- force-refresh\nSELECT count(*) FROM sales";

        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse(sql);

        assertTrue(parsed.metadata.noCache);
        assertTrue(parsed.metadata.cacheRefresh);
        assertTrue(normalize(parsed.cleanSql).contains("SELECT count(*) FROM sales"));
    }

    private static String normalize(String sql) {
        return sql == null ? null : sql.replace("\r", "").replace("\n", "").replace("\\n", "").trim();
    }
}
