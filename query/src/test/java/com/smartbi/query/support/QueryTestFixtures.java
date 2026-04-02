package com.smartbi.query.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

public final class QueryTestFixtures {

    private static final String RESOURCE = "query-test-fixtures.properties";
    private static final Properties PROPERTIES = load();

    private QueryTestFixtures() {
    }

    public static String get(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing query test fixture: " + key);
        }
        return value;
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream inputStream = QueryTestFixtures.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing query test fixture resource: " + RESOURCE);
            }
            properties.load(inputStream);
            return properties;
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load " + RESOURCE, ex);
        }
    }
}
