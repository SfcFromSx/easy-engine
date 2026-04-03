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

    static boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        return matchesTable(metadata, connection.getCatalog(), tableName) || matchesTable(metadata, null, tableName);
    }

    static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        return matchesColumn(metadata, connection.getCatalog(), tableName, columnName)
                || matchesColumn(metadata, null, tableName, columnName);
    }

    static boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        return matchesIndex(metadata, connection.getCatalog(), tableName, indexName)
                || matchesIndex(metadata, null, tableName, indexName);
    }

    static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static boolean matchesTable(DatabaseMetaData metadata, String catalog, String tableName) throws SQLException {
        try (ResultSet resultSet = metadata.getTables(catalog, null, null, new String[]{"TABLE"})) {
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
        try (ResultSet resultSet = metadata.getColumns(catalog, null, null, null)) {
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
        } catch (SQLException ignored) {
            try (ResultSet resultSet = metadata.getIndexInfo(null, null, tableName, false, false)) {
                while (resultSet.next()) {
                    String currentName = resultSet.getString("INDEX_NAME");
                    if (equalsIgnoreCase(currentName, indexName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.toLowerCase(Locale.ROOT).equals(right.toLowerCase(Locale.ROOT));
    }
}
