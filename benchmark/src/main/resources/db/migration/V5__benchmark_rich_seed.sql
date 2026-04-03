-- 丰富压测铺底：修正 learn_kylin 不友好的 SELECT 1、扩充全局模板、预置测试集与多策略任务。
-- 依赖：V2 已有 ping/sample_agg 与 default-kylin-cached；V4 已有 benchmark_test_set* 与 benchmark_job.test_set_id。

-- ---------------------------------------------------------------------------
-- 1) 修正 V2 中两条模板（Kylin cube 语义）
-- ---------------------------------------------------------------------------
UPDATE benchmark_sql_template
SET sql_text    = 'SELECT count(*) FROM KYLIN_SALES',
    description = 'learn_kylin：全表行数（与集成测试一致）',
    weight      = 8
WHERE name = 'ping';

UPDATE benchmark_sql_template
SET sql_text    = 'SELECT sum(price) AS sum_price FROM KYLIN_SALES',
    description = 'learn_kylin：销售额合计',
    weight      = 5
WHERE name = 'sample_agg';

-- ---------------------------------------------------------------------------
-- 2) 全局 SQL 模板（加权随机池）
-- ---------------------------------------------------------------------------
INSERT INTO benchmark_sql_template (name, sql_text, weight, description) VALUES
('kylin_avg_price', 'SELECT avg(price) AS avg_price FROM KYLIN_SALES', 4, 'learn_kylin：均价'),
('kylin_max_price', 'SELECT max(price) AS max_price FROM KYLIN_SALES', 3, 'learn_kylin：最高价'),
('kylin_min_price', 'SELECT min(price) AS min_price FROM KYLIN_SALES', 3, 'learn_kylin：最低价'),
('kylin_sum_item_count', 'SELECT sum(item_count) AS units FROM KYLIN_SALES', 4, 'learn_kylin：销量件数合计'),
('kylin_group_lstg_format', 'SELECT lstg_format_name, sum(price) AS s FROM KYLIN_SALES GROUP BY lstg_format_name', 5, 'learn_kylin：按销售渠道聚合'),
('kylin_group_part_dt', 'SELECT part_dt, sum(price) AS s FROM KYLIN_SALES GROUP BY part_dt ORDER BY part_dt LIMIT 20', 5, 'learn_kylin：按日聚合+排序截断'),
('kylin_group_seller', 'SELECT seller_id, count(*) AS c FROM KYLIN_SALES GROUP BY seller_id ORDER BY c DESC LIMIT 10', 4, 'learn_kylin：卖家 TOP'),
('kylin_two_dim', 'SELECT lstg_format_name, part_dt, sum(price) AS s FROM KYLIN_SALES GROUP BY lstg_format_name, part_dt LIMIT 30', 3, 'learn_kylin：二维分组'),
('kylin_filter_date', 'SELECT count(*) AS c FROM KYLIN_SALES WHERE part_dt >= ''2012-01-01''', 4, 'learn_kylin：日期过滤'),
('kylin_distinct_seller', 'SELECT count(distinct seller_id) AS d FROM KYLIN_SALES', 3, 'learn_kylin：去重卖家数'),
('kylin_order_by_price', 'SELECT price FROM KYLIN_SALES ORDER BY price DESC LIMIT 5', 2, 'learn_kylin：排序 LIMIT'),
('nocache_rowcount', '/* no-cache */ SELECT count(*) FROM KYLIN_SALES', 3, '驱动：跳过缓存的行数'),
('force_refresh_rowcount', '/* force-refresh */ SELECT count(*) FROM KYLIN_SALES', 2, '驱动：强制刷新语义'),
('cache_refresh_rowcount', '/* cache-refresh */ SELECT count(*) FROM KYLIN_SALES', 2, '驱动：cache-refresh 注释'),
('hint_default_count', '/* ENGINE=default */ SELECT count(*) FROM KYLIN_SALES', 3, '路由：显式 default'),
('presto_nation_count', '/* ENGINE=presto_local */ SELECT count(*) FROM nation', 4, 'Presto tpch.tiny：nation 行数'),
('presto_orders_count', '/* ENGINE=presto_local */ SELECT count(*) FROM orders', 4, 'Presto tpch.tiny：orders 行数'),
('presto_lineitem_sum', '/* ENGINE=presto_local */ SELECT sum(quantity) AS q FROM lineitem', 3, 'Presto tpch.tiny：lineitem 聚合'),
('presto_customer_region', '/* ENGINE=presto_local */ SELECT regionkey, count(*) AS c FROM customer GROUP BY regionkey', 3, 'Presto：customer 按地区'),
('presto_part_types', '/* ENGINE=presto_local */ SELECT type, count(*) AS c FROM part GROUP BY type ORDER BY c DESC LIMIT 5', 2, 'Presto：part 类型分布');

-- ---------------------------------------------------------------------------
-- 3) 预置测试集 learn_kylin_regression（绑定任务走「测试集」路径）
-- ---------------------------------------------------------------------------
INSERT INTO benchmark_test_set (name, description, source_filename)
SELECT 'learn_kylin_regression',
       'Flyway 预置：Kylin learn_kylin + Presto tpch + 注释/路由混合',
       'flyway:V5'
