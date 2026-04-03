package com.smartbi.engine.config;

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

@Configuration
public class FlywayConfig {

    private static final String LEGACY_HISTORY_TABLE = "flyway_schema_history";
    private static final String MANAGER_HISTORY_TABLE = "manager_flyway_schema_history";

    @Bean
    public FlywayConfigurationCustomizer managerFlywayTableCustomizer() {
        return configuration -> configuration.table(MANAGER_HISTORY_TABLE);
    }

    @Bean
    public FlywayMigrationStrategy managerFlywayMigrationStrategy(DataSource dataSource) {
        return flyway -> {
            renameLegacyHistoryTableIfNeeded(dataSource);
            flyway.repair();
            flyway.migrate();
        };
    }

    private void renameLegacyHistoryTableIfNeeded(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            if (!tableExists(connection, LEGACY_HISTORY_TABLE) || tableExists(connection, MANAGER_HISTORY_TABLE)) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE " + LEGACY_HISTORY_TABLE + " RENAME TO " + MANAGER_HISTORY_TABLE);
            }
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to migrate manager Flyway schema history table", ex);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        return matchesTable(metadata, connection.getCatalog(), tableName) || matchesTable(metadata, null, tableName);
    }

    private boolean matchesTable(DatabaseMetaData metadata, String catalog, String tableName) throws SQLException {
        try (ResultSet resultSet = metadata.getTables(catalog, null, null, new String[]{"TABLE"})) {
            while (resultSet.next()) {
                String currentName = resultSet.getString("TABLE_NAME");
                if (currentName != null && currentName.toLowerCase(Locale.ROOT).equals(tableName)) {
                    return true;
                }
            }
            return false;
        }
    }
}
