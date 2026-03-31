CREATE TABLE IF NOT EXISTS query_datasource_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    type VARCHAR(64) NOT NULL,
    driver_class VARCHAR(512) NOT NULL,
    jdbc_url TEXT NOT NULL,
    username VARCHAR(256),
    password VARCHAR(256),
    max_pool_size INTEGER NOT NULL DEFAULT 4,
    min_idle INTEGER NOT NULL DEFAULT 1,
    connection_timeout_ms BIGINT NOT NULL DEFAULT 10000,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_query_datasource_name ON query_datasource_config (name);

INSERT INTO query_datasource_config (
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
    'default',
    'kylin',
    'org.apache.kylin.jdbc.Driver',
    'jdbc:kylin://localhost:17070/learn_kylin',
    'ADMIN',
    'KYLIN',
    4,
    1,
    10000,
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM query_datasource_config WHERE name = 'default'
);

INSERT INTO query_datasource_config (
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
    'presto_local',
    'presto',
    'com.facebook.presto.jdbc.PrestoDriver',
    'jdbc:presto://localhost:18081/tpch/tiny',
    'admin',
    '',
    4,
    1,
    10000,
    FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM query_datasource_config WHERE name = 'presto_local'
);
