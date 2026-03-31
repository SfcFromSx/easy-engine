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
    sql_fingerprint VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_sql_exec_received ON sql_execution_record (received_at);
CREATE INDEX IF NOT EXISTS idx_sql_exec_fingerprint ON sql_execution_record (sql_fingerprint);
CREATE INDEX IF NOT EXISTS idx_sql_exec_ds ON sql_execution_record (datasource_name);

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

CREATE UNIQUE INDEX IF NOT EXISTS uq_accel_name_schema ON acceleration_table (name, schema_name);

CREATE TABLE IF NOT EXISTS sql_pattern_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sql_fingerprint VARCHAR(64) NOT NULL,
    clean_sql_sample TEXT,
    execution_count BIGINT NOT NULL DEFAULT 0,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    avg_duration_ms DOUBLE,
    signature_json TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_pattern_fingerprint ON sql_pattern_stats (sql_fingerprint);
