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
        assertTrue(parsed.executionSql.contains("YH_QUERYID=abc123"));
        assertEquals("abc123", parsed.metadata.queryId);
    }

    @Test
    void keepsYhTargetEngineAsMetadata() {
        ParsedSql parsed = SqlCommentParser.parse("/* YH_TARGET_ENGINE=presto_local YH_CUSTOM_FLAG=blue */\nSELECT * FROM sales");

        assertEquals("presto_local", parsed.metadata.extraMetadata.get("YH_TARGET_ENGINE"));
        assertEquals("blue", parsed.metadata.extraMetadata.get("YH_CUSTOM_FLAG"));
        assertFalse(parsed.cleanSql.contains("YH_TARGET_ENGINE"));
    }

    @Test
    void removesRoutingMetadataKeyFromComments() {
        String sql = "/* YH_TARGET_ENGINE=presto_local YH_QUERYID=abc123 */\nSELECT * FROM sales";

        String updated = SqlCommentParser.removeMetadataKey(sql, "YH_TARGET_ENGINE");

        assertFalse(updated.contains("YH_TARGET_ENGINE"));
        assertTrue(updated.contains("YH_QUERYID=abc123"));
    }
}
