package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;

public class V8__remove_default_dashboard_demo_seeds extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String traceTable = MigrationSupport.firstExistingTable(
                connection,
                "manager_sql_execution_record",
                "sql_execution_record");
        String patternTable = MigrationSupport.firstExistingTable(
                connection,
                "manager_sql_pattern_stats",
                "sql_pattern_stats");
        String accelerationTable = MigrationSupport.firstExistingTable(
                connection,
                "manager_acceleration_table",
                "acceleration_table");
        if (traceTable == null || patternTable == null || accelerationTable == null) {
            return;
        }

        MigrationSupport.execute(connection,
                "DELETE FROM " + traceTable + " " +
                        "WHERE raw_payload IS NULL " +
                        "  AND sql_fingerprint IN ('f1_sum_count', 'f2_sum_price', 'f3_group_format', 'f4_presto_nation', 'f5_top_seller')");

        MigrationSupport.execute(connection,
                "DELETE FROM " + patternTable + " " +
                        "WHERE sql_fingerprint IN ('f1_sum_count', 'f2_sum_price', 'f3_group_format', 'f5_top_seller')");

        MigrationSupport.execute(connection,
                "DELETE FROM " + accelerationTable + " " +
                        "WHERE schema_name = 'public' " +
                        "  AND source = 'MANUAL' " +
                        "  AND ( " +
                        "      (name = 'mv_sales_by_format' " +
                        "          AND ddl_text = 'CREATE MATERIALIZED VIEW mv_sales_by_format AS SELECT lstg_format_name, sum(price) as total_price FROM KYLIN_SALES GROUP BY lstg_format_name' " +
                        "          AND refresh_sql = 'REFRESH MATERIALIZED VIEW mv_sales_by_format' " +
                        "          AND cron_expr = '0 0 2 * * ?' " +
                        "          AND recommendation_note = '优化模式 f3_group_format，覆盖约 15% 的请求流量。') " +
                        "      OR " +
                        "      (name = 'idx_seller_sales' " +
                        "          AND ddl_text = 'CREATE INDEX idx_seller_sales ON KYLIN_SALES(seller_id, price)' " +
                        "          AND refresh_sql IS NULL " +
                        "          AND cron_expr IS NULL " +
                        "          AND recommendation_note = '加速卖家 TopN 分析。') " +
                        "  )");
    }
}
