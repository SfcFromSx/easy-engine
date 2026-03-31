CREATE TABLE benchmark_sql_template (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(256) NOT NULL,
    sql_text    TEXT NOT NULL,
    weight      INT NOT NULL DEFAULT 1,
    description VARCHAR(512)
);

CREATE TABLE benchmark_job (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(256) NOT NULL,
    jdbc_url            TEXT NOT NULL,
    jdbc_user           VARCHAR(256),
    jdbc_password       VARCHAR(256),
    driver_class        VARCHAR(512) NOT NULL,
    concurrent_threads  INT NOT NULL DEFAULT 4,
    rounds              INT NOT NULL DEFAULT 100,
    strategy            VARCHAR(64) NOT NULL DEFAULT 'RANDOM_WEIGHT',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE benchmark_run (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id          BIGINT NOT NULL REFERENCES benchmark_job(id),
    started_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at        TIMESTAMP,
    status          VARCHAR(32) NOT NULL,
    total_queries   BIGINT,
    success_count   BIGINT,
    error_count     BIGINT,
    duration_ms     BIGINT,
    qps             DOUBLE,
    p50_ms          DOUBLE,
    p95_ms          DOUBLE,
    p99_ms          DOUBLE,
    error_sample    TEXT
);
