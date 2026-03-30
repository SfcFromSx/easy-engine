ALTER TABLE sql_execution_record
    ADD COLUMN IF NOT EXISTS parameter_payload TEXT;
