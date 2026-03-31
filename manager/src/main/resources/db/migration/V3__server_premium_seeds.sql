-- V3: 引入「高端版」核心引擎铺底。
-- 模拟：零售（KYLIN_SALES）的高频分析请求、数据湖（TPCH）的原始轨迹。
-- 目标：确立「Trace -> Pattern -> Accel」的链条可感。

-- 1) 模拟原始 SQL 追踪 (Traces)
INSERT INTO sql_execution_record (received_at, datasource_name, datasource_type, original_sql, clean_sql, sql_fingerprint, duration_ms, success, cache_hit, parse_status) VALUES
(TIMESTAMPADD(MINUTE, -20, CURRENT_TIMESTAMP), 'learn_kylin', 'KYLIN', 'SELECT count(*) FROM KYLIN_SALES', 'SELECT count(*) FROM KYLIN_SALES', 'f1_sum_count', 12, true, true, 'PARSED'),
(TIMESTAMPADD(MINUTE, -18, CURRENT_TIMESTAMP), 'learn_kylin', 'KYLIN', 'SELECT sum(price) FROM KYLIN_SALES', 'SELECT sum(price) FROM KYLIN_SALES', 'f2_sum_price', 45, true, false, 'PARSED'),
(TIMESTAMPADD(MINUTE, -15, CURRENT_TIMESTAMP), 'learn_kylin', 'KYLIN', 'SELECT lstg_format_name, sum(price) FROM KYLIN_SALES GROUP BY lstg_format_name', 'SELECT lstg_format_name, sum(price) FROM KYLIN_SALES GROUP BY lstg_format_name', 'f3_group_format', 88, true, false, 'PARSED'),
(TIMESTAMPADD(MINUTE, -10, CURRENT_TIMESTAMP), 'presto_local', 'PRESTO', 'SELECT count(*) FROM nation', 'SELECT count(*) FROM nation', 'f4_presto_nation', 560, true, false, 'PARSED'),
(TIMESTAMPADD(MINUTE, -5, CURRENT_TIMESTAMP), 'learn_kylin', 'KYLIN', 'SELECT seller_id, sum(price) FROM KYLIN_SALES GROUP BY seller_id ORDER BY 2 DESC LIMIT 10', 'SELECT seller_id, sum(price) FROM KYLIN_SALES GROUP BY seller_id ORDER BY 2 DESC LIMIT 10', 'f5_top_seller', 120, true, false, 'PARSED');

-- 2) 模拟核心模式分析结果 (Patterns)
INSERT INTO sql_pattern_stats (sql_fingerprint, clean_sql_sample, execution_count, avg_duration_ms, last_seen_at) VALUES
('f1_sum_count', 'SELECT count(*) FROM KYLIN_SALES', 850, 15.2, CURRENT_TIMESTAMP),
('f2_sum_price', 'SELECT sum(price) FROM KYLIN_SALES', 420, 48.5, CURRENT_TIMESTAMP),
('f3_group_format', 'SELECT lstg_format_name, sum(price) FROM KYLIN_SALES GROUP BY lstg_format_name', 125, 92.1, CURRENT_TIMESTAMP),
('f5_top_seller', 'SELECT seller_id, sum(price) FROM KYLIN_SALES GROUP BY seller_id ORDER BY 2 DESC LIMIT 10', 88, 115.0, CURRENT_TIMESTAMP);

-- 3) 模拟加速优化实体 (Accelerations)
INSERT INTO acceleration_table (name, schema_name, ddl_text, refresh_sql, cron_expr, status, source, recommendation_note) VALUES
('mv_sales_by_format', 'public', 
  'CREATE MATERIALIZED VIEW mv_sales_by_format AS SELECT lstg_format_name, sum(price) as total_price FROM KYLIN_SALES GROUP BY lstg_format_name',
  'REFRESH MATERIALIZED VIEW mv_sales_by_format',
  '0 0 2 * * ?',
  'ACTIVE', 'MANUAL', '优化模式 f3_group_format，覆盖约 15% 的请求流量。'
),
('idx_seller_sales', 'public',
  'CREATE INDEX idx_seller_sales ON KYLIN_SALES(seller_id, price)',
  NULL, NULL, 'ACTIVE', 'MANUAL', '加速卖家 TopN 分析。'
);
