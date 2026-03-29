# Service Capabilities

This file summarizes the current functional surface of the Easy Engine services.

## Status Legend

- `Implemented`: present in code and usable in the current service boundary.
- `Partial`: present, but constrained, incomplete, or missing a related lifecycle piece.
- `Planned`: not implemented yet, but aligned with the intended direction.
- `Out of Scope`: intentionally excluded from that service boundary.

## Query

| Capability | Status | Notes |
|---|---|---|
| Accept query requests | `Implemented` | `POST /kylin/api/query` is the supported public interface. |
| Execute read-only SQL on the default datasource | `Implemented` | Statement and prepared execution are supported. |
| Reject non-query SQL | `Implemented` | Non-query statements return an exception-style response. |
| Route queries by preserved metadata or engine hint | `Implemented` | `YH_TARGET_ENGINE` takes precedence, then `engine`, then default datasource. |
| Redis-backed query cache | `Implemented` | Includes datasource isolation, TTL, cache key override, and bypass semantics. |
| Prepared-parameter cache fingerprinting | `Implemented` | Prepared inputs participate in cache identity. |
| Trace publishing to Redis for manager ingestion | `Implemented` | Query execution emits trace payloads asynchronously. |
| Multiple datasource registry | `Implemented` | Default plus named routed datasources are supported. |
| Independent authentication API | `Out of Scope` | Query is query-only. |
| Independent metadata catalog API | `Out of Scope` | Metadata APIs were removed from query. |
| Query cancellation controls | `Planned` | No operator kill or cancel endpoint exists today. |

## Manager

| Capability | Status | Notes |
|---|---|---|
| Consume trace payloads from Redis | `Implemented` | Scheduled polling is active. |
| Persist trace history to PostgreSQL | `Implemented` | Raw payloads and normalized execution records are stored. |
| Parse SQL structure with Calcite | `Implemented` | Preview and ingestion-time parsing are both present. |
| Maintain SQL fingerprint and pattern statistics | `Implemented` | Pattern stats are upserted during ingestion. |
| Expose trace history API | `Implemented` | Paginated trace browsing is available. |
| Expose stats summary API | `Implemented` | Summary counts for traces, parse status, and patterns are available. |
| Expose top-pattern API | `Implemented` | Used for acceleration suggestions. |
| JDBC-side SQL rewrite advisory API | `Implemented` | Returns advice, does not execute SQL. |
| Manual acceleration table definition | `Implemented` | Manual create flow exists. |
| Acceleration table recommendation from patterns | `Implemented` | Can generate drafts from pattern stats. |
| Activate acceleration table by executing DDL and refresh SQL | `Implemented` | Activation runs DDL and refresh once. |
| Lifecycle CRUD completeness | `Partial` | Core operations exist, but scheduler maturity is still limited. |
| Scheduled refresh execution based on cron | `Partial` | Stored, but not fully orchestrated as a recurring platform. |
| Batch orchestration platform | `Planned` | Intended direction, not fully surfaced. |
| Query execution | `Out of Scope` | Manager is control-plane only. |

## Benchmark

| Capability | Status | Notes |
|---|---|---|
| Benchmark datasource CRUD | `Implemented` | Create, list, get, update, and guarded delete are supported. |
| Datasource connectivity test | `Implemented` | Connectivity-only validation endpoint exists. |
| Datasource ad hoc SQL debug query | `Implemented` | Statement-style debug path exists. |
| Global SQL template CRUD | `Implemented` | Supports weight and execution mode fields. |
| Test set CRUD | `Implemented` | Manual create/update/delete plus item listing are available. |
| Excel test-set import | `Implemented` | Supports prepared columns. |
| Benchmark job CRUD | `Implemented` | Create, list, get, update, and guarded delete are supported. |
| Start benchmark run | `Implemented` | Async run creation and execution are supported. |
| Run progress polling | `Implemented` | Active run and per-run progress are exposed. |
| Run completion metrics | `Implemented` | Latency and success/error metrics are persisted. |
| Structured evaluation report | `Implemented` | `evaluationJson` and comparison context are produced. |
| Previous-run comparison delta | `Implemented` | Context endpoint compares with prior completed runs. |
| Stale running-run recovery on startup | `Implemented` | Orphan `RUNNING` rows are reconciled to `FAILED`. |
| PreparedStatement benchmark execution | `Implemented` | Templates and test sets can drive prepared execution. |
| Prepared SQL debug tooling in datasource query path | `Partial` | Datasource debug is still statement-oriented. |
| Run cancel / stop / abort | `Planned` | No stop endpoint exists today. |

## Cross-Service Notes

| Concern | Status | Notes |
|---|---|---|
| Query-only boundary for `query` | `Implemented` | Query serves execution only. |
| Trace contract between `query` and `manager` | `Implemented` | Redis trace pipeline is active. |
| Benchmark path through cached JDBC to query | `Implemented` | Standard path is `benchmark -> kylin-jdbc-cache -> query`. |
| Full scheduler platform in manager | `Partial` | Architecture expects more than the current codebase exposes. |
