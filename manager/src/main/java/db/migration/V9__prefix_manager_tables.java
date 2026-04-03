package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;

public class V9__prefix_manager_tables extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        renameIfNeeded(connection, "sql_execution_record", "manager_sql_execution_record");
        renameIfNeeded(connection, "sql_pattern_stats", "manager_sql_pattern_stats");
        renameIfNeeded(connection, "acceleration_table", "manager_acceleration_table");
        renameIfNeeded(connection, "query_datasource_config", "manager_query_datasource_config");
    }

    private void renameIfNeeded(Connection connection, String legacyTable, String targetTable) throws Exception {
        if (!MigrationSupport.tableExists(connection, legacyTable)
                || MigrationSupport.tableExists(connection, targetTable)) {
            return;
        }
        MigrationSupport.execute(connection,
                "ALTER TABLE " + legacyTable + " RENAME TO " + targetTable);
    }
}
