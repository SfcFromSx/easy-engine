package com.smartbi.query.cache;

import com.smartbi.query.api.dto.StatementParameterDto;
import com.smartbi.query.config.QueryProperties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreparedParameterSupportTest {

    // Covers PreparedParameterSupport#shouldUseCache happy-path for non-parameterized SQL and null params.
    @Test
    void shouldAlwaysUseCacheForNonParameterizedSql() {
        CachePolicy policy = new CachePolicy(new QueryProperties.Cache());

        assertTrue(PreparedParameterSupport.shouldUseCache("SELECT 1", null, true, policy));
        assertTrue(PreparedParameterSupport.shouldUseCache("SELECT 1", Collections.singletonList(param("java.lang.Integer", "1")), false, policy));
    }

    // Covers PreparedParameterSupport#shouldUseCache fallback and unsupported-parameter branches.
    @Test
    void shouldDisableCacheForParameterizedSqlWhenPreparedCachingIsDisabledOrUnsupported() {
        CachePolicy policy = new CachePolicy(new QueryProperties.Cache());

        assertFalse(PreparedParameterSupport.shouldUseCache("SELECT * FROM SALES WHERE ID = ?", null, false, policy));
        assertTrue(PreparedParameterSupport.shouldUseCache("SELECT * FROM SALES WHERE ID = ?", null, true, policy));
        assertTrue(PreparedParameterSupport.shouldUseCache("SELECT * FROM SALES WHERE ID = ?", Collections.emptyList(), true, policy));
        assertTrue(PreparedParameterSupport.shouldUseCache("SELECT * FROM SALES WHERE ID = ?", Arrays.asList((StatementParameterDto) null), true, policy));
        assertFalse(PreparedParameterSupport.shouldUseCache("SELECT * FROM SALES WHERE ID = ?", Collections.singletonList(param("java.util.UUID", "abc")), true, policy));
    }

    // Covers PreparedParameterSupport#fingerprint happy-path for null, empty, null-entry, and null-value parameters.
    @Test
    void shouldBuildFingerprintsForSupportedPreparedParameters() {
        CachePolicy policy = new CachePolicy(new QueryProperties.Cache());

        assertNull(PreparedParameterSupport.fingerprint(Collections.singletonList(param("java.lang.Integer", "1")), "SELECT 1", true, policy));
        assertNull(PreparedParameterSupport.fingerprint(Collections.singletonList(param("java.lang.Integer", "1")), "SELECT * FROM SALES WHERE ID = ?", false, policy));
        assertEquals("", PreparedParameterSupport.fingerprint(null, "SELECT * FROM SALES WHERE ID = ?", true, policy));
        assertEquals("", PreparedParameterSupport.fingerprint(Collections.<StatementParameterDto>emptyList(), "SELECT * FROM SALES WHERE ID = ?", true, policy));
        assertEquals("1=0:<NULL>;", PreparedParameterSupport.fingerprint(Collections.<StatementParameterDto>singletonList(null), "SELECT * FROM SALES WHERE ID = ?", true, policy));
        assertEquals(
                "1=23:java.lang.String:<NULL>;",
                PreparedParameterSupport.fingerprint(Collections.singletonList(param("java.lang.String", null)), "SELECT * FROM SALES WHERE NAME = ?", true, policy)
        );
    }

    // Covers PreparedParameterSupport#fingerprint unsupported-class fallback branch.
    @Test
    void shouldReturnNullFingerprintForUnsupportedPreparedParameterTypes() {
        CachePolicy policy = new CachePolicy(new QueryProperties.Cache());

        assertNull(PreparedParameterSupport.fingerprint(Collections.singletonList(param("java.util.UUID", "abc")), "SELECT * FROM SALES WHERE ID = ?", true, policy));
    }

    private static StatementParameterDto param(String className, String value) {
        StatementParameterDto dto = new StatementParameterDto();
        dto.setClassName(className);
        dto.setValue(value);
        return dto;
    }
}
