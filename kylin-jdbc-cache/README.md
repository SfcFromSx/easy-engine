# Kylin JDBC Cache Driver

`kylin-jdbc-cache` is a JDBC-compatible adapter that adds Redis-backed caching, route hints, and asynchronous trace publishing on top of the Kylin JDBC driver.

## Key Features

- transparent JDBC wrapper behavior,
- Redis-backed cache keys with TTL and manual overrides,
- datasource routing hints,
- asynchronous trace publishing,
- graceful degradation when Redis is unavailable.

## Important Query Integration Note

When the adapter forwards requests to Easy Engine `query`, driver-level route hints may already have been consumed. Use preserved metadata comments such as `/* YH_TARGET_ENGINE=presto_local */` when query-side routing must remain visible.

## Build

```bash
cd /Users/sfc/Documents/projects/engine/kylin-jdbc-cache
mvn clean package
```

## Canonical Docs

- [docs/modules/kylin-jdbc-cache.md](/Users/sfc/Documents/projects/engine/docs/modules/kylin-jdbc-cache.md)
- [docs/modules/query.md](/Users/sfc/Documents/projects/engine/docs/modules/query.md)
