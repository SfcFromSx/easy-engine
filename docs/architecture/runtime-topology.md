# Runtime Topology

## Default Local Topology

```mermaid
flowchart LR
  subgraph Clients
    BI["BI / app clients (Kylin JDBC)"]
    Bench["Benchmark (Kylin JDBC)"]
  end

  subgraph QueryPlane
    Query["query"]
    Redis["Redis (result cache)"]
  end

  subgraph ControlPlane
    Manager["manager"]
    PG["PostgreSQL"]
  end

  subgraph Engines
    Kylin["Kylin"]
    Presto["Presto"]
    Hive["Hive"]
  end

  BI --> Query
  Bench --> Query
  Query --> Redis
  Query --> Kylin
  Query --> Presto
  Query --> Hive
  Query --> PG
  Manager --> PG
  Query -.->|polls datasource configs| Manager
```

## Service Ports

- `manager`: `8090`
- `benchmark`: `8091`
- `query`: `8092`
- `postgres`: `5433`
- `redis`: `6380`
- `kylin`: `17070`
- `presto`: `18081`

## Operational Notes

- `benchmark` connects to `query` using the standard Apache Kylin JDBC driver (`jdbc:kylin://localhost:8092/<project>`).
- `query` fetches datasource configurations from `manager` on startup. It falls back to static config if manager is unreachable.
- `query` writes execution trace records directly to PostgreSQL; no Redis trace queue is used.
- `query` should keep serving traffic if Redis is unavailable by degrading to direct datasource execution.
- `manager` should not bring down the runtime when Calcite parsing has transient failures.
- The root harness should run from the repository root, not from individual submodules.
