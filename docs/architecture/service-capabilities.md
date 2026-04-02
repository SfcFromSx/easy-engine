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
| Execute read-only SQL on the default datasource | `Implemented` | Statement execution is direct; prepared execution stays supported, with Kylin-routed prepared requests literalized inside `query` before execution. |
| Reject non-query SQL | `Implemented` | Non-query statements return an exception-style response. |
| Route queries by preserved metadata | `Implemented` | `YH_TARGET_ENGINE` is the only routing hint; executable SQL is normalized with a leading `YH_TARGET_ENGINE` comment and otherwise falls back to the default datasource. |
| Consume shared analyze module for routing and SQL parsing | `Implemented` | `query` uses the shared `analyze` jar for comment parsing, routing rewrite analysis, and dialect-adapter hooks. |
| Redis-backed query cache | `Implemented` | Includes datasource isolation, TTL, cache key override, and bypass semantics. |
| Prepared-parameter cache fingerprinting | `Implemented` | Prepared inputs participate in cache identity. |
| Trace writing to MySQL | `Implemented` | Query execution writes trace records directly to MySQL with `executionMode` and an optional failed-prepared `parameterPayload`. |
| Multiple datasource registry | `Implemented` | Default plus named routed datasources are supported. |
| JDBC authentication handshake shim | `Implemented` | `GET`/`POST /kylin/api/user/authentication` returns a lightweight authenticated payload for Kylin JDBC clients that connect to `query`. |
| Independent authentication API | `Out of Scope` | Query does not expose a standalone login/session API beyond the JDBC handshake shim. |
| Independent metadata catalog API | `Out of Scope` | Metadata APIs were removed from query. |
| Query cancellation controls | `Planned` | No operator kill or cancel endpoint exists today. |

## Manager

| Capability | Status | Notes |
|---|---|---|
| Read trace records from MySQL | `Implemented` | Manager reads `SqlExecutionRecord` rows written directly by `query`. |
| Persist trace history to MySQL | `Implemented` | Raw payloads and normalized execution records are stored, including `executionMode` and optional `parameterPayload`. |
| Parse SQL structure with shared Calcite analyzer | `Implemented` | Preview and ingestion-time parsing both delegate to the shared `analyze` module. |
| Maintain SQL fingerprint and pattern statistics | `Implemented` | Pattern stats are upserted during ingestion. |
| Expose trace history API | `Implemented` | Paginated trace browsing is available, including `executionMode` and optional `parameterPayload` for failed prepared traces. |
| Expose stats summary API | `Implemented` | Summary counts for traces, parse status, and patterns are available. |
| Expose top-pattern API | `Implemented` | Used for acceleration suggestions. |
| Expose query routing context API | `Implemented` | Returns datasource configs plus active acceleration rules for `query`. |
| JDBC-side SQL rewrite advisory API | `Implemented` | Returns shared-analyze rewrite advice, does not execute SQL. |
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
| SQL Lib CRUD | `Implemented` | Supports weight, execution mode, source filename, and upload-time metadata. |
| SQL Lib file upload | `Implemented` | Supports `.xlsx`, `.xls`, Excel-compatible `.et`, `.csv`, `.txt`, and `.sql`. |
| Test set CRUD | `Implemented` | Metadata create/update/delete plus paged SQL Lib-backed item listing are available. |
| Direct test-set file import | `Implemented` | Legacy upload endpoint now rejects direct imports and redirects operators to SQL Lib upload. |
| Benchmark job CRUD | `Implemented` | Create, list, get, update, and guarded delete are supported. |
| Start benchmark run | `Implemented` | Async run creation and execution are supported. |
| Run progress polling | `Implemented` | Active run and per-run progress are exposed. |
| Run completion metrics | `Implemented` | Latency and success/error metrics are persisted. |
| Structured evaluation report | `Implemented` | `evaluationJson` and comparison context are produced, including grouped failure diagnostics by SQL label, execution mode, and routed target. |
| Previous-run comparison delta | `Implemented` | Context endpoint compares with prior completed runs. |
| Stale running-run recovery on startup | `Implemented` | Orphan `RUNNING` rows are reconciled to `FAILED`. |
| PreparedStatement benchmark execution | `Implemented` | SQL Lib and linked test sets can drive prepared execution. |
| Prepared SQL debug tooling in datasource query path | `Partial` | Datasource debug is still statement-oriented. |
| Run cancel / stop / abort | `Planned` | No stop endpoint exists today. |

## Cross-Service Notes

| Concern | Status | Notes |
|---|---|---|
| Query-only boundary for `query` | `Implemented` | Query serves execution only. |
| Trace contract between `query` and `manager` | `Implemented` | Query writes directly to MySQL; manager reads from the same DB. |
| Shared `analyze` module without a fourth service | `Implemented` | Runtime topology stays at three services while `query` and `manager` share in-process SQL analysis code. |
| Benchmark path via Kylin JDBC to query | `Implemented` | Standard path is `benchmark (Kylin JDBC) -> query`. |
| Full scheduler platform in manager | `Partial` | Architecture expects more than the current codebase exposes. |
