# Testing Standard

This document is the execution checklist for agents adding or validating work in Easy Engine. Use it together with [validation-matrix.md](/Users/sfc/Documents/projects/engine/docs/operations/validation-matrix.md).

## Core Rule

Every task must finish with evidence, not only implementation.

Agents should validate at four layers when relevant:

1. task-scoped automated commands from the task entry in `tasks.md`
2. scenario-level behavior for the changed path
3. structured record or API artifact verification after the scenario runs
4. local refresh and service health confirmation when the harness applies them

If a task changes structured telemetry, reports, traces, or operator-visible JSON, it is not enough to assert only `status == OK` or `build passed`. The test or validation must also query the stored or returned structured artifacts and verify their content.

## Required Checklist

- Read the task's `validation_commands` in [tasks.md](/Users/sfc/Documents/projects/engine/tasks.md) before implementation.
- Keep validation module-scoped unless the task changes a shared contract.
- If the touched module has a function-to-test matrix, update it in the same change.
- Prefer adding or extending automated tests in the touched module over manual-only verification.
- Add a short adjacent comment above every benchmark/query/manager test case that names the covered production function or component method.
- When a scenario produces structured records, query them after execution and assert they exist with the expected shape and values.
- When a task changes frontend operator behavior, validate both backend data shape and frontend rendering assumptions.
- Record validation evidence in the task result using concrete commands, files, and artifacts.
- For `query` test changes, keep [docs/modules/query-test-matrix.md](/Users/sfc/Documents/projects/engine/docs/modules/query-test-matrix.md) aligned with the production function inventory and add a short adjacent comment above each `@Test` naming the mapped production function as `ClassName#methodName`.

## Current Automated Commands

### Benchmark

```bash
mvn -q -f benchmark/pom.xml test
npm --prefix benchmark/frontend run test
npm --prefix benchmark/frontend run build
```

### Query

```bash
mvn -q -f query/pom.xml test
```

### Manager

```bash
mvn -q -f manager/pom.xml test
npm --prefix manager/frontend run test
npm --prefix manager/frontend run build
```

## Module Inventory

### Benchmark

Current backend test inventory:

- [`benchmark/src/test/java/com/smartbi/benchmark/web/BenchmarkSmokeTest.java`](/Users/sfc/Documents/projects/engine/benchmark/src/test/java/com/smartbi/benchmark/web/BenchmarkSmokeTest.java): basic controller smoke coverage.
- [`docs/modules/benchmark-test-matrix.md`](/Users/sfc/Documents/projects/engine/docs/modules/benchmark-test-matrix.md): benchmark function inventory, exclusions, and branch-to-test traceability.
- [`benchmark/src/test/java/com/smartbi/benchmark/report/BenchmarkRunReportServiceTest.java`](/Users/sfc/Documents/projects/engine/benchmark/src/test/java/com/smartbi/benchmark/report/BenchmarkRunReportServiceTest.java): report-shaping checks.
- [`benchmark/src/test/java/com/smartbi/benchmark/run/BenchmarkAsyncRunnerExecutionModeIntegrationTest.java`](/Users/sfc/Documents/projects/engine/benchmark/src/test/java/com/smartbi/benchmark/run/BenchmarkAsyncRunnerExecutionModeIntegrationTest.java): runner-path integration coverage for execution-mode and failure grouping.
- [`benchmark/src/test/java/com/smartbi/benchmark/web/RunControllerContextTest.java`](/Users/sfc/Documents/projects/engine/benchmark/src/test/java/com/smartbi/benchmark/web/RunControllerContextTest.java): `/runs/{id}/context` API contract.
- [`benchmark/src/test/java/com/smartbi/benchmark/migration/BenchmarkFlywaySeedTest.java`](/Users/sfc/Documents/projects/engine/benchmark/src/test/java/com/smartbi/benchmark/migration/BenchmarkFlywaySeedTest.java): Flyway seed and migration coverage.
- [`benchmark/src/test/java/com/smartbi/benchmark/run/BenchmarkExecutionServiceTest.java`](/Users/sfc/Documents/projects/engine/benchmark/src/test/java/com/smartbi/benchmark/run/BenchmarkExecutionServiceTest.java): run start-guard and stale-run recovery coverage.
- [`benchmark/src/test/java/com/smartbi/benchmark/testset/TestSetImportServiceTest.java`](/Users/sfc/Documents/projects/engine/benchmark/src/test/java/com/smartbi/benchmark/testset/TestSetImportServiceTest.java): Excel import branch coverage.
- [`benchmark/src/test/java/com/smartbi/benchmark/web/*.java`](/Users/sfc/Documents/projects/engine/benchmark/src/test/java/com/smartbi/benchmark/web): controller/service unit coverage for datasource, job, preflight, query, run, template, and test-set logic.

