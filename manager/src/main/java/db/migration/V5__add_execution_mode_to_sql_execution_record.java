package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;

public class V5__add_execution_mode_to_sql_execution_record extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String tableName = MigrationSupport.firstExistingTable(
                connection,
                "manager_sql_execution_record",
                "sql_execution_record");
        if (tableName == null) {
            return;
        }
        if (MigrationSupport.columnExists(connection, tableName, "execution_mode")
                && MigrationSupport.columnExists(connection, tableName, "parameter_payload")
                && MigrationSupport.columnExists(connection, tableName, "cache_key")
                && MigrationSupport.indexExists(connection, tableName, "idx_sql_exec_cache_hit_key")) {
            return;
        }
        MigrationSupport.recreateSqlExecutionRecordTable(connection, tableName);
    }
}
