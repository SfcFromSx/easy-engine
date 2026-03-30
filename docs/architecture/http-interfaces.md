# HTTP Interfaces

This file summarizes the currently exposed server interfaces in Easy Engine.

## Query

Base service: `query`

| Method | Path | Status | Purpose | Notes |
|---|---|---|---|---|
| `POST` | `/kylin/api/query` | `Implemented` | Execute query requests | Query-only interface; supports routed and prepared execution. |

## Manager

Base service: `manager`

| Method | Path | Status | Purpose | Notes |
|---|---|---|---|---|
| `GET` | `/api/v1/stats/summary` | `Implemented` | Aggregated trace and pattern stats | Control-plane summary endpoint. |
| `GET` | `/api/v1/patterns/top` | `Implemented` | Return top SQL patterns | Used for acceleration suggestions. |
| `POST` | `/api/v1/parse/preview` | `Implemented` | Parse and preview SQL structure | Calcite-backed analysis. |
| `GET` | `/api/v1/acceleration-tables` | `Implemented` | List acceleration tables | Sorted by updated time. |
| `POST` | `/api/v1/acceleration-tables` | `Implemented` | Create manual acceleration table definition | Saves draft metadata. |
| `POST` | `/api/v1/acceleration-tables/from-pattern` | `Implemented` | Create draft from pattern stats | Generates DDL and refresh SQL. |
| `PATCH` | `/api/v1/acceleration-tables/{id}/status` | `Implemented` | Change acceleration status | Activation executes DDL and refresh SQL. |
| `GET` | `/api/v1/traces` | `Implemented` | Page trace history | Supports pagination and fingerprint filters; returns `executionMode` and optional `parameterPayload` for failed prepared traces. |
| `POST` | `/api/v1/jdbc/sql-rewrite` | `Implemented` | Ask manager for JDBC rewrite advice | Does not execute SQL. |
| `GET` | `/api/v1/acceleration-tables/{id}` | `Planned` | Table detail | Not exposed today. |
| `PUT` | `/api/v1/acceleration-tables/{id}` | `Planned` | Update acceleration definition | Not exposed today. |
| `DELETE` | `/api/v1/acceleration-tables/{id}` | `Planned` | Delete acceleration definition | Not exposed today. |

## Benchmark

Base service: `benchmark`

### Preflight

| Method | Path | Status | Purpose | Notes |
|---|---|---|---|---|
| `GET` | `/api/v1/preflight` | `Implemented` | Environment connectivity check | Probes Kylin REST and Presto info endpoints. |

### Datasources

| Method | Path | Status | Purpose | Notes |
|---|---|---|---|---|
| `GET` | `/api/v1/datasources` | `Implemented` | List benchmark datasources |  |
| `GET` | `/api/v1/datasources/{id}` | `Implemented` | Get one datasource |  |
| `POST` | `/api/v1/datasources` | `Implemented` | Create datasource |  |
| `PUT` | `/api/v1/datasources/{id}` | `Implemented` | Update datasource |  |
| `DELETE` | `/api/v1/datasources/{id}` | `Implemented` | Delete datasource | Guarded if jobs still reference it. |
| `POST` | `/api/v1/datasources/test` | `Implemented` | Test datasource connection | Connectivity-only validation. |
| `POST` | `/api/v1/datasources/{id}/query` | `Implemented` | Execute debug SQL through a datasource | Statement-style debug path. |

### Jobs

| Method | Path | Status | Purpose | Notes |
|---|---|---|---|---|
| `GET` | `/api/v1/jobs` | `Implemented` | List benchmark jobs |  |
| `GET` | `/api/v1/jobs/{id}` | `Implemented` | Get one job |  |
| `POST` | `/api/v1/jobs` | `Implemented` | Create job |  |
| `PUT` | `/api/v1/jobs/{id}` | `Implemented` | Update job |  |
| `DELETE` | `/api/v1/jobs/{id}` | `Implemented` | Delete job | Guarded if run history exists. |

### Test Sets

| Method | Path | Status | Purpose | Notes |
|---|---|---|---|---|
| `GET` | `/api/v1/test-sets` | `Implemented` | List test sets | Returns count and descriptive fields. |
| `GET` | `/api/v1/test-sets/{id}/items` | `Implemented` | List test set items | Ordered by sort order. |
| `POST` | `/api/v1/test-sets` | `Implemented` | Create test set | Manual metadata create. |
| `PUT` | `/api/v1/test-sets/{id}` | `Implemented` | Update test set |  |
| `POST` | `/api/v1/test-sets/upload` | `Implemented` | Import test set from Excel | Supports optional prepared columns. |
| `DELETE` | `/api/v1/test-sets/{id}` | `Implemented` | Delete test set | Cascades through DB constraints. |

### Templates

| Method | Path | Status | Purpose | Notes |
|---|---|---|---|---|
| `GET` | `/api/v1/templates` | `Implemented` | List templates | Supports pagination and keyword filter. |
| `POST` | `/api/v1/templates` | `Implemented` | Create template | Supports execution mode and params. |
| `PUT` | `/api/v1/templates/{id}` | `Implemented` | Update template |  |
| `DELETE` | `/api/v1/templates/{id}` | `Implemented` | Delete template |  |

### Runs

| Method | Path | Status | Purpose | Notes |
|---|---|---|---|---|
| `POST` | `/api/v1/runs/start` | `Implemented` | Start a benchmark run | Validates job sources before launch. |
| `GET` | `/api/v1/runs/{id}` | `Implemented` | Get run detail | Includes metrics and progress fields. |
| `GET` | `/api/v1/runs/{id}/context` | `Implemented` | Get run detail plus comparison context | Includes previous-run delta payload and parsed failure breakdown from `evaluationJson`. |
| `GET` | `/api/v1/runs` | `Implemented` | List runs for a job | Paged by `jobId`. |
| `GET` | `/api/v1/runs/active` | `Implemented` | Return current active run if any | Stale rows are reconciled on startup. |
| `POST` | `/api/v1/runs/{id}/cancel` | `Planned` | Cancel an active run | Not exposed today. |
