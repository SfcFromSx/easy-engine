UPDATE benchmark_sql_template
SET name = 'presto_prepared_region_count',
    sql_text = '/* YH_TARGET_ENGINE=presto_local */ SELECT count(*) AS c FROM region WHERE regionkey = ?',
    description = 'Presto：PreparedStatement 参数过滤（region）',
    execution_mode = 'PREPARED_STATEMENT',
    param_json = '[{"type":"INTEGER","value":1}]'
WHERE name = 'kylin_prepared_seller_count';

UPDATE benchmark_test_set_item
SET label = 'presto_prepared_region_count',
    sql_text = '/* YH_TARGET_ENGINE=presto_local */ SELECT count(*) AS c FROM region WHERE regionkey = ?',
    execution_mode = 'PREPARED_STATEMENT',
    param_json = '[{"type":"INTEGER","value":1}]'
WHERE label = 'kylin_prepared_seller_count'
  AND test_set_id = (SELECT id FROM benchmark_test_set WHERE name = 'learn_kylin_regression' LIMIT 1);

DELETE FROM benchmark_test_set_item
WHERE label = 'kylin_prepared_seller_count'
  AND test_set_id = (SELECT id FROM benchmark_test_set WHERE name = 'smoke_kylin_only' LIMIT 1);
