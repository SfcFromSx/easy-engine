# Local Development

This runbook describes the default local environment for Easy Engine.

## Infrastructure

Start shared infrastructure first:

```bash
docker compose up -d postgres redis
```

If you need OLAP engines locally:

```bash
docker compose --profile olap up -d presto kylin
```

## Service Startup Order

1. Install the cached JDBC adapter when benchmark depends on it:

```bash
mvn -f kylin-jdbc-cache/pom.xml install
```

2. Start `query`:

```bash
cd /Users/sfc/Documents/projects/engine/query
mvn spring-boot:run
```

3. Start `manager`:

```bash
cd /Users/sfc/Documents/projects/engine/manager
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

4. Start `benchmark`:

```bash
cd /Users/sfc/Documents/projects/engine/benchmark
mvn spring-boot:run
```

5. Start frontends as needed:

```bash
npm --prefix manager/frontend run dev -- --host 127.0.0.1 --port 4173
npm --prefix benchmark/frontend run dev
```

## Operator Notes

- Benchmark is the preferred control surface for benchmark runs, preflight checks, and structured run reports.
- The benchmark path disables driver-side routing for normal job execution, so preserved metadata comments should be used for routed `query` flows.
- If Flyway reports a checksum mismatch after editing an applied migration during local work, repair and migrate explicitly:

```bash
mvn -f benchmark/pom.xml compile flyway:repair flyway:migrate
```

## Default Ports

- `manager`: `8090`
- `benchmark`: `8091`
- `query`: `8092`
- Redis: `6380`
- PostgreSQL: `5433`
- Kylin: `17070`
- Presto: `18081`
