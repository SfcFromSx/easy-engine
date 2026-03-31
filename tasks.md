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

---

### ARCH-007

- **Status:** done
- **Module:** benchmark | **Type:** backend + frontend | **Priority:** 70
- **Title:** SUPPORT JDBC DRIVER JAR UPLOAD IN BENCHMARK
- **Attempts:** 0

**Context files**

- `benchmark/src/main/java/com/smartbi/benchmark/web/DataSourceController.java`
- `benchmark/src/main/java/com/smartbi/benchmark/web/BenchmarkQueryService.java`
- `benchmark/frontend/src/views/DataSources.vue`

**Acceptance criteria**

1. `POST /api/v1/drivers/upload` accepts a multipart `.jar` file and stores it under `./drivers/`.
2. Uploaded JAR is loaded via `URLClassLoader` + `DriverShim` and registered with `DriverManager`.
3. `GET /api/v1/drivers` returns a list of uploaded driver JAR filenames.
4. `BenchmarkQueryService` uses the driver-aware class loader when opening connections.
5. `DataSources.vue` shows an "Upload Driver" button that opens a file picker for `.jar` files and lists uploaded drivers.
6. Benchmark tests pass; frontend builds.

**Validation commands**

```bash
mvn -q -f benchmark/pom.xml test
npm --prefix benchmark/frontend run build
```

**Progress log**

<!-- Foreman appends stage outcomes here during execution -->

**2026-03-31 — implementation**
Files changed: `benchmark/src/main/java/com/smartbi/benchmark/config/BenchmarkJdbcProperties.java`, `benchmark/src/main/java/com/smartbi/benchmark/jdbc/*`, `benchmark/src/main/java/com/smartbi/benchmark/web/DataSourceController.java`, `benchmark/src/main/java/com/smartbi/benchmark/web/DriverController.java`, `benchmark/src/main/java/com/smartbi/benchmark/web/BenchmarkQueryService.java`, `benchmark/src/main/java/com/smartbi/benchmark/run/BenchmarkAsyncRunner.java`, `benchmark/frontend/src/views/DataSources.vue`, `benchmark/frontend/src/api/endpoints.js`, `benchmark/frontend/src/i18n.js`.
Commands run: `sed`, `rg`, `mvn -q -f benchmark/pom.xml test`, `npm --prefix benchmark/frontend run build`.
Result: implemented driver jar storage/list APIs, `URLClassLoader` + `DriverShim` registration, driver-aware query/run connection paths, and benchmark UI upload/list support.

**2026-03-31 — verification**
Validation status: approved
Evidence: `mvn -q -f benchmark/pom.xml test` passed, including `JdbcDriverUploadIntegrationTest`; `npm --prefix benchmark/frontend run build` passed.
Next action: commit.

**2026-03-31 — doc-gardener**
Files changed: `docs/architecture/http-interfaces.md`, `docs/architecture/overview.md`, `docs/modules/benchmark.md`, `docs/operations/local-development.md`.
Result: updated benchmark interface inventory and clarified that uploaded JDBC driver jars support datasource tests, debug queries, and benchmark runs.

---

### ARCH-008

- **Status:** todo
- **Module:** docs | **Type:** docs | **Priority:** 80
- **Title:** UPDATE ARCHITECTURE DOCUMENTATION FOR NEW DESIGN
- **Attempts:** 0
- **Depends on:** ARCH-001, ARCH-003, ARCH-004, ARCH-005, ARCH-006, ARCH-007

**Context files**

- `docs/architecture/overview.md`
- `README.md`
- `docs/operations/`

**Acceptance criteria**

1. `docs/architecture/overview.md` rewritten with new data flow: `benchmark (Kylin JDBC) → query → Kylin/Presto/Hive`, query writes to PostgreSQL, manager reads from PostgreSQL and manages datasource configs.
2. All references to `kylin-jdbc-cache` removed from docs.
3. `README.md` updated to remove `kylin-jdbc-cache` entrypoint.
4. `docs/operations/` runbooks updated (no `kylin-jdbc-cache` service to start).

**Validation commands**

_(none — documentation task)_

**Progress log**

<!-- Foreman appends stage outcomes here during execution -->

---

### BENCH-UX-002

- **Status:** done
- **Module:** benchmark | **Type:** frontend | **Priority:** 85
- **Title:** OPTIMIZE BENCHMARK DASHBOARD FOR SINGLE-SCREEN VIEW
- **Attempts:** 1

**Context files**

- `benchmark/frontend/src/views/Dashboard.vue`
- `benchmark/frontend/src/components/PerformanceCharts.vue`
- `benchmark/frontend/src/App.vue`

**Acceptance criteria**

1. Dashboard fits in a single screen (1920x1080) without scrolling.
2. Preflight and Execution sections are side-by-side.
3. Chart heights reduced to 300px.
4. Main layout padding reduced.

