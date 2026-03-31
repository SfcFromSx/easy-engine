# Manager Module

`manager` is the Easy Engine control plane. It reads query trace history from the shared metadata database, stores execution history, parses SQL structure, maintains pattern statistics, and manages acceleration metadata.

## Responsibilities

- Read execution records from MySQL.
- Persist control-plane records to MySQL.
- Parse SQL with Apache Calcite.
- Maintain SQL fingerprint and pattern statistics.
- Expose stats, trace, pattern, parse-preview, and acceleration APIs.

## Operator Notes

- The dashboard plus the traces, patterns, and acceleration views now share the same page-header, filter-bar, table-shell, and card styling so operators see one consistent control-plane layout across the main manager routes.
- On screens at or below 960px wide, the traces, patterns, and acceleration list views switch from dense desktop tables to stacked record cards to preserve readable controls, SQL snippets, and row actions on smaller devices.
- The datasource catalog now exposes client-side filters for keyword, datasource type, and default/custom scope after the full datasource list is fetched from `/api/v1/query-datasources`.
- The traces, patterns, and acceleration list views expose route-backed filters so reload, bookmark, and back/forward navigation preserve the active list conditions.
- Applying or clearing trace, pattern, or acceleration filters resets the current page to page 1 before reloading the existing `/traces`, `/patterns/top`, or `/acceleration-tables` data with the corresponding request parameters.
- `/api/v1/traces` supports practical operator filters for fingerprint, datasource name, source flag, cache state, parse status, and SQL keyword matches on top of pagination.
- `/api/v1/patterns/top` supports fingerprint, clean-SQL keyword, and minimum execution-count filters on top of pagination.
- `/api/v1/acceleration-tables` supports keyword, status, schema name, and source filters on top of pagination.
- Fresh default manager schemas now start with empty traces, pattern stats, and acceleration tables; the dashboard stays empty until live traces are ingested or operators create acceleration entries themselves.
- `/api/v1/traces` now exposes an explicit `executionMode` field from ingested trace payloads, backed by `sql_execution_record.execution_mode`, so operators do not need to inspect `rawPayload` to distinguish statement versus prepared execution.
- `/api/v1/traces` also exposes `parameterPayload`, backed by `sql_execution_record.parameter_payload`, so failed prepared executions can be debugged from the normal API response instead of accidental stack-trace leakage.
- Legacy traces that do not send `parameterPayload` still ingest successfully and surface `null` or a missing field for backward compatibility.
- This task only stores the readable payload supplied by `query`; it does not change JDBC binding or reconstruct parameters from driver state.

## Current Review Notes

- `POST /api/v1/jdbc/sql-rewrite` is a best-effort advisory hook, not a semantic SQL rewriter. Today it only checks whether an ACTIVE acceleration table's `refreshSql` contains the incoming query text and, on a match, prepends a hint comment while leaving the query text itself unchanged.
- `POST /api/v1/acceleration-tables/from-pattern` creates draft scaffolding only. The generated DDL, refresh SQL, and fixed `0 30 2 * * ?` cron are operator-editable starting points and should not be treated as production-safe physical design or scheduling guidance, especially on the default MySQL runtime.
- `POST /api/v1/parse/preview` extracts table, group-by, and aggregate metadata reliably for plain `SELECT` statements and `ORDER BY` wrappers around a `SELECT`. Complex shapes such as CTE roots, `UNION` roots, and non-`SELECT` statements currently return the parsed `rootKind` plus a stringified SQL shape, but they do not provide complete lineage or aggregate coverage.

## Run

```bash
docker compose up -d mysql redis
cd /Users/sfc/Documents/projects/engine/manager
mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd /Users/sfc/Documents/projects/engine/manager/frontend
npm run dev -- --host 127.0.0.1 --port 5173
```

## Verify

```bash
mvn -q -f manager/pom.xml test
npm --prefix manager/frontend run test
npm --prefix manager/frontend run build
```

## Related Docs

- [docs/modules/manager-test-matrix.md](/Users/sfc/Documents/projects/engine/docs/modules/manager-test-matrix.md)
- [docs/architecture/overview.md](/Users/sfc/Documents/projects/engine/docs/architecture/overview.md)
- [docs/architecture/http-interfaces.md](/Users/sfc/Documents/projects/engine/docs/architecture/http-interfaces.md)
- [docs/operations/local-development.md](/Users/sfc/Documents/projects/engine/docs/operations/local-development.md)
