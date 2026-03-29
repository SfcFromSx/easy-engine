-- 1. Create benchmark_data_source table
CREATE TABLE IF NOT EXISTS benchmark_data_source (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    jdbc_url TEXT NOT NULL,
    jdbc_user VARCHAR(255),
    jdbc_password VARCHAR(255),
    driver_class VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Add data_source_id to benchmark_job
ALTER TABLE benchmark_job ADD COLUMN data_source_id INTEGER;

-- 3. Migrate existing JDBC data from jobs to datasources
-- We create one datasource for each distinct JDBC URL/User/Driver combo
INSERT INTO benchmark_data_source (name, jdbc_url, jdbc_user, jdbc_password, driver_class)
SELECT 
    'DataSource for ' || name, 
    jdbc_url, 
    jdbc_user, 
    jdbc_password, 
    driver_class
FROM benchmark_job;

-- 4. Update benchmark_job with the new data_source_id
-- Simple mapping: ID in datasource matches row ID in jobs for this initial migration
UPDATE benchmark_job j
SET data_source_id = ds.id
FROM benchmark_data_source ds
WHERE ds.jdbc_url = j.jdbc_url 
  AND ds.name = 'DataSource for ' || j.name;

-- 5. Now we can safely remove columns from benchmark_job (optional, but keep for now or drop)
-- For the entity to work, we must at least remove them from the domain model (already done).
-- We'll drop them from the schema to be clean.
ALTER TABLE benchmark_job DROP COLUMN jdbc_url;
ALTER TABLE benchmark_job DROP COLUMN jdbc_user;
ALTER TABLE benchmark_job DROP COLUMN jdbc_password;
ALTER TABLE benchmark_job DROP COLUMN driver_class;
