package com.smartbi.engine.support;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public final class ManagerTestFixtures {

    private static final String RESOURCE = "test-fixtures.yml";
    private static final Properties PROPERTIES = load();
    private static final Map<String, String> PREPARED_JDBC_URLS = new ConcurrentHashMap<>();

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

    public static String mysqlJdbcUrl(String databaseNameKey) {
        return PREPARED_JDBC_URLS.computeIfAbsent(databaseNameKey,
                ignored -> prepareDatabase(get(databaseNameKey), null));
    }

    public static String mysqlJdbcUrlWithInit(String databaseNameKey, String resourceKey) {
        String cacheKey = databaseNameKey + "|" + resourceKey;
        return PREPARED_JDBC_URLS.computeIfAbsent(cacheKey,
                ignored -> prepareDatabase(get(databaseNameKey), get(resourceKey)));
    }

    private static String prepareDatabase(String databaseName, String initResource) {
        validateDatabaseName(databaseName);
        recreateDatabase(databaseName);
        if (initResource != null) {
            executeSqlScript(databaseJdbcUrl(databaseName), initResource);
        }
        return databaseJdbcUrl(databaseName);
    }

    private static void recreateDatabase(String databaseName) {
        String appUser = get("manager.test.shared.username");
        String appPassword = get("manager.test.shared.password");
        try (Connection connection = DriverManager.getConnection(
                get("manager.test.admin.jdbc-url"),
                get("manager.test.admin.username"),
                get("manager.test.admin.password"));
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS `" + databaseName + "`");
            statement.execute("CREATE DATABASE `" + databaseName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            statement.execute("CREATE USER IF NOT EXISTS '" + escapeSqlLiteral(appUser) + "'@'%' "
                    + "IDENTIFIED BY '" + escapeSqlLiteral(appPassword) + "'");
            statement.execute("GRANT ALL PRIVILEGES ON `" + databaseName + "`.* TO '"
                    + escapeSqlLiteral(appUser) + "'@'%'");
            statement.execute("FLUSH PRIVILEGES");
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to prepare manager test database: " + databaseName, ex);
        }
    }

    private static void executeSqlScript(String jdbcUrl, String resourcePath) {
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl,
                get("manager.test.shared.username"),
                get("manager.test.shared.password"))) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(resourcePath));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to seed manager test database from resource: " + resourcePath, ex);
        }
    }

    private static String databaseJdbcUrl(String databaseName) {
        return get("manager.test.shared.jdbc-url-prefix")
                + databaseName
                + get("manager.test.shared.jdbc-url-suffix");
    }

    private static void validateDatabaseName(String databaseName) {
        if (!databaseName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalStateException("Unsafe manager test database name: " + databaseName);
        }
    }

    private static String escapeSqlLiteral(String value) {
        return value.replace("'", "''");
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