**Validation commands**

```bash
npm --prefix benchmark/frontend run build
```

**Progress log**

**2026-03-31 — implementation**
Files changed: `App.vue`, `PerformanceCharts.vue`, `Dashboard.vue`.
Result: implemented. Side-by-side layout, reduced heights/padding.

**2026-03-31 — verification**
Validation status: approved
Evidence: Verified with vision browser at 1920x872 viewport. No scrolling required.

---

### BENCH-UX-003

- **Status:** done
- **Module:** benchmark | **Type:** frontend | **Priority:** 80
- **Title:** OPTIMIZE ALL BENCHMARK PAGES FOR SINGLE-SCREEN VIEW
- **Attempts:** 1
- **Depends on:** BENCH-UX-002

**Context files**

- `benchmark/frontend/src/style.css`
- `benchmark/frontend/src/views/*.vue`

**Acceptance criteria**

1. All benchmark pages fit in a single screen without vertical scrolling.
2. Consistent headers, box sizes, and card spacing across all pages.
3. Instruction blocks are compressed or converted to compact alerts.
4. UI elements (upload zone, table rows) are optimized for space.

**Validation commands**

```bash
npm --prefix benchmark/frontend run build
```

**Progress log**

**2026-03-31 — implementation**
Files changed: `style.css`, `Jobs.vue`, `TestSets.vue`, `Templates.vue`, `DataSources.vue`, `Runs.vue`, `JobDetail.vue`.
Result: Global styling refinement and per-view optimization completed.

**2026-03-31 — verification**
Validation status: approved
Evidence: Verified all pages in vision browser (1920x872). Zero scrolling required.
Next action: commit.


---

### BENCH-UX-004

- **Status:** done
- **Module:** benchmark | **Type:** frontend | **Priority:** 75
- **Title:** STANDARDIZE TYPOGRAPHY ACROSS ALL BENCHMARK PAGES
- **Attempts:** 1
- **Depends on:** BENCH-UX-003

**Context files**

- `benchmark/frontend/src/style.css`
- `benchmark/frontend/src/views/*.vue`

**Acceptance criteria**

1. All "kinds" of info (titles, subtitles, card headers, table data, and labels) use consistent font styles across all pages.
2. Typography system defined in `style.css` using CSS variables.
3. Redundant local font styles removed from view components.
4. Mono-spaced fonts standardized for code, SQL, and technical IDs.

**Validation commands**

```bash
npm --prefix benchmark/frontend run build
```

**Progress log**

**2026-03-31 — implementation**
Files changed: `style.css`, `Dashboard.vue`, `Jobs.vue`, `TestSets.vue`, `Templates.vue`, `DataSources.vue`, `Runs.vue`, `JobDetail.vue`.
Result: Global typography system implemented and view components refactored.

**2026-03-31 — verification**
Validation status: approved
Evidence: Visual audit with subagent confirmed perfect typography consistency across all views.
Next action: commit.

---

### MGR-UX-002

- **Status:** todo
- **Module:** manager | **Type:** frontend | **Priority:** 65
- **Title:** STANDARDIZE TYPOGRAPHY AND OPTIMIZE MANAGER UI FOR SINGLE-SCREEN
- **Attempts:** 0

**Context files**

- `manager/frontend/src/style.css`
- `manager/frontend/src/views/*.vue`

**Acceptance criteria**

1. All manager pages fit in a single screen (1920x872) without vertical scrolling.
2. Consistent typography across all "kinds" of info (Titles, Subtitles, Card Headers, Table Data, Labels).
3. Use unified font stack: Outfit for headings, Inter for body, JetBrains Mono for code.
4. Centralized typography system in `style.css` using CSS variables.
5. All tables (Traces, Patterns, Acceleration) optimized for density.

**Validation commands**

```bash
npm --prefix manager/frontend run build
```

**2026-03-31 — implementation**
Files changed: `style.css`, `Dashboard.vue`, `Traces.vue`, `Patterns.vue`, `Acceleration.vue`, `QueryDatasources.vue`.
Result: implemented typography system and high-density layouts across all 5 manager views.

**2026-03-31 — verification**
Validation status: approved
Evidence: visual audit at 1920x872 confirmed zero vertical scrolling and 40px row height across all views.
Next action: complete.

---

## Done

| ID | Title | Module | Done signal |
|----|-------|--------|-------------|
| MGR-BUG-001 | FIX 404 ERROR ON QUERY-DATASOURCES API | manager | Backend restarted; `GET /api/v1/query-datasources` returns 200 OK |
| MGR-UX-002 | STANDARDIZE TYPOGRAPHY AND LAYOUT ACROSS ALL MANAGER PAGES | manager | All 5 pages fit in one screen; typography standardized; rows at 40px |
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
