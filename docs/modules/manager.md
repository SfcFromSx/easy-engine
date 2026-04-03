# Manager Module

`manager` is the Easy Engine control plane. It reads query trace history from the shared metadata database, stores execution history, uses the shared `analyze` module for SQL structure analysis and advisory rewrite logic, maintains pattern statistics, and manages acceleration metadata.

## Responsibilities

- Read execution records from MySQL.
- Persist control-plane records to MySQL.
- Parse SQL through the shared `analyze` module's Calcite-backed analyzer.
- Maintain SQL fingerprint and pattern statistics.
- Expose stats, trace, pattern, parse-preview, query-routing-context, JDBC rewrite, acceleration, and cache-management APIs.

## Operator Notes

- The dashboard plus the traces, patterns, and acceleration views now share the same page-header, filter-bar, table-shell, and card styling so operators see one consistent control-plane layout across the main manager routes.
- On screens at or below 960px wide, the traces, patterns, and acceleration list views switch from dense desktop tables to stacked record cards to preserve readable controls, SQL snippets, and row actions on smaller devices.
- The datasource catalog now exposes client-side filters for keyword, datasource type, and default/custom scope after the full datasource list is fetched from `/api/v1/query-datasources`.
- The cache management page now exposes only lightweight policy metadata by default; operators must enter a narrower managed-key prefix before the UI will list Redis cache keys.
- `query` now consumes `/api/v1/query-routing-context` to refresh datasource and active-acceleration routing context without adding a fourth runtime service.
- The traces, patterns, and acceleration list views expose route-backed filters so reload, bookmark, and back/forward navigation preserve the active list conditions.
- Applying or clearing trace, pattern, or acceleration filters resets the current page to page 1 before reloading the existing `/traces`, `/patterns/top`, or `/acceleration-tables` data with the corresponding request parameters.
- `/api/v1/traces` supports practical operator filters for fingerprint, datasource name, source flag, cache state, parse status, and SQL keyword matches on top of pagination.
- `/api/v1/patterns/top` supports fingerprint, clean-SQL keyword, and minimum execution-count filters on top of pagination.
- `/api/v1/acceleration-tables` supports keyword, status, schema name, and source filters on top of pagination.
- `/api/v1/cache/keys` now requires a prefix narrower than `kylin_cache:` and pages through matching keys by Redis cursor instead of materializing or sorting the full namespace. Per-row size/TTL reads are limited to the current page of matched keys.
- `/api/v1/cache/info` is now a lightweight policy endpoint describing the managed namespace and the disabled exact-summary behavior; it no longer computes live global key counts or total byte sizes from Redis.
- Cache-key detail, create, update, and delete operations remain limited to `kylin_cache:` entries, and existing keys still cannot be renamed.
- Fresh default manager schemas now start with empty traces, pattern stats, and acceleration tables; the dashboard stays empty until live traces are ingested or operators create acceleration entries themselves.
- `/api/v1/traces` now exposes an explicit `executionMode` field from ingested trace payloads, backed by `manager_sql_execution_record.execution_mode`, so operators do not need to inspect `rawPayload` to distinguish statement versus prepared execution.
- `/api/v1/traces` now also exposes `cacheKey`, backed by `manager_sql_execution_record.cache_key`, so operators can filter by and inspect the exact Redis key used on cache-eligible query traces.
- `/api/v1/traces` also exposes `parameterPayload`, backed by `manager_sql_execution_record.parameter_payload`, so failed prepared executions can be debugged from the normal API response instead of accidental stack-trace leakage.
- Manager-owned metadata tables now use the `manager_` prefix consistently: `manager_sql_execution_record`, `manager_sql_pattern_stats`, `manager_acceleration_table`, `manager_query_datasource_config`, and `manager_flyway_schema_history`.
- Legacy traces that do not send `cacheKey` or `parameterPayload` still ingest successfully and surface `null` or a missing field for backward compatibility.
- This task only stores the readable payload supplied by `query`; it does not change JDBC binding or reconstruct parameters from driver state.

## Current Review Notes

- `POST /api/v1/jdbc/sql-rewrite` is a best-effort advisory hook, not a semantic SQL rewriter. It now emits `ENGINE`-based leading comments, can override `ENGINE` via Redis lookups keyed by `YH_RPTID`, strips legacy `YH_TARGET_ENGINE` metadata from normalized execution SQL, and adds `cache-table` when an ACTIVE acceleration rule matches while still leaving the SQL body unchanged.
- `POST /api/v1/acceleration-tables/from-pattern` creates draft scaffolding only. The generated DDL, refresh SQL, and fixed `0 30 2 * * ?` cron are operator-editable starting points and should not be treated as production-safe physical design or scheduling guidance, especially on the default MySQL runtime.
- `POST /api/v1/parse/preview` extracts table, group-by, and aggregate metadata reliably for plain `SELECT` statements and `ORDER BY` wrappers around a `SELECT`. Complex shapes such as CTE roots, `UNION` roots, and non-`SELECT` statements currently return the parsed `rootKind` plus a stringified SQL shape, but they do not provide complete lineage or aggregate coverage.

## Run

```bash
docker compose up -d mysql redis
bash scripts/init-db.sh dev manager
cd /Users/sfc/Documents/projects/engine/manager
bash /Users/sfc/Documents/projects/engine/scripts/with-java8.sh mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd /Users/sfc/Documents/projects/engine/manager/frontend
npm run dev -- --host 127.0.0.1 --port 5173
```

`manager` no longer applies Flyway migrations during normal startup. Initialize
the metadata schema first with `bash scripts/init-db.sh <dev|test|pro> manager`.
Runtime settings now live in `application-dev.yml`, `application-test.yml`, and
`application-pro.yml`. Those profile YAMLs are now MySQL-first; H2 remains
limited to test-scoped override resources used by regression coverage.
All Maven-backed startup and validation commands for `manager` must run on a
full Java 8 JDK, which `scripts/with-java8.sh` enforces.

## Verify

```bash
bash scripts/with-java8.sh mvn -q -pl analyze,manager -am test -Dspring.mvc.pathmatch.matching-strategy=ant_path_matcher
npm --prefix manager/frontend run test
npm --prefix manager/frontend run build
```

Because `manager` now depends on the shared `analyze` module, reactor builds from the repo root are the reliable verification path.

## Related Docs

- [docs/modules/manager-test-matrix.md](/Users/sfc/Documents/projects/engine/docs/modules/manager-test-matrix.md)
- [docs/architecture/overview.md](/Users/sfc/Documents/projects/engine/docs/architecture/overview.md)
- [docs/architecture/http-interfaces.md](/Users/sfc/Documents/projects/engine/docs/architecture/http-interfaces.md)
- [docs/operations/local-development.md](/Users/sfc/Documents/projects/engine/docs/operations/local-development.md)
