package com.smartbi.benchmark.support;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

public final class BenchmarkTestFixtures {

    private static final String RESOURCE = "test-fixtures.yml";
    private static final Properties PROPERTIES = load();

    private BenchmarkTestFixtures() {
    }

    static Properties properties() {
        return PROPERTIES;
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
            YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
            factory.setResources(new ClassPathResource(RESOURCE));
            Properties properties = factory.getObject();
            if (properties == null) {
                throw new IllegalStateException("Empty benchmark test fixture resource: " + RESOURCE);
            }
            return properties;
        } catch (Exception ex) {
            throw new IllegalStateException("Missing benchmark test fixture resource: " + RESOURCE, ex);
        }
    }
}
