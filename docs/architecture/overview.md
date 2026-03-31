# Easy Engine Architecture Overview

Easy Engine runs as three cooperating services with a single clear boundary for each role:

1. `benchmark`: benchmark orchestration service and operator UI.
2. `query`: JDBC-facing execution service and source of truth for execution semantics.
3. `manager`: control-plane service for datasource configs, traces, patterns, and acceleration metadata.

The standard benchmark end-to-end path is:

```text
benchmark (Kylin JDBC) -> query -> Kylin / Presto / Hive
                               |-> MySQL (SqlExecutionRecord)
manager <----------------------+
manager ----> query datasource configs
```

## Architectural Boundaries

- `benchmark` owns benchmark-only concerns: benchmark datasources, templates, test sets, runs, preflight checks, and JDBC driver uploads used by its own datasource test/debug/benchmark paths.
- `query` owns execution semantics: JDBC compatibility, preserved metadata parsing, routing precedence, cache behavior, query execution, and trace emission.
- `manager` owns control-plane state: query datasource configuration, trace browsing, SQL pattern analysis, and acceleration metadata lifecycle.

No standalone JDBC adapter service exists in the active architecture. JDBC clients, including `benchmark`, connect directly to `query`.

## Request Flow

1. `benchmark` (or any JDBC client) connects via `jdbc:kylin://query-host:8092/<project>`.
2. `query` parses SQL comments and preserved metadata such as `YH_TARGET_ENGINE`.
3. `query` fetches active datasource definitions from `manager` via `GET /api/v1/query-datasources`.
4. `query` resolves the target datasource using preserved routing metadata first, driver-style engine hints second, and the default datasource last.
5. `query` serves a cache hit from Redis or executes the read-only SQL against Kylin, Presto, or Hive.
6. `query` writes a `SqlExecutionRecord` directly to MySQL, including `executionMode` and readable failed-prepared `parameterPayload` data when applicable.
7. `manager` reads those MySQL trace rows, exposes them through `/api/v1/traces`, parses SQL with Calcite, and updates pattern statistics.

## Control-Plane Flow

1. Operators manage backend datasource definitions in `manager`.
2. `manager` persists those configs and exposes them through `/api/v1/query-datasources`.
3. `query` polls `manager` for datasource configs and falls back to static configuration if `manager` is temporarily unavailable.
4. `benchmark` remains a consumer of the JDBC interface rather than an owner of routing semantics.

## Runtime Defaults

- `manager`: `8090`
- `benchmark`: `8091`
- `query`: `8092`
- Redis: `6380`
- MySQL: `3307`

## Ownership Rules

- `query` is the sole owner of execution semantics: routing, caching, SQL rewriting, backend selection, and trace emission.
- `manager` is the sole owner of datasource configuration and control-plane state.
- `benchmark` does not own production datasource routing or query execution semantics.
- English docs are canonical; Chinese mirrors are selective convenience artifacts only.
