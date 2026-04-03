package com.smartbi.benchmark.support;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.util.Properties;

public final class BenchmarkTestFixtures {

    private static final String RESOURCE = "application-test.properties";
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
        try {
            return PropertiesLoaderUtils.loadProperties(new ClassPathResource(RESOURCE));
        } catch (Exception ex) {
            throw new IllegalStateException("Missing benchmark test fixture resource: " + RESOURCE, ex);
        }
    }
}
