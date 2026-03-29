# Easy Engine Manager

Easy Engine Manager is the control-plane module for trace ingestion, SQL pattern analysis, and acceleration metadata.

## Local Run

```bash
docker compose up -d postgres redis
cd /Users/sfc/Documents/projects/engine/manager
mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd /Users/sfc/Documents/projects/engine/manager/frontend
npm run dev -- --host 127.0.0.1 --port 4173
```

The frontend proxies `/api/*` to `http://127.0.0.1:8090`.

## Key APIs

- `GET /api/v1/stats/summary`
- `GET /api/v1/traces`
- `GET /api/v1/patterns/top`
- `POST /api/v1/parse/preview`
- `POST /api/v1/acceleration-tables`
- `POST /api/v1/acceleration-tables/from-pattern`
- `PATCH /api/v1/acceleration-tables/{id}/status`

## Verification

```bash
mvn -q -f /Users/sfc/Documents/projects/engine/manager/pom.xml test
npm --prefix /Users/sfc/Documents/projects/engine/manager/frontend run build
```

## Canonical Docs

- [docs/modules/manager.md](/Users/sfc/Documents/projects/engine/docs/modules/manager.md)
- [docs/architecture/http-interfaces.md](/Users/sfc/Documents/projects/engine/docs/architecture/http-interfaces.md)
