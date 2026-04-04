# Product Backlog

This file merges the durable backlog signals from the previous root `todo.md` and legacy `doc/todo.md`.

## High Priority

### Root Docs and Naming

- `DOC-LOOP-001`: keep legacy doc redirects minimal and move all canonical content into `docs/`.
- `DOC-CN-001`: maintain selected Chinese mirrors for human-facing docs.

### Manager

- `MGR-DASH-001`: audit dashboard metrics and loading-state consistency against the backend summary contract.
- `TRACE-MODE-001`: record `STATEMENT` versus `PREPARED_STATEMENT` explicitly in query/manager execution records.
- `TRACE-PARAM-001`: expose actual SQL parameters or a readable parameter payload for failed executions.
- Improve acceleration lifecycle depth and scheduling maturity.

### Benchmark

- `BENCH-E2E-001`: add stronger benchmark E2E scenario coverage that verifies structured run artifacts and context records after each scenario completes.
- `BENCH-REG-001`: make benchmark regression failures easier to diagnose by surfacing which SQL labels / execution modes / routed targets failed.
- `BENCH-ACTIVE-001`: recover stale `RUNNING` benchmark runs so the dashboard active-run card does not stay stuck on phantom jobs.
- `BENCH-RUNS-API-001`: fix the `/api/v1/runs` list contract so global run queries do not fail with a backend 500.
- Improve datasource query tooling for prepared execution parity.
- `STYLE-04`: benchmark migration coverage still depends on a Testcontainers-compatible Docker environment, so local `mvn test` is not yet a full migration guarantee.
- `LIST-FILTER-001`: add practical list-page filters and search controls across manager and benchmark operator tables.

### Query

- `QUERY-DOC-001`: keep routed-query documentation aligned with preserved metadata behavior.
- `QUERY-FUNC-001`: audit and harden prepared-statement cache semantics in `query`.
- Continue hardening the post-redesign split between `benchmark`, `query`, and `manager` after the ARCH task set recorded in [tasks.md](/Users/sfc/Documents/projects/engine/tasks.md).
- `QUERY-ARCH-001`: define `query` as the execution source of truth and document the migration boundary from `jdbc`.
- `QUERY-ARCH-002`: inventory execution semantics still owned by `jdbc` and rank migration priority for future extraction.

## Medium Priority

- `STYLE-01`: fix the invalid `border-bottom: 1px border #f1f5f9;` CSS in `manager/frontend/src/views/Dashboard.vue`.
- `MGR-CSS-001`: audit and fix remaining manager frontend CSS inconsistencies, spacing defects, and table/filter-bar styling drift.
- `STYLE-02`: split `benchmark/src/main/java/com/smartbi/benchmark/run/BenchmarkAsyncRunner.java` into smaller responsibilities.
- `STYLE-03`: reduce multiple SLF4J bindings introduced by the fat JDBC jar to cut runtime log noise.
- Reduce any remaining duplicated routing and cache semantics across services.
- Improve manager mobile density and table ergonomics.
- Add stronger browser-level end-to-end validation for operator flows.
- `TEST-01`: reduce cross-service E2E validation to a single reproducible command for `benchmark`, `query`, and `manager`.
- `TEST-02`: add seed-level assertions for renamed branding, doc assumptions, and runtime contracts in benchmark tests.

## Issue Intake

- `ISSUE-COLLECT-001`: collect newly discovered issues into [INBOX.md](/Users/sfc/Documents/projects/engine/INBOX.md) only when they still need human review before task creation.
- Harness-framework issues should go to the inbox only when the required change is still undecided; directly requested or already-decided harness work should be tracked as tasks instead.
