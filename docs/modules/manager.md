# Manager Module

`manager` is the Easy Engine control plane. It ingests query traces, stores execution history, parses SQL structure, maintains pattern statistics, and manages acceleration metadata.

## Responsibilities

- Consume trace payloads from Redis.
- Persist execution records to PostgreSQL.
- Parse SQL with Apache Calcite.
- Maintain SQL fingerprint and pattern statistics.
- Expose stats, trace, pattern, parse-preview, and acceleration APIs.

## Operator Notes

- The dashboard plus the traces, patterns, and acceleration views now share the same page-header, filter-bar, table-shell, and card styling so operators see one consistent control-plane layout across the main manager routes.
- On screens at or below 960px wide, the traces, patterns, and acceleration list views switch from dense desktop tables to stacked record cards to preserve readable controls, SQL snippets, and row actions on smaller devices.
- The traces and patterns list views expose a route-backed SQL fingerprint filter.
- Applying or clearing the fingerprint filter resets the current page to page 1 and reloads the existing `/traces` or `/patterns/top` data with the same `fingerprint` request parameter.
- The acceleration list remains pagination-only because the current backend only supports `page` and `size`.
- `/api/v1/traces` now exposes an explicit `executionMode` field from ingested trace payloads, backed by `sql_execution_record.execution_mode`, so operators do not need to inspect `rawPayload` to distinguish statement versus prepared execution.
- `/api/v1/traces` also exposes `parameterPayload`, backed by `sql_execution_record.parameter_payload`, so failed prepared executions can be debugged from the normal API response instead of accidental stack-trace leakage.
- Legacy traces that do not send `parameterPayload` still ingest successfully and surface `null` or a missing field for backward compatibility.
- This task only stores the readable payload supplied by `query`; it does not change JDBC binding or reconstruct parameters from driver state.

## Run

```bash
docker compose up -d postgres redis
cd /Users/sfc/Documents/projects/engine/manager
mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd /Users/sfc/Documents/projects/engine/manager/frontend
npm run dev -- --host 127.0.0.1 --port 4173
```

## Verify

```bash
mvn -q -f manager/pom.xml test
npm --prefix manager/frontend run build
```

## Related Docs

- [docs/architecture/overview.md](/Users/sfc/Documents/projects/engine/docs/architecture/overview.md)
- [docs/architecture/http-interfaces.md](/Users/sfc/Documents/projects/engine/docs/architecture/http-interfaces.md)
- [docs/operations/local-development.md](/Users/sfc/Documents/projects/engine/docs/operations/local-development.md)
