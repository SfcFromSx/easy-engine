package com.smartbi.engine.trace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FingerprintUtilTest {

    @Test
    // Covers FingerprintUtil#sha256Hex.
    void sha256HexReturnsStableDigestAndNullPassThrough() {
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                FingerprintUtil.sha256Hex("test"));
        assertNull(FingerprintUtil.sha256Hex(null));
    }

    @Test
    // Covers FingerprintUtil#normalizeForFingerprint.
    void normalizeForFingerprintCollapsesWhitespaceAndLowercases() {
        assertEquals("select * from sales", FingerprintUtil.normalizeForFingerprint("  SELECT   *  FROM  Sales "));
        assertEquals("", FingerprintUtil.normalizeForFingerprint(null));
    }
}
