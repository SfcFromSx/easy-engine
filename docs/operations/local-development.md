# Local Development

This runbook describes the default local environment for Easy Engine.

## Infrastructure

Start shared infrastructure first:

```bash
docker compose up -d mysql redis
```

If you need OLAP engines locally:

```bash
docker compose --profile olap up -d presto kylin
```

The local Kylin image pins both engine and query Spark masters to `local[2]`.
That keeps the real Kylin container responsive for benchmark smoke and
Kylin-specific E2E runs instead of blocking on the standalone container's
embedded YARN `sparder_on_docker` bootstrap path. Easy Engine's `query`
service still only exposes `POST /kylin/api/query`.

## Database Initialization

Initialize the metadata schema explicitly before starting `manager` or `benchmark`:

```bash
bash scripts/init-db.sh dev
```

The init script runs the `manager` and `benchmark` Flyway migrations against the
selected profile's metadata database settings. Normal service startup no longer
creates tables or seeds rows automatically.

## Service Startup Order

1. Start `query`:

```bash
cd /Users/sfc/Documents/projects/engine/query
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

2. Start `manager`:

```bash
cd /Users/sfc/Documents/projects/engine/manager
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

3. Start `benchmark`:

```bash
cd /Users/sfc/Documents/projects/engine/benchmark
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

4. Start frontends as needed:

```bash
npm --prefix manager/frontend run dev -- --host 127.0.0.1 --port 5173
npm --prefix benchmark/frontend run dev
```

## Operator Notes

- There is no standalone JDBC adapter service in the active architecture. JDBC clients connect directly to `query`.
- Benchmark is the preferred control surface for benchmark runs, preflight checks, and structured run reports.
- Benchmark connects to `query` using the standard Apache Kylin JDBC driver (`jdbc:kylin://localhost:8092/<project>`). Upload additional JDBC driver JARs via the Benchmark UI under Data Sources > Upload Driver before running datasource tests or benchmark jobs that depend on them.
- Each backend module now owns `application-dev.yml`, `application-test.yml`, and `application-pro.yml`. Local startup uses `dev`, automated tests use `test`, test-environment containers should set `SPRING_PROFILES_ACTIVE=test`, and production should set `SPRING_PROFILES_ACTIVE=pro`.
- The default local metadata store is MySQL on `localhost:3307`; those local defaults now live only in the `dev` profile rather than Java, scripts, or Maven defaults.
- `manager` and `benchmark` now expect the metadata schema to be initialized already; if you skip `bash scripts/init-db.sh`, startup fails fast on missing tables instead of mutating the database during boot.
- Preserved metadata comments (e.g. `YH_TARGET_ENGINE`) in SQL are used to route requests to specific backends within `query`.
- If Flyway reports a checksum mismatch after editing an applied migration during local work, repair and migrate explicitly:

```bash
SPRING_PROFILES_ACTIVE=dev mvn -f manager/pom.xml -Dflyway.url="$MANAGER_DB_JDBC_URL" -Dflyway.user="$MANAGER_DB_USER" -Dflyway.password="$MANAGER_DB_PASSWORD" flyway:repair
SPRING_PROFILES_ACTIVE=dev mvn -f benchmark/pom.xml -Dflyway.url="$BENCHMARK_DB_JDBC_URL" -Dflyway.user="$BENCHMARK_DB_USER" -Dflyway.password="$BENCHMARK_DB_PASSWORD" flyway:repair
bash scripts/init-db.sh dev
```

## Default Ports

- `manager`: `8090`
- `benchmark`: `8091`
- `query`: `8092`
- Redis: `6380`
- MySQL: `3307`
- Kylin: `17070`
- Presto: `18081`
