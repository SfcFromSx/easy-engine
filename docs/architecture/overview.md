# Easy Engine Architecture Overview

Easy Engine is organized as four cooperating modules:

1. `query`: the query-only execution service and the long-term source of truth for execution semantics.
2. `manager`: the control-plane service.
3. `benchmark`: the benchmark orchestration service and UI.
4. `kylin-jdbc-cache`: the JDBC compatibility adapter that can front `query`.

The standard benchmark end-to-end path is:

```text
benchmark -> kylin-jdbc-cache -> query -> Kylin or Presto
```

## Architectural Boundaries

- `query` accepts query requests, parses hints and preserved metadata, routes requests, reads and writes cache, executes read-only SQL, and emits trace payloads with explicit execution-mode metadata. It is the authoritative owner of future execution behavior in this repository.
- `manager` consumes trace payloads, persists history including optional readable parameter payloads for failed prepared executions, parses SQL with Calcite, maintains pattern statistics, and exposes control-plane APIs for acceleration metadata.
- `benchmark` manages benchmark datasources, templates, test sets, and runs. It does not own production query execution semantics.
- `kylin-jdbc-cache` is an adapter layer, not a standalone control-plane or query server. It preserves JDBC-facing compatibility concerns but is not the future home for execution semantics.

## Query and JDBC Migration Boundary

This task documents the ownership boundary only. It does not move behavior and does not require JDBC code changes.

`query` owns execution semantics:

- routing precedence, including preserved metadata such as `YH_TARGET_ENGINE`, surviving driver hints, and default datasource fallback
- cache semantics that determine whether a request is served from Redis or executed against the datasource
- preserved metadata handling that must survive adapter handoff when query-side behavior depends on it
- the trace contract emitted for downstream consumers, including `executionMode` and readable failed-prepared `parameterPayload` metadata

`kylin-jdbc-cache` owns JDBC compatibility concerns:

- consuming driver-facing hints before forwarding SQL when that is required for JDBC compatibility
- adapter behavior needed to present JDBC-compatible caching and fallback behavior to driver callers
- JDBC-side request and response handling that can front `query` without redefining query execution ownership

## Data Flow

1. A client or benchmark run issues a query through `kylin-jdbc-cache` or directly to `query`.
2. `query` parses comments and preserved metadata such as `YH_TARGET_ENGINE`.
3. `query` prefers preserved routing metadata, then driver-style engine hints, then the default datasource.
4. `query` serves a cache hit from Redis or executes the SQL against the selected datasource.
5. `query` publishes a trace payload to Redis with `executionMode` and, for failed prepared executions, an optional readable `parameterPayload` derived from request DTO parameters.
6. `manager` consumes the trace payload, stores it in PostgreSQL, exposes the compatible trace record through `/api/v1/traces`, parses SQL with Calcite, and updates pattern statistics.

## Runtime Defaults

- `manager`: `8090`
- `benchmark`: `8091`
- `query`: `8092`
- Redis: `6380`
- PostgreSQL: `5433`

## Compatibility Rules

- `query` remains the source of truth for routing, cache semantics, preserved metadata handling, and trace payloads even when requests arrive through `kylin-jdbc-cache`.
- `kylin-jdbc-cache` must stay compatible with the active `query` execution contract when adapting JDBC-facing traffic.
- `manager` must remain backward compatible with the active trace payload contract, including optional fields added for newer query traces.
- Job-server responsibilities belong to `manager`; there is no separate job server in this repository.
- English docs are canonical; Chinese mirrors are selective convenience artifacts only.
