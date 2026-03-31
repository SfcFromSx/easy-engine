# Easy Engine Architecture Overview

Easy Engine is organized as three cooperating modules:

1. `query`: the query execution service and source of truth for execution semantics.
2. `manager`: the control-plane service — manages datasource configs, acceleration tables, and trace history.
3. `benchmark`: the benchmark orchestration service and UI.

The standard benchmark end-to-end path is:

```text
benchmark (Kylin JDBC) -> query -> Kylin / Presto / Hive
                               |-> writes SqlExecutionRecord to PostgreSQL
manager <-- reads PostgreSQL, manages datasource configs
```

## Architectural Boundaries

- `query` accepts query requests via the standard Apache Kylin JDBC protocol, parses hints and preserved metadata, routes requests to the configured backend (Kylin, Presto, Hive), reads and writes the Redis result cache, executes read-only SQL, and writes trace records directly to PostgreSQL.
- `manager` owns datasource configuration (exposed via `/api/v1/query-datasources`), consumes trace records from PostgreSQL, parses SQL with Calcite, maintains pattern statistics, and exposes control-plane APIs for acceleration metadata.
- `benchmark` manages benchmark datasources, templates, test sets, and runs. It connects to `query` using the standard Apache Kylin JDBC driver. It supports uploading arbitrary JDBC driver JARs for datasource testing.

## Request Flow

1. `benchmark` (or any JDBC client) connects via `jdbc:kylin://query-host:8092/<project>`.
2. `query` parses SQL comments and preserved metadata such as `YH_TARGET_ENGINE`.
3. `query` fetches active datasource definitions from `manager` (`GET /api/v1/query-datasources`).
4. `query` prefers preserved routing metadata, then driver-style engine hints, then the default datasource.
5. `query` serves a cache hit from Redis or executes the SQL against the selected backend datasource.
6. `query` writes a `SqlExecutionRecord` directly to PostgreSQL with `executionMode` and, for failed prepared executions, a readable `parameterPayload`.
7. `manager` reads `SqlExecutionRecord` rows from PostgreSQL, exposes the trace record through `/api/v1/traces`, parses SQL with Calcite, and updates pattern statistics.

## Runtime Defaults

- `manager`: `8090`
- `benchmark`: `8091`
- `query`: `8092`
- Redis: `6380`
- PostgreSQL: `5433`

## Ownership Rules

- `query` is the sole owner of execution semantics: routing, caching, SQL rewriting, and trace emission.
- `manager` is the sole owner of datasource configuration and control-plane state.
- `benchmark` does not own production query execution semantics.
- English docs are canonical; Chinese mirrors are selective convenience artifacts only.
