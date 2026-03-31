# Tasks

This is the canonical task ledger for Easy Engine. The foreman model reads this file, picks a task at human direction, works directly in the current session, and writes progress and outcomes back into each task entry.

## Status values

| Status | Meaning |
|--------|---------|
| `todo` | Ready to work, dependencies met |
| `in_progress` | Currently being worked |
| `done` | Completed and committed |
| `blocked` | Failed max attempts, needs human review |

---

## Todo

### MGR-CSS-002

- **Status:** todo
- **Module:** manager | **Type:** frontend | **Priority:** 0
- **Title:** Redesign and rewrite manager frontend UI (discard legacy CSS/layouts)
- **Attempts:** 0

**Context files**

- `manager/frontend/src/App.vue`
- `manager/frontend/src/style.css`
- `manager/frontend/src/views/Dashboard.vue`
- `manager/frontend/src/views/Traces.vue`
- `manager/frontend/src/views/Patterns.vue`
- `manager/frontend/src/views/Acceleration.vue`
- `docs/modules/manager.md`

**Acceptance criteria**

1. Legacy CSS and layout structures are discarded in favor of a modern, premium redesign for all main manager views.
2. The new UI uses a cohesive visual system with vibrant colors, dark mode support, and smooth transitions.
3. All manager routes remain fully functional and integrate with existing backend APIs without any backend code modifications.
4. The implementation adheres to premium web aesthetics (e.g., custom typography, gradients, and micro-animations) to provide a world-class user experience.
5. `npm --prefix manager/frontend run build` succeeds.

**Validation commands**

```bash
npm --prefix manager/frontend run build
```

**Progress log**

<!-- Foreman appends stage outcomes here during execution -->

---

### BENCH-CSS-001

- **Status:** todo
- **Module:** benchmark | **Type:** frontend | **Priority:** 0
- **Title:** Redesign and rewrite benchmark frontend UI (discard legacy CSS/layouts)
- **Attempts:** 0

**Context files**

- `benchmark/frontend/src/App.vue`
- `benchmark/frontend/src/style.css`
- `benchmark/frontend/src/views/Dashboard.vue`
- `benchmark/frontend/src/views/JobDetail.vue`
- `benchmark/frontend/src/views/Runs.vue`
- `docs/modules/benchmark.md`

**Acceptance criteria**

1. Legacy CSS and layout structures are discarded in favor of a modern, premium redesign for all main benchmark views.
2. The new UI implements high-fidelity visuals (e.g., glassmorphism, gradients, and unified interactive states).
3. All benchmark routes remain fully functional and integrate with existing backend APIs without any backend code modifications.
4. The implementer prioritizes visual excellence and responsiveness as defined in project design standards.
5. `npm --prefix benchmark/frontend run build` succeeds.

**Validation commands**

```bash
npm --prefix benchmark/frontend run build
```

**Progress log**

<!-- Foreman appends stage outcomes here during execution -->

---

### BENCH-ACTIVE-001

- **Status:** todo
- **Module:** benchmark | **Type:** backend | **Priority:** 42
- **Title:** RECOVER AND PREVENT STALE RUNNING BENCHMARK RUNS FROM BLOCKING THE ACTIVE-RUN SURFACE
- **Attempts:** 0 | **Last failure:** previous delegated-harness implementer attempt failed with code 1 on attempt 2/2

**Context files**

- `benchmark/src/main/java/com/smartbi/benchmark/run/BenchmarkExecutionService.java`
- `benchmark/src/main/java/com/smartbi/benchmark/run/BenchmarkAsyncRunner.java`
- `benchmark/src/main/java/com/smartbi/benchmark/web/RunController.java`
- `benchmark/src/main/java/com/smartbi/benchmark/repo/BenchmarkRunRepository.java`
- `benchmark/frontend/src/views/Dashboard.vue`
- `benchmark/frontend/src/views/JobDetail.vue`
- `docs/modules/benchmark.md`

**Acceptance criteria**

1. A stale benchmark run that remains in RUNNING state after its worker has died no longer keeps `/api/v1/runs/active` non-empty forever.
2. The benchmark dashboard stops showing a phantom long-running job once the backend has determined that no live benchmark execution remains.
3. Stale RUNNING recovery works without requiring a manual service restart or starting a new benchmark job just to trigger reconciliation.
4. The fix is covered by benchmark-side tests for stale RUNNING recovery and active-run selection behavior.

**Validation commands**

```bash
mvn -q -f benchmark/pom.xml test
npm --prefix benchmark/frontend run build
```

**Progress log**

<!-- Foreman appends stage outcomes here during execution -->

---

### BENCH-RUNS-API-001

- **Status:** todo
- **Module:** benchmark | **Type:** backend | **Priority:** 43
- **Title:** FIX BENCHMARK RUNS LIST CONTRACT SO GLOBAL RUN QUERIES DO NOT FAIL WITH 500
- **Attempts:** 0

**Context files**

- `benchmark/src/main/java/com/smartbi/benchmark/web/RunController.java`
- `benchmark/src/main/java/com/smartbi/benchmark/repo/BenchmarkRunRepository.java`
- `benchmark/frontend/src/views/Runs.vue`
- `benchmark/frontend/src/views/Dashboard.vue`
- `docs/modules/benchmark.md`

**Acceptance criteria**

1. Calling the benchmark runs list endpoint without `jobId` no longer fails with HTTP 500.
2. The backend contract for `/api/v1/runs` is explicit and consistent with current frontend assumptions, whether that means supporting global listing or rejecting unsupported requests in a controlled, documented way.
3. Benchmark-side automated tests cover the chosen `/api/v1/runs` contract so the global-listing or explicit-validation behavior does not regress.

