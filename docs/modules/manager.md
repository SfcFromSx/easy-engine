# Manager Module

`manager` is the Easy Engine control plane. It ingests query traces, stores execution history, parses SQL structure, maintains pattern statistics, and manages acceleration metadata.

## Responsibilities

- Consume trace payloads from Redis.
- Persist execution records to PostgreSQL.
- Parse SQL with Apache Calcite.
- Maintain SQL fingerprint and pattern statistics.
- Expose stats, trace, pattern, parse-preview, and acceleration APIs.

## Run

```bash
docker compose up -d postgres redis
cd /Users/sfc/Documents/projects/engine/manager
mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd /Users/sfc/Documents/projects/engine/manager/frontend
npm run dev -- --host 127.0.0.1 --port 4173
```

## Verify

```bash
mvn -q -f manager/pom.xml test
npm --prefix manager/frontend run build
```

## Related Docs

- [docs/architecture/overview.md](/Users/sfc/Documents/projects/engine/docs/architecture/overview.md)
- [docs/architecture/http-interfaces.md](/Users/sfc/Documents/projects/engine/docs/architecture/http-interfaces.md)
- [docs/operations/local-development.md](/Users/sfc/Documents/projects/engine/docs/operations/local-development.md)
