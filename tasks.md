# Tasks

This is the canonical task ledger for Easy Engine. The foreman model reads this file, picks a task at human direction, works directly in the current session, and writes progress and outcomes back into each task entry.

Before any agent picks or starts a task from this ledger, it must read [AGENTS.md](/Users/sfc/Documents/projects/engine/AGENTS.md) first and follow that contract.

## Status values

| Status | Meaning |
|--------|---------|
| `todo` | Ready to work, dependencies met |
| `in_progress` | Currently being worked |
| `done` | Completed and committed |
| `blocked` | Failed max attempts, needs human review |

---

## Todo

### MGR-TEST-001

- **Status:** in_progress
- **Module:** manager | **Type:** testing + docs | **Priority:** 20
- **Title:** INVENTORY MANAGER FUNCTIONS AND ADD TEST-TO-FUNCTION TRACEABILITY
- **Attempts:** 0

**Context files**

- `manager/pom.xml`
- `manager/src/main/java/com/smartbi/engine/`
- `manager/src/test/java/com/smartbi/engine/`
- `manager/frontend/package.json`
- `manager/frontend/src/`
- `docs/modules/manager.md`
- `docs/operations/testing-standard.md`

**Acceptance criteria**

1. Add `docs/modules/manager-test-matrix.md` with a function inventory for non-trivial handwritten production functions in both `manager/src/main/java/com/smartbi/engine` and `manager/frontend/src`, grouped by backend and frontend area.
2. The manager test matrix maps each listed function and each meaningful logic branch to one or more concrete automated test cases, using `ClassName#methodName` for Java code and `path:functionName` or component method names for frontend code.
3. Manager backend tests are expanded so controller, service, persistence, and schema-backed branches have assertion-backed coverage for normal, boundary, and failure behavior where those branches exist, or the matrix documents an explicit exclusion and reason.
4. A frontend automated test command is added for `manager/frontend`, and frontend tests cover non-trivial operator-visible view, routing, state, and helper logic instead of relying on `build` alone.
5. Every manager test case includes a short adjacent comment that states what it tests and names the corresponding production function or component method.
6. `docs/operations/testing-standard.md` is updated to reference the manager test matrix and the required test-to-function traceability rule for future manager changes.
7. The manager module emits coverage artifacts or equivalent recorded evidence during validation so remaining uncovered functions or branches are visible in the task log.
8. `mvn -q -f manager/pom.xml test`, `npm --prefix manager/frontend run test`, and `npm --prefix manager/frontend run build` pass.

**Validation commands**

```bash
mvn -q -f manager/pom.xml test
npm --prefix manager/frontend run test
npm --prefix manager/frontend run build
```

**Progress log**

---

### BENCH-TEST-001

- **Status:** in_progress
- **Module:** benchmark | **Type:** testing + docs | **Priority:** 21
- **Title:** INVENTORY BENCHMARK FUNCTIONS AND CLOSE TEST BRANCH GAPS
- **Attempts:** 0

**Context files**

- `benchmark/pom.xml`
- `benchmark/src/main/java/com/smartbi/benchmark/`
- `benchmark/src/test/java/com/smartbi/benchmark/`
- `benchmark/frontend/package.json`
- `benchmark/frontend/src/`
- `docs/modules/benchmark.md`
- `docs/operations/testing-standard.md`

**Acceptance criteria**

1. Add `docs/modules/benchmark-test-matrix.md` with a function inventory for non-trivial handwritten production functions in both `benchmark/src/main/java/com/smartbi/benchmark` and `benchmark/frontend/src`, grouped by backend and frontend area.
2. The benchmark test matrix maps each listed function and each meaningful logic branch to one or more concrete automated test cases, using `ClassName#methodName` for Java code and `path:functionName` or component method names for frontend code.
3. Benchmark backend tests are expanded so run orchestration, reporting, datasource, template, and controller logic have assertion-backed coverage for success, partial-failure, and failure branches where those branches exist, or the matrix documents an explicit exclusion and reason.
4. A frontend automated test command is added for `benchmark/frontend`, and frontend tests cover non-trivial operator-visible view, filtering, run-state, chart-shaping, and helper logic instead of relying on `build` alone.
5. Every benchmark test case includes a short adjacent comment that states what it tests and names the corresponding production function or component method.
6. `docs/operations/testing-standard.md` is updated to reference the benchmark test matrix and the required test-to-function traceability rule for future benchmark changes.
7. The benchmark module emits coverage artifacts or equivalent recorded evidence during validation so remaining uncovered functions or branches are visible in the task log.
8. `mvn -q -f benchmark/pom.xml test`, `npm --prefix benchmark/frontend run test`, and `npm --prefix benchmark/frontend run build` pass.

