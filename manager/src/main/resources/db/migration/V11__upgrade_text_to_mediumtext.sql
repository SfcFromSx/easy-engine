-- V11: REBUILD LEGACY TEXT TABLES TO MEDIUMTEXT WITHOUT PATCH-STYLE MODIFY COLUMN

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.TABLES
        WHERE LOWER(table_schema) = LOWER(DATABASE())
          AND LOWER(table_name) = 'manager_sql_pattern_stats__v11_old'
    ),
    'DROP TABLE manager_sql_pattern_stats__v11_old',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @rebuild_pattern_stats = (
    SELECT CASE
        WHEN EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.TABLES
            WHERE LOWER(table_schema) = LOWER(DATABASE())
              AND LOWER(table_name) = 'manager_sql_pattern_stats'
        ) AND (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE LOWER(table_schema) = LOWER(DATABASE())
              AND LOWER(table_name) = 'manager_sql_pattern_stats'
              AND LOWER(column_name) IN ('clean_sql_sample', 'signature_json')
              AND LOWER(data_type) = 'mediumtext'
        ) <> 2
        THEN 1 ELSE 0
    END
);
SET @sql = IF(@rebuild_pattern_stats = 1,
    'RENAME TABLE manager_sql_pattern_stats TO manager_sql_pattern_stats__v11_old',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = IF(@rebuild_pattern_stats = 1,
    'CREATE TABLE manager_sql_pattern_stats (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        sql_fingerprint VARCHAR(64) NOT NULL,
        clean_sql_sample MEDIUMTEXT,
        execution_count BIGINT NOT NULL DEFAULT 0,
        last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        avg_duration_ms DOUBLE,
        signature_json MEDIUMTEXT
    )',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = IF(@rebuild_pattern_stats = 1,
    'INSERT INTO manager_sql_pattern_stats (
        id,
        sql_fingerprint,
        clean_sql_sample,
        execution_count,
        last_seen_at,
        avg_duration_ms,
        signature_json
    )
    SELECT
        id,
        sql_fingerprint,
        clean_sql_sample,
        execution_count,
        last_seen_at,
        avg_duration_ms,
        signature_json
    FROM manager_sql_pattern_stats__v11_old',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = IF(@rebuild_pattern_stats = 1,
    'CREATE UNIQUE INDEX uq_pattern_fingerprint ON manager_sql_pattern_stats (sql_fingerprint)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = IF(@rebuild_pattern_stats = 1,
    'DROP TABLE manager_sql_pattern_stats__v11_old',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.TABLES
        WHERE LOWER(table_schema) = LOWER(DATABASE())
          AND LOWER(table_name) = 'manager_acceleration_table__v11_old'
    ),
    'DROP TABLE manager_acceleration_table__v11_old',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @rebuild_acceleration_table = (
    SELECT CASE
        WHEN EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.TABLES
            WHERE LOWER(table_schema) = LOWER(DATABASE())
              AND LOWER(table_name) = 'manager_acceleration_table'
        ) AND (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE LOWER(table_schema) = LOWER(DATABASE())
              AND LOWER(table_name) = 'manager_acceleration_table'
              AND LOWER(column_name) IN ('ddl_text', 'refresh_sql', 'recommendation_note')
              AND LOWER(data_type) = 'mediumtext'
        ) <> 3
        THEN 1 ELSE 0
    END
);
SET @sql = IF(@rebuild_acceleration_table = 1,
    'RENAME TABLE manager_acceleration_table TO manager_acceleration_table__v11_old',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = IF(@rebuild_acceleration_table = 1,
    'CREATE TABLE manager_acceleration_table (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        name VARCHAR(256) NOT NULL,
        schema_name VARCHAR(256) NOT NULL DEFAULT ''public'',
        ddl_text MEDIUMTEXT NOT NULL,
        refresh_sql MEDIUMTEXT,
        cron_expr VARCHAR(128),
        status VARCHAR(32) NOT NULL,
        source VARCHAR(32) NOT NULL,
        recommendation_note MEDIUMTEXT
    )',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = IF(@rebuild_acceleration_table = 1,
    'INSERT INTO manager_acceleration_table (
        id,
        created_at,
        updated_at,
        name,
        schema_name,
        ddl_text,
        refresh_sql,
        cron_expr,
        status,
        source,
        recommendation_note
    )
    SELECT
        id,
        created_at,
        updated_at,
        name,
        schema_name,
        ddl_text,
        refresh_sql,
        cron_expr,
        status,
        source,
        recommendation_note
    FROM manager_acceleration_table__v11_old',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = IF(@rebuild_acceleration_table = 1,
    'CREATE UNIQUE INDEX uq_accel_name_schema ON manager_acceleration_table (name, schema_name)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = IF(@rebuild_acceleration_table = 1,
    'DROP TABLE manager_acceleration_table__v11_old',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.TABLES
        WHERE LOWER(table_schema) = LOWER(DATABASE())
          AND LOWER(table_name) = 'manager_query_datasource_config__v11_old'
    ),
    'DROP TABLE manager_query_datasource_config__v11_old',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @rebuild_datasource_config = (
    SELECT CASE
        WHEN EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.TABLES
            WHERE LOWER(table_schema) = LOWER(DATABASE())
              AND LOWER(table_name) = 'manager_query_datasource_config'
        ) AND (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE LOWER(table_schema) = LOWER(DATABASE())
              AND LOWER(table_name) = 'manager_query_datasource_config'
              AND LOWER(column_name) = 'jdbc_url'
              AND LOWER(data_type) = 'mediumtext'
        ) <> 1
        THEN 1 ELSE 0
    END
);
SET @sql = IF(@rebuild_datasource_config = 1,
    'RENAME TABLE manager_query_datasource_config TO manager_query_datasource_config__v11_old',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = IF(@rebuild_datasource_config = 1,
    'CREATE TABLE manager_query_datasource_config (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(256) NOT NULL,
        type VARCHAR(64) NOT NULL,
        driver_class VARCHAR(512) NOT NULL,
        jdbc_url MEDIUMTEXT NOT NULL,
        username VARCHAR(256),
        password VARCHAR(256),
        max_pool_size INTEGER NOT NULL DEFAULT 4,
        min_idle INTEGER NOT NULL DEFAULT 1,
        connection_timeout_ms BIGINT NOT NULL DEFAULT 10000,
        is_default BOOLEAN NOT NULL DEFAULT FALSE,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    )',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = IF(@rebuild_datasource_config = 1,
    'INSERT INTO manager_query_datasource_config (
        id,
        name,
        type,
        driver_class,
        jdbc_url,
        username,
        password,
        max_pool_size,
        min_idle,
        connection_timeout_ms,
        is_default,
        created_at,
        updated_at
    )
    SELECT
        id,
        name,
        type,
        driver_class,
        jdbc_url,
        username,
        password,
        max_pool_size,
        min_idle,
        connection_timeout_ms,
        is_default,
        created_at,
        updated_at
    FROM manager_query_datasource_config__v11_old',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = IF(@rebuild_datasource_config = 1,
    'CREATE UNIQUE INDEX uq_query_datasource_name ON manager_query_datasource_config (name)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = IF(@rebuild_datasource_config = 1,
    'DROP TABLE manager_query_datasource_config__v11_old',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
