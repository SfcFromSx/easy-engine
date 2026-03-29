UPDATE benchmark_data_source
SET jdbc_url = replace(jdbc_url,
                       'jdbc:kylin-cached://localhost:7070/',
                       'jdbc:kylin-cached://127.0.0.1:8092/')
WHERE driver_class = 'com.kylin.CachedKylinDriver'
  AND jdbc_url LIKE 'jdbc:kylin-cached://localhost:7070/%';
