package com.smartbi.query.cache;

import com.smartbi.query.config.QueryProperties;
import com.smartbi.query.parsing.SqlCommentParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachePolicyTest {

    // Covers CachePolicy#isQuerySql happy-path detection for supported read-only statements.
    @Test
    void shouldRecognizeSupportedReadOnlyStatements() {
        CachePolicy policy = new CachePolicy(new QueryProperties.Cache());

        assertTrue(policy.isQuerySql("SELECT 1"));
        assertTrue(policy.isQuerySql(" with x as (select 1) select * from x"));
        assertTrue(policy.isQuerySql("/*+ INDEX(s idx_sales) */ SELECT 1"));
        assertTrue(policy.isQuerySql("-- optimizer routing note\nSELECT 1"));
        assertTrue(policy.isQuerySql("/* comment */\nWITH x AS (SELECT 1) SELECT * FROM x"));
        assertTrue(policy.isQuerySql("SHOW TABLES"));
        assertTrue(policy.isQuerySql("DESCRIBE SALES"));
        assertTrue(policy.isQuerySql("EXPLAIN SELECT 1"));
    }

    // Covers CachePolicy#isQuerySql default-path rejection for null and write statements.
    @Test
    void shouldRejectNullAndWriteStatementsAsNonQuerySql() {
        CachePolicy policy = new CachePolicy(new QueryProperties.Cache());

        assertFalse(policy.isQuerySql(null));
        assertFalse(policy.isQuerySql("DELETE FROM SALES"));
        assertFalse(policy.isQuerySql("/*+ INDEX(s idx_sales) */ DELETE FROM SALES"));
        assertFalse(policy.isQuerySql("INSERT INTO SALES VALUES (1)"));
    }

    // Covers CachePolicy#shouldBypassCacheBeforeLookup happy-path and fallback branches for safe mode.
    @Test
    void shouldBypassCacheOnlyForSafeModeVolatileQueries() {
        QueryProperties.Cache cache = new QueryProperties.Cache();
        CachePolicy disabledPolicy = new CachePolicy(cache);
        SqlCommentParser.ParsedSql volatileSql = SqlCommentParser.parse("SELECT current_timestamp");

        assertFalse(disabledPolicy.shouldBypassCacheBeforeLookup(volatileSql));
        assertFalse(disabledPolicy.shouldBypassCacheBeforeLookup(null));

        cache.setSafeModeEnabled(true);
        CachePolicy enabledPolicy = new CachePolicy(cache);

        assertTrue(enabledPolicy.shouldBypassCacheBeforeLookup(volatileSql));
        assertFalse(enabledPolicy.shouldBypassCacheBeforeLookup(SqlCommentParser.parse("SELECT NAME FROM SALES")));
    }

    // Covers CachePolicy#isFingerprintableParameterType happy-path and unsupported-class branches.
    @Test
    void shouldRecognizeFingerprintableParameterTypes() {
        CachePolicy policy = new CachePolicy(new QueryProperties.Cache());

        assertTrue(policy.isFingerprintableParameterType(null));
        assertTrue(policy.isFingerprintableParameterType("java.lang.Integer"));
        assertTrue(policy.isFingerprintableParameterType("java.sql.Timestamp"));
        assertFalse(policy.isFingerprintableParameterType("java.util.Date"));
        assertFalse(policy.isFingerprintableParameterType("java.util.UUID"));
    }

    // Covers CachePolicy#cacheModeKeyTag configuration rendering.
    @Test
    void shouldRenderCacheModeKeyTagFromSafeModeSetting() {
        QueryProperties.Cache cache = new QueryProperties.Cache();
        CachePolicy policy = new CachePolicy(cache);

        assertEquals("safeMode=false", policy.cacheModeKeyTag());

        cache.setSafeModeEnabled(true);
        assertEquals("safeMode=true", new CachePolicy(cache).cacheModeKeyTag());
    }
}