WHERE NOT EXISTS (SELECT 1 FROM benchmark_test_set WHERE name = 'learn_kylin_regression');

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 0, 'count_all', 'SELECT count(*) FROM KYLIN_SALES', 5
FROM benchmark_test_set t WHERE t.name = 'learn_kylin_regression';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 1, 'sum_price', 'SELECT sum(price) FROM KYLIN_SALES', 4
FROM benchmark_test_set t WHERE t.name = 'learn_kylin_regression';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 2, 'group_format', 'SELECT lstg_format_name, sum(price) FROM KYLIN_SALES GROUP BY lstg_format_name', 4
FROM benchmark_test_set t WHERE t.name = 'learn_kylin_regression';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 3, 'group_day', 'SELECT part_dt, sum(price) FROM KYLIN_SALES GROUP BY part_dt ORDER BY part_dt LIMIT 10', 3
FROM benchmark_test_set t WHERE t.name = 'learn_kylin_regression';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 4, 'distinct_sellers', 'SELECT count(distinct seller_id) FROM KYLIN_SALES', 2
FROM benchmark_test_set t WHERE t.name = 'learn_kylin_regression';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 5, 'filter_day', 'SELECT count(*) FROM KYLIN_SALES WHERE part_dt >= ''2012-01-01''', 3
FROM benchmark_test_set t WHERE t.name = 'learn_kylin_regression';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 6, 'seller_top', 'SELECT seller_id, count(*) c FROM KYLIN_SALES GROUP BY seller_id ORDER BY c DESC LIMIT 5', 3
FROM benchmark_test_set t WHERE t.name = 'learn_kylin_regression';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 7, 'nocache', '/* no-cache */ SELECT count(*) FROM KYLIN_SALES', 3
FROM benchmark_test_set t WHERE t.name = 'learn_kylin_regression';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 8, 'force_refresh', '/* force-refresh */ SELECT count(*) FROM KYLIN_SALES', 2
FROM benchmark_test_set t WHERE t.name = 'learn_kylin_regression';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 9, 'hint_default', '/* ENGINE=default */ SELECT sum(price) FROM KYLIN_SALES', 3
FROM benchmark_test_set t WHERE t.name = 'learn_kylin_regression';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 10, 'presto_nation', '/* ENGINE=presto_local */ SELECT count(*) FROM nation', 4
FROM benchmark_test_set t WHERE t.name = 'learn_kylin_regression';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 11, 'presto_orders', '/* ENGINE=presto_local */ SELECT count(*) FROM orders', 4
FROM benchmark_test_set t WHERE t.name = 'learn_kylin_regression';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 12, 'presto_lineitem', '/* ENGINE=presto_local */ SELECT sum(extendedprice) FROM lineitem', 3
FROM benchmark_test_set t WHERE t.name = 'learn_kylin_regression';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 13, 'presto_region', '/* ENGINE=presto_local */ SELECT r.name, count(*) c FROM nation n JOIN region r ON n.regionkey = r.regionkey GROUP BY r.name', 2
FROM benchmark_test_set t WHERE t.name = 'learn_kylin_regression';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 14, 'kylin_avg', 'SELECT avg(price) FROM KYLIN_SALES', 3
FROM benchmark_test_set t WHERE t.name = 'learn_kylin_regression';

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight)
SELECT t.id, 15, 'kylin_limit_scan', 'SELECT price FROM KYLIN_SALES ORDER BY price DESC LIMIT 3', 2
FROM benchmark_test_set t WHERE t.name = 'learn_kylin_regression';

-- ---------------------------------------------------------------------------
-- 4) 任务：更新默认任务 + 新增 smoke / 测试集轮询 / 缓存穿透
-- ---------------------------------------------------------------------------
UPDATE benchmark_job
SET rounds             = 36,
    concurrent_threads = 4,
    strategy           = 'RANDOM_WEIGHT',
    test_set_id        = NULL
WHERE name = 'default-kylin-cached';

INSERT INTO benchmark_job (name, jdbc_url, jdbc_user, jdbc_password, driver_class, concurrent_threads, rounds, strategy, test_set_id)
SELECT 'benchmark-smoke-global',
       'jdbc:kylin://127.0.0.1:8092/learn_kylin',
       'ADMIN',
       'KYLIN',
       'org.apache.kylin.jdbc.Driver',
       2,
       24,
       'RANDOM_WEIGHT',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM benchmark_job WHERE name = 'benchmark-smoke-global');

INSERT INTO benchmark_job (name, jdbc_url, jdbc_user, jdbc_password, driver_class, concurrent_threads, rounds, strategy, test_set_id)
SELECT 'benchmark-testset-round-robin',
       'jdbc:kylin://127.0.0.1:8092/learn_kylin',
       'ADMIN',
       'KYLIN',
       'org.apache.kylin.jdbc.Driver',
       3,
       48,
       'ROUND_ROBIN',
       (SELECT id FROM benchmark_test_set WHERE name = 'learn_kylin_regression' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM benchmark_job WHERE name = 'benchmark-testset-round-robin');

INSERT INTO benchmark_job (name, jdbc_url, jdbc_user, jdbc_password, driver_class, concurrent_threads, rounds, strategy, test_set_id)
SELECT 'benchmark-cache-penetration',
       'jdbc:kylin://127.0.0.1:8092/learn_kylin',
       'ADMIN',
       'KYLIN',
       'org.apache.kylin.jdbc.Driver',
       3,
       40,
       'CACHE_PENETRATION',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM benchmark_job WHERE name = 'benchmark-cache-penetration');
