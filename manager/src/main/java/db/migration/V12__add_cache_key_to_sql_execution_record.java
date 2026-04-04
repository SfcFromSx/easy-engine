package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;

public class V12__add_cache_key_to_sql_execution_record extends BaseJavaMigration {

    private static final String INDEX_NAME = "idx_sql_exec_cache_hit_key";

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
        if (MigrationSupport.columnExists(connection, tableName, "cache_key")
                && MigrationSupport.indexExists(connection, tableName, INDEX_NAME)) {
            return;
        }
        MigrationSupport.recreateSqlExecutionRecordTable(connection, tableName);
    }
}
