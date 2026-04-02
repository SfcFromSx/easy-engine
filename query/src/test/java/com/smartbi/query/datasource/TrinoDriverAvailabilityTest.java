package com.smartbi.query.datasource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrinoDriverAvailabilityTest {

    // Covers query runtime packaging for Trino-backed datasource configs.
    @Test
    void shouldLoadBundledTrinoJdbcDriver() throws Exception {
        Class<?> driverClass = Class.forName("io.trino.jdbc.TrinoDriver");

        assertEquals("io.trino.jdbc.TrinoDriver", driverClass.getName());
    }
}
