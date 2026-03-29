# Benchmark Module

`benchmark` manages benchmark datasources, jobs, templates, test sets, and execution runs. It is the preferred operator interface for benchmark-driven validation of the cached JDBC path.

## Responsibilities

- Manage benchmark datasources.
- Manage SQL templates with statement and prepared execution modes.
- Manage test sets and Excel imports.
- Launch benchmark runs and store structured run reports.
- Expose preflight checks for Kylin and Presto dependencies.

## Standard Path

```text
benchmark -> kylin-jdbc-cache -> query -> Kylin or Presto
```

## Prepared Execution Notes

- `execution_mode` supports `STATEMENT` and `PREPARED_STATEMENT`.
- `param_json` carries prepared parameter arrays.
- Benchmark runs support prepared execution more fully than the datasource debug endpoint.

## Run

```bash
docker compose up -d postgres redis
cd /Users/sfc/Documents/projects/engine/query && mvn spring-boot:run
cd /Users/sfc/Documents/projects/engine/manager && mvn spring-boot:run
cd /Users/sfc/Documents/projects/engine/benchmark && mvn spring-boot:run
cd /Users/sfc/Documents/projects/engine/benchmark/frontend && npm run dev
```

## Verify

```bash
mvn -q -f benchmark/pom.xml test
npm --prefix benchmark/frontend run build
```

## Related Docs

- [docs/architecture/overview.md](/Users/sfc/Documents/projects/engine/docs/architecture/overview.md)
- [docs/operations/local-development.md](/Users/sfc/Documents/projects/engine/docs/operations/local-development.md)
- [benchmark/README.md](/Users/sfc/Documents/projects/engine/benchmark/README.md)
