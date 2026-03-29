# Easy Engine Benchmark

Easy Engine Benchmark manages benchmark datasources, SQL templates, test sets, and benchmark runs.

## Standard Path

```text
benchmark -> kylin-jdbc-cache -> query -> Kylin or Presto
```

## Key Notes

- jobs reference `BenchmarkDataSource`,
- benchmark supports both `STATEMENT` and `PREPARED_STATEMENT`,
- prepared parameters are stored in `param_json`,
- query-side routing should use preserved metadata such as `YH_TARGET_ENGINE` when the request must survive the cached JDBC adapter.

## Local Run

```bash
docker compose up -d postgres redis
mvn -f /Users/sfc/Documents/projects/engine/kylin-jdbc-cache/pom.xml install
cd /Users/sfc/Documents/projects/engine/query && mvn spring-boot:run
cd /Users/sfc/Documents/projects/engine/manager && mvn spring-boot:run
cd /Users/sfc/Documents/projects/engine/benchmark && mvn spring-boot:run
cd /Users/sfc/Documents/projects/engine/benchmark/frontend && npm run dev
```

## Verification

```bash
mvn -q -f /Users/sfc/Documents/projects/engine/benchmark/pom.xml test
npm --prefix /Users/sfc/Documents/projects/engine/benchmark/frontend run build
```

## Canonical Docs

- [docs/modules/benchmark.md](/Users/sfc/Documents/projects/engine/docs/modules/benchmark.md)
- [docs/operations/local-development.md](/Users/sfc/Documents/projects/engine/docs/operations/local-development.md)
