# Query Module

`query` is the Easy Engine query execution service. It accepts read-only query traffic via the standard Apache Kylin JDBC protocol, parses comments and preserved metadata, routes to the selected datasource, manages Redis-backed result cache, and writes trace records directly to PostgreSQL.

## Responsibilities

- Serve `POST /kylin/api/query`.
- Preserve compatibility with `PreparedQueryRequest` and `SQLResponseStub` expectations.
- Define the routing, caching, preserved metadata, and trace semantics that adapter layers must preserve.
- Prefer preserved routing metadata such as `YH_TARGET_ENGINE` over driver-consumed engine hints.
- Publish trace payloads with explicit `executionMode` values so downstream operators can distinguish `STATEMENT` from `PREPARED_STATEMENT` without SQL-text inspection.
- Publish a dedicated `parameterPayload` field for failed prepared executions, derived from the submitted `params` DTOs as readable JSON text so operators can debug bindings without stack-trace scraping or `rawPayload` inspection.

## Ownership Boundary

`query` owns:

- routing precedence across preserved metadata, surviving driver hints, and datasource fallback
- cache semantics for deciding hits, misses, and datasource execution
- preserved metadata handling that must remain visible after any adapter handoff
- trace and execution metadata contracts such as `executionMode` and failed-prepared `parameterPayload`

## Trace Contract Notes

- `parameterPayload` is only emitted for failed `PREPARED_STATEMENT` traces in this task; statement executions and successful prepared executions keep it `null` or omit it.
- The payload is sourced from `PreparedQueryRequestDto.getParams()` / `StatementParameterDto` values only.
- This task does not change JDBC binding, SQL interpolation, or driver code.

## Routing Notes

Query-side routing precedence is:

1. preserved metadata comments such as `YH_TARGET_ENGINE`
2. driver-style engine hints that still reach `query`
3. the default datasource fallback

- The standard benchmark path is `benchmark (Kylin JDBC) -> query`.
- When a JDBC client must select a specific engine, preserved metadata comments are the reliable mechanism:

```sql
/* YH_TARGET_ENGINE=presto_local */
SELECT count(*) FROM nation
```

## Run

```bash
cd /Users/sfc/Documents/projects/engine/query
mvn spring-boot:run
```

## Verify

```bash
mvn -q -f query/pom.xml test
```

## Related Docs

- [docs/architecture/overview.md](/Users/sfc/Documents/projects/engine/docs/architecture/overview.md)
- [docs/architecture/service-capabilities.md](/Users/sfc/Documents/projects/engine/docs/architecture/service-capabilities.md)
- [docs/operations/local-development.md](/Users/sfc/Documents/projects/engine/docs/operations/local-development.md)
