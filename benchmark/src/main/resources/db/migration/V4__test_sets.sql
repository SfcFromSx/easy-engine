CREATE TABLE benchmark_test_set (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name            VARCHAR(512) NOT NULL,
    description     VARCHAR(1024),
    source_filename VARCHAR(512)
);

CREATE TABLE benchmark_test_set_item (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_set_id    BIGINT NOT NULL REFERENCES benchmark_test_set(id) ON DELETE CASCADE,
    sort_order     INT NOT NULL DEFAULT 0,
    sql_lib_id     BIGINT,
    label          VARCHAR(512),
    sql_text       MEDIUMTEXT NOT NULL,
    weight         INT NOT NULL DEFAULT 1,
    execution_mode VARCHAR(32) NOT NULL DEFAULT 'STATEMENT',
    param_json     MEDIUMTEXT
);

CREATE INDEX idx_test_set_item_set ON benchmark_test_set_item (test_set_id, sort_order);
CREATE INDEX idx_test_set_item_sql_lib ON benchmark_test_set_item (sql_lib_id);

ALTER TABLE benchmark_job ADD COLUMN IF NOT EXISTS test_set_id BIGINT NULL REFERENCES benchmark_test_set(id) ON DELETE SET NULL;
