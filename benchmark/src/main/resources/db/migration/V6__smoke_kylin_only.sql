-- 冒烟任务改为仅 Kylin learn_kylin 短 SQL，避免随机抽到 Presto/重查询导致长时间 RUNNING 或依赖 Presto。
INSERT INTO benchmark_test_set (name, description, source_filename)
SELECT 'smoke_kylin_only',
       '冒烟专用：仅 KYLIN_SALES 轻量查询，不依赖 Presto',
       'flyway:V6'
WHERE NOT EXISTS (SELECT 1 FROM benchmark_test_set WHERE name = 'smoke_kylin_only');

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 0, 'cnt', 'SELECT count(*) FROM KYLIN_SALES', 5
FROM benchmark_test_set t WHERE t.name = 'smoke_kylin_only';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 1, 'sum_p', 'SELECT sum(price) FROM KYLIN_SALES', 4
FROM benchmark_test_set t WHERE t.name = 'smoke_kylin_only';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 2, 'avg_p', 'SELECT avg(price) FROM KYLIN_SALES', 3
FROM benchmark_test_set t WHERE t.name = 'smoke_kylin_only';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 3, 'fmt', 'SELECT lstg_format_name, sum(price) FROM KYLIN_SALES GROUP BY lstg_format_name', 3
FROM benchmark_test_set t WHERE t.name = 'smoke_kylin_only';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 4, 'day_top', 'SELECT part_dt, sum(price) FROM KYLIN_SALES GROUP BY part_dt ORDER BY part_dt LIMIT 5', 2
FROM benchmark_test_set t WHERE t.name = 'smoke_kylin_only';

UPDATE benchmark_job j
SET test_set_id = (SELECT id FROM benchmark_test_set WHERE name = 'smoke_kylin_only' LIMIT 1),
    rounds        = 16,
    concurrent_threads = 2
WHERE j.name = 'benchmark-smoke-global';
