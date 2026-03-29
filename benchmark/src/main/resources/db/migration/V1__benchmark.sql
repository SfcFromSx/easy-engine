CREATE TABLE benchmark_sql_template (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(256) NOT NULL,
    sql_text    TEXT NOT NULL,
    weight      INT NOT NULL DEFAULT 1,
    description VARCHAR(512)
);

CREATE TABLE benchmark_job (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(256) NOT NULL,
    jdbc_url            TEXT NOT NULL,
    jdbc_user           VARCHAR(256),
    jdbc_password       VARCHAR(256),
    driver_class        VARCHAR(512) NOT NULL,
    concurrent_threads  INT NOT NULL DEFAULT 4,
    rounds              INT NOT NULL DEFAULT 100,
    strategy            VARCHAR(64) NOT NULL DEFAULT 'RANDOM_WEIGHT',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE benchmark_run (
    id              BIGSERIAL PRIMARY KEY,
    job_id          BIGINT NOT NULL REFERENCES benchmark_job(id),
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at        TIMESTAMPTZ,
    status          VARCHAR(32) NOT NULL,
    total_queries   BIGINT,
    success_count   BIGINT,
    error_count     BIGINT,
    duration_ms     BIGINT,
    qps             DOUBLE PRECISION,
    p50_ms          DOUBLE PRECISION,
    p95_ms          DOUBLE PRECISION,
    p99_ms          DOUBLE PRECISION,
    error_sample    TEXT
);
