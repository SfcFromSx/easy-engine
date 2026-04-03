package com.smartbi.analyze.sql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlCommentParserTest {

    @Test
    void stripsEngineHintsButKeepsMetadata() {
        ParsedSql parsed = SqlCommentParser.parse("-- engine=presto_local\n/* YH_QUERYID=abc123 */\nSELECT * FROM sales");

        assertFalse(parsed.executionSql.contains("engine=presto_local"));
        assertEquals("presto_local", parsed.metadata.engine);
        assertTrue(parsed.executionSql.contains("YH_QUERYID=abc123"));
        assertEquals("abc123", parsed.metadata.queryId);
    }

    @Test
    void keepsRptSearchModeAndLegacyYhTargetEngineAsMetadata() {
        ParsedSql parsed = SqlCommentParser.parse(
                "/* YH_RPTSEARCHMODE=dashboard YH_TARGET_ENGINE=presto_local YH_CUSTOM_FLAG=blue */\nSELECT * FROM sales");

        assertEquals("dashboard", parsed.metadata.rptSearchMode);
        assertEquals("presto_local", parsed.metadata.extraMetadata.get("YH_TARGET_ENGINE"));
        assertEquals("blue", parsed.metadata.extraMetadata.get("YH_CUSTOM_FLAG"));
        assertFalse(parsed.cleanSql.contains("YH_RPTSEARCHMODE"));
    }

    @Test
    void stripsAllCommentsFromCleanSqlButPreservesUnrecognizedCommentsInExecutionSql() {
        ParsedSql parsed = SqlCommentParser.parse(
                "/* ordinary comment */\n/* YH_QUERYID=abc123 */\nSELECT * FROM sales -- trailing note");

        assertEquals("SELECT * FROM sales", parsed.cleanSql);
        assertTrue(parsed.executionSql.contains("ordinary comment"));
        assertTrue(parsed.executionSql.contains("YH_QUERYID=abc123"));
        assertTrue(parsed.executionSql.contains("trailing note"));
        assertEquals("abc123", parsed.metadata.queryId);
    }

    @Test
    void removesRoutingMetadataKeyFromComments() {
        String sql = "/* ENGINE=presto_local YH_QUERYID=abc123 */\nSELECT * FROM sales";

        String updated = SqlCommentParser.removeMetadataKey(sql, "ENGINE");

        assertFalse(updated.contains("ENGINE=presto_local"));
        assertTrue(updated.contains("YH_QUERYID=abc123"));
    }
}
