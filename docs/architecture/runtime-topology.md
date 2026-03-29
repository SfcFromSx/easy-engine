# Runtime Topology

## Default Local Topology

```mermaid
flowchart LR
  subgraph Clients
    BI["BI / app clients"]
    Bench["Benchmark"]
  end

  subgraph Adapter
    CacheJdbc["kylin-jdbc-cache"]
  end

  subgraph QueryPlane
    Query["query"]
    Redis["Redis"]
  end

  subgraph ControlPlane
    Manager["manager"]
    PG["PostgreSQL"]
  end

  subgraph Engines
    Kylin["Kylin"]
    Presto["Presto"]
  end

  BI --> CacheJdbc
  Bench --> CacheJdbc
  CacheJdbc --> Query
  Query --> Redis
  Query --> Kylin
  Query --> Presto
  Redis --> Manager
  Manager --> PG
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

- `benchmark` depends on `query` semantics when using the cached JDBC route.
- `query` should keep serving traffic if Redis is unavailable by degrading to direct datasource execution.
- `manager` should not bring down the runtime when Redis or Calcite parsing has transient failures.
- The root harness should run from the repository root, not from individual submodules.
