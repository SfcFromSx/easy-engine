package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

final class MigrationSupport {

    private MigrationSupport() {
    }

    static String firstExistingTable(Connection connection, String... tableNames) throws SQLException {
        for (String tableName : tableNames) {
            if (tableExists(connection, tableName)) {
                return tableName;
            }
        }
        return null;
    }

    static boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        return matchesTable(metadata, connection.getCatalog(), tableName);
    }

    static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        return matchesColumn(metadata, connection.getCatalog(), tableName, columnName);
    }

    static boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        return matchesIndex(metadata, connection.getCatalog(), tableName, indexName);
    }

    static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    static void recreateSqlExecutionRecordTable(Connection connection, String tableName) throws SQLException {
        execute(connection, "DROP TABLE IF EXISTS " + tableName);
        execute(connection,
                "CREATE TABLE " + tableName + " (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "raw_payload MEDIUMTEXT, " +
                        "datasource_name VARCHAR(256), " +
                        "datasource_type VARCHAR(64), " +
                        "original_sql MEDIUMTEXT, " +
                        "clean_sql MEDIUMTEXT, " +
                        "param_fingerprint VARCHAR(512), " +
                        "parameter_payload MEDIUMTEXT, " +
                        "execution_mode VARCHAR(32), " +
                        "success BOOLEAN, " +
                        "cache_hit BOOLEAN, " +
                        "cache_key VARCHAR(1024), " +
                        "duration_ms BIGINT, " +
                        "error_message MEDIUMTEXT, " +
                        "parse_status VARCHAR(32) NOT NULL, " +
                        "parse_error MEDIUMTEXT, " +
                        "signature_json MEDIUMTEXT, " +
                        "sql_fingerprint VARCHAR(64)" +
                        ")");
        execute(connection, "CREATE INDEX idx_sql_exec_received ON " + tableName + " (received_at)");
        execute(connection, "CREATE INDEX idx_sql_exec_fingerprint ON " + tableName + " (sql_fingerprint)");
        execute(connection, "CREATE INDEX idx_sql_exec_ds ON " + tableName + " (datasource_name)");
        execute(connection,
                "CREATE INDEX idx_sql_exec_cache_hit_key ON "
                        + tableName
                        + " (cache_hit, cache_key /*!80000 (191) */)");
    }

    private static boolean matchesTable(DatabaseMetaData metadata, String catalog, String tableName) throws SQLException {
        try (ResultSet resultSet = metadata.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
            while (resultSet.next()) {
                String currentName = resultSet.getString("TABLE_NAME");
                if (equalsIgnoreCase(currentName, tableName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesColumn(DatabaseMetaData metadata,
                                         String catalog,
                                         String tableName,
                                         String columnName) throws SQLException {
        try (ResultSet resultSet = metadata.getColumns(catalog, null, tableName, columnName)) {
            while (resultSet.next()) {
                String currentTable = resultSet.getString("TABLE_NAME");
                String currentColumn = resultSet.getString("COLUMN_NAME");
                if (equalsIgnoreCase(currentTable, tableName) && equalsIgnoreCase(currentColumn, columnName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesIndex(DatabaseMetaData metadata,
                                        String catalog,
                                        String tableName,
                                        String indexName) throws SQLException {
        try (ResultSet resultSet = metadata.getIndexInfo(catalog, null, tableName, false, false)) {
            while (resultSet.next()) {
                String currentName = resultSet.getString("INDEX_NAME");
                if (equalsIgnoreCase(currentName, indexName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.toLowerCase(Locale.ROOT).equals(right.toLowerCase(Locale.ROOT));
    }
}
