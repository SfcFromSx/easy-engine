CREATE TABLE IF NOT EXISTS manager_sql_execution_record (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    received_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    raw_payload       MEDIUMTEXT,
    datasource_name   VARCHAR(256),
    datasource_type   VARCHAR(64),
    original_sql      MEDIUMTEXT,
    clean_sql         MEDIUMTEXT,
    param_fingerprint VARCHAR(512),
    parameter_payload MEDIUMTEXT,
    execution_mode    VARCHAR(32),
    success           BOOLEAN,
    cache_hit         BOOLEAN,
    cache_key         VARCHAR(1024),
    duration_ms       BIGINT,
    error_message     MEDIUMTEXT,
    parse_status      VARCHAR(32) NOT NULL,
    parse_error       MEDIUMTEXT,
    signature_json    MEDIUMTEXT,
    sql_fingerprint   VARCHAR(64)
);

CREATE INDEX idx_sql_exec_received ON manager_sql_execution_record (received_at);
CREATE INDEX idx_sql_exec_fingerprint ON manager_sql_execution_record (sql_fingerprint);
CREATE INDEX idx_sql_exec_ds ON manager_sql_execution_record (datasource_name);
CREATE INDEX idx_sql_exec_cache_hit_key
    ON manager_sql_execution_record (cache_hit, cache_key /*!80000 (191) */);

CREATE TABLE IF NOT EXISTS manager_acceleration_table (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name                VARCHAR(256) NOT NULL,
    schema_name         VARCHAR(256) NOT NULL DEFAULT 'public',
    ddl_text            MEDIUMTEXT NOT NULL,
    refresh_sql         MEDIUMTEXT,
    cron_expr           VARCHAR(128),
    status              VARCHAR(32) NOT NULL,
    source              VARCHAR(32) NOT NULL,
    recommendation_note MEDIUMTEXT
);

CREATE UNIQUE INDEX uq_accel_name_schema ON manager_acceleration_table (name, schema_name);

CREATE TABLE IF NOT EXISTS manager_sql_pattern_stats (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    sql_fingerprint  VARCHAR(64) NOT NULL,
    clean_sql_sample MEDIUMTEXT,
    execution_count  BIGINT NOT NULL DEFAULT 0,
    last_seen_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    avg_duration_ms  DOUBLE,
    signature_json   MEDIUMTEXT
);

CREATE UNIQUE INDEX uq_pattern_fingerprint ON manager_sql_pattern_stats (sql_fingerprint);
