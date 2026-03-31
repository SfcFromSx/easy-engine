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

No open tasks.

---

## Done

| ID | Title | Module | Done signal |
|----|-------|--------|-------------|
| ARCH-009 | MIGRATE DEFAULT DATABASE FROM POSTGRESQL TO MYSQL | platform | MySQL is the default local metadata database across runtime config, migrations, scripts, and docs; manager/query/benchmark validations pass, and smoke remains environment-dependent on local Kylin availability |
| MGR-BUG-001 | FIX 404 ERROR ON QUERY-DATASOURCES API | manager | Backend restarted; `GET /api/v1/query-datasources` returns 200 OK |
| MGR-UX-002 | STANDARDIZE TYPOGRAPHY AND LAYOUT ACROSS ALL MANAGER PAGES | manager | All 5 pages fit in one screen; typography standardized; rows at 40px |
| ARCH-008 | UPDATE ARCHITECTURE DOCUMENTATION FOR NEW DESIGN | docs | Architecture overview and runbooks updated for the current three-service design; no `kylin-jdbc-cache` references remain in README/docs |
| ARCH-006 | ADD DATASOURCE CONFIG MANAGEMENT PAGE TO MANAGER UI | manager | Datasource page, route, and nav link added; `npm --prefix manager/frontend run build` passes |
| ARCH-007 | SUPPORT JDBC DRIVER JAR UPLOAD IN BENCHMARK | benchmark | `/api/v1/drivers` upload/list added; uploaded JDBC jars now work in datasource tests, debug queries, and benchmark runs; benchmark tests and frontend build pass |
| ARCH-005 | QUERY POLLS MANAGER FOR DATASOURCE CONFIGS | query | Manager-backed datasource loading added with static fallback; `mvn -q -f query/pom.xml test` passes |
| ARCH-004 | EXPOSE DATASOURCE CONFIG CRUD API IN MANAGER | manager | V7 seeds query datasource configs; `/api/v1/query-datasources` CRUD added; `mvn -q -f manager/pom.xml test` passes |
| ARCH-003 | WRITE TRACES DIRECTLY TO POSTGRESQL FROM QUERY | query | Query writes trace rows and pattern stats directly to PostgreSQL; `mvn -q -f query/pom.xml test` passes |
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
