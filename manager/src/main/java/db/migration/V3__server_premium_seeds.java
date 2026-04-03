package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;

public class V3__server_premium_seeds extends BaseJavaMigration {

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
            throw new IllegalStateException("Manager V3 seed migration could not resolve the expected target tables");
        }

        MigrationSupport.execute(connection,
                "INSERT INTO " + traceTable + " (received_at, datasource_name, datasource_type, original_sql, clean_sql, sql_fingerprint, duration_ms, success, cache_hit, parse_status) VALUES " +
                        "(TIMESTAMPADD(MINUTE, -20, CURRENT_TIMESTAMP), 'learn_kylin', 'KYLIN', 'SELECT count(*) FROM KYLIN_SALES', 'SELECT count(*) FROM KYLIN_SALES', 'f1_sum_count', 12, true, true, 'PARSED')," +
                        "(TIMESTAMPADD(MINUTE, -18, CURRENT_TIMESTAMP), 'learn_kylin', 'KYLIN', 'SELECT sum(price) FROM KYLIN_SALES', 'SELECT sum(price) FROM KYLIN_SALES', 'f2_sum_price', 45, true, false, 'PARSED')," +
                        "(TIMESTAMPADD(MINUTE, -15, CURRENT_TIMESTAMP), 'learn_kylin', 'KYLIN', 'SELECT lstg_format_name, sum(price) FROM KYLIN_SALES GROUP BY lstg_format_name', 'SELECT lstg_format_name, sum(price) FROM KYLIN_SALES GROUP BY lstg_format_name', 'f3_group_format', 88, true, false, 'PARSED')," +
                        "(TIMESTAMPADD(MINUTE, -10, CURRENT_TIMESTAMP), 'presto_local', 'PRESTO', 'SELECT count(*) FROM nation', 'SELECT count(*) FROM nation', 'f4_presto_nation', 560, true, false, 'PARSED')," +
                        "(TIMESTAMPADD(MINUTE, -5, CURRENT_TIMESTAMP), 'learn_kylin', 'KYLIN', 'SELECT seller_id, sum(price) FROM KYLIN_SALES GROUP BY seller_id ORDER BY 2 DESC LIMIT 10', 'SELECT seller_id, sum(price) FROM KYLIN_SALES GROUP BY seller_id ORDER BY 2 DESC LIMIT 10', 'f5_top_seller', 120, true, false, 'PARSED')");

        MigrationSupport.execute(connection,
                "INSERT INTO " + patternTable + " (sql_fingerprint, clean_sql_sample, execution_count, avg_duration_ms, last_seen_at) VALUES " +
                        "('f1_sum_count', 'SELECT count(*) FROM KYLIN_SALES', 850, 15.2, CURRENT_TIMESTAMP)," +
                        "('f2_sum_price', 'SELECT sum(price) FROM KYLIN_SALES', 420, 48.5, CURRENT_TIMESTAMP)," +
                        "('f3_group_format', 'SELECT lstg_format_name, sum(price) FROM KYLIN_SALES GROUP BY lstg_format_name', 125, 92.1, CURRENT_TIMESTAMP)," +
                        "('f5_top_seller', 'SELECT seller_id, sum(price) FROM KYLIN_SALES GROUP BY seller_id ORDER BY 2 DESC LIMIT 10', 88, 115.0, CURRENT_TIMESTAMP)");

        MigrationSupport.execute(connection,
                "INSERT INTO " + accelerationTable + " (name, schema_name, ddl_text, refresh_sql, cron_expr, status, source, recommendation_note) VALUES " +
                        "('mv_sales_by_format', 'public', " +
                        "'CREATE MATERIALIZED VIEW mv_sales_by_format AS SELECT lstg_format_name, sum(price) as total_price FROM KYLIN_SALES GROUP BY lstg_format_name', " +
                        "'REFRESH MATERIALIZED VIEW mv_sales_by_format', " +
                        "'0 0 2 * * ?', 'ACTIVE', 'MANUAL', '优化模式 f3_group_format，覆盖约 15% 的请求流量。')," +
                        "('idx_seller_sales', 'public', " +
                        "'CREATE INDEX idx_seller_sales ON KYLIN_SALES(seller_id, price)', " +
                        "NULL, NULL, 'ACTIVE', 'MANUAL', '加速卖家 TopN 分析。')");
    }
}
