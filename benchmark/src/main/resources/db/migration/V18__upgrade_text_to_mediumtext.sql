-- V18: UPGRADE TEXT COLUMNS TO MEDIUMTEXT FOR LONG SQL SUPPORT
ALTER TABLE benchmark_sql_template MODIFY COLUMN sql_text MEDIUMTEXT;
ALTER TABLE benchmark_sql_template MODIFY COLUMN param_json MEDIUMTEXT;

ALTER TABLE benchmark_job MODIFY COLUMN jdbc_url MEDIUMTEXT;

ALTER TABLE benchmark_run MODIFY COLUMN error_sample MEDIUMTEXT;
ALTER TABLE benchmark_run MODIFY COLUMN job_snapshot_json MEDIUMTEXT;
ALTER TABLE benchmark_run MODIFY COLUMN evaluation_json MEDIUMTEXT;

ALTER TABLE benchmark_test_set_item MODIFY COLUMN sql_text MEDIUMTEXT;
ALTER TABLE benchmark_test_set_item MODIFY COLUMN param_json MEDIUMTEXT;

ALTER TABLE benchmark_data_source MODIFY COLUMN jdbc_url MEDIUMTEXT;
