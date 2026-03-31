UPDATE benchmark_job job
JOIN benchmark_data_source current_ds
  ON current_ds.id = job.data_source_id
JOIN (
    SELECT MIN(id) AS keep_id,
           jdbc_url,
           COALESCE(jdbc_user, '') AS jdbc_user_key,
           COALESCE(jdbc_password, '') AS jdbc_password_key,
           driver_class
    FROM benchmark_data_source
    GROUP BY jdbc_url, COALESCE(jdbc_user, ''), COALESCE(jdbc_password, ''), driver_class
) canonical
  ON canonical.jdbc_url = current_ds.jdbc_url
 AND canonical.jdbc_user_key = COALESCE(current_ds.jdbc_user, '')
 AND canonical.jdbc_password_key = COALESCE(current_ds.jdbc_password, '')
 AND canonical.driver_class = current_ds.driver_class
SET job.data_source_id = canonical.keep_id
WHERE job.data_source_id <> canonical.keep_id;

UPDATE benchmark_data_source
SET name = 'engine-query-default'
WHERE id = (
    SELECT keep_id
    FROM (
        SELECT MIN(id) AS keep_id
        FROM benchmark_data_source
        WHERE jdbc_url = 'jdbc:kylin://127.0.0.1:8092/learn_kylin'
          AND COALESCE(jdbc_user, '') = 'ADMIN'
          AND COALESCE(jdbc_password, '') = 'KYLIN'
          AND driver_class = 'org.apache.kylin.jdbc.Driver'
    ) seeded
);

DELETE duplicate_ds
FROM benchmark_data_source duplicate_ds
JOIN benchmark_data_source canonical_ds
  ON duplicate_ds.jdbc_url = canonical_ds.jdbc_url
 AND COALESCE(duplicate_ds.jdbc_user, '') = COALESCE(canonical_ds.jdbc_user, '')
 AND COALESCE(duplicate_ds.jdbc_password, '') = COALESCE(canonical_ds.jdbc_password, '')
 AND duplicate_ds.driver_class = canonical_ds.driver_class
 AND duplicate_ds.id > canonical_ds.id
LEFT JOIN benchmark_job job
  ON job.data_source_id = duplicate_ds.id
WHERE job.id IS NULL;