**Validation commands**

```bash
mvn -q -f benchmark/pom.xml test
npm --prefix benchmark/frontend run build
```

**Progress log**

<!-- Foreman appends stage outcomes here during execution -->

---

### QUERY-FUNC-001

- **Status:** todo
- **Module:** query | **Type:** backend | **Priority:** 56
- **Title:** AUDIT AND HARDEN PREPARED-STATEMENT CACHE SEMANTICS IN QUERY
- **Attempts:** 0

**Context files**

- `query/src/main/java/com/smartbi/query/service/PreparedStatementCache.java`
- `query/src/main/java/com/smartbi/query/service/QueryExecutionService.java`
- `query/src/test/java/com/smartbi/query`
- `docs/modules/query.md`

**Acceptance criteria**

1. The prepared-statement cache hit and eviction semantics in `query` are audited and any identified correctness gaps are fixed.
2. Cache behavior under concurrent access or repeated parameterized queries does not silently return stale or mismatched results.
3. Correctness in query is treated as a first-class execution concern.
4. Any code changes stay inside query and its tests; jdbc code is not modified.

**Validation commands**

```bash
mvn -q -f query/pom.xml test
```

**Progress log**

<!-- Foreman appends stage outcomes here during execution -->

---

### QUERY-ARCH-002

- **Status:** todo
- **Module:** query | **Type:** architecture | **Priority:** 57
- **Title:** INVENTORY EXECUTION SEMANTICS STILL OWNED BY JDBC AND RANK MIGRATION PRIORITY
- **Attempts:** 0

**Context files**

- `docs/architecture/overview.md`
- `docs/modules/query.md`
- `docs/product/backlog.md`
- `query/src/main/java/com/smartbi/query`
- `kylin-jdbc-cache/src/main/java/com/kylin`

**Acceptance criteria**

1. There is a durable inventory of execution semantics grouped by area such as cache, routing, prepared execution, trace, and fallback.
2. Each item is classified as query-owned, jdbc-owned, shared temporarily, or deprecated, with migration priority noted.
3. The task does not require jdbc code changes.

**Validation commands**

_(none — documentation task)_

**Progress log**

<!-- Foreman appends stage outcomes here during execution -->

---

### ISSUE-COLLECT-001

- **Status:** todo
- **Module:** docs | **Type:** triage | **Priority:** 999
- **Title:** COLLECT NEWLY DISCOVERED ISSUES INTO A HUMAN-CONFIRMATION INBOX
- **Attempts:** 0
- **Depends on:** BENCH-E2E-001, BENCH-ACTIVE-001, BENCH-RUNS-API-001, LIST-FILTER-001, MGR-CSS-001, QUERY-DOC-001, QUERY-ARCH-001, QUERY-FUNC-001, QUERY-ARCH-002

**Context files**

- `INBOX.md`
- `docs/product/backlog.md`
- `tasks.md`
- `AGENTS.md`
- `docs/operations/human-collaboration.md`

**Acceptance criteria**

1. Newly discovered but unconfirmed issues are written into `INBOX.md` with evidence and suggested next steps.
2. The issue inbox remains distinct from implementation tasks and does not automatically expand into coding work.
3. Each collected issue is clearly marked as awaiting human confirmation.
4. Harness-framework issues are collected into the inbox instead of being changed directly unless a human explicitly approves a harness change.

**Validation commands**

_(none — documentation task)_

**Progress log**

<!-- Foreman appends stage outcomes here during execution -->

---

## Done

| ID | Title | Module | Outcome summary |
|----|-------|--------|-----------------|
| BENCH-E2E-001 | STRENGTHEN BENCHMARK E2E SCENARIO COVERAGE | benchmark | Tests assert structured run artifacts; builds pass |
| DOC-LOOP-001 | CONVERT LEGACY DOC REDIRECTS INTO CONCISE CANONICAL POINTERS | docs | Legacy doc/ tree removed; README shims reduced to pointers |
| DOC-CN-001 | KEEP SELECTED CHINESE MIRRORS ALIGNED WITH ENGLISH SOURCE DOCS | docs | Mirrors synced |
| MGR-DASH-001 | AUDIT MANAGER DASHBOARD METRICS AND LOADING-STATE CONSISTENCY | manager | Dashboard corrected |
| HARNESS-RUNLOOP-001 | IMPLEMENT A CONTINUOUS RUN-UNTIL-EMPTY HARNESS LOOP | docs | Loop implemented |
| TRACE-MODE-001 | RECORD EXECUTION MODE FOR EVERY SQL TRACE | query | Execution mode added to trace and benchmark records |
| TRACE-PARAM-001 | EXPOSE SQL PARAMETER VALUES FOR FAILED EXECUTIONS | query | parameterPayload added; ingested by manager |
| BENCH-REG-001 | HARDEN REGRESSION VISIBILITY FOR PRESTO AND PREPARED EXECUTION FAILURES | benchmark | Regression fields asserted in E2E tests |
| BENCH-UX-001 | IMPROVE PREPARED-STATEMENT AUTHORING CLARITY IN BENCHMARK UI | benchmark | UI flow improved |
| LIST-FILTER-001 | ADD SEARCH AND FILTER CONTROLS TO LIST VIEWS | frontend | Filters added; builds pass |
| MGR-CSS-001 | AUDIT AND FIX MANAGER FRONTEND CSS ISSUES | manager | CSS defects fixed; build passes |
| QUERY-DOC-001 | EXPAND QUERY ROUTING DOCUMENTATION | query | YH_TARGET_ENGINE precedence documented |
| QUERY-ARCH-001 | DEFINE QUERY AS EXECUTION SOURCE OF TRUTH | query | Architecture boundaries documented |
