CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INT NOT NULL PRIMARY KEY,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INT NOT NULL,
    success BOOLEAN NOT NULL
);

INSERT INTO flyway_schema_history (
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    execution_time,
    success
) VALUES (
    1,
    '8',
    '<< Flyway Baseline >>',
    'BASELINE',
    '<< Flyway Baseline >>',
    NULL,
    'sa',
    0,
    TRUE
);

CREATE TABLE IF NOT EXISTS sql_execution_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    raw_payload TEXT,
    datasource_name VARCHAR(256),
    datasource_type VARCHAR(64),
    original_sql TEXT,
    clean_sql TEXT,
    param_fingerprint VARCHAR(512),
    success BOOLEAN,
    cache_hit BOOLEAN,
    duration_ms BIGINT,
    error_message TEXT,
    parse_status VARCHAR(32) NOT NULL,
    parse_error TEXT,
    signature_json TEXT,
    sql_fingerprint VARCHAR(64),
    execution_mode VARCHAR(32),
    parameter_payload TEXT
);

CREATE TABLE IF NOT EXISTS acceleration_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name VARCHAR(256) NOT NULL,
    schema_name VARCHAR(256) NOT NULL DEFAULT 'public',
    ddl_text TEXT NOT NULL,
    refresh_sql TEXT,
    cron_expr VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    source VARCHAR(32) NOT NULL,
    recommendation_note TEXT
);

CREATE TABLE IF NOT EXISTS sql_pattern_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sql_fingerprint VARCHAR(64) NOT NULL,
    clean_sql_sample TEXT,
    execution_count BIGINT NOT NULL DEFAULT 0,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    avg_duration_ms DOUBLE,
    signature_json TEXT
);

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
) VALUES (
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
);
