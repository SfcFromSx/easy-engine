# Query Module

`query` is the Easy Engine query execution service. It accepts read-only query traffic via the standard Apache Kylin JDBC protocol, parses comments and preserved metadata, routes to the selected datasource, manages Redis-backed result cache, and writes trace records directly to MySQL.

## Responsibilities

- Serve `POST /kylin/api/query`.
- Preserve compatibility with `PreparedQueryRequest` and `SQLResponseStub` expectations.
- Define the routing, caching, preserved metadata, and trace semantics that adapter layers must preserve.
- Prefer preserved routing metadata such as `YH_TARGET_ENGINE` over driver-consumed engine hints.
- Publish trace payloads with explicit `executionMode` values so downstream operators can distinguish `STATEMENT` from `PREPARED_STATEMENT` without SQL-text inspection.
- Publish a dedicated `parameterPayload` field for failed prepared executions, derived from the submitted `params` DTOs as readable JSON text so operators can debug bindings without stack-trace scraping or `rawPayload` inspection.

## Compatibility Shim Scope

- `query` intentionally exposes `POST /kylin/api/query` plus a lightweight `GET`/`POST /kylin/api/user/authentication` handshake shim on its Kylin-shaped surface.
- The authentication shim returns a static authenticated payload so Kylin JDBC clients can complete their connection handshake against `query`; it is not a standalone login/session API.
- Compatibility endpoints such as `/kylin/api/tables_and_columns` are not implemented in `query`; requests to those paths should expect `404`.
- Basic Auth compatibility for actual query execution remains limited to `/kylin/api/query` when `engine.query.auth.*` is configured.
- Metadata browsing is out of scope for `query`; use the upstream datasource or a real Kylin deployment if a client still depends on Kylin metadata endpoints.

## Request Contract

- `sql` and `params` are the authoritative request inputs.
- `project` is accepted for client compatibility but is not used for routing or execution.
- `acceptPartial` is accepted but ignored; the current service always returns `partial = false`.
- `backdoorToggles` is accepted but ignored; there is no query-side implementation behind that compatibility field today.

## Response Contract

- `columnMetas`, `results`, `duration`, `isException`, `exceptionMessage`, and `storageCacheUsed` are authoritative for the current service behavior.
- `cube` is a compatibility label populated with the routed datasource name, not a Kylin cube identifier.
- `affectedRowCount` is always `0` because `query` only accepts read-only SQL.
- `partial` is always `false`; partial-result execution is not implemented.
- `totalScanCount` is the returned row count, not a backend scan metric.
- `hitExceptionCache` is currently always `false`; exception-result caching is not implemented.

## Ownership Boundary

`query` owns:

- routing precedence across preserved metadata, surviving driver hints, and datasource fallback
- cache semantics for deciding hits, misses, and datasource execution
- preserved metadata handling that must remain visible after any adapter handoff
- trace and execution metadata contracts such as `executionMode` and failed-prepared `parameterPayload`

## Trace Contract Notes

- `parameterPayload` is only emitted for failed `PREPARED_STATEMENT` traces in this task; statement executions and successful prepared executions keep it `null` or omit it.
- The payload is sourced from `PreparedQueryRequestDto.getParams()` / `StatementParameterDto` values only.
- The external prepared request contract stays the same, but Kylin-routed prepared requests are literalized inside `query` before the downstream statement executes.

## Prepared Parameter Contract

- Typed conversion is supported for `java.lang.String`, `java.lang.Integer`, `java.lang.Long`, `java.lang.Short`, `java.lang.Double`, `java.lang.Float`, `java.math.BigDecimal`, `java.lang.Boolean`, `java.sql.Date`, `java.sql.Time`, and `java.sql.Timestamp`.
- For non-Kylin datasources, `query` preserves the current JDBC prepared path and passes converted values through `PreparedStatement#setObject(...)`.
- For routed Kylin datasources, `query` renders prepared parameters into SQL literals before execution because downstream Kylin planning rejects `?` placeholders; numerics stay unquoted, booleans render as `TRUE`/`FALSE`, strings/date/time/timestamp values are single-quoted, and `null` renders as `NULL`.
- Unsupported `className` values also fall back to the raw string value, so execution may still succeed if the driver coerces it, but prepared-result cache fingerprinting is disabled for those requests.
- `java.util.Date` is intentionally treated as unsupported today because the service does not define a canonical string-to-`java.util.Date` conversion format.
- Kylin literalization validates placeholder count before datasource execution and returns the normal exception-style response when the request parameter count does not match the SQL placeholders.

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
