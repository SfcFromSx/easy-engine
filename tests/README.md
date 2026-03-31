# Engine E2E Tests

Full-coverage end-to-end test suite for the Easy Engine three-service stack.
Tests cover the complete data path: benchmark → query → Kylin/Presto, Redis cache,
MySQL trace records, and the manager control-plane API.

## Prerequisites

All services must be running before executing the tests:

```bash
# Infrastructure + OLAP engines
docker compose up -d mysql redis
docker compose --profile olap up -d kylin presto

# Wait for Kylin to finish starting (~5–10 min first boot)
# The local Kylin image also pins Spark to local[2] so auth becomes ready
# without waiting on the embedded standalone YARN sparder bootstrap.
# Then start the three Java services:
cd query   && mvn spring-boot:run &
cd manager && mvn spring-boot:run -Dspring-boot.run.profiles=dev &
cd benchmark && mvn spring-boot:run &
```

## Running the tests

```bash
mvn -f tests/pom.xml test
```

Override service endpoints if not using default ports:

```bash
mvn -f tests/pom.xml test \
  -De2e.query.url=http://localhost:8092 \
  -De2e.manager.url=http://localhost:8090 \
  -De2e.mysql.host=localhost -De2e.mysql.port=3307 \
  -De2e.mysql.url=jdbc:mysql://localhost:3307/engine_db?useSSL=false\&allowPublicKeyRetrieval=true\&serverTimezone=UTC \
  -De2e.redis.host=localhost -De2e.redis.port=6380 \
  -De2e.kylin.host=localhost -De2e.kylin.port=17070 \
  -De2e.presto.url=jdbc:presto://localhost:18081/tpch/tiny
```

## Test classes

| Class | What it covers |
|---|---|
| `MysqlInfraE2ETest` | MySQL connectivity, schema correctness, Flyway migrations, seed data |
| `QueryHttpE2ETest` | Statement, prepared-statement, invalid SQL, DML rejection, YH_TARGET_ENGINE routing |
| `TraceRecordE2ETest` | MySQL trace record fields, executionMode, failed query trace, manager API, pattern stats |
| `CacheE2ETest` | Redis cache population, cache hit (storageCacheUsed=true), datasource isolation, key prefix |
| `ManagerApiE2ETest` | Datasource config CRUD, trace browsing, stats summary, acceleration table lifecycle |
| `KylinJdbcE2ETest` | Kylin JDBC connection to query service, Statement, PreparedStatement, metadata, trace write |
| `PrestoRoutingE2ETest` | Presto routing via hint, trace datasource_name, direct Presto JDBC, count match, fallback |

## Evidence collected per test run

- HTTP response fields: `isException`, `storageCacheUsed`, `columnMetas`, `results`, `duration`
- MySQL rows: `sql_execution_record` (executionMode, success, duration_ms, datasource_name, cache_hit, error_message)
- MySQL rows: `sql_pattern_stats` (execution_count increments)
- MySQL schema: expected manager and benchmark Flyway history tables present and clean
- Redis: cache keys created with correct prefix, PING reachable
- Kylin REST: `/kylin/api/user/authentication` returns 200
- Presto JDBC: direct count matches count routed through query service
