UPDATE benchmark_data_source
SET jdbc_url = replace(replace(jdbc_url,
                               'jdbc:kylin-cached://localhost:17070/',
                               'jdbc:kylin-cached://127.0.0.1:8092/'),
                       'jdbc:kylin-cached://127.0.0.1:17070/',
                       'jdbc:kylin-cached://127.0.0.1:8092/')
WHERE driver_class = 'com.kylin.CachedKylinDriver';

UPDATE benchmark_data_source
SET jdbc_url = CASE
    WHEN position('datasource.routing.enabled=' in jdbc_url) > 0
        THEN regexp_replace(jdbc_url, 'datasource\.routing\.enabled=[^&]+', 'datasource.routing.enabled=false')
    ELSE jdbc_url || '&datasource.routing.enabled=false'
END
WHERE driver_class = 'com.kylin.CachedKylinDriver'
  AND jdbc_url LIKE 'jdbc:kylin-cached://%';
