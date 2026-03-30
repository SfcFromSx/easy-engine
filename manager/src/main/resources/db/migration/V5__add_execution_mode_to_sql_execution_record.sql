ALTER TABLE sql_execution_record
    ADD COLUMN IF NOT EXISTS execution_mode VARCHAR(32);
