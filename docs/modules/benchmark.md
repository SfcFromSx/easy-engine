# Benchmark Module

`benchmark` manages benchmark datasources, SQL Lib entries, test sets, jobs, and execution runs. It is the preferred operator interface for benchmark-driven validation of query execution.

## Responsibilities

- Manage benchmark datasources.
- Manage SQL Lib entries with statement and prepared execution modes.
- Manage test sets as ordered references to SQL Lib entries.
- Launch benchmark runs and store structured run reports.
- Expose preflight checks for Kylin and Presto dependencies.

## Standard Path

```text
benchmark (Kylin JDBC) -> query -> Kylin / Presto / Hive
```

Benchmark connects to `query` using the standard Apache Kylin JDBC driver (`jdbc:kylin://localhost:8092/<project>`). Additional JDBC driver JARs can be uploaded via the Benchmark UI under Data Sources > Upload Driver, then reused for datasource tests, debug queries, and benchmark runs.

## Prepared Execution Notes

- Jobs without a linked test set execute from SQL Lib.
- Jobs with a linked test set execute the ordered SQL Lib references stored on that test set.
- `execution_mode` supports `STATEMENT` and `PREPARED_STATEMENT`.
- `STATEMENT` executes raw SQL text.
- `PREPARED_STATEMENT` expects ordered `param_json` arrays for parameter binding.
- SQL Lib authoring and SQL Lib upload flows expose `execution_mode` and `param_json` for prepared workflows.
- Benchmark runs support prepared execution more fully than the datasource debug endpoint.

## Operator Notes

- Datasources load full lists in the frontend and expose client-side keyword search plus a dedicated driver-class filter.
- Jobs load full lists in the frontend and expose client-side keyword search plus dedicated datasource, strategy, and test-set filters. The keyword matcher now also covers job ID, concurrency, and rounds.
- SQL Lib keeps request-backed pagination and now supports keyword search, `executionMode`, `sourceFilename`, and optional upload-time filtering. The primary API is `GET /api/v1/sql-lib`, while `/api/v1/templates` remains a compatibility alias for this task.
- SQL Lib file upload moved to `POST /api/v1/sql-lib/upload` and accepts `.xlsx`, `.xls`, Excel-compatible `.et`, `.csv`, `.txt`, and `.sql`. Uploaded rows store `sourceFilename` and `uploadedAt`; `.txt`/`.sql` inputs are split by semicolon while ignoring quoted-string and SQL-comment delimiters.
- Test sets keep the existing client-side keyword plus source filters, but new test sets are metadata-only containers. Operators now manage linked SQLs from a list-first dialog: browse paginated test-case memberships on the left, select one row to inspect current SQL Lib detail on the right, add more SQLs from SQL Lib, remove memberships, reorder memberships, and jump to SQL Lib for editing.
- Test sets no longer accept direct file uploads or manual SQL authoring. Their authoring surface is limited to adding/removing/reordering SQL Lib references. `POST /api/v1/test-sets/upload` now rejects direct import attempts with guidance to upload into SQL Lib first.
- Test-set APIs now center on reference membership: `GET /api/v1/test-sets/{id}/items` returns paged SQL Lib-backed rows, `POST /api/v1/test-sets/{id}/items/add-sql-lib` appends ordered SQL Lib IDs, `DELETE /api/v1/test-sets/{id}/items/{itemId}` removes one membership, and `PUT /api/v1/test-sets/{id}/items/reorder` persists a new order.
- Runs keep request-backed pagination and now support optional `jobId` plus optional `status`. The job filter is clearable so operators can return to global history, and changing either filter resets pagination to page 1 before reloading `/runs`.
- `GET /api/v1/runs` now supports both filtered and global history reads: pass `jobId` for the existing per-job list, or omit it to fetch the newest benchmark runs across all jobs without a backend error.
- `GET /api/v1/preflight` only performs live probes for Kylin REST and Presto. The returned `mysql.status=OK` row is currently a UI shortcut based on the benchmark service already starting with its configured MySQL datasource; it is not a live socket/query probe and should not be treated as standalone database liveness evidence.
- Benchmark preflight probe endpoints and Kylin credentials are configuration-driven through `benchmark.preflight.*`. The benchmark module no longer hard-codes those probe values in Java; operators can override them with `BENCHMARK_PREFLIGHT_KYLIN_AUTH_URL`, `BENCHMARK_PREFLIGHT_KYLIN_USER`, `BENCHMARK_PREFLIGHT_KYLIN_PASSWORD`, and `BENCHMARK_PREFLIGHT_PRESTO_INFO_URL`.
- Benchmark run diagnostics now record an explicit execution-mode summary in `jobSnapshotJson` and `evaluationJson`, including mixed-mode source sets when a run combines `STATEMENT` and `PREPARED_STATEMENT` SQL sources.
- Failed or partially failed benchmark runs now persist a grouped `failureBreakdown` in `evaluationJson` and `/api/v1/runs/{id}/context`, with SQL label, execution mode, routed target, failure count, and a bounded sample message for each diagnostic group.
- For failed runs with no valid samples, `evaluationJson.verdict`, `summary`, `phase`, `issues`, `diagnostics.failureBreakdown`, and `meta.executionModeSummary` are the stable contract. The failure-path `metrics.note` and `jdbcComparisonHints.dimensions` content remain best-effort placeholder guidance rather than measured latency/QPS output.
- The dashboard active-run card reads `/api/v1/runs/active`; that endpoint now reconciles stale `RUNNING` rows before responding and returns the most recently started live run when more than one exists.
- Flyway seed content under `V2`, `V5`, and `V6` is starter content for local bootstrap and regression runs. `sample_agg` no longer survives as the original `SELECT 1` placeholder on a fresh schema, but the remaining seeded SQL Lib entries, test sets, and jobs still model `learn_kylin` and `presto_local` examples and should be edited or removed for production-like environments instead of being mistaken for live workload evidence.
- A reproducible public TPC-H / Presto sample import now lives under `benchmark/src/main/resources/samples/test-sets/`; see `docs/modules/benchmark-sample-data.md` for the upstream Trino source, normalization notes, and the exact `curl` re-import command.

