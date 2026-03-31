INSERT INTO benchmark_sql_template (name, sql_text, weight, description) VALUES
('ping', 'SELECT 1', 5, 'Connectivity / driver sanity'),
('sample_agg', 'SELECT 1 AS c', 3, 'Replace with real Kylin/Presto SQL for your model');

INSERT INTO benchmark_job (name, jdbc_url, jdbc_user, jdbc_password, driver_class, concurrent_threads, rounds, strategy)
VALUES (
    'default-kylin-cached',
    'jdbc:kylin://127.0.0.1:8092/learn_kylin',
    'ADMIN',
    'KYLIN',
    'org.apache.kylin.jdbc.Driver',
    4,
    20,
    'RANDOM_WEIGHT'
);
