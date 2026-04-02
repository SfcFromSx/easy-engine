package com.smartbi.benchmark.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

public final class BenchmarkTestFixtures {

    private static final String RESOURCE = "benchmark-test-fixtures.properties";
    private static final Properties PROPERTIES = load();

    private BenchmarkTestFixtures() {
    }

    public static String get(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing benchmark test fixture: " + key);
        }
        return value;
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream inputStream = BenchmarkTestFixtures.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing benchmark test fixture resource: " + RESOURCE);
            }
            properties.load(inputStream);
            return properties;
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load " + RESOURCE, ex);
        }
    }
}
