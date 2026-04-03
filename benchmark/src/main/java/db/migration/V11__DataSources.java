package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;

public class V11__DataSources extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        MigrationSupport.execute(connection, "CREATE TABLE IF NOT EXISTS benchmark_data_source (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL, " +
                "jdbc_url MEDIUMTEXT NOT NULL, " +
                "jdbc_user VARCHAR(255), " +
                "jdbc_password VARCHAR(255), " +
                "driver_class VARCHAR(255) NOT NULL, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                ")");

        if (!MigrationSupport.columnExists(connection, "benchmark_job", "data_source_id")) {
            MigrationSupport.execute(connection,
                    "ALTER TABLE benchmark_job ADD COLUMN data_source_id BIGINT");
        }

        if (!hasLegacyJdbcColumns(connection)) {
            return;
        }

        MigrationSupport.execute(connection,
                "INSERT INTO benchmark_data_source (name, jdbc_url, jdbc_user, jdbc_password, driver_class) " +
                        "SELECT CONCAT('DataSource for ', name), jdbc_url, jdbc_user, jdbc_password, driver_class " +
                        "FROM benchmark_job " +
                        "WHERE jdbc_url IS NOT NULL AND driver_class IS NOT NULL");

        MigrationSupport.execute(connection,
                "UPDATE benchmark_job " +
                        "SET data_source_id = (" +
                        "    SELECT ds.id " +
                        "    FROM benchmark_data_source ds " +
                        "    WHERE ds.jdbc_url = benchmark_job.jdbc_url " +
                        "      AND ds.name = CONCAT('DataSource for ', benchmark_job.name) " +
                        "    LIMIT 1" +
                        ") " +
                        "WHERE data_source_id IS NULL");

        dropColumnIfExists(connection, "jdbc_url");
        dropColumnIfExists(connection, "jdbc_user");
        dropColumnIfExists(connection, "jdbc_password");
        dropColumnIfExists(connection, "driver_class");
    }

    private boolean hasLegacyJdbcColumns(Connection connection) throws Exception {
        return MigrationSupport.columnExists(connection, "benchmark_job", "jdbc_url")
                && MigrationSupport.columnExists(connection, "benchmark_job", "jdbc_user")
                && MigrationSupport.columnExists(connection, "benchmark_job", "jdbc_password")
                && MigrationSupport.columnExists(connection, "benchmark_job", "driver_class");
    }

    private void dropColumnIfExists(Connection connection, String columnName) throws Exception {
        if (!MigrationSupport.columnExists(connection, "benchmark_job", columnName)) {
            return;
        }
        MigrationSupport.execute(connection,
                "ALTER TABLE benchmark_job DROP COLUMN " + columnName);
    }
}
