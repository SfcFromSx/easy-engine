ALTER TABLE benchmark_sql_template
    ADD COLUMN execution_mode VARCHAR(32) NOT NULL DEFAULT 'STATEMENT';
ALTER TABLE benchmark_sql_template
    ADD COLUMN param_json TEXT;

ALTER TABLE benchmark_test_set_item
    ADD COLUMN execution_mode VARCHAR(32) NOT NULL DEFAULT 'STATEMENT';
ALTER TABLE benchmark_test_set_item
    ADD COLUMN param_json TEXT;

UPDATE benchmark_job
SET jdbc_url = regexp_replace(
        replace(
            replace(jdbc_url,
                    'jdbc:kylin-cached://localhost:17070/',
                    'jdbc:kylin://127.0.0.1:8092/'),
            'jdbc:kylin-cached://127.0.0.1:17070/',
            'jdbc:kylin://127.0.0.1:8092/'
        ),
        '([?&])(redis\.(host|port)|sql\.trace\.(enabled|redis\.enabled)|datasource\.routing\.enabled)=[^&]*',
        '',
        'g'
    ),
    driver_class = 'org.apache.kylin.jdbc.Driver'
WHERE driver_class = 'com.kylin.CachedKylinDriver';

UPDATE benchmark_sql_template
SET sql_text = replace(sql_text, '-- engine=presto_local', '/* ENGINE=presto_local */')
WHERE name IN ('presto_nation_count', 'presto_orders_count', 'presto_lineitem_sum', 'presto_customer_region', 'presto_part_types');

UPDATE benchmark_test_set_item
SET sql_text = replace(sql_text, '-- engine=presto_local', '/* ENGINE=presto_local */')
WHERE label IN ('presto_nation', 'presto_orders', 'presto_lineitem', 'presto_region');

INSERT INTO benchmark_sql_template (name, sql_text, weight, description, execution_mode, param_json)
SELECT 'kylin_prepared_seller_count',
       'SELECT count(*) AS c FROM KYLIN_SALES WHERE seller_id = ?',
       3,
       'Kylin：PreparedStatement 参数过滤',
       'PREPARED_STATEMENT',
       '[{"type":"INTEGER","value":10000000}]'
WHERE NOT EXISTS (SELECT 1 FROM benchmark_sql_template WHERE name = 'kylin_prepared_seller_count');

INSERT INTO benchmark_sql_template (name, sql_text, weight, description, execution_mode, param_json)
SELECT 'presto_prepared_nation_count',
       '/* ENGINE=presto_local */ SELECT count(*) AS c FROM nation WHERE nationkey = ?',
       3,
       'Presto：PreparedStatement 参数过滤',
       'PREPARED_STATEMENT',
       '[{"type":"INTEGER","value":1}]'
WHERE NOT EXISTS (SELECT 1 FROM benchmark_sql_template WHERE name = 'presto_prepared_nation_count');

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight, execution_mode, param_json)
SELECT t.id, 16, 'kylin_prepared_seller_count',
       'SELECT count(*) AS c FROM KYLIN_SALES WHERE seller_id = ?',
       3,
       'PREPARED_STATEMENT',
       '[{"type":"INTEGER","value":10000000}]'
FROM benchmark_test_set t
WHERE t.name = 'learn_kylin_regression'
  AND NOT EXISTS (
      SELECT 1 FROM benchmark_test_set_item i
      WHERE i.test_set_id = t.id AND i.label = 'kylin_prepared_seller_count'
  );

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight, execution_mode, param_json)
SELECT t.id, 17, 'presto_prepared_nation_count',
       '/* ENGINE=presto_local */ SELECT count(*) AS c FROM nation WHERE nationkey = ?',
       3,
       'PREPARED_STATEMENT',
       '[{"type":"INTEGER","value":1}]'
FROM benchmark_test_set t
WHERE t.name = 'learn_kylin_regression'
  AND NOT EXISTS (
      SELECT 1 FROM benchmark_test_set_item i
      WHERE i.test_set_id = t.id AND i.label = 'presto_prepared_nation_count'
  );

INSERT INTO benchmark_test_set_item (test_set_id, sort_order, label, sql_text, weight, execution_mode, param_json)
SELECT t.id, 4, 'kylin_prepared_seller_count',
       'SELECT count(*) AS c FROM KYLIN_SALES WHERE seller_id = ?',
       2,
       'PREPARED_STATEMENT',
       '[{"type":"INTEGER","value":10000000}]'
FROM benchmark_test_set t
WHERE t.name = 'smoke_kylin_only'
  AND NOT EXISTS (
      SELECT 1 FROM benchmark_test_set_item i
      WHERE i.test_set_id = t.id AND i.label = 'kylin_prepared_seller_count'
  );

INSERT INTO benchmark_job (name, jdbc_url, jdbc_user, jdbc_password, driver_class, concurrent_threads, rounds, strategy, test_set_id)
SELECT 'benchmark-query-e2e-mixed',
       'jdbc:kylin://127.0.0.1:8092/learn_kylin',
       'ADMIN',
       'KYLIN',
       'org.apache.kylin.jdbc.Driver',
       3,
       24,
       'ROUND_ROBIN',
       (SELECT id FROM benchmark_test_set WHERE name = 'learn_kylin_regression' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM benchmark_job WHERE name = 'benchmark-query-e2e-mixed');
