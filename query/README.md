# Easy Engine Query

Easy Engine Query is the query-only execution surface for Easy Engine.

## Scope

- serves `POST /kylin/api/query`,
- handles routing, caching, and trace publishing,
- remains compatible with the cached JDBC path,
- does not own independent auth or metadata APIs.

## Routing Priority

1. preserved metadata such as `YH_TARGET_ENGINE`
2. surviving driver-style engine hints
3. default datasource

## Local Run

```bash
cd /Users/sfc/Documents/projects/engine/query
mvn spring-boot:run
```

## Verification

```bash
mvn -q -f /Users/sfc/Documents/projects/engine/query/pom.xml test
```

## Canonical Docs

- [docs/modules/query.md](/Users/sfc/Documents/projects/engine/docs/modules/query.md)
- [docs/architecture/overview.md](/Users/sfc/Documents/projects/engine/docs/architecture/overview.md)
