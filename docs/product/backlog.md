# Product Backlog

This file merges the durable backlog signals from the previous root `todo.md` and legacy `doc/todo.md`.

## High Priority

### Root Docs and Naming

- `DOC-LOOP-001`: keep legacy doc redirects minimal and move all canonical content into `docs/`.
- `DOC-CN-001`: maintain selected Chinese mirrors for human-facing docs.

### Manager

- `MGR-DASH-001`: audit dashboard metrics and loading-state consistency against the backend summary contract.
- Improve acceleration lifecycle depth and scheduling maturity.

### Benchmark

- Improve datasource query tooling for prepared execution parity.
- `STYLE-04`: benchmark migration coverage still depends on a Testcontainers-compatible Docker environment, so local `mvn test` is not yet a full migration guarantee.
- `LIST-FILTER-001`: add practical list-page filters and search controls across manager and benchmark operator tables.

### Query

- `QUERY-DOC-001`: keep routed-query documentation aligned with preserved metadata behavior.
- Continue protecting compatibility between `query` and `kylin-jdbc-cache`.
- `QUERY-ARCH-001`: define `query` as the execution source of truth and document the long-term migration boundary from `jdbc`.
- `QUERY-FUNC-001`: audit and harden prepared-statement cache semantics in `query`.
- `QUERY-ARCH-002`: inventory execution semantics still owned by `jdbc` and rank migration priority for future extraction.

## Medium Priority

- `STYLE-01`: fix the invalid `border-bottom: 1px border #f1f5f9;` CSS in `manager/frontend/src/views/Dashboard.vue`.
- `MGR-CSS-001`: audit and fix remaining manager frontend CSS inconsistencies, spacing defects, and table/filter-bar styling drift.
- `STYLE-02`: split `benchmark/src/main/java/com/smartbi/benchmark/run/BenchmarkAsyncRunner.java` into smaller responsibilities.
- `STYLE-03`: reduce multiple SLF4J bindings introduced by the fat JDBC jar to cut runtime log noise.
- Reduce duplicated routing and cache semantics between `query` and `kylin-jdbc-cache`.
- Improve manager mobile density and table ergonomics.
- Add stronger browser-level end-to-end validation for operator flows.
- `TEST-01`: reduce cross-service E2E validation to a single reproducible command for `benchmark`, `query`, and `manager`.
- `TEST-02`: add seed-level assertions for renamed branding, doc assumptions, and runtime contracts in benchmark tests.

## Structural Risks

- The root Git boundary now exists, but archived nested repository history for `kylin-jdbc-cache` still needs an explicit long-term ownership decision.
- The workspace still carries module-local helper artifacts and legacy Java test helpers in `kylin-jdbc-cache` that may deserve further normalization.
