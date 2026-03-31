UPDATE benchmark_data_source
SET jdbc_url = regexp_replace(
        replace(
            replace(jdbc_url,
                    'jdbc:kylin-cached://localhost:17070/',
                    'jdbc:kylin://127.0.0.1:8092/'),
            'jdbc:kylin-cached://127.0.0.1:17070/',
            'jdbc:kylin://127.0.0.1:8092/'
        ),
        '([?&])(redis\.(host|port)|sql\.trace\.(enabled|redis\.enabled)|datasource\.routing\.enabled)=[^&]*',
        '',
        'g'
    ),
    driver_class = 'org.apache.kylin.jdbc.Driver'
WHERE driver_class = 'com.kylin.CachedKylinDriver';
