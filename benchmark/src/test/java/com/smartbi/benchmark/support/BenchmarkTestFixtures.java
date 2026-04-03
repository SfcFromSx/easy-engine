package com.smartbi.benchmark.support;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

public final class BenchmarkTestFixtures {

    private static final String RESOURCE = "application-test.yml";
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
            YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
            factory.setResources(new ClassPathResource(RESOURCE));
            Properties properties = factory.getObject();
            if (properties == null) {
                throw new IllegalStateException("No benchmark test fixtures resolved from " + RESOURCE);
            }
            return properties;
        } catch (Exception ex) {
            throw new IllegalStateException("Missing benchmark test fixture resource: " + RESOURCE, ex);
        }
    }
}
