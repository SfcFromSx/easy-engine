# Query Module

`query` is the Easy Engine query-only execution service. It accepts read-only query traffic, parses comments and preserved metadata, routes to the selected datasource, manages Redis-backed cache behavior, and publishes traces for `manager`.

## Responsibilities

- Serve `POST /kylin/api/query`.
- Preserve compatibility with `PreparedQueryRequest` and `SQLResponseStub` expectations.
- Align routing, caching, and trace semantics with `kylin-jdbc-cache`.
- Prefer preserved routing metadata such as `YH_TARGET_ENGINE` over driver-consumed engine hints.
- Publish trace payloads with explicit `executionMode` values so downstream operators can distinguish `STATEMENT` from `PREPARED_STATEMENT` without SQL-text inspection.
- Publish a dedicated `parameterPayload` field for failed prepared executions, derived from the submitted `params` DTOs as readable JSON text so operators can debug bindings without stack-trace scraping or `rawPayload` inspection.

## Trace Contract Notes

- `parameterPayload` is only emitted for failed `PREPARED_STATEMENT` traces in this task; statement executions and successful prepared executions keep it `null` or omit it.
- The payload is sourced from `PreparedQueryRequestDto.getParams()` / `StatementParameterDto` values only.
- This task does not change JDBC binding, SQL interpolation, or driver code.

## Routing Notes

- The standard benchmark path is `benchmark -> kylin-jdbc-cache -> query`.
- In that path, driver hints such as `-- engine:presto_local` may be consumed before they reach `query`.
- To guarantee query-side routing, use preserved metadata comments such as:

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