## Run

```bash
docker compose up -d mysql redis
bash scripts/init-db.sh dev
cd /Users/sfc/Documents/projects/engine/query && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd /Users/sfc/Documents/projects/engine/manager && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd /Users/sfc/Documents/projects/engine/benchmark && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd /Users/sfc/Documents/projects/engine/benchmark/frontend && npm run dev
```

`benchmark` no longer applies Flyway migrations during normal startup. Run
`bash scripts/init-db.sh <dev|test|pro> [manager] [benchmark]` before booting
`manager` or `benchmark` so the shared metadata schema and seed data are
created explicitly. Benchmark runtime settings are now centralized in
`application-dev.yml`, `application-test.yml`, and `application-pro.yml`.

## Verify

```bash
mvn -q -f benchmark/pom.xml test
npm --prefix benchmark/frontend run test
npm --prefix benchmark/frontend run build
```

## Related Docs

- [docs/architecture/overview.md](/Users/sfc/Documents/projects/engine/docs/architecture/overview.md)
- [docs/operations/local-development.md](/Users/sfc/Documents/projects/engine/docs/operations/local-development.md)
- [docs/modules/benchmark-sample-data.md](/Users/sfc/Documents/projects/engine/docs/modules/benchmark-sample-data.md)
- [docs/modules/benchmark-test-matrix.md](/Users/sfc/Documents/projects/engine/docs/modules/benchmark-test-matrix.md)
- [docs/operations/validation-matrix.md](/Users/sfc/Documents/projects/engine/docs/operations/validation-matrix.md)
