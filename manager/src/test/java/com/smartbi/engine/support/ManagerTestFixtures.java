package com.smartbi.engine.support;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.util.Properties;

public final class ManagerTestFixtures {

    private static final String RESOURCE = "test-fixtures.yml";
    private static final Properties PROPERTIES = load();

    private ManagerTestFixtures() {
    }

    static Properties properties() {
        return PROPERTIES;
    }

    public static String get(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing manager test fixture: " + key);
        }
        return value;
    }

    public static String h2JdbcUrl(String databaseNameKey) {
        return "jdbc:h2:mem:" + get(databaseNameKey) + ";" + get("manager.test.shared.jdbc-options");
    }

    public static String h2JdbcUrlWithInit(String databaseNameKey, String resourceKey) {
        return h2JdbcUrl(databaseNameKey)
                + ";INIT=RUNSCRIPT FROM '" + classpathFile(get(resourceKey)).replace("'", "''") + "'";
    }

    private static String classpathFile(String path) {
        try {
            File file = new ClassPathResource(path).getFile();
            return file.getAbsolutePath();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to resolve manager test resource: " + path, ex);
        }
    }

    private static Properties load() {
        try {
            YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
            factory.setResources(new ClassPathResource(RESOURCE));
            Properties properties = factory.getObject();
            if (properties == null) {
                throw new IllegalStateException("Empty manager test fixture resource: " + RESOURCE);
            }
            return properties;
        } catch (Exception ex) {
            throw new IllegalStateException("Missing manager test fixture resource: " + RESOURCE, ex);
        }
    }
}