**Validation commands**

```bash
mvn -q -f benchmark/pom.xml test
npm --prefix benchmark/frontend run test
npm --prefix benchmark/frontend run build
```

**Progress log**

---

### BENCH-UX-005

- **Status:** in_progress
- **Module:** benchmark | **Type:** frontend | **Priority:** 22
- **Title:** COMPACT LONG SQL CONTENT IN TEMPLATE LIST
- **Attempts:** 0

**Context files**

- `benchmark/frontend/src/views/Templates.vue`
- `benchmark/frontend/src/i18n.js`
- `benchmark/frontend/test/`

**Acceptance criteria**

1. The benchmark SQL templates list no longer renders the full SQL text inline for long statements.
2. Each row shows a compact preview that keeps the table readable even when a template contains hundreds or thousands of lines.
3. Operators still have an obvious way to inspect the full SQL text for a row without editing backend data.
4. Existing template search, edit, delete, and debug actions continue to work.
5. Frontend automated coverage is added for the preview behavior.
6. `npm --prefix benchmark/frontend run test` and `npm --prefix benchmark/frontend run build` pass.

**Validation commands**

```bash
npm --prefix benchmark/frontend run test
npm --prefix benchmark/frontend run build
```

**Progress log**

---

---

## Done

| ID | Title | Module | Done signal |
|----|-------|--------|-------------|
| HARNESS-GOV-001 | ENFORCE TASK FINALIZATION, COMMIT HYGIENE, AND BLOCKED-ISSUE ESCALATION | platform | Completion and escalation rules are now aligned across `AGENTS.md`, the operations runbook, git workflow, and `INBOX.md`; `.agent/config.json` now uses the `<task-id>: <short title>` commit template; `scripts/task_audit.py --check` now catches done-task drift, duplicate IDs, non-grandfathered missing commit links, and blocked-task entries that omit `Next action:` or `Escalation:`; `ARCH-010` now records `Escalation: none`; legacy harness drift is documented in `INBOX-20260331-001`; and `python3 scripts/task_audit.py --check`, the contract grep check, and the git-history check all pass. |
| QUERY-TEST-001 | INVENTORY QUERY FUNCTIONS AND MAP TEST CASES TO LOGIC BRANCHES | query | `docs/modules/query-test-matrix.md` now maps the non-trivial query function inventory to concrete automated tests, every query `@Test` carries an adjacent `ClassName#methodName` traceability comment, JaCoCo artifacts are emitted under `query/target/site/jacoco/`, and `mvn -q -f query/pom.xml test` passes; remaining report misses are limited to excluded boilerplate/wiring classes and non-meaningful defensive branches documented in the matrix |
| ARCH-014 | RESTORE LOCAL KYLIN CONTAINER READINESS FOR E2E | platform | Fresh `kylin` image rebuild now returns `200` from `/kylin/api/user/authentication`, `docker compose ps --all` reports `healthy`, and in-container YARN shows `Total Nodes:1` with no `sparder_on_docker` app stuck in `ACCEPTED`; remaining benchmark/query failures are now app-layer follow-up instead of container readiness |
| ARCH-013 | VERIFY MYSQL STACK HEALTH FOR LOCAL E2E | platform | MySQL, Redis, Presto, manager, query, and benchmark were verified on local ports; benchmark startup required the forward `V16__benchmark_job_data_source_id_bigint.sql` migration; the remaining Kylin blocker was recorded for follow-up |
| ARCH-012 | FIX QUERY MYSQL REGRESSIONS AND TEST ISOLATION | query | `query` tests now force manager-config fallback in test scope, the stray query auth endpoint is removed, `mvn -q -f query/pom.xml test` passes, and the app starts on `8092` |
| ARCH-011 | NORMALIZE MYSQL MIGRATION DRIFT IN TASKS AND DOCS | docs | Current repo instructions and done signals no longer describe PostgreSQL as the active runtime metadata store; `tests/README.md` and `tasks.md` now match the MySQL design |
| ARCH-009 | MIGRATE DEFAULT DATABASE FROM POSTGRESQL TO MYSQL | platform | MySQL is the default local metadata database across runtime config, migrations, scripts, and docs; manager/query/benchmark validations pass, and smoke remains environment-dependent on local Kylin availability |
| MGR-BUG-001 | FIX 404 ERROR ON QUERY-DATASOURCES API | manager | Backend restarted; `GET /api/v1/query-datasources` returns 200 OK |
| MGR-UX-002 | STANDARDIZE TYPOGRAPHY AND LAYOUT ACROSS ALL MANAGER PAGES | manager | All 5 pages fit in one screen; typography standardized; rows at 40px |
| ARCH-008 | UPDATE ARCHITECTURE DOCUMENTATION FOR NEW DESIGN | docs | Architecture overview and runbooks updated for the current three-service design; no `kylin-jdbc-cache` references remain in README/docs |
| ARCH-006 | ADD DATASOURCE CONFIG MANAGEMENT PAGE TO MANAGER UI | manager | Datasource page, route, and nav link added; `npm --prefix manager/frontend run build` passes |
| ARCH-007 | SUPPORT JDBC DRIVER JAR UPLOAD IN BENCHMARK | benchmark | `/api/v1/drivers` upload/list added; uploaded JDBC jars now work in datasource tests, debug queries, and benchmark runs; benchmark tests and frontend build pass |
| ARCH-005 | QUERY POLLS MANAGER FOR DATASOURCE CONFIGS | query | Manager-backed datasource loading added with static fallback; `mvn -q -f query/pom.xml test` passes |
| ARCH-004 | EXPOSE DATASOURCE CONFIG CRUD API IN MANAGER | manager | V7 seeds query datasource configs; `/api/v1/query-datasources` CRUD added; `mvn -q -f manager/pom.xml test` passes |
| ARCH-003 | WRITE TRACES DIRECTLY TO POSTGRESQL FROM QUERY | query | Query writes trace rows and pattern stats directly to MySQL; `mvn -q -f query/pom.xml test` passes at the time of completion |
| ARCH-002 | REMOVE REDIS TRACE CONSUMER FROM MANAGER | manager | Redis consumer path removed; manager tests pass without Redis |
| BENCH-UX-004 | STANDARDIZE TYPOGRAPHY ACROSS ALL BENCHMARK PAGES | benchmark | Typography consistent across all pages |
| ARCH-001 | REMOVE KYLIN-JDBC-CACHE MODULE | docs | Module directory deleted; manager/query/benchmark tests pass |
| BENCH-UX-003 | OPTIMIZE ALL BENCHMARK PAGES FOR SINGLE-SCREEN VIEW | benchmark | All pages fit in one screen |
| BENCH-UX-002 | OPTIMIZE BENCHMARK DASHBOARD FOR SINGLE-SCREEN VIEW | benchmark | Dashboard fits in one screen |
| QUERY-ARCH-001 | DEFINE QUERY AS EXECUTION SOURCE OF TRUTH | query | Architecture boundaries documented |
| QUERY-DOC-001 | EXPAND QUERY ROUTING DOCUMENTATION | query | YH_TARGET_ENGINE precedence documented |
| TRACE-MODE-001 | RECORD EXECUTION MODE FOR EVERY SQL TRACE | query | Execution mode added to trace and benchmark records |
| TRACE-PARAM-001 | EXPOSE SQL PARAMETER VALUES FOR FAILED EXECUTIONS | query | parameterPayload added; ingested by manager |
| BENCH-E2E-001 | STRENGTHEN BENCHMARK E2E SCENARIO COVERAGE | benchmark | Tests assert structured run artifacts; builds pass |
| BENCH-REG-001 | HARDEN REGRESSION VISIBILITY FOR PRESTO AND PREPARED EXECUTION FAILURES | benchmark | Regression fields asserted in E2E tests |
| BENCH-UX-001 | IMPROVE PREPARED-STATEMENT AUTHORING CLARITY IN BENCHMARK UI | benchmark | UI flow improved |
| LIST-FILTER-001 | ADD SEARCH AND FILTER CONTROLS TO LIST VIEWS | frontend | Filters added; builds pass |
| MGR-CSS-001 | AUDIT AND FIX MANAGER FRONTEND CSS ISSUES | manager | CSS defects fixed; build passes |
| MGR-DASH-001 | AUDIT MANAGER DASHBOARD METRICS AND LOADING-STATE CONSISTENCY | manager | Dashboard corrected |
| HARNESS-RUNLOOP-001 | IMPLEMENT A CONTINUOUS RUN-UNTIL-EMPTY HARNESS LOOP | docs | Loop implemented |
| DOC-LOOP-001 | CONVERT LEGACY DOC REDIRECTS INTO CONCISE CANONICAL POINTERS | docs | Legacy doc/ tree removed; README shims reduced to pointers |
| DOC-CN-001 | KEEP SELECTED CHINESE MIRRORS ALIGNED WITH ENGLISH SOURCE DOCS | docs | Mirrors synced |
