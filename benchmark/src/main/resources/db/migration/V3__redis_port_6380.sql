UPDATE benchmark_job
SET jdbc_url = REPLACE(jdbc_url, 'redis.port=6379', 'redis.port=6380')
WHERE jdbc_url LIKE '%redis.port=6379%';
