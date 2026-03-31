package com.smartbi.query.parsing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlCommentParserTest {

    // Covers SqlCommentParser#parse driver-hint stripping and metadata preservation branches.
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

    // Covers SqlCommentParser#parse flag parsing for no-cache and refresh hints.
    @Test
    void shouldParseNoCacheAndRefreshFlags() {
        String sql = "-- no-cache\n-- force-refresh\nSELECT count(*) FROM sales";

        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse(sql);

        assertTrue(parsed.metadata.noCache);
        assertTrue(parsed.metadata.cacheRefresh);
        assertTrue(normalize(parsed.cleanSql).contains("SELECT count(*) FROM sales"));
    }

    // Covers SqlCommentParser#parse null and empty fallback branches.
    @Test
    void shouldReturnEmptyMetadataForNullAndEmptySql() {
        SqlCommentParser.ParsedSql nullParsed = SqlCommentParser.parse(null);
        SqlCommentParser.ParsedSql emptyParsed = SqlCommentParser.parse("");

        assertEquals(null, nullParsed.cleanSql);
        assertEquals(null, nullParsed.executionSql);
        assertEquals("", emptyParsed.cleanSql);
        assertEquals("", emptyParsed.executionSql);
        assertTrue(emptyParsed.metadata.extraMetadata.isEmpty());
    }

    // Covers SqlCommentParser#parse quoted-string immunity for comment-looking literals.
    @Test
    void shouldIgnoreCommentSyntaxInsideQuotedStrings() {
        String sql = "SELECT '/* not a hint */' AS literal_value, '-- still not a hint' AS marker";

        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse(sql);

        assertEquals(sql, parsed.cleanSql);
        assertEquals(sql, parsed.executionSql);
        assertTrue(parsed.metadata.extraMetadata.isEmpty());
    }

    // Covers SqlCommentParser#parse metadata-only comment preservation and unknown YH_* retention.
    @Test
    void shouldPreserveMetadataOnlyCommentsAndUnknownYhMetadata() {
        String sql = "/* YH_TARGET_ENGINE=presto_local YH_CUSTOM_FLAG=blue */\nSELECT * FROM sales";

        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse(sql);

        assertTrue(parsed.executionSql.contains("YH_TARGET_ENGINE=presto_local"));
        assertTrue(parsed.executionSql.contains("YH_CUSTOM_FLAG=blue"));
        assertFalse(parsed.cleanSql.contains("YH_TARGET_ENGINE"));
        assertEquals("presto_local", parsed.metadata.extraMetadata.get("YH_TARGET_ENGINE"));
        assertEquals("blue", parsed.metadata.extraMetadata.get("YH_CUSTOM_FLAG"));
    }

    // Covers SqlCommentParser#safeParse error-path fallback when parse throws.
    @Test
    void shouldFallbackToOriginalSqlWhenSafeParseEncountersInvalidHintValues() {
        String sql = "-- cache-ttl=NaN\nSELECT * FROM sales";

        SqlCommentParser.ParsedSql parsed = SqlCommentParser.safeParse(sql);

        assertEquals(sql, parsed.cleanSql);
        assertEquals(sql, parsed.executionSql);
        assertTrue(parsed.metadata.extraMetadata.isEmpty());
        assertEquals(null, parsed.metadata.cacheTtl);
    }

    private static String normalize(String sql) {
        return sql == null ? null : sql.replace("\r", "").replace("\n", "").replace("\\n", "").trim();
    }
}
