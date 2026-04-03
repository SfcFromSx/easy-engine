package com.smartbi.query.support;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.util.Properties;

public final class QueryTestFixtures {

    private static final String RESOURCE = "application-test.properties";
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
        try {
            return PropertiesLoaderUtils.loadProperties(new ClassPathResource(RESOURCE));
        } catch (Exception ex) {
            throw new IllegalStateException("Missing query test fixture resource: " + RESOURCE, ex);
        }
    }
}
