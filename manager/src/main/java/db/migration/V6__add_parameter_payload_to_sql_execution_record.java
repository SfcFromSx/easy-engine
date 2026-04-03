package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;

public class V6__add_parameter_payload_to_sql_execution_record extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String tableName = MigrationSupport.firstExistingTable(
                connection,
                "manager_sql_execution_record",
                "sql_execution_record");
        if (tableName == null || MigrationSupport.columnExists(connection, tableName, "parameter_payload")) {
            return;
        }
        MigrationSupport.execute(connection,
                "ALTER TABLE " + tableName + " ADD COLUMN parameter_payload MEDIUMTEXT");
    }
}
