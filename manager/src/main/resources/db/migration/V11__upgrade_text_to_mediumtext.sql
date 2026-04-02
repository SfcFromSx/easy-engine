-- V11: UPGRADE TEXT COLUMNS TO MEDIUMTEXT FOR LONG SQL SUPPORT
ALTER TABLE manager_sql_execution_record MODIFY COLUMN raw_payload MEDIUMTEXT;
ALTER TABLE manager_sql_execution_record MODIFY COLUMN original_sql MEDIUMTEXT;
ALTER TABLE manager_sql_execution_record MODIFY COLUMN clean_sql MEDIUMTEXT;
ALTER TABLE manager_sql_execution_record MODIFY COLUMN parameter_payload MEDIUMTEXT;
ALTER TABLE manager_sql_execution_record MODIFY COLUMN error_message MEDIUMTEXT;
ALTER TABLE manager_sql_execution_record MODIFY COLUMN parse_error MEDIUMTEXT;
ALTER TABLE manager_sql_execution_record MODIFY COLUMN signature_json MEDIUMTEXT;

ALTER TABLE manager_sql_pattern_stats MODIFY COLUMN clean_sql_sample MEDIUMTEXT;
ALTER TABLE manager_sql_pattern_stats MODIFY COLUMN signature_json MEDIUMTEXT;

ALTER TABLE manager_acceleration_table MODIFY COLUMN ddl_text MEDIUMTEXT;
ALTER TABLE manager_acceleration_table MODIFY COLUMN refresh_sql MEDIUMTEXT;
ALTER TABLE manager_acceleration_table MODIFY COLUMN recommendation_note MEDIUMTEXT;

ALTER TABLE manager_query_datasource_config MODIFY COLUMN jdbc_url MEDIUMTEXT;
