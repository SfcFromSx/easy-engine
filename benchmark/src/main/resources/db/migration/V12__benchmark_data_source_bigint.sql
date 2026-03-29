ALTER TABLE benchmark_data_source
    ALTER COLUMN id TYPE BIGINT;

ALTER TABLE benchmark_data_source
    ALTER COLUMN id SET DEFAULT nextval('benchmark_data_source_id_seq');

ALTER TABLE benchmark_job
    ALTER COLUMN data_source_id TYPE BIGINT;