Frontend validation today:

- `npm --prefix benchmark/frontend run test`
- `npm --prefix benchmark/frontend run build`
- coverage artifacts under `benchmark/frontend/coverage`

Benchmark-specific checklist:

- Keep [`docs/modules/benchmark-test-matrix.md`](/Users/sfc/Documents/projects/engine/docs/modules/benchmark-test-matrix.md) aligned with every benchmark logic change and document any exclusions explicitly.
- For run-execution changes, execute a realistic benchmark scenario through `BenchmarkAsyncRunner` or the relevant web/controller flow.
- After completion, assert the persisted `BenchmarkRun` row fields, not only the returned HTTP status.
- Parse and verify `jobSnapshotJson`.
- Parse and verify `evaluationJson`.
- Query and verify `/api/v1/runs/{id}/context`.
- If failures are expected, assert `failureBreakdown` grouping, counts, routed target, execution mode, and sample message.
- If active-run behavior is touched, assert `/api/v1/runs/active` before and after completion or recovery.
- If the Runs drawer contract changes, keep [`benchmark/frontend/src/views/Runs.vue`](/Users/sfc/Documents/projects/engine/benchmark/frontend/src/views/Runs.vue) aligned with the structured JSON shape.
- Keep benchmark frontend logic testable through shared helpers/composables where appropriate, and do not rely on `build` alone for operator-visible behavior.

### Query

Current backend test inventory:

- [modules/query-test-matrix.md](/Users/sfc/Documents/projects/engine/docs/modules/query-test-matrix.md): canonical query function inventory and branch-to-test traceability map.
- [`query/src/test/java/com/smartbi/query/parsing/SqlCommentParserTest.java`](/Users/sfc/Documents/projects/engine/query/src/test/java/com/smartbi/query/parsing/SqlCommentParserTest.java): parsing-level unit checks.
- [`query/src/test/java/com/smartbi/query/service/QueryCacheServiceTest.java`](/Users/sfc/Documents/projects/engine/query/src/test/java/com/smartbi/query/service/QueryCacheServiceTest.java): cache-service unit checks.
- [`query/src/test/java/com/smartbi/query/web/QueryWebIntegrationTest.java`](/Users/sfc/Documents/projects/engine/query/src/test/java/com/smartbi/query/web/QueryWebIntegrationTest.java): end-to-end query web and trace publication behavior.
- [`query/src/test/java/com/smartbi/query/support/InMemoryQueryInfrastructure.java`](/Users/sfc/Documents/projects/engine/query/src/test/java/com/smartbi/query/support/InMemoryQueryInfrastructure.java) and [`query/src/test/java/com/smartbi/query/support/QueryTestConfiguration.java`](/Users/sfc/Documents/projects/engine/query/src/test/java/com/smartbi/query/support/QueryTestConfiguration.java): test scaffolding.

Query-specific checklist:

- Update [docs/modules/query-test-matrix.md](/Users/sfc/Documents/projects/engine/docs/modules/query-test-matrix.md) whenever a non-trivial query function or branch changes.
- Add a short adjacent comment above each query `@Test` that states what it covers and names the mapped production function as `ClassName#methodName`.
- For routing, cache, prepared execution, or trace changes, prefer extending `QueryWebIntegrationTest` so the executed request path is realistic.
- Assert both the direct query response and the published trace payload when trace or execution metadata changes.
- If the change affects downstream manager ingestion contracts, verify the manager-side persistence/API path too, not only query-side output.
- When running `mvn -q -f query/pom.xml test`, record the JaCoCo report path under `query/target/site/jacoco/` and summarize any remaining uncovered inventoried branches or explicit exclusions from the matrix.

