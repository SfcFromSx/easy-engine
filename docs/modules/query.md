# Query Module

`query` is the Easy Engine query execution service. It accepts read-only query traffic via the standard Apache Kylin JDBC protocol, uses the shared `analyze` module to parse comments and preserved metadata, routes to the selected datasource, manages a Redis-backed result cache with synchronous reads plus best-effort asynchronous writes, and writes trace records directly to MySQL.

## Responsibilities

- Serve `POST /kylin/api/query`.
- Preserve compatibility with `PreparedQueryRequest` and `SQLResponseStub` expectations.
- Define the routing, caching, preserved metadata, and trace semantics that adapter layers must preserve.
- Route only by preserved routing metadata such as `YH_TARGET_ENGINE`; driver-style `engine` hints are stripped and ignored.
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
- `sql` must resolve to a supported read-only query shape after comment stripping; blank or non-query requests are rejected with the normal exception payload and do not reach datasource execution.
- Unknown top-level request fields are rejected through the same exception payload because `/kylin/api/query` only supports the query-request envelope, not other Kylin compatibility request shapes.
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

- routing precedence across preserved metadata and datasource fallback
- cache semantics for deciding hits, misses, and datasource execution
- asynchronous best-effort persistence of cacheable datasource results into Redis after execution
- preserved metadata handling that must remain visible after any adapter handoff
- trace and execution metadata contracts such as `executionMode` and failed-prepared `parameterPayload`

## Trace Contract Notes

- `parameterPayload` is only emitted for failed `PREPARED_STATEMENT` traces in this task; statement executions and successful prepared executions keep it `null` or omit it.
- The payload is sourced from `PreparedQueryRequestDto.getParams()` / `StatementParameterDto` values only.
- The external prepared request contract stays the same, but Kylin-routed prepared requests are literalized inside `query` before the downstream statement executes.

## Prepared Parameter Contract

- Typed conversion is supported for `java.lang.String`, `java.lang.Integer`, `java.lang.Long`, `java.lang.Short`, `java.lang.Double`, `java.lang.Float`, `java.math.BigDecimal`, `java.lang.Boolean`, `java.sql.Date`, `java.sql.Time`, and `java.sql.Timestamp`.
- For non-Kylin datasources such as H2, Presto, and Trino, `query` preserves the current JDBC prepared path and passes converted values through `PreparedStatement#setObject(...)`.
- For routed Kylin datasources, `query` renders prepared parameters into SQL literals before execution because downstream Kylin planning rejects `?` placeholders; numerics stay unquoted, booleans render as `TRUE`/`FALSE`, strings/date/time/timestamp values are single-quoted, and `null` renders as `NULL`.
- Unsupported `className` values also fall back to the raw string value, so execution may still succeed if the driver coerces it, but prepared-result cache fingerprinting is disabled for those requests.
- `java.util.Date` is intentionally treated as unsupported today because the service does not define a canonical string-to-`java.util.Date` conversion format.
- Kylin literalization validates placeholder count before datasource execution and returns the normal exception-style response when the request parameter count does not match the SQL placeholders.

## Tips

- Seeing `Kylin OK` in benchmark preflight or an operator dashboard only proves that the Kylin service, auth path, and basic connectivity are healthy. It does not prove that Kylin-backed prepared queries are healthy.
- If statement SQL works and trace rows are written, but prepared SQL with `?` still fails, the usual cause is downstream Kylin planning on the placeholder form rather than service availability.
- It is normal to still see `?` in incoming requests, saved trace SQL, or other compatibility-facing views. `query` preserves the external prepared request contract and only literalizes parameters internally right before execution against routed Kylin datasources.
- Because of that split, operator-visible SQL can still look prepared while the actual SQL sent to Kylin already contains concrete literals such as `'2010-01-01'`, `123`, `TRUE`, or `NULL`.

## Routing Notes

Query-side routing precedence is:

1. preserved metadata comments such as `YH_TARGET_ENGINE`
2. the default datasource fallback

- `query` rewrites executable SQL to start with a normalized `/* YH_TARGET_ENGINE=... */` comment.
- Active acceleration matches can add `cache-table=<schema>.<table>` to that leading comment, while phase 1 keeps the SQL body unchanged.

- The standard benchmark path is `benchmark (Kylin JDBC) -> query`.
- Trino datasource configs should use `type=trino`, `driverClass=io.trino.jdbc.TrinoDriver`, and a normal Trino JDBC URL such as `jdbc:trino://host:8080/catalog/schema`.
- When a JDBC client must select a specific engine, preserved metadata comments are the reliable mechanism:

```sql
/* YH_TARGET_ENGINE=presto_local */
SELECT count(*) FROM nation
```

## Run

```bash
cd /Users/sfc/Documents/projects/engine/query
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

`query` now keeps environment-specific runtime settings in
`application-dev.yml`, `application-test.yml`, and `application-pro.yml`. Use
`SPRING_PROFILES_ACTIVE=test` or `SPRING_PROFILES_ACTIVE=pro` outside local
development instead of relying on code-level defaults.

## Verify

```bash
mvn -q -pl analyze,query -am test
```

Because `query` now depends on the shared `analyze` module, reactor builds from the repo root are the reliable verification path.

## Related Docs

- [docs/architecture/overview.md](/Users/sfc/Documents/projects/engine/docs/architecture/overview.md)
- [docs/architecture/service-capabilities.md](/Users/sfc/Documents/projects/engine/docs/architecture/service-capabilities.md)
- [docs/operations/local-development.md](/Users/sfc/Documents/projects/engine/docs/operations/local-development.md)
