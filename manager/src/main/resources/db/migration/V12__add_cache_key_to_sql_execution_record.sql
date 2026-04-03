ALTER TABLE manager_sql_execution_record
    ADD COLUMN cache_key VARCHAR(1024);

CREATE INDEX idx_sql_exec_cache_hit_key
    ON manager_sql_execution_record (cache_hit, cache_key /*!80000 (191) */);
