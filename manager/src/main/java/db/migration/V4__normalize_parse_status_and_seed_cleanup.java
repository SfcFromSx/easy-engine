package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;

public class V4__normalize_parse_status_and_seed_cleanup extends BaseJavaMigration {

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
        MigrationSupport.execute(connection,
                "UPDATE " + tableName + " SET parse_status = 'OK' WHERE parse_status = 'PARSED'");
    }
}
