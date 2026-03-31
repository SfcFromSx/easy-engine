-- Remove the V3 demo rows from the default operator path so a fresh manager
-- schema starts empty until live traces are ingested.

DELETE FROM sql_execution_record
WHERE raw_payload IS NULL
  AND sql_fingerprint IN (
      'f1_sum_count',
      'f2_sum_price',
      'f3_group_format',
      'f4_presto_nation',
      'f5_top_seller'
  );

DELETE FROM sql_pattern_stats
WHERE sql_fingerprint IN (
      'f1_sum_count',
      'f2_sum_price',
      'f3_group_format',
      'f5_top_seller'
  );

DELETE FROM acceleration_table
WHERE schema_name = 'public'
  AND source = 'MANUAL'
  AND (
      (
          name = 'mv_sales_by_format'
          AND ddl_text = 'CREATE MATERIALIZED VIEW mv_sales_by_format AS SELECT lstg_format_name, sum(price) as total_price FROM KYLIN_SALES GROUP BY lstg_format_name'
          AND refresh_sql = 'REFRESH MATERIALIZED VIEW mv_sales_by_format'
          AND cron_expr = '0 0 2 * * ?'
          AND recommendation_note = '优化模式 f3_group_format，覆盖约 15% 的请求流量。'
      )
      OR (
          name = 'idx_seller_sales'
          AND ddl_text = 'CREATE INDEX idx_seller_sales ON KYLIN_SALES(seller_id, price)'
          AND refresh_sql IS NULL
          AND cron_expr IS NULL
          AND recommendation_note = '加速卖家 TopN 分析。'
      )
  );