### Manager

Current backend test inventory:

- [`manager/src/test/java/com/smartbi/engine/accel/AccelerationServiceTest.java`](/Users/sfc/Documents/projects/engine/manager/src/test/java/com/smartbi/engine/accel/AccelerationServiceTest.java)
- [`manager/src/test/java/com/smartbi/engine/jdbc/JdbcSqlAdvisorServiceTest.java`](/Users/sfc/Documents/projects/engine/manager/src/test/java/com/smartbi/engine/jdbc/JdbcSqlAdvisorServiceTest.java)
- [`manager/src/test/java/com/smartbi/engine/parse/SqlParseServiceTest.java`](/Users/sfc/Documents/projects/engine/manager/src/test/java/com/smartbi/engine/parse/SqlParseServiceTest.java)
- [`manager/src/test/java/com/smartbi/engine/trace/TraceIngestionTest.java`](/Users/sfc/Documents/projects/engine/manager/src/test/java/com/smartbi/engine/trace/TraceIngestionTest.java)
- [`manager/src/test/java/com/smartbi/engine/trace/TraceFlywayExecutionModeIntegrationTest.java`](/Users/sfc/Documents/projects/engine/manager/src/test/java/com/smartbi/engine/trace/TraceFlywayExecutionModeIntegrationTest.java): Flyway-plus-validate integration coverage for trace schema and API.
- [`manager/src/test/java/com/smartbi/engine/web/AccelerationLifecycleTest.java`](/Users/sfc/Documents/projects/engine/manager/src/test/java/com/smartbi/engine/web/AccelerationLifecycleTest.java)
- [`manager/src/test/java/com/smartbi/engine/web/ManagerApiTest.java`](/Users/sfc/Documents/projects/engine/manager/src/test/java/com/smartbi/engine/web/ManagerApiTest.java)
- [`manager/src/test/java/com/smartbi/engine/web/StatsControllerTest.java`](/Users/sfc/Documents/projects/engine/manager/src/test/java/com/smartbi/engine/web/StatsControllerTest.java)
- [`manager/src/test/java/com/smartbi/engine/web/TraceControllerTest.java`](/Users/sfc/Documents/projects/engine/manager/src/test/java/com/smartbi/engine/web/TraceControllerTest.java)

Frontend validation today:

- `npm --prefix manager/frontend run test`
- `npm --prefix manager/frontend run build`
- [`docs/modules/manager-test-matrix.md`](/Users/sfc/Documents/projects/engine/docs/modules/manager-test-matrix.md): function-to-test inventory for backend and frontend manager logic

Manager-specific checklist:

- Keep [`docs/modules/manager-test-matrix.md`](/Users/sfc/Documents/projects/engine/docs/modules/manager-test-matrix.md) aligned whenever manager logic or tests change.
- Every manager backend/frontend test should include a short adjacent comment that names the covered production function as `ClassName#methodName` or `path:functionName`.
- If a task changes trace or persisted execution metadata, verify repository persistence and `/api/v1/traces` API exposure.
- If a task changes schema-backed fields, prefer a Flyway-backed integration path, not only `create-drop` tests.
- For frontend tasks, confirm the backend contract is unchanged or explicitly updated in docs.

## Required Structured Artifact Checks

Use these as the default expectations when a task touches structured output.

### Benchmark structured artifacts

- `BenchmarkRun` persisted fields
- `jobSnapshotJson`
- `evaluationJson`
- `/api/v1/runs/{id}/context`
- `failureBreakdown` when failures occur
- `/api/v1/runs/active` when active-run semantics change

### Query structured artifacts

- direct query HTTP response
- published trace payload
- any execution-mode, parameter-payload, or cache metadata emitted by query

### Manager structured artifacts

- persisted `SqlExecutionRecord` fields
- trace ingestion compatibility for legacy payloads when relevant
- `/api/v1/traces` and related DTO shape

## Escalation Rules

Escalate beyond the default module command set when:

- the task changes cross-service contracts
- the task changes structured records consumed by another module
- the task changes active-run, reconciliation, or recovery logic
- the task changes harness workflow or machine-state behavior

When in doubt, prefer one realistic integration scenario plus explicit structured-artifact assertions over adding another shallow status-only test.
