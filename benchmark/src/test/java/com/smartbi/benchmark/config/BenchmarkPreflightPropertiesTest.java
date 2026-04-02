package com.smartbi.benchmark.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class BenchmarkPreflightPropertiesTest {

    @Test
    void shouldNotEmbedProbeDefaultsInJavaCode() {
        BenchmarkPreflightProperties props = new BenchmarkPreflightProperties();

        assertNull(props.getKylinAuthUrl());
        assertNull(props.getKylinUser());
        assertNull(props.getKylinPassword());
        assertNull(props.getPrestoInfoUrl());
    }
}
