-- V8: 引入「尊享版」压测场景，逻辑化任务排布与深度铺底。
-- 包含：Kylin 高性能基准、Presto 数据湖探测、缓存命中/穿透策略对比、多表关联压力测试。

-- 1) 扩充 SQL 模板库
INSERT INTO benchmark_sql_template (name, sql_text, weight, description) VALUES
('kylin_agg_precise_count', 'SELECT count(distinct seller_id) FROM KYLIN_SALES', 5, 'Kylin: 高维精确去重统计 (Bitmap)'),
('kylin_agg_topn_seller', 'SELECT seller_id, sum(price) as total_sales FROM KYLIN_SALES GROUP BY seller_id ORDER BY total_sales DESC LIMIT 5', 5, 'Kylin: 卖家销售额 TopN (TopN Measure)'),
('presto_lake_nation_region', '/* ENGINE=presto_local */ SELECT r.name as region, count(*) as nations FROM nation n JOIN region r ON n.regionkey = r.regionkey GROUP BY r.name', 4, 'Presto: 跨表关联查询 (Nation x Region)'),
('presto_lake_lineitem_agg', '/* ENGINE=presto_local */ SELECT returnflag, linestatus, sum(quantity) as sum_qty FROM lineitem GROUP BY returnflag, linestatus', 3, 'Presto: TPCH Lineitem 大表聚合'),
('cache_stress_heavy_agg', 'SELECT part_dt, lstg_format_name, sum(price), avg(price), count(*) FROM KYLIN_SALES GROUP BY part_dt, lstg_format_name', 5, 'Cache Stress: 复杂维度组合聚合 (测试缓存命中)'),
('cache_penetration_no_cache', '/* no-cache */ SELECT count(*) FROM KYLIN_SALES WHERE part_dt = ''2013-01-01''', 3, 'Cache Stress: 显式禁缓存单点查询');

-- 2) 预置「极速 Kylin」测试集
INSERT INTO benchmark_test_set (name, description, source_filename) VALUES
('Kylin-Performance-Suite', '针对 Kylin 预计算特性的性能基准测试集，涵盖聚合、TopN 与过滤。', 'V8-Premium');

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT (SELECT id FROM benchmark_test_set WHERE name = 'Kylin-Performance-Suite'), 0, 'total_count', 'SELECT count(*) FROM KYLIN_SALES', 5;
INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT (SELECT id FROM benchmark_test_set WHERE name = 'Kylin-Performance-Suite'), 1, 'sales_by_format', 'SELECT lstg_format_name, sum(price) FROM KYLIN_SALES GROUP BY lstg_format_name', 4;
INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT (SELECT id FROM benchmark_test_set WHERE name = 'Kylin-Performance-Suite'), 2, 'top_sellers', 'SELECT seller_id, sum(price) s FROM KYLIN_SALES GROUP BY seller_id ORDER BY s DESC LIMIT 10', 4;
INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT (SELECT id FROM benchmark_test_set WHERE name = 'Kylin-Performance-Suite'), 3, 'precise_distinct', 'SELECT count(distinct seller_id) FROM KYLIN_SALES', 3;

-- 3) 预置「Presto 数据湖」测试集
INSERT INTO benchmark_test_set (name, description, source_filename) VALUES
('Presto-Lake-Exploration', '直接透传至 Presto 的原生 SQL 测试集，验证逻辑路由与数据湖查询能力。', 'V8-Premium');

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT (SELECT id FROM benchmark_test_set WHERE name = 'Presto-Lake-Exploration'), 0, 'presto_nation', '/* ENGINE=presto_local */ SELECT count(*) FROM nation', 5;
INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT (SELECT id FROM benchmark_test_set WHERE name = 'Presto-Lake-Exploration'), 1, 'presto_join', '/* ENGINE=presto_local */ SELECT n.name, r.name FROM nation n JOIN region r ON n.regionkey = r.regionkey', 4;

-- 4) 预置核心任务 (Jobs)
-- a) Kylin 黄金基准 (并发 4, 100 轮)
INSERT INTO benchmark_job (name, jdbc_url, jdbc_user, jdbc_password, driver_class, concurrent_threads, rounds, strategy, test_set_id)
VALUES (
  'Kylin-Golden-Baseline',
  'jdbc:kylin://127.0.0.1:8092/learn_kylin',
  'ADMIN', 'KYLIN', 'org.apache.kylin.jdbc.Driver',
  4, 100, 'RANDOM_WEIGHT',
  (SELECT id FROM benchmark_test_set WHERE name = 'Kylin-Performance-Suite')
);

-- b) Presto 穿透压力测试
INSERT INTO benchmark_job (name, jdbc_url, jdbc_user, jdbc_password, driver_class, concurrent_threads, rounds, strategy, test_set_id)
VALUES (
  'Presto-Direct-Stress',
  'jdbc:kylin://127.0.0.1:8092/learn_kylin',
  'ADMIN', 'KYLIN', 'org.apache.kylin.jdbc.Driver',
  2, 20, 'ROUND_ROBIN',
  (SELECT id FROM benchmark_test_set WHERE name = 'Presto-Lake-Exploration')
);

-- c) 缓存命中率对比任务 (禁用缓存 vs 开启缓存)
INSERT INTO benchmark_job (name, jdbc_url, jdbc_user, jdbc_password, driver_class, concurrent_threads, rounds, strategy, test_set_id)
VALUES (
  'Cache-Efficiency-Test',
  'jdbc:kylin://127.0.0.1:8092/learn_kylin',
  'ADMIN', 'KYLIN', 'org.apache.kylin.jdbc.Driver',
  4, 120, 'RANDOM_WEIGHT',
  NULL -- 使用全局模板
);
