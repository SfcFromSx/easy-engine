# Query Module

`query` is the Easy Engine query-only execution service. It accepts read-only query traffic, parses comments and preserved metadata, routes to the selected datasource, manages Redis-backed cache behavior, and publishes traces for `manager`. It is also the long-term source of truth for execution semantics, even when JDBC traffic is fronted by `kylin-jdbc-cache`.

## Responsibilities

- Serve `POST /kylin/api/query`.
- Preserve compatibility with `PreparedQueryRequest` and `SQLResponseStub` expectations.
- Define the routing, caching, preserved metadata, and trace semantics that adapter layers must preserve.
- Prefer preserved routing metadata such as `YH_TARGET_ENGINE` over driver-consumed engine hints.
- Publish trace payloads with explicit `executionMode` values so downstream operators can distinguish `STATEMENT` from `PREPARED_STATEMENT` without SQL-text inspection.
- Publish a dedicated `parameterPayload` field for failed prepared executions, derived from the submitted `params` DTOs as readable JSON text so operators can debug bindings without stack-trace scraping or `rawPayload` inspection.

## Ownership Boundary

This task documents the migration boundary only. It does not move behavior and does not require JDBC code changes.

`query` continues to own:

- routing precedence across preserved metadata, surviving driver hints, and datasource fallback
- cache semantics for deciding hits, misses, and datasource execution
- preserved metadata handling that must remain visible after any adapter handoff
- trace and execution metadata contracts such as `executionMode` and failed-prepared `parameterPayload`

`kylin-jdbc-cache` remains responsible for JDBC compatibility work:

- consuming driver-facing hints before forwarding SQL when needed for JDBC callers
- adapter-specific JDBC request, response, and fallback behavior
- compatibility shims that front `query` without redefining query-owned semantics

## Trace Contract Notes

- `parameterPayload` is only emitted for failed `PREPARED_STATEMENT` traces in this task; statement executions and successful prepared executions keep it `null` or omit it.
- The payload is sourced from `PreparedQueryRequestDto.getParams()` / `StatementParameterDto` values only.
- This task does not change JDBC binding, SQL interpolation, or driver code.

## Routing Notes

Query-side routing precedence is:

1. preserved metadata comments such as `YH_TARGET_ENGINE`
2. driver-style engine hints that still reach `query`
3. the default datasource fallback

- The standard benchmark path is `benchmark -> kylin-jdbc-cache -> query`.
- In that path, cached-JDBC processing can consume or strip driver hints such as `-- engine:presto_local` before the request reaches `query`; that hint consumption remains an adapter concern, not the owner of routing semantics.
- When a benchmark or JDBC path must select a specific engine at the `query` layer, preserved metadata comments are the reliable mechanism because they survive that handoff:

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
