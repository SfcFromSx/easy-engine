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

## Service Startup Order

1. Start `query`:

```bash
cd /Users/sfc/Documents/projects/engine/query
mvn spring-boot:run
```

2. Start `manager`:

```bash
cd /Users/sfc/Documents/projects/engine/manager
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

3. Start `benchmark`:

```bash
cd /Users/sfc/Documents/projects/engine/benchmark
mvn spring-boot:run
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
- The default metadata store is MySQL on `localhost:3307`; the shared local credentials remain `engine` / `engine123`.
- Preserved metadata comments (e.g. `YH_TARGET_ENGINE`) in SQL are used to route requests to specific backends within `query`.
- If Flyway reports a checksum mismatch after editing an applied migration during local work, repair and migrate explicitly:

```bash
mvn -f benchmark/pom.xml compile flyway:repair flyway:migrate
```

## Default Ports

- `manager`: `8090`
- `benchmark`: `8091`
- `query`: `8092`
- Redis: `6380`
- MySQL: `3307`
- Kylin: `17070`
- Presto: `18081`
