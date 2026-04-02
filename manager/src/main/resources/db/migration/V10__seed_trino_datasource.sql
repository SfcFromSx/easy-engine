INSERT INTO manager_query_datasource_config (
    name,
    type,
    driver_class,
    jdbc_url,
    username,
    password,
    max_pool_size,
    min_idle,
    connection_timeout_ms,
    is_default
)
SELECT
    'trino_local',
    'trino',
    'io.trino.jdbc.TrinoDriver',
    'jdbc:trino://localhost:18080/tpch/tiny',
    'admin',
    '',
    4,
    1,
    10000,
    FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM manager_query_datasource_config WHERE name = 'trino_local'
);
