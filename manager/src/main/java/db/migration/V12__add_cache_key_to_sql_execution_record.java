package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;

public class V12__add_cache_key_to_sql_execution_record extends BaseJavaMigration {

    private static final String TABLE_NAME = "manager_sql_execution_record";
    private static final String INDEX_NAME = "idx_sql_exec_cache_hit_key";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!MigrationSupport.columnExists(connection, TABLE_NAME, "cache_key")) {
            MigrationSupport.execute(connection,
                    "ALTER TABLE " + TABLE_NAME + " ADD COLUMN cache_key VARCHAR(1024)");
        }
        if (!MigrationSupport.indexExists(connection, TABLE_NAME, INDEX_NAME)) {
            MigrationSupport.execute(connection,
                    "CREATE INDEX " + INDEX_NAME
                            + " ON " + TABLE_NAME + " (cache_hit, cache_key /*!80000 (191) */)");
        }
    }
}
