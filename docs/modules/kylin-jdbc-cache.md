# Kylin JDBC Cache Module

`kylin-jdbc-cache` is a JDBC adapter that wraps the Apache Kylin JDBC driver with Redis-backed caching, routing hints, and asynchronous trace reporting.

## Responsibilities

- Provide transparent JDBC-compatible caching semantics.
- Support route hints and cache control hints.
- Publish trace payloads asynchronously without blocking query execution.
- Degrade to direct datasource execution if Redis is unavailable.

## Important Interaction with `query`

- The adapter parses its own hints before forwarding SQL.
- Driver-level route hints may not survive into `query`.
- Use preserved metadata comments when query-side routing must remain visible beyond the adapter.

## Build

```bash
cd /Users/sfc/Documents/projects/engine/kylin-jdbc-cache
mvn clean package
```

## Related Docs

- [docs/modules/query.md](/Users/sfc/Documents/projects/engine/docs/modules/query.md)
- [docs/operations/validation-matrix.md](/Users/sfc/Documents/projects/engine/docs/operations/validation-matrix.md)
