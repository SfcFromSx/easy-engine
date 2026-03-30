# Easy Engine Architecture Overview

Easy Engine is organized as four cooperating modules:

1. `query`: the query-only execution service.
2. `manager`: the control-plane service.
3. `benchmark`: the benchmark orchestration service and UI.
4. `kylin-jdbc-cache`: the JDBC adapter that can front `query`.

The standard benchmark end-to-end path is:

```text
benchmark -> kylin-jdbc-cache -> query -> Kylin or Presto
```

## Architectural Boundaries

- `query` accepts query requests, parses hints and preserved metadata, routes requests, reads and writes cache, executes read-only SQL, and emits trace payloads with explicit execution-mode metadata.
- `manager` consumes trace payloads, persists history including optional readable parameter payloads for failed prepared executions, parses SQL with Calcite, maintains pattern statistics, and exposes control-plane APIs for acceleration metadata.
- `benchmark` manages benchmark datasources, templates, test sets, and runs. It does not own production query execution semantics.
- `kylin-jdbc-cache` is an adapter layer, not a standalone control-plane or query server.

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

- `query` routing, cache semantics, and trace payloads must stay aligned with `kylin-jdbc-cache`.
- `manager` must remain backward compatible with the active trace payload contract, including optional fields added for newer query traces.
- Job-server responsibilities belong to `manager`; there is no separate job server in this repository.
- English docs are canonical; Chinese mirrors are selective convenience artifacts only.
