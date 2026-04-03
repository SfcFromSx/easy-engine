package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class V18__sql_lib_reference_workflow extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("ALTER TABLE benchmark_sql_template MODIFY COLUMN sql_text MEDIUMTEXT");
            statement.execute("ALTER TABLE benchmark_sql_template MODIFY COLUMN param_json MEDIUMTEXT");
            statement.execute("ALTER TABLE benchmark_run MODIFY COLUMN error_sample MEDIUMTEXT");
            statement.execute("ALTER TABLE benchmark_run MODIFY COLUMN job_snapshot_json MEDIUMTEXT");
            statement.execute("ALTER TABLE benchmark_run MODIFY COLUMN evaluation_json MEDIUMTEXT");
            statement.execute("ALTER TABLE benchmark_test_set_item MODIFY COLUMN sql_text MEDIUMTEXT");
            statement.execute("ALTER TABLE benchmark_test_set_item MODIFY COLUMN param_json MEDIUMTEXT");
            statement.execute("ALTER TABLE benchmark_data_source MODIFY COLUMN jdbc_url MEDIUMTEXT");
            if (!MigrationSupport.columnExists(context.getConnection(), "benchmark_sql_template", "source_filename")) {
                statement.execute("ALTER TABLE benchmark_sql_template ADD COLUMN source_filename VARCHAR(512) NULL");
            }
            if (!MigrationSupport.columnExists(context.getConnection(), "benchmark_sql_template", "uploaded_at")) {
                statement.execute("ALTER TABLE benchmark_sql_template ADD COLUMN uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
            }
            if (!MigrationSupport.columnExists(context.getConnection(), "benchmark_test_set_item", "sql_lib_id")) {
                statement.execute("ALTER TABLE benchmark_test_set_item ADD COLUMN sql_lib_id BIGINT NULL");
            }
            if (!MigrationSupport.indexExists(context.getConnection(), "benchmark_test_set_item", "idx_test_set_item_sql_lib")) {
                statement.execute("CREATE INDEX idx_test_set_item_sql_lib ON benchmark_test_set_item (sql_lib_id)");
            }
            statement.execute("UPDATE benchmark_sql_template SET uploaded_at = CURRENT_TIMESTAMP WHERE uploaded_at IS NULL");
        }

        String selectItemsSql = "SELECT item.id, item.test_set_id, item.sort_order, item.label, item.sql_text, item.weight, item.execution_mode, item.param_json, test_set.name AS test_set_name " +
                "FROM benchmark_test_set_item item " +
                "LEFT JOIN benchmark_test_set test_set ON test_set.id = item.test_set_id " +
                "WHERE item.sql_lib_id IS NULL " +
                "ORDER BY item.id ASC";
        try (PreparedStatement selectItems = context.getConnection().prepareStatement(selectItemsSql);
             ResultSet resultSet = selectItems.executeQuery();
             PreparedStatement insertSqlLib = context.getConnection().prepareStatement(
                     "INSERT INTO benchmark_sql_template (name, sql_text, weight, description, execution_mode, param_json, source_filename, uploaded_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                     Statement.RETURN_GENERATED_KEYS);
             PreparedStatement updateItem = context.getConnection().prepareStatement(
                     "UPDATE benchmark_test_set_item SET sql_lib_id = ? WHERE id = ?")) {
            while (resultSet.next()) {
                long itemId = resultSet.getLong("id");
                long testSetId = resultSet.getLong("test_set_id");
                int sortOrder = resultSet.getInt("sort_order");
                String label = resultSet.getString("label");
                String sqlText = resultSet.getString("sql_text");
                int weight = Math.max(1, resultSet.getInt("weight"));
                String executionMode = resultSet.getString("execution_mode");
                String paramJson = resultSet.getString("param_json");
                String testSetName = resultSet.getString("test_set_name");

                insertSqlLib.setString(1, label != null && !label.trim().isEmpty()
                        ? label.trim()
                        : "Migrated " + (testSetName != null ? testSetName : ("test-set-" + testSetId)) + " #" + (sortOrder + 1));
                insertSqlLib.setString(2, sqlText);
                insertSqlLib.setInt(3, weight);
                insertSqlLib.setString(4, "Migrated from benchmark_test_set_item#" + itemId);
                insertSqlLib.setString(5, executionMode != null && !executionMode.trim().isEmpty() ? executionMode : "STATEMENT");
                insertSqlLib.setString(6, paramJson);
                insertSqlLib.setString(7, null);
                insertSqlLib.executeUpdate();

                try (ResultSet generatedKeys = insertSqlLib.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        throw new IllegalStateException("Failed to backfill sql_lib_id for benchmark_test_set_item#" + itemId);
                    }
                    long sqlLibId = generatedKeys.getLong(1);
                    updateItem.setLong(1, sqlLibId);
                    updateItem.setLong(2, itemId);
                    updateItem.executeUpdate();
                }
            }
        }
    }
}
