package com.smartbi.query.trace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FingerprintUtilTest {

    // Covers FingerprintUtil#sha256Hex null-handling and deterministic hashing branches.
    @Test
    void shouldHashStringsDeterministically() {
        assertNull(FingerprintUtil.sha256Hex(null));
        assertEquals(FingerprintUtil.sha256Hex("select 1"), FingerprintUtil.sha256Hex("select 1"));
        assertNotEquals(FingerprintUtil.sha256Hex("select 1"), FingerprintUtil.sha256Hex("select 2"));
    }

    // Covers FingerprintUtil#normalizeForFingerprint null and whitespace-normalization branches.
    @Test
    void shouldNormalizeSqlForFingerprinting() {
        assertEquals("", FingerprintUtil.normalizeForFingerprint(null));
        assertEquals("select * from sales", FingerprintUtil.normalizeForFingerprint(" SELECT   *\nFROM   SALES "));
    }
}
