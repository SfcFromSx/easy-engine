package com.kylin.cache;

import com.kylin.SqlCommentParser;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * {@link SqlCommentParser} 的单元测试。
 * 覆盖所有注释解析场景，包括空格、Tab、大小写、优先级等。
 */
public class SqlCommentParserTest {

    @Test
    public void testNoCache() {
        String sql = "-- no-cache\nSELECT * FROM sales";
        SqlCommentParser.ParsedSql result = SqlCommentParser.parse(sql);
        assertTrue(result.metadata.noCache);
        assertFalse(result.cleanSql.contains("no-cache"));
        assertFalse(result.executionSql.contains("no-cache"));
    }

    @Test
    public void testForceRefreshStrippedAndSetsCacheRefresh() {
        String sql = "-- force-refresh\nSELECT count(*) FROM KYLIN_SALES";
        SqlCommentParser.ParsedSql result = SqlCommentParser.parse(sql);
        assertTrue(result.metadata.cacheRefresh);
        assertTrue(result.cleanSql.trim().toLowerCase().startsWith("select"));
        assertFalse(result.cleanSql.contains("force-refresh"));
    }

    @Test
    public void testCorporateMetadataPreserved() {
        String sql = "-- YH_QUERYID=cc3199ee\nSELECT * FROM sales";
        SqlCommentParser.ParsedSql result = SqlCommentParser.parse(sql);
        assertEquals("cc3199ee", result.metadata.queryId);
        assertFalse(result.cleanSql.contains("YH_QUERYID"));
        assertTrue(result.executionSql.contains("YH_QUERYID"));
    }

    @Test
    public void testEngineHintStripped() {
        String sql = "-- engine=presto_1\nSELECT 1";
        SqlCommentParser.ParsedSql result = SqlCommentParser.parse(sql);
        assertEquals("presto_1", result.metadata.engine);
        assertFalse(result.cleanSql.contains("engine"));
        assertFalse(result.executionSql.contains("engine"));
    }

    @Test
    public void testMixedHintsAndMetadata() {
        String sql = "-- engine=presto\n-- YH_USER=admin\nSELECT * FROM t";
        SqlCommentParser.ParsedSql result = SqlCommentParser.parse(sql);
        assertEquals("presto", result.metadata.engine);
        assertEquals("admin", result.metadata.extraMetadata.get("YH_USER"));
    }

    @Test
    public void testAnnotationAtEnd() {
        String sql = "SELECT 1 -- engine=kylin";
        SqlCommentParser.ParsedSql result = SqlCommentParser.parse(sql);
        assertEquals("kylin", result.metadata.engine);
    }

    @Test
    public void testBlockCommentVariantStyles() {
        String sql = "SELECT /* engine:presto */ 1 /* YH_ID=123 */ FROM t";
        SqlCommentParser.ParsedSql result = SqlCommentParser.parse(sql);
        assertEquals("presto", result.metadata.engine);
        assertEquals("123", result.metadata.extraMetadata.get("YH_ID"));
    }

    // --- 全量覆盖场景: 空格与 Tab ---

    @Test
    public void testExtremeWhitespaceAndTabs() {
        // 测试 key=value 周边的各种空格和制表符
        String sql = "-- \t engine \t = \t presto_1 \t \n" +
                     "-- \t cache-ttl \t : \t 60 \t \nSELECT 1";
        SqlCommentParser.ParsedSql result = SqlCommentParser.parse(sql);
        assertEquals("presto_1", result.metadata.engine);
        assertEquals(Integer.valueOf(60), result.metadata.cacheTtl);
    }

    @Test
    public void testNoSpaceAfterDash() {
        // --紧跟指令的情况
        String sql = "--engine=presto\nSELECT 1";
        SqlCommentParser.ParsedSql result = SqlCommentParser.parse(sql);
        assertEquals("presto", result.metadata.engine);
    }

    // --- 全量覆盖场景: 大小写与冲突 ---

    @Test
    public void testCaseInsensitivity() {
        // 指令 Key 应不区分大小写
        String sql = "-- ENGINE=Presto_Type\n-- No-Cache\nSELECT 1";
        SqlCommentParser.ParsedSql result = SqlCommentParser.parse(sql);
        assertEquals("Presto_Type", result.metadata.engine);
        assertTrue(result.metadata.noCache);
    }

    @Test
    public void testSameKeyLastWin() {
        // 同一 Key 出现多次，后出的应覆盖先出的
        String sql = "-- engine=presto\n-- engine=kylin\nSELECT 1";
        SqlCommentParser.ParsedSql result = SqlCommentParser.parse(sql);
        assertEquals("kylin", result.metadata.engine);
    }

    // --- 全量覆盖场景: 边界值与特例 ---

    @Test
    public void testSpecialCharactersInValue() {
        // Value 中包含冒号、连字符等特殊字符
        String sql = "-- cache-key=user:role-123_abc\nSELECT 1";
        SqlCommentParser.ParsedSql result = SqlCommentParser.parse(sql);
        assertEquals("user:role-123_abc", result.metadata.cacheKey);
    }

    @Test
    public void testUnrecognizedYHMetadataStoredInMap() {
        // 未定义的 YH_ 字段应进入 extraMetadata Map
        String sql = "-- YH_UNKNOWN_FIELD=999\nSELECT 1";
        SqlCommentParser.ParsedSql result = SqlCommentParser.parse(sql);
        assertEquals("999", result.metadata.extraMetadata.get("YH_UNKNOWN_FIELD"));
        assertFalse(result.cleanSql.contains("YH_UNKNOWN_FIELD"));
    }

    @Test
    public void testMultipleHintsOnOneLine() {
        // 一个注释块内有多个指令
        String sql = "/* engine=presto, cache-ttl:120 */ SELECT 1";
        SqlCommentParser.ParsedSql result = SqlCommentParser.parse(sql);
        assertEquals("presto", result.metadata.engine);
        assertEquals(Integer.valueOf(120), result.metadata.cacheTtl);
    }

    @Test
    public void testNonHintCommentPreservedInCleanSql() {
        // 普通注释放回 cleanSql (除非它是我们识别的指令)
        String sql = "-- just a normal comment\nSELECT 1";
        SqlCommentParser.ParsedSql result = SqlCommentParser.parse(sql);
        assertTrue(result.cleanSql.contains("-- just a normal comment"));
    }

    @Test
    public void testCommentInsideStringLiteralIgnored() {
        // SQL 字符串内部包含类似注释的内容，不应被识别为指令
        String sql = "SELECT '-- engine=presto' FROM t";
        SqlCommentParser.ParsedSql result = SqlCommentParser.parse(sql);
        assertNull(result.metadata.engine);
        assertEquals(sql, result.cleanSql);
    }
}
