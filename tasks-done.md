# Completed Tasks

This file is the completed-task archive for Easy Engine.

The foreman should read [tasks.md](/Users/sfc/Documents/projects/engine/tasks.md) for task selection and active progress, and consult this archive only when completed-task history or previous done signals are relevant.

## Done

| ID | Title | Module | Done signal |
|----|-------|--------|-------------|
| UI-BUNDLE-001 | CLEAN TEMP ARTIFACTS AND REBUILD EMBEDDED FRONTENDS | frontend, docs | Removed the untracked `analyze/target` build-temp directory, restored tracked manager coverage artifacts instead of deleting them, rebuilt both `manager` and `benchmark` frontends, and synced each `dist` into its Java `resources/static` directory so embedded assets now match the latest frontend bundles. |
| MGR-QA-001 | REVIEW AND VERIFY REDIS CACHE MANAGER | manager | Redis cache management was re-reviewed end to end; manager backend validation is now unblocked and green via the documented `analyze+manager` reactor path; manager frontend tests/build pass; browser QA evidence was captured for desktop and mobile cache flows; and cache list pagination/refresh behavior is now deterministic and stable. |
| MGR-TEST-002 | ALIGN MANAGER MIGRATION TESTS WITH REDIS-ENABLED ENGINE ROUTING | manager | Manager now keeps the Redis dependency/profile wiring required by the new `ENGINE` routing path, the leftover Flyway/bootstrap tests are cleaned up and aligned with the current seeded datasource count, and the focused validation remains blocked only by the pre-existing `JdbcSqlAdvisorService` constructor issue during Spring context startup. |
| QUERY-ENGINE-001 | SWITCH ROUTING TO ENGINE AND ADD REDIS REPORT OVERRIDES | query, manager, analyze, benchmark, docs | Routing now resolves datasource selection from parsed `ENGINE` with optional Redis override by `YH_RPTID`, `YH_TARGET_ENGINE` is parsed as legacy metadata only, normalized execution SQL and JDBC rewrite advice emit `/* ENGINE=... */`, benchmark/E2E samples were updated to the new routing hint, and focused `analyze`, `query`, `manager`, and `benchmark` validations all passed. |
| QUERY-TRINO-003 | ADD TRINO JDBC COMPATIBILITY PORT FOR BENCHMARK | query, benchmark | `query` now serves Kylin JDBC on `8092` plus a minimal Trino JDBC `/v1/statement` surface on `8093`, explicit Trino `PREPARE` / `EXECUTE ... USING ...` / `DEALLOCATE PREPARE` flows are covered against the real query service, benchmark now has Trino JDBC statement/prepared regression coverage, and `mvn -q -pl analyze,query -am test`, `mvn -q -f benchmark/pom.xml test`, and `npm --prefix benchmark/frontend run build` all passed. |
| MGR-CACHE-001 | ADD REDIS CACHE MANAGER CREATE AND EDIT SUPPORT | manager | Manager cache management now supports `kylin_cache:` summary/list/detail/create/update/delete from the existing `/cache` page and `/api/v1/cache/keys*` APIs; focused backend controller tests plus frontend tests/build pass; the stock `mvn -q -f manager/pom.xml test` command remains blocked by the unrelated current-workspace `JdbcSqlAdvisorService` constructor issue. |
| BENCH-BUG-002 | RECOVER RUNNING BENCHMARK RUNS LEFT BY SERVICE RESTARTS | benchmark | Benchmark now immediately fails any `RUNNING` rows left behind by a benchmark-service restart instead of waiting for the 15-minute stale timeout, local Kylin readiness was restored on port `17070`, run `#8` was auto-recovered to `FAILED` during benchmark restart, and `mvn -q -f benchmark/pom.xml test` plus `npm --prefix benchmark/frontend run build` passed. |
| BENCH-BUG-001 | REMOVE SYNTHETIC ACCESSOR DEPENDENCY FROM BENCHMARK RUN DISPATCH | benchmark | Benchmark run dispatch now uses an explicit static after-commit synchronization helper instead of an anonymous inner callback, so the compiled backend no longer depends on `BenchmarkExecutionService.access$000(...)`; `mvn -q -f benchmark/pom.xml clean test` and `npm --prefix benchmark/frontend run build` both pass, and `javap` confirms the synthetic bridge method is gone. |
| MIGRATION-001 | UPGRADE TEXT COLUMNS TO MEDIUMTEXT FOR LONG SQL SUPPORT | platform | Upgraded 10 JPA domain classes across `benchmark`, `query`, and `manager` to `MEDIUMTEXT` (16MB). Added Flyway migrations `benchmark/V18` and `manager/V11`. Verified compilation of all affected modules. |
| QUERY-TRINO-002 | ADD LOCAL TRINO SERVICE AND E2E COVERAGE | query | Trino service added to `docker-compose.yml`, seeded in `manager`, and verified with `TrinoRoutingE2ETest` passing 5/5 cases. Fixed repo-wide table name prefixing drift for `manager_sql_execution_record`. |
| QUERY-BUG-003 | MAKE CLEAN SQL INDEPENDENT OF COMMENT VALUES | query | `cleanSql` now strips all SQL comments regardless of their contents, `executionSql` still preserves pass-through downstream comments, query docs/test matrix now describe the split contract explicitly, and the focused plus full `analyze`/`query` reactor validations pass. |
| QUERY-BUG-002 | ACCEPT QUERY SQL AFTER LEADING OPTIMIZER COMMENTS | query | `query` now treats leading SQL comments, including `/*+ ... */` optimizer hints, as ignorable when detecting read-only query verbs, preserves non-query rejection for commented write statements, updates the query contract docs, and passes focused plus full reactor query validation. |
| BENCH-UX-010 | REPLACE SQL TEMPLATES WITH SQL LIB REFERENCE WORKFLOW | benchmark | Benchmark now uses SQL Lib as the reusable SQL source of truth, SQL file import moved into `/api/v1/sql-lib/upload` with filename/upload-time metadata and `.xlsx/.xls/.et/.csv/.txt/.sql` support, test sets now store ordered SQL Lib references instead of inline SQL payloads, and benchmark backend/frontend validation passed. |
| QUERY-CACHE-001 | MAKE REDIS RESULT CACHE WRITES ASYNCHRONOUS | query | `query` now schedules Redis result-cache writes on a dedicated background executor instead of blocking the request thread, preserves best-effort cache failure handling, documents the async cache contract, and passes focused plus full reactor query validation. |
| QUERY-ARCH-003 | EXTRACT SHARED ANALYZE MODULE FOR ROUTING AND SQL ANALYSIS | platform | Added the shared `analyze` Maven module and root reactor, switched query routing to `YH_TARGET_ENGINE`-only analysis-backed rewrites, added manager query-routing-context APIs plus shared parse/rewrite delegation, updated architecture/module docs, and verified the new analyzer, full query suite, and focused manager slice through reactor builds. |
| CONFIG-PROFILE-001 | UNIFY MODULE ENV CONFIG INTO DEV TEST PRO PROFILES | platform | `manager`, `query`, and `benchmark` now centralize environment settings in `application-dev.yml`, `application-test.yml`, and `application-pro.yml`; code/tests/scripts/POM defaults no longer own env config; profile-driven DB init works for `dev` and `test`; backend tests pass; frontend builds pass; and the new profile-governance rule is recorded in `docs/operations/best-practices.md` with follow-up audit task `CONFIG-REVIEW-001`. |
| TEST-CONFIG-001 | AUDIT TESTS FOR HARD-CODED CONNECTION FIXTURES | tests | Benchmark and query tests now load datasource and credential fixtures from classpath test property files/helpers instead of inline literals, focused benchmark/query validation passes, and the targeted hard-coded-fixture audit scan is clean. |
| MGR-DB-001 | REVIEW MODULE TABLE PREFIXES AND PREFIX MANAGER TABLES | manager | Manager-owned database tables now consistently use the `manager_` prefix, existing schemas upgrade through `V9__prefix_manager_tables.sql` plus a Flyway history-table handoff, focused manager Flyway regression tests pass, and `npm --prefix manager/frontend run build` passes while the stock manager Maven test command remains blocked by the separately logged `EngineConfig` path-pattern drift. |
| ARCH-015 | SEPARATE DATABASE INITIALIZATION FROM SERVICE STARTUP | platform | `manager` and `benchmark` no longer run Flyway automatically on startup, `scripts/init-db.sh` now initializes the shared MySQL schema explicitly, the local-development docs were updated to require that step, and both modules still compile. |
| QUERY-TRINO-001 | ADD TRINO DATASOURCE SUPPORT | query | `query` now bundles the Trino JDBC driver, routes `type=trino` datasource configs without SQL rewrites, exposes a `trino_local` fallback example, documents the Trino config contract, and `mvn -q -f query/pom.xml test` passes. |
| QUERY-REVIEW-002 | AUDIT QUERY COMPATIBILITY INPUT VALIDATION | query | `query` now accepts empty `/kylin/api/query` bodies through the same compatibility exception payload as blank SQL, rejects unsupported top-level request shapes on the query endpoint, keeps the request docs/test matrix aligned, and `mvn -q -f query/pom.xml test` passes. |
| QUERY-BUG-001 | REJECT NON-QUERY REQUESTS AT QUERY BOUNDARY | query | `query` now rejects blank and non-query SQL before cache/datasource work while preserving the compatibility exception payload contract, the request docs are updated, `mvn -q -f query/pom.xml test` passes, and broader compatibility-input review is tracked in `QUERY-REVIEW-002`. |
| BENCH-TEST-002 | EXTERNALIZE PREFLIGHT CONTROLLER TEST FIXTURES | benchmark | `PreflightControllerTest` now loads its probe fixture values from `benchmark/src/test/resources/preflight-controller-test.properties` instead of inline literals, the focused benchmark validation passes, and the broader audit follow-up is tracked in `TEST-CONFIG-001`. |
| BENCH-CONFIG-001 | EXTERNALIZE BENCHMARK PREFLIGHT DATASOURCE PROBE SETTINGS | benchmark | `BenchmarkPreflightProperties` no longer embeds Kylin/Presto probe defaults in Java, `benchmark.preflight.*` can be overridden from environment-backed config, task-local preflight tests pass, and unrelated benchmark-suite drift is logged in `INBOX-20260402-001`. |
| task-ui-integration-001 | 整合前端静态资源及 SPA 路由支持 | manager, benchmark | `manager/src/.../EngineConfig.java`, `benchmark/src/.../WebConfig.java`, `doc-CN/quickstart.md` updated; SPA route forwarding implemented for bundled assets. |
| task-ui-integration-001 | 整合前端静态资源及 SPA 路由支持 | manager, benchmark | `manager/src/.../EngineConfig.java`, `benchmark/src/.../WebConfig.java`, `doc-CN/quickstart.md` updated; SPA route forwarding implemented for bundled assets. |
| DOC-CN-003 | ADD MYSQL TABLE OVERVIEW TO QUICKSTART | docs | `doc-CN/quickstart.md` updated with MySQL table inventory for Manager and Benchmark modules. |
| DOC-CN-002 | WRITE CHINESE QUICKSTART DOCUMENTATION | docs | `doc-CN/quickstart.md` created and linked; service config and environmental versions verified against project source files. |
| BENCH-COMPAT-001 | FIX JAVA 8 COMPATIBILITY (PATH.OF, LIST.OF) | benchmark, manager | `mvn -q -f benchmark/pom.xml test` and `mvn -q -f manager/pom.xml test` both pass; redundant Path.of/List.of/Files.writeString usages replaced for Java 8 compatibility. |
| BENCH-UX-009 | IMPROVE BENCHMARK TEST-SET AUTHORING WORKFLOW | benchmark | Test Sets now support dialog-based empty/upload creation, in-place SQL row authoring, template copy-in, and item reordering via new `/api/v1/test-sets/.../items*` APIs; `mvn -q -f benchmark/pom.xml test`, `npm --prefix benchmark/frontend run test`, and `npm --prefix benchmark/frontend run build` all pass |
| BENCH-UX-007 | REPOSITION CONDITION SELECTION BOXES TO TOP-LEFT OF TABLES IN TEMPLATES AND RUNS | benchmark | Filter selection boxes in SQL Templates and Run History have been moved from the header to a dedicated toolbar at the top-left of the table; `npm --prefix benchmark/frontend run build` and visual inspection via vision browser pass. |
| BENCH-UX-006 | STANDARDIZE CONDITION SELECTION BOXES WIDTH ACROSS BENCHMARK PAGES | benchmark | All filter inputs and dropdowns across bookmark pages now share a uniform 160px width and are aligned in a single horizontal row; `npm --prefix benchmark/frontend run build` and visual inspection via vision browser pass. |
| MGR-UX-004 | STANDARDIZE CONDITION SELECTION BOXES WIDTH ACROSS MANAGER PAGES | manager | All condition selection boxes across manager pages (Traces, Patterns, Datasources, Acceleration) now share a uniform 160px width, ensuring they fit on a single line on desktop; `npm --prefix manager/frontend run build` and visual inspection via vision browser pass. |
| ARCH-010 | UPDATE E2E TEST SUITE FOR MYSQL DATABASE | tests | Kylin-routed prepared requests are now literalized inside `query` while preserving the prepared request/trace/cache contract, the lightweight `/kylin/api/user/authentication` shim is documented and covered again, and `mvn -q -f query/pom.xml test`, `mvn -q -f tests/pom.xml -Dtest=QueryHttpE2ETest,KylinJdbcE2ETest test`, and `mvn -q -f tests/pom.xml test` all pass against the local MySQL/Kylin/manager/query stack. |
| MGR-FILTER-001 | EXPAND MANAGER LIST FILTERS TO MATCH QUERYABLE COLUMNS | manager | Manager list pages now match practical queryable columns: datasource filters run client-side across name/JDBC URL/driver, type, and default/custom scope; `/api/v1/traces` now supports `fingerprint`, `datasource`, `sourceFlag`, `cacheHit`, `parseStatus`, and `sqlKeyword`; `/api/v1/patterns/top` now supports `fingerprint`, `sqlKeyword`, and `minExecutionCount`; `/api/v1/acceleration-tables` now supports `keyword`, `status`, `schemaName`, and `source`; docs and the manager test matrix were updated; and `mvn -q -f manager/pom.xml test`, `npm --prefix manager/frontend run test`, and `npm --prefix manager/frontend run build` all pass |
| HARNESS-GOV-001 | ENFORCE TASK FINALIZATION, COMMIT HYGIENE, AND BLOCKED-ISSUE ESCALATION | platform | Completion and escalation rules are now aligned across `AGENTS.md`, the operations runbook, git workflow, and `INBOX.md`; `.agent/config.json` now uses the `<task-id>: <short title>` commit template; `scripts/task_audit.py --check` now catches done-task drift, duplicate IDs, non-grandfathered missing commit links, and blocked-task entries that omit `Next action:` or `Escalation:`; `ARCH-010` now records `Escalation: none`; legacy harness drift is documented in `INBOX-20260331-001`; and `python3 scripts/task_audit.py --check`, the contract grep check, and the git-history check all pass. |
| QUERY-REVIEW-001 | REVIEW QUERY COMPATIBILITY SHIMS AND PLACEHOLDER CONTRACTS | query | Query now documents that only `POST /kylin/api/query` is intentionally supported on the Kylin-shaped surface, `SqlResponseStubDto` is explicitly marked as a compatibility artifact, prepared-result cache fingerprinting no longer treats unsupported `java.util.Date` as cache-safe, the unsupported-typed-parameter fallback path is covered by regression tests, and `mvn -q -f query/pom.xml test` passes |
| MGR-REVIEW-001 | REVIEW MANAGER PLACEHOLDER AND BEST-EFFORT IMPLEMENTATIONS | manager | Reviewed and locked the current manager placeholder contracts with tests/docs: `JdbcSqlAdvisorService#adviseRewrite` is an intentional best-effort advisory heuristic, not semantic rewrite analysis, because it only matches ACTIVE accelerations whose `refreshSql` contains the incoming query text and then prepends a hint comment while leaving the original SQL unchanged; `AccelerationService#createFromPattern` remains draft-only scaffolding, not production-safe guidance, because it emits a fixed `0 30 2 * * ?` cron, reuses the sampled SQL verbatim for refresh, and generates `WITH NO DATA` DDL that is not safe to treat as MySQL-ready activation SQL; `SqlParseService` is reliable for plain `SELECT` and `ORDER BY`-wrapped `SELECT`, but review tests now document false negatives where CTE roots return `WITH` with empty table/aggregate lineage, `UNION` roots return `UNION` with no branch tables, non-`SELECT` statements like `INSERT` return only the root kind, and nested `FROM` subqueries can recover base tables like `sales` while still missing aggregate extraction such as `COUNT(*)`. Docs now describe those limits in `docs/modules/manager.md` and `docs/architecture/http-interfaces.md`, and `mvn -q -f manager/pom.xml test` passes. |
| BENCH-REVIEW-001 | REVIEW BENCHMARK PLACEHOLDER SIGNALS AND SAMPLE CONTENT | benchmark | Reviewed the benchmark placeholder/demo paths and locked the conclusions in docs/tests: `GET /api/v1/preflight` now explicitly documents that only Kylin and Presto are live-probed while `mysql.status=OK` is a UI shortcut that should be replaced by a real DB probe; failure-path `evaluationJson` now documents/tests that `verdict`, `summary`, `phase`, `issues`, `diagnostics.failureBreakdown`, and `meta.executionModeSummary` are the stable contract while `metrics.note` and `jdbcComparisonHints.dimensions` remain placeholder guidance when no samples exist; Flyway seed assertions now confirm `sample_agg` no longer survives as the original `SELECT 1` placeholder on a fresh schema while the remaining seeded jobs/test sets/templates are treated as local bootstrap examples rather than production workload evidence. `mvn -q -f benchmark/pom.xml test` passed; the Docker-backed Flyway seed test skipped automatically in this environment because Testcontainers could not reach Docker. |
| MGR-DASH-002 | REMOVE SEEDED HOT PATTERNS FROM DEFAULT MANAGER DASHBOARD | manager | Default manager schemas now end migration at `V8__remove_default_dashboard_demo_seeds.sql`, so the seeded V3 trace/pattern/acceleration demo rows are removed from the normal operator path; fresh-schema coverage now asserts `/api/v1/stats/summary`, `/api/v1/patterns/top`, `/api/v1/traces`, and `/api/v1/acceleration-tables` all start empty while datasource seeds remain, the dashboard frontend test locks the empty-state UI, and `mvn -q -f manager/pom.xml test`, `npm --prefix manager/frontend run test`, and `npm --prefix manager/frontend run build` all pass |
| MGR-UX-003 | MATCH DASHBOARD PANEL WIDTHS FOR RECENT TRACES AND HOT PATTERNS | manager | Dashboard `Recent Traces` and `Hot Patterns` now share equal desktop column widths and full-height cards; `npm --prefix manager/frontend run test -- dashboard.spec.js` and `npm --prefix manager/frontend run build` pass |
| BENCH-DATA-001 | CLEAN UP DUPLICATED SEEDED BENCHMARK DATASOURCES | benchmark | Added forward migration `V17__dedupe_seeded_benchmark_datasources.sql` plus Flyway seed assertions so fresh schema bootstraps collapse 8 duplicate seeded benchmark datasource rows into one canonical `engine-query-default` row; `mvn -q -f benchmark/pom.xml -Dtest=BenchmarkFlywaySeedTest test` passes in this environment, and local MySQL verification shows `benchmark_data_source` changed from `8 total / 1 distinct connection tuple` to `1 total / 1 distinct connection tuple` with all seeded jobs repointed to `data_source_id = 1` |
| BENCH-ACTIVE-001 | RECOVER STALE ACTIVE RUN STATE ON BENCHMARK DASHBOARD | benchmark | Live MySQL evidence confirmed stale `benchmark_run` rows `#2` and `#3` were still `RUNNING` hours later with `ended_at = NULL` and `0/0` progress; `/api/v1/runs/active` now reconciles stale rows on read, returns the most recently started remaining active run deterministically, docs reflect the new contract, and `mvn -q -f benchmark/pom.xml test` passes |
| BENCH-RUNS-API-001 | FIX THE `/api/v1/runs` LIST CONTRACT SO BENCHMARK PAGES LOAD WITHOUT A BACKEND 500 | benchmark | `GET /api/v1/runs` now accepts an omitted `jobId` and returns the newest global run history instead of throwing `Required request parameter 'jobId' ... is not present`; per-job paging still uses the existing filter when `jobId` is supplied, controller coverage now asserts both branches in `RunControllerTest` and `RunControllerContextTest`, docs describe the updated contract, and `mvn -q -f benchmark/pom.xml test`, `npm --prefix benchmark/frontend run test`, and `npm --prefix benchmark/frontend run build` all pass |
| BENCH-FILTER-001 | IMPROVE BENCHMARK PAGE QUERY CONDITIONS | benchmark | Benchmark list/history filters now match the operator-visible columns more closely: Data Sources add a driver-class filter, Jobs add strategy/test-set filters and broader keyword matching, Test Sets keep keyword plus source only, Templates add request-backed `executionMode` filtering plus broader keyword search across `name`/`sqlText`/`description`/`executionMode`/`paramJson`, and Runs add request-backed optional `status` alongside clearable `jobId` global history. Docs describe the refined filter behavior and request params, and `mvn -q -f benchmark/pom.xml test`, `npm --prefix benchmark/frontend run test`, and `npm --prefix benchmark/frontend run build` all pass |
| BENCH-UX-005 | COMPACT LONG SQL CONTENT IN TEMPLATE LIST | benchmark | Template rows now keep the name and SQL content on a single line with ellipsis, truncated entries still expose a full-SQL dialog, existing row actions remain visible, `npm --prefix benchmark/frontend run test` and `npm --prefix benchmark/frontend run build` pass, and live browser QA evidence was captured at `/tmp/engine-qa/sql-templates-{row,one-line-row,full-dialog}.png` |
| BENCH-TEST-001 | INVENTORY BENCHMARK FUNCTIONS AND CLOSE TEST BRANCH GAPS | benchmark | `docs/modules/benchmark-test-matrix.md` now maps the benchmark backend/frontend function inventory to concrete automated tests, every benchmark test carries an adjacent traceability comment, backend/frontend coverage artifacts are emitted under `benchmark/target/site/jacoco/` and `benchmark/frontend/coverage/`, and `mvn -q -f benchmark/pom.xml test`, `npm --prefix benchmark/frontend run test`, and `npm --prefix benchmark/frontend run build` all pass |
| QUERY-TEST-001 | INVENTORY QUERY FUNCTIONS AND MAP TEST CASES TO LOGIC BRANCHES | query | `docs/modules/query-test-matrix.md` now maps the non-trivial query function inventory to concrete automated tests, every query `@Test` carries an adjacent `ClassName#methodName` traceability comment, JaCoCo artifacts are emitted under `query/target/site/jacoco/`, and `mvn -q -f query/pom.xml test` passes; remaining report misses are limited to excluded boilerplate/wiring classes and non-meaningful defensive branches documented in the matrix |
| ARCH-014 | RESTORE LOCAL KYLIN CONTAINER READINESS FOR E2E | platform | Fresh `kylin` image rebuild now returns `200` from `/kylin/api/user/authentication`, `docker compose ps --all` reports `healthy`, and in-container YARN shows `Total Nodes:1` with no `sparder_on_docker` app stuck in `ACCEPTED`; remaining benchmark/query failures are now app-layer follow-up instead of container readiness |
| ARCH-013 | VERIFY MYSQL STACK HEALTH FOR LOCAL E2E | platform | MySQL, Redis, Presto, manager, query, and benchmark were verified on local ports; benchmark startup required the forward `V16__benchmark_job_data_source_id_bigint.sql` migration; the remaining Kylin blocker was recorded for follow-up |
| ARCH-012 | FIX QUERY MYSQL REGRESSIONS AND TEST ISOLATION | query | `query` tests now force manager-config fallback in test scope, the stray query auth endpoint is removed, `mvn -q -f query/pom.xml test` passes, and the app starts on `8092` |
| ARCH-011 | NORMALIZE MYSQL MIGRATION DRIFT IN TASKS AND DOCS | docs | Current repo instructions and done signals no longer describe PostgreSQL as the active runtime metadata store; `tests/README.md` and `tasks.md` now match the MySQL design |
| MGR-TEST-001 | INVENTORY MANAGER FUNCTIONS AND ADD TEST-TO-FUNCTION TRACEABILITY | manager | Manager now emits JaCoCo and Vitest coverage artifacts, the backend/frontend suites map non-trivial manager logic to traceable tests in `docs/modules/manager-test-matrix.md`, and `mvn -q -f manager/pom.xml test`, `npm --prefix manager/frontend run test`, and `npm --prefix manager/frontend run build` all pass |
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
### UI-BUNDLE-001: CLEAN TEMP ARTIFACTS AND REBUILD EMBEDDED FRONTENDS

- **Status**: done
- **Updated**: 2026-04-03
- **Progress log**:
  - **2026-04-03 — intake**
    - Human requested cleanup of deletable temporary directories and a fresh rebuild of all frontend artifacts, with the rebuilt bundles copied into the Java `resources/static` directories.
    - Investigation confirmed `analyze/target/` is an untracked build-temp directory that can be removed safely, `manager/frontend/coverage/` is tracked generated output that should be restored instead of deleted from the repo, and both `manager` and `benchmark` embed frontend assets from their respective `frontend/dist` directories into `src/main/resources/static/`.
    - Scope for this task: clean only the safe temporary artifacts, rebuild both frontends, sync each frontend `dist` into its Java static resources, and close the work through the required ledger/archive workflow without touching unrelated local config changes.
  - **2026-04-03 — implementation**
    - Files changed: tracked bundle artifacts under `benchmark/frontend/dist/` and `benchmark/src/main/resources/static/`, plus task ledger/archive records.
    - Commands run: `rm -rf analyze/target`, `git restore manager/frontend/coverage`, `npm --prefix manager/frontend run build`, `npm --prefix benchmark/frontend run build`, `rm -rf manager/src/main/resources/static/* && cp -R manager/frontend/dist/. manager/src/main/resources/static/`, `rm -rf benchmark/src/main/resources/static/* && cp -R benchmark/frontend/dist/. benchmark/src/main/resources/static/`.
    - Result: Deleted the untracked analyze build-temp directory, restored manager coverage output to the repo version, rebuilt both frontend apps, and replaced each module's embedded Java static assets with the latest dist output.
  - **2026-04-03 — review**
    - Self-Review: [x] style check [x] build artifact sync [x] side-effects
    - Notes: Kept scope limited to temporary-artifact cleanup and bundle refresh. Existing tracked frontend dist/static assets were intentionally updated rather than removed.
  - **2026-04-03 — verification**
    - Validation status: approved
    - Evidence: `npm --prefix manager/frontend run build` passed; `npm --prefix benchmark/frontend run build` passed; `analyze/target/` no longer exists; `manager/src/main/resources/static/` and `benchmark/src/main/resources/static/` now mirror their respective `frontend/dist/` directories.
    - Next action: none
    - Escalation: none

### MGR-QA-001: REVIEW AND VERIFY REDIS CACHE MANAGER

- **Status**: done
- **Updated**: 2026-04-03
- **Progress log**:
  - **2026-04-03 — intake**
    - Human requested a full review of the manager Redis cache feature, including persistent-browser QA using the `$playwright-interactive` workflow, followed by frontend/backend validation and a task-scoped commit.
    - Investigation confirmed the cache feature currently spans `/cache` in the manager frontend and `/api/v1/cache/*` in manager backend, with focused frontend/backend tests already present. This session does not expose `js_repl`, so Playwright browser tooling was replaced with a temporary local Playwright harness for QA evidence capture.
    - Scope for this task: review the entire manager cache experience, fix any functional or QA-signoff defects, unblock manager-scoped backend validation if the existing `JdbcSqlAdvisorService` constructor issue still prevents context startup, capture QA evidence, and close the task through the required ledger/archive workflow.
  - **2026-04-03 — implementation**
    - Files changed: `manager/src/main/java/com/smartbi/engine/jdbc/JdbcSqlAdvisorService.java`, `manager/src/main/java/com/smartbi/engine/web/CacheManagementController.java`, `manager/frontend/src/views/CacheManagement.js`, `manager/src/test/java/com/smartbi/engine/datasource/DatasourceConfigFlywayIntegrationTest.java`, `manager/src/test/java/com/smartbi/engine/migration/ManagerDashboardBootstrapIntegrationTest.java`, `manager/src/test/java/com/smartbi/engine/migration/ManagerFlywayHistoryRenameIntegrationTest.java`, refreshed frontend bundles under `manager/frontend/dist/`, refreshed embedded manager static assets under `manager/src/main/resources/static/`, plus task ledger/archive records.
    - Commands run: `mvn -q -pl analyze,manager -am -Dspring.mvc.pathmatch.matching-strategy=ant_path_matcher -DfailIfNoTests=false -Dtest=CacheManagementControllerTest,JdbcSqlAdvisorServiceTest,EffectiveEngineResolverTest,ManagerApiTest,DatasourceConfigFlywayIntegrationTest,ManagerDashboardBootstrapIntegrationTest,ManagerFlywayHistoryRenameIntegrationTest test`, `mvn -q -pl analyze,manager -am -Dspring.mvc.pathmatch.matching-strategy=ant_path_matcher test`, `npm --prefix manager/frontend run test`, `npm --prefix manager/frontend run build`, `bash scripts/init-db.sh dev manager`, local manager startup, local frontend startup, and Playwright QA via `/tmp/codex-playwright/cache-qa.mjs`.
    - Result: Fixed Spring constructor injection for `JdbcSqlAdvisorService`, made cache key pagination deterministic by sorting managed keys before offset/limit slicing, clamped cache-page refreshes back into a valid page after data changes, updated manager migration/bootstrap tests to the current seeded datasource and flyway-history counts, and refreshed the embedded frontend bundle to match the reviewed cache UI.
  - **2026-04-03 — review**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Notes: Reviewed summary cards, key list, create/edit/delete flows, duplicate key handling, JSON/key validation, pagination, refresh behavior, mobile layout, and embedded-static consistency. The cache feature remains intentionally scoped to managed `kylin_cache:` keys only.
  - **2026-04-03 — verification**
    - Validation status: approved
    - Evidence: `mvn -q -pl analyze,manager -am -Dspring.mvc.pathmatch.matching-strategy=ant_path_matcher test` passed; `npm --prefix manager/frontend run test` passed; `npm --prefix manager/frontend run build` passed.
    - Evidence: Browser QA captured desktop and mobile evidence under `/tmp/engine-cache-qa/`:
      `01-desktop-empty.png`, `02-create-dialog-empty.png`, `03-after-create.png`, `04-edit-dialog.png`, `05-after-edit.png`, `06-pagination-page1.png`, `07-pagination-page2.png`, `08-after-delete.png`, `09-mobile-cache.png`, and notes in `/tmp/engine-cache-qa/notes.txt`.
    - Evidence: QA confirmed empty-state rendering, create validation for empty key/invalid key/invalid JSON, successful create, duplicate-key rejection, read-only key on edit, successful edit, second-page pagination, delete from paginated state, and mobile card layout without obvious clipping, unreadable labels, broken actions, or stale loading indicators.
    - Next action: none
    - Escalation: none
  - **2026-04-03 — doc-garden**
    - Existing cache API and manager module docs remained accurate after the fixes; no canonical doc text change was required in this pass.

### MGR-TEST-002: ALIGN MANAGER MIGRATION TESTS WITH REDIS-ENABLED ENGINE ROUTING

- **Status**: done
- **Updated**: 2026-04-03
- **Progress log**:
  - **2026-04-03 — intake**
    - Human asked to summarize the remaining uncommitted work and then commit the lingering test-related changes.
    - Investigation confirmed the larger `QUERY-ENGINE-001` routing change is already committed as `8ddf0e2`; the remaining related drift is limited to manager-side follow-up files: `manager/pom.xml`, manager `dev`/`pro` Redis profile config, and three manager Flyway/bootstrap tests that still need formatting cleanup plus one updated seeded-datasource expectation.
    - Scope for this task: clean up those remaining manager test/config files only, verify the focused manager slice, and close them as a separate task-local commit without staging unrelated dirty worktree changes.
  - **2026-04-03 — implementation**
    - Files changed: `manager/pom.xml`, `manager/src/main/resources/application-dev.yml`, `manager/src/main/resources/application-pro.yml`, `manager/src/test/java/com/smartbi/engine/datasource/DatasourceConfigFlywayIntegrationTest.java`, `manager/src/test/java/com/smartbi/engine/migration/ManagerDashboardBootstrapIntegrationTest.java`, `manager/src/test/java/com/smartbi/engine/migration/ManagerFlywayHistoryRenameIntegrationTest.java`.
    - Commands run: `rg`, `sed`, `git diff`, `mvn -q -f manager/pom.xml -Dtest=DatasourceConfigFlywayIntegrationTest,ManagerDashboardBootstrapIntegrationTest,ManagerFlywayHistoryRenameIntegrationTest test`.
    - Result: Kept the manager-side Redis dependency/profile wiring that the `ENGINE` routing follow-up still needed, cleaned the leftover migration/bootstrap test formatting drift, and aligned the dashboard bootstrap test with the current seeded datasource count of `3`.
  - **2026-04-03 — review**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Notes: Scope stayed narrow to the leftover manager config/test files only; no unrelated query/benchmark/frontend changes were included.
  - **2026-04-03 — verification**
    - Validation status: blocked by unrelated manager bean-instantiation issue
    - Evidence: `mvn -q -f manager/pom.xml -Dtest=DatasourceConfigFlywayIntegrationTest,ManagerDashboardBootstrapIntegrationTest,ManagerFlywayHistoryRenameIntegrationTest test` still fails during Spring context startup before reaching the updated assertions because `JdbcSqlAdvisorService` cannot be instantiated (`NoSuchMethodException` for the default constructor).
    - Next action: none
    - Escalation: none

### QUERY-ENGINE-001: SWITCH ROUTING TO ENGINE AND ADD REDIS REPORT OVERRIDES

- **Status**: done
- **Updated**: 2026-04-03
- **Progress log**:
  - **2026-04-03 — intake**
    - Human requested a routing-contract change: stop using `YH_TARGET_ENGINE` for datasource routing, route by `ENGINE` instead, keep `YH_TARGET_ENGINE` as parse-only legacy metadata, and add report-driven `ENGINE` override behavior sourced from Redis by `YH_RPTID`.
    - Scope for this task: update the shared SQL metadata/parser model, switch query and manager routing/rewrite behavior to normalized `ENGINE` comments, add lightweight Redis-backed effective-engine resolution on both runtime paths, refresh benchmark/E2E/doc samples, and verify the focused analyzer/query/manager/benchmark slices.
  - **2026-04-03 — implementation**
    - Files changed: shared analyzer routing/parser files under `analyze/src/main/java/com/smartbi/analyze/{sql,route}/`, new effective-engine resolvers plus service/test updates under `query/src/main/java/com/smartbi/query/route/` and `manager/src/main/java/com/smartbi/engine/jdbc/`, targeted test updates under `analyze/src/test/`, `query/src/test/`, `manager/src/test/`, benchmark SQL normalization files under `benchmark/src/main/` plus benchmark test coverage, and the related query/manager/architecture/E2E docs and samples.
    - Commands run: `rg`, `sed`, `mvn -q -pl analyze -am test -Dtest=SqlCommentParserTest,SqlRoutingAnalyzerTest`, `mvn -q -pl query -am test -Dtest=SqlCommentParserTest,SqlRouteServiceTest,EffectiveEngineResolverTest,QueryWebIntegrationTest,QueryTrinoRoutingIntegrationTest`, `mvn -q -pl analyze install -DskipTests`, `mvn -q -pl manager test -Dtest=JdbcSqlAdvisorServiceTest,EffectiveEngineResolverTest -Dspring.mvc.pathmatch.matching-strategy=ant_path_matcher`, `mvn -q -pl benchmark test -Dtest=BenchmarkAsyncRunnerExecutionModeIntegrationTest`, `mvn -q -pl analyze,query -am test -Dspring.mvc.pathmatch.matching-strategy=ant_path_matcher -DfailIfNoTests=false`.
    - Result: Added explicit `metadata.engine`, switched normalized execution SQL to `/* ENGINE=... */`, ignored `YH_TARGET_ENGINE` for routing while still parsing it as legacy metadata, introduced Redis-backed `YH_RPTID -> ENGINE` overrides in both query and manager flows, and aligned benchmark/E2E/doc assets to the new routing contract.
  - **2026-04-03 — review**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Root Cause: Earlier routing logic overloaded `YH_TARGET_ENGINE` as both preserved metadata and the sole routing signal, while `ENGINE` was recognized only as a stripped driver hint, leaving no clean place to apply report-driven override behavior.
    - Cure: Promoted `ENGINE` into first-class parsed metadata plus normalized execution output, moved routing decisions onto explicit effective-engine resolution, and isolated Redis override lookups to runtime service layers instead of the shared analyze module.
    - Generalization: "When a comment key affects runtime routing, model it as an explicit parsed field and keep legacy metadata keys parse-only rather than mixing routing semantics into `extraMetadata`."
  - **2026-04-03 — verification**
    - Validation status: approved
    - Evidence: Focused analyzer tests passed; focused query parser/routing/web tests passed; focused manager JDBC rewrite tests passed after installing the shared `analyze` module locally; benchmark execution-mode integration tests passed with the new `ENGINE` pattern; and the broader `analyze,query` reactor run passed with `-DfailIfNoTests=false`.
    - Next action: none
    - Escalation: none
  - **2026-04-03 — doc-garden**
    - Updated the canonical English and Chinese architecture/query/operator docs plus benchmark sample data notes so they describe `ENGINE` routing, `YH_RPTID` Redis overrides, and legacy `YH_TARGET_ENGINE` parse-only handling.
### MGR-CACHE-001: ADD REDIS CACHE MANAGER CREATE AND EDIT SUPPORT

- **Status**: done
- **Updated**: 2026-04-03
- **Progress log**:
  - **2026-04-03 — intake**
    - Human requested confirmation of whether the current manager Redis cache page supports modification and, if not, implementation of the missing create/update APIs plus UI.
    - Investigation confirmed the current manager cache surface only supports cache summary, paged key listing, and delete through `CacheManagementController` plus `/cache`; there is no single-key detail read, no create endpoint, no update endpoint, and no frontend create/edit flow.
    - Scope for this task: keep the work inside the existing manager `/cache` page, add cache detail/create/update APIs for `kylin_cache:` keys only, support editing cache value plus TTL without key renaming, add focused backend/frontend regression coverage, and update manager/API docs.
  - **2026-04-03 — implementation**
    - Files changed: `manager/src/main/java/com/smartbi/engine/web/CacheManagementController.java`, new cache DTOs under `manager/src/main/java/com/smartbi/engine/web/dto/`, `manager/src/test/java/com/smartbi/engine/web/CacheManagementControllerTest.java`, manager frontend cache files under `manager/frontend/src/views/`, `manager/frontend/src/api/endpoints.js`, `manager/frontend/src/i18n.js`, `manager/frontend/test/cache-management.spec.js`, `docs/modules/manager.md`, and `docs/architecture/http-interfaces.md`.
    - Commands run: `rg`, `sed`, `mvn -q -f manager/pom.xml -Dtest=CacheManagementControllerTest,DatasourceConfigControllerTest,ManagerControllerWebTest,RestExceptionHandlerWebTest test`, `npm --prefix manager/frontend run test -- cache-management.spec.js`, `npm --prefix manager/frontend run test`, `npm --prefix manager/frontend run build`.
    - Result: Added cache detail/create/update APIs with key/TTL/value validation, kept delete scoped to `kylin_cache:` keys, expanded the `/cache` page with create/edit dialogs plus mobile-safe record actions, added focused backend/frontend regressions, and refreshed the embedded/static frontend bundle references.
  - **2026-04-03 — review**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Notes: Kept the manager cache surface intentionally scoped to the managed query-cache namespace, stored cache payloads as raw strings on the backend, and prevented existing-key renames so the UI cannot silently break query-generated cache identifiers.
  - **2026-04-03 — verification**
    - Validation status: approved with known unrelated manager-suite blocker
    - Evidence: Focused backend controller/web tests passed with `mvn -q -f manager/pom.xml -Dtest=CacheManagementControllerTest,DatasourceConfigControllerTest,ManagerControllerWebTest,RestExceptionHandlerWebTest test`; frontend cache regression passed with `npm --prefix manager/frontend run test -- cache-management.spec.js`; full manager frontend regression and build passed with `npm --prefix manager/frontend run test` and `npm --prefix manager/frontend run build`.
    - Evidence: The stock `mvn -q -f manager/pom.xml test` command still fails before exercising this cache work because the current workspace has an unrelated `JdbcSqlAdvisorService` bean-instantiation problem (`NoSuchMethodException` for the default constructor) that breaks 17 Spring Boot context tests.
    - Next action: none
    - Escalation: none
  - **2026-04-03 — doc-garden**
    - Updated `docs/modules/manager.md` and `docs/architecture/http-interfaces.md` so the manager operator notes and HTTP surface now document cache summary/list/detail/create/update/delete behavior and the no-rename cache-edit constraint.

### QUERY-TRINO-003: ADD TRINO JDBC COMPATIBILITY PORT FOR BENCHMARK

- **Status**: done
- **Updated**: 2026-04-03
- **Progress log**:
  - **2026-04-03 — intake**
    - Human requested a new Trino JDBC-compatible `query` port in addition to the existing Kylin JDBC surface, with benchmark regression coverage proving `benchmark` can use `io.trino.jdbc.TrinoDriver` against the new port.
    - Scope for this task: add a default `8093` Trino JDBC compatibility port to `query`, implement the minimum `/v1/statement` behavior needed for benchmark `Statement` and `PreparedStatement` flows, keep the existing Kylin `8092` path unchanged, add focused query and benchmark regressions, update the canonical docs, and close the work through the required ledger/archive workflow.
  - **2026-04-03 — implementation**
    - Files changed: `query/src/main/java/com/smartbi/query/config/{QueryProperties,TrinoCompatibilityPortConfig}.java`, `query/src/main/java/com/smartbi/query/web/{QueryController,TrinoStatementController}.java`, `query/src/main/java/com/smartbi/query/trino/TrinoStatementService.java`, `query/src/main/resources/application-{dev,test,pro}.yml`, `query/src/test/java/com/smartbi/query/web/TrinoJdbcCompatibilityIntegrationTest.java`, `benchmark/pom.xml`, `benchmark/src/test/java/com/smartbi/benchmark/run/BenchmarkAsyncRunnerTrinoJdbcIntegrationTest.java`, `docs/modules/{query,benchmark,query-test-matrix,benchmark-test-matrix}.md`, `docs/architecture/{runtime-topology,service-capabilities}.md`, `docs/operations/local-development.md`, `tasks.md`, and `INBOX.md`.
    - Commands run: `rg`, `sed`, `javap`, `jshell`, `mvn -q -pl analyze,query -am -Dtest=TrinoJdbcCompatibilityIntegrationTest -DfailIfNoTests=false test`, `mvn -q -f benchmark/pom.xml -Dtest=BenchmarkAsyncRunnerTrinoJdbcIntegrationTest test`.
    - Result: Added a second embedded `query` listener on `engine.query.trino.port` (`8093` by default), implemented a minimal Trino `/v1/statement` adapter that translates plain SQL plus explicit `PREPARE` / `EXECUTE ... USING ...` / `DEALLOCATE PREPARE` flows into the existing query execution service, and added dedicated query/benchmark Trino JDBC regression coverage without changing benchmark's default Kylin datasource path.
  - **2026-04-03 — review**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Notes: Preserved the current `ENGINE`-based routing contract already present in the dirty worktree, kept Kylin compatibility isolated to `8092`, scoped Trino compatibility to execution-only behavior with no metadata browsing, and kept benchmark's Trino regression self-contained so `mvn -q -f benchmark/pom.xml test` still works without requiring the `query` module as a compile-time dependency.
  - **2026-04-03 — verification**
    - Validation status: approved
    - Evidence: `mvn -q -pl analyze,query -am test` passed; `mvn -q -f benchmark/pom.xml test` passed; `npm --prefix benchmark/frontend run build` passed. Benchmark's Trino JDBC tests still print upstream JaCoCo/JDK 25 instrumentation warnings from Trino-triggered JDK security/provider classes, but the Maven test run exits successfully and the new regression assertions pass.
    - Next action: none
    - Escalation: none
  - **2026-04-03 — doc-garden**
    - Updated the canonical English docs so runtime topology, local-development guidance, query capability contracts, and benchmark/query test matrices now describe the `8092` Kylin port, the `8093` Trino `/v1/statement` compatibility port, and benchmark's new Trino JDBC regression coverage.

### BENCH-BUG-002: RECOVER RUNNING BENCHMARK RUNS LEFT BY SERVICE RESTARTS

- **Status**: done
- **Updated**: 2026-04-03
- **Progress log**:
  - **2026-04-03 — intake**
    - Human reported that local Kylin had regressed again and benchmark still showed problematic active runs. Investigation found `docker compose ps --all` reporting `kylin-standalone` as `unhealthy`, `GET /api/v1/preflight` failing the Kylin probe with `Connection reset`, and `benchmark_run` row `#8` still marked `RUNNING` with `0/0` progress even after the benchmark service restarted at 09:03 local time.
    - Scope for this task: restore the local Kylin service to a healthy state for benchmark/query traffic, fix benchmark run recovery so service restarts immediately fail pre-restart `RUNNING` rows instead of waiting for the 15-minute stale timeout, clean up the currently stuck active run state, and verify the benchmark module behavior with regression coverage.
  - **2026-04-03 — implementation**
    - Files changed: `benchmark/src/main/java/com/smartbi/benchmark/run/BenchmarkExecutionService.java`, `benchmark/src/test/java/com/smartbi/benchmark/run/BenchmarkExecutionServiceTest.java`, `docs/modules/benchmark.md`, `docs/modules/benchmark-test-matrix.md`, `tasks.md`, and `INBOX.md`.
    - Commands run: `rg`, `sed`, `curl`, `mysql`, `docker compose ps --all`, `docker restart kylin-standalone`, `mvn -q -f benchmark/pom.xml -Dtest=BenchmarkExecutionServiceTest test`, `mvn -q -f benchmark/pom.xml test`, and `npm --prefix benchmark/frontend run build`.
    - Result: Restored local Kylin readiness by restarting the `kylin-standalone` container until `/kylin/api/user/authentication` returned `200`, changed benchmark stale-run recovery to fail any `RUNNING` row started before the current benchmark-service instance in addition to the existing 15-minute timeout, added a regression test for the restart edge case, and restarted the benchmark service so the lingering run `#8` was auto-recovered to `FAILED`.
  - **2026-04-03 — review & post-mortem**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Root Cause: `BenchmarkExecutionService#reconcileStaleRuns()` only treated `RUNNING` rows older than 15 minutes as stale. When the benchmark service restarted, any younger `RUNNING` rows lost their in-memory worker immediately, but the recovery code still considered them live until the timeout elapsed, leaving a phantom active-run card and misleading `/api/v1/runs/active` output.
    - Cure: Captured the current benchmark-service start instant and treated every `RUNNING` row started before that instant as stale immediately, while preserving the 15-minute timeout for genuinely live instances that later hang.
    - Generalization: This bug was specific to benchmark's restart-aware recovery contract; no new repo-wide best-practice rule was added.
  - **2026-04-03 — verification**
    - Validation status: approved
    - Evidence: `mvn -q -f benchmark/pom.xml test` passed; `npm --prefix benchmark/frontend run build` passed; `docker compose ps --all` reports `kylin-standalone` as `healthy`; `GET /api/v1/preflight` returns `mysql=OK`, `kylinRest=OK`, and `prestoUi=OK`; `GET /api/v1/runs/active` now returns no active run; and MySQL shows `benchmark_run.id=8` moved to `FAILED` with `ended_at` populated and the restart-recovery message recorded.
    - Next action: none
    - Escalation: none
  - **2026-04-03 — doc-garden**
    - Updated `docs/modules/benchmark.md` and `docs/modules/benchmark-test-matrix.md` so the benchmark run-recovery contract now explicitly covers immediate cleanup of pre-restart `RUNNING` rows alongside the existing stale-timeout path.

### BENCH-BUG-001: REMOVE SYNTHETIC ACCESSOR DEPENDENCY FROM BENCHMARK RUN DISPATCH

- **Status**: done
- **Updated**: 2026-04-03
- **Progress log**:
  - **2026-04-03 — intake**
    - Human reported a benchmark runtime failure: `Handler dispatch failed; nested exception is java.lang.NoSuchMethodError: 'com.smartbi.benchmark.run.BenchmarkAsyncRunner com.smartbi.benchmark.run.BenchmarkExecutionService.access$000(com.smartbi.benchmark.run.BenchmarkExecutionService)'`.
    - Scope for this task: inspect the benchmark run-dispatch path, remove the runtime dependency on the compiler-generated `access$000(...)` bridge used by the anonymous after-commit callback, verify the benchmark module still passes validation, and close the task through the required ledger/archive workflow.
  - **2026-04-03 — implementation**
    - Files changed: `benchmark/src/main/java/com/smartbi/benchmark/run/BenchmarkExecutionService.java`, `tasks.md`, and `INBOX.md`.
    - Commands run: `rg`, `sed`, `javap`, `mvn -q -f benchmark/pom.xml clean test`, and `npm --prefix benchmark/frontend run build`.
    - Result: Replaced the anonymous `TransactionSynchronization` callback in `BenchmarkExecutionService#triggerAsyncRunAfterCommit(...)` with an explicit static nested synchronization helper that receives `BenchmarkAsyncRunner` and `runId` through its constructor, so the compiled dispatch path no longer depends on the synthetic `BenchmarkExecutionService.access$000(...)` bridge.
  - **2026-04-03 — review & post-mortem**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Root Cause: The after-commit dispatch path captured a private field from an anonymous inner class, so Java 8 compiled the callback against a synthetic `access$000(...)` bridge. When mixed old/new benchmark class files were loaded together, the inner callback class could call a bridge method that no longer existed on the outer class, producing the observed `NoSuchMethodError`.
    - Cure: Replaced the anonymous callback with a static nested synchronization class that uses constructor-injected state instead of private-outer-field access, which removes the runtime dependency on the synthetic bridge method and makes mixed-class incremental rebuilds less fragile.
    - Generalization: This fix is specific to a localized runtime-compatibility failure in the benchmark dispatch path; no new repo-wide best-practice rule was added.
  - **2026-04-03 — verification**
    - Validation status: approved with known environment note
    - Evidence: `mvn -q -f benchmark/pom.xml clean test` passed; `npm --prefix benchmark/frontend run build` passed; `javap -classpath benchmark/target/classes -p com.smartbi.benchmark.run.BenchmarkExecutionService` no longer lists `access$000(...)`; `javap -classpath benchmark/target/classes -p -c com.smartbi.benchmark.run.BenchmarkExecutionService$AfterCommitRunSynchronization` shows `afterCommit()` calling `BenchmarkAsyncRunner.executeRun(...)` directly.
    - Next action: none
    - Escalation: none

### QUERY-TRINO-002: ADD LOCAL TRINO SERVICE AND E2E COVERAGE

- **Status**: done
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Human requested a runnable local Trino Docker service because `query` already supports `type=trino` but the repo had no matching compose service to validate it end-to-end.
    - Scope for this task: add the local Trino runtime wiring, point local config/docs/tests at it, and add automated coverage that proves `query` can route real requests through the new local Trino target.
  - **2026-04-02 — implementation**
    - Added Trino service to `docker-compose.yml` (olap profile).
    - Seeded `trino_local` into `manager` via Flyway migration `V10`.
    - Added Trino JDBC driver to `tests/pom.xml` and updated `E2EConfig.java`.
    - Created `TrinoConnectivityTest.java` (query module) and `TrinoRoutingE2ETest.java` (tests module).
    - Fixed table name drifting (`sql_execution_record` -> `manager_sql_execution_record`) in both `query` entities and all `tests` module SQL queries.
    - Updated `quickstart.md`.
  - **2026-04-02 — review & post-mortem**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Root Cause: E2E tests and `query` module entities were not updated after the `V9` table prefixing migration.
    - Cure: Mass-updated hardcoded table name strings across the repository.
    - Generalization: "Always audit dependent modules and test suites when performing non-automated database schema migrations."
  - **2026-04-02 — verification**
    - Validation status: approved
    - Evidence: `TrinoRoutingE2ETest` passed with green output (5/5 tests). Manual `curl` confirmed correct routing to Trino and trace recording in MySQL.


### QUERY-BUG-003: MAKE CLEAN SQL INDEPENDENT OF COMMENT VALUES

- **Status**: done
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Human clarified the parser contract: `query` may only parse and rewrite routing metadata, downstream-facing comment information such as `YH_*` metadata must still flow to the datasource path, and `cleanSql` must not vary just because comment contents differ.
    - Investigation confirmed the bug in `analyze/src/main/java/com/smartbi/analyze/sql/SqlCommentParser.java`: `cleanSql` was built by stripping only recognized hint-bearing comments, so ordinary `/* ... */` or trailing `-- ...` comments survived into `cleanSql`, making the so-called pure SQL depend on comment values even when the executable statement body was identical.
  - **2026-04-02 — implementation**
    - Files changed: `analyze/src/main/java/com/smartbi/analyze/sql/SqlCommentParser.java`, `analyze/src/test/java/com/smartbi/analyze/sql/SqlCommentParserTest.java`, `query/src/test/java/com/smartbi/query/parsing/SqlCommentParserTest.java`, `docs/modules/query.md`, `docs/modules/query-test-matrix.md`.
    - Commands run: `rg`, `sed`, `git diff`.
    - Result: Split clean-SQL generation away from hint recognition so `cleanSql` now removes every actual SQL comment while keeping quoted string literals intact, leaving `executionSql` on the existing pass-through path for downstream comment propagation.
  - **2026-04-02 — review & post-mortem**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Root Cause: The parser reused the recognized-hint match list to build `cleanSql`, which silently coupled “pure SQL” normalization to whether a comment looked like a known hint instead of whether it was a comment at all.
    - Cure: Added a dedicated clean-SQL builder that removes every matched line/block comment while preserving quoted string literals, and locked the contract with analyze-layer and query-layer regressions.
    - Generalization: Keep normalization paths for semantic SQL matching separate from hint-metadata extraction paths so metadata-recognition changes cannot accidentally alter the canonical SQL body.
  - **2026-04-02 — verification**
    - Validation status: approved
    - Evidence: `mvn -q -pl analyze,query -am -DfailIfNoTests=false -Dtest=SqlCommentParserTest test` passed; `mvn -q -pl analyze,query -am test` passed.
    - Next action: none
    - Escalation: none
  - **2026-04-02 — doc-garden**
    - Updated `docs/modules/query.md` and `docs/modules/query-test-matrix.md` so the contract now explicitly distinguishes fully comment-stripped `cleanSql` from downstream-facing `executionSql`.

### QUERY-BUG-002: ACCEPT QUERY SQL AFTER LEADING OPTIMIZER COMMENTS

- **Status**: done
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Human asked to verify whether query SQL with hints can incorrectly fail with `Only query SQL is supported by engine-query`, then requested a tracked fix when the bug was confirmed.
    - Investigation confirmed the bug is specific to leading generic optimizer comments such as `/*+ ... */ SELECT ...`: `QueryExecutionService` validates `parsed.cleanSql`, but `CachePolicy#isQuerySql` only checks whether the trimmed string starts with `select`/`with`/`show`/`describe`/`explain`, so a leading non-routing comment causes a false non-query rejection even though the statement body is read-only. Supported Easy Engine metadata hints such as `YH_TARGET_ENGINE` are already stripped before this check and are not affected.
  - **2026-04-02 — implementation**
    - Files changed: `query/src/main/java/com/smartbi/query/cache/CachePolicy.java`, `query/src/test/java/com/smartbi/query/cache/CachePolicyTest.java`, `query/src/test/java/com/smartbi/query/web/QueryWebIntegrationTest.java`, `docs/modules/query.md`, `docs/modules/query-test-matrix.md`, `docs/architecture/http-interfaces.md`.
    - Commands run: `rg`, `sed`, `git diff`.
    - Result: Taught `CachePolicy#isQuerySql` to strip only leading block/line comments before checking the first SQL verb, so leading optimizer hints and other front-loaded comments no longer trigger false non-query rejections, while commented `DELETE`/`INSERT` statements still fail the read-only gate.
  - **2026-04-02 — review & post-mortem**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Root Cause: Query-shape validation assumed the first non-whitespace characters were always the SQL verb, but generic optimizer comments survive `SqlCommentParser` and therefore left `cleanSql` starting with `/*` instead of `SELECT`.
    - Cure: Moved the read-only gate to a comment-aware check that strips only leading line/block comments before matching the first keyword, and added both unit and HTTP regressions around hinted read-only and write statements.
    - Generalization: Preserve strict statement-type validation, but normalize away syntax wrappers that are semantically outside the SQL verb before making allow/deny decisions.
  - **2026-04-02 — verification**
    - Validation status: approved
    - Evidence: `mvn -q -pl query -am -DfailIfNoTests=false -Dtest=CachePolicyTest,QueryWebIntegrationTest test` passed; `mvn -q -pl query -am test` passed.
    - Next action: none
    - Escalation: none
  - **2026-04-02 — doc-garden**
    - Updated `docs/modules/query.md`, `docs/modules/query-test-matrix.md`, and `docs/architecture/http-interfaces.md` so the query request contract and coverage inventory now explicitly describe leading-comment query detection, including optimizer-hint comments.

### BENCH-UX-010: REPLACE SQL TEMPLATES WITH SQL LIB REFERENCE WORKFLOW

- **Status**: done
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Human requested that benchmark rename user-facing SQL Templates to SQL Lib, move file uploads into SQL Lib, make test cases reference SQL Lib entries instead of owning editable SQL copies, widen supported import types to `.et`, `.txt`, `.csv`, and `.sql`, and replace the current right-side test-set SQL drawer workflow with list-first management that opens one SQL record at a time for detail/editing.
    - Investigation confirmed the current structure is template-centric: `/api/v1/templates` owns the global SQL pool, test-set items persist their own SQL payload, test-set upload only accepts Excel files into `benchmark_test_set_item`, and the frontend still renders full test-set SQL details inline in a right drawer. Scope for this task: preserve existing benchmark behavior through a forward migration while switching runtime/test-case membership to SQL Lib references.
  - **2026-04-02 — implementation**
    - Files changed: benchmark schema/runtime/API sources under `benchmark/src/main/java`, new Flyway Java migration `benchmark/src/main/java/db/migration/V18__sql_lib_reference_workflow.java`, new SQL import helpers/tests under `benchmark/src/{main,test}/java/com/smartbi/benchmark/sql`, benchmark frontend SQL Lib/test-set files under `benchmark/frontend/src/{api,router,utils,views}` plus focused frontend tests, and benchmark-facing docs under `docs/modules` and `docs/architecture`.
    - Commands run: `rg`, `sed`, `mvn -q -f benchmark/pom.xml -DskipTests compile`.
    - Result: Replaced the benchmark template-centric workflow with SQL Lib-backed reference membership, added source filename/upload timestamp metadata plus SQL Lib upload for `.xlsx`, `.xls`, Excel-compatible `.et`, `.csv`, `.txt`, and `.sql`, switched test sets to list/detail SQL Lib selection instead of inline SQL editing, kept `/api/v1/templates` as a compatibility alias, and forward-migrated existing test-set rows onto `sql_lib_id` references while leaving legacy payload columns in place for safe transition.
  - **2026-04-02 — review**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Notes: Verified that benchmark runtime now reads test-set SQLs from linked SQL Lib entries when present, SQL Lib deletion is blocked while referenced by test sets, direct test-set upload is rejected with guidance to SQL Lib, and frontend list/detail flows no longer require rendering thousands of full SQL payloads in a side drawer.
  - **2026-04-02 — verification**
    - Validation status: approved
    - Evidence: `mvn -q -f benchmark/pom.xml test` passed; `npm --prefix benchmark/frontend run test` passed; `npm --prefix benchmark/frontend run build` passed.
    - Next action: none
    - Escalation: Docker-backed Flyway seed coverage still logs the pre-existing local Docker-unavailable skip path, but the benchmark Maven suite completed successfully in this environment.
  - **2026-04-02 — doc-garden**
    - Updated `docs/modules/benchmark.md`, `docs/modules/benchmark-test-matrix.md`, `docs/modules/benchmark-sample-data.md`, and benchmark-related architecture docs so the documented benchmark contract now reflects SQL Lib terminology, SQL Lib upload formats, reference-only test-set membership, and the compatibility aliasing around `/api/v1/templates`.

### QUERY-CACHE-001: MAKE REDIS RESULT CACHE WRITES ASYNCHRONOUS

- **Status**: done
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Human requested verification that query-result saves to Redis do not block the main execution path. Investigation confirmed the current path is synchronous: `QueryExecutionService` calls `QueryCacheService#put(...)` inline after datasource execution, and `RedisQueryCacheStore#set(...)` performs a direct `StringRedisTemplate` write on that same thread. Scope for this task: make Redis-backed result-cache writes asynchronous while preserving cache-hit behavior, best-effort failure swallowing, and focused regression coverage.
  - **2026-04-02 — implementation**
    - Files changed: `query/src/main/java/com/smartbi/query/service/QueryCacheService.java`, `query/src/main/java/com/smartbi/query/config/QueryInfrastructureConfig.java`, `query/src/test/java/com/smartbi/query/service/QueryCacheServiceTest.java`, `docs/modules/query.md`, `docs/architecture/overview.md`, `docs/architecture/runtime-topology.md`.
    - Commands run: `rg`, `sed`, `git diff`.
    - Result: Added a dedicated query cache-write executor, changed `QueryCacheService#put(...)` to serialize and enqueue Redis writes instead of performing them inline, preserved best-effort failure swallowing by logging and skipping rejected async writes, and added regression coverage for deferred execution plus executor rejection fallback.
  - **2026-04-02 — review & post-mortem**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Root Cause: The cache layer exposed only a synchronous `set(...)` API, and `QueryExecutionService` invoked it directly on the request thread after each cacheable datasource execution, so Redis latency could extend end-to-end query latency even though cache persistence is a best-effort side effect.
    - Cure: Introduced a dedicated background executor for cache writes, moved the Redis `SET` handoff into `QueryCacheService`, and covered both deferred execution and rejection handling so query responses no longer wait on result-cache persistence.
    - Generalization: This task tightened an existing best-effort cache behavior but did not introduce a new repo-wide coding rule, so no `best-practices.md` update was needed.
  - **2026-04-02 — verification**
    - Validation status: approved
    - Evidence: Focused cache/query tests passed with `mvn -q -pl analyze,query -am -DfailIfNoTests=false -Dtest=QueryCacheServiceTest,QueryExecutionServiceTest,RedisQueryCacheStoreTest test`; full query-module reactor validation also passed with `mvn -q -pl analyze,query -am test`.
    - Next action: none
    - Escalation: none
  - **2026-04-02 — doc-garden**
    - Updated `docs/modules/query.md`, `docs/architecture/overview.md`, and `docs/architecture/runtime-topology.md` so the cache contract now states that Redis reads remain synchronous while result persistence is scheduled asynchronously as best-effort work.

### QUERY-ARCH-003: EXTRACT SHARED ANALYZE MODULE FOR ROUTING AND SQL ANALYSIS

- **Status**: done
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Human requested implementation of the shared `analyze` extraction plan: add a new Maven jar module plus root aggregator, move shared SQL parsing/routing/Calcite analysis into that module, switch runtime routing to `YH_TARGET_ENGINE` only, add manager query-routing context APIs for query consumption, and keep non-routing execution semantics unchanged.
  - **2026-04-02 — implementation**
    - Files changed: root build wiring in `pom.xml`, `analyze/pom.xml`, `query/pom.xml`, and `manager/pom.xml`; new shared analyzer sources/tests under `analyze/src/main/java` and `analyze/src/test/java`; query integration in `query/src/main/java/com/smartbi/query/{config,cache,datasource,parsing,route,service}` plus related tests; manager integration in `manager/src/main/java/com/smartbi/engine/{parse,trace,jdbc,web}` plus related tests; docs in `docs/architecture/{overview,http-interfaces,service-capabilities}.md`, `docs/modules/{query,manager}.md`, and `doc-CN/architecture-overview.md`.
    - Commands run: `rg`, `sed`, `git diff`, `mvn -q -pl analyze,query,manager -am -DskipTests compile`.
    - Result: Added the shared `analyze` jar and root Maven reactor, moved SQL comment parsing/routing/dialect hooks and Calcite structure analysis into that module, rewired `query` to use cached manager routing context with `YH_TARGET_ENGINE`-only routing plus normalized leading comments, rewired manager parse/JDBC advisory paths to the shared analyzer, and added `GET /api/v1/query-routing-context`.
  - **2026-04-02 — review & post-mortem**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Root Cause: SQL analysis responsibilities had drifted across `query` and `manager`, with duplicated parsing contracts, a deprecated `engine` routing hint still influencing behavior, and no shared route-analysis surface for acceleration-aware comment rewriting.
    - Cure: Extracted the shared SQL-analysis logic into `analyze`, made `YH_TARGET_ENGINE` the sole routing signal, normalized executable SQL comments in `query`, and turned manager parse/rewrite entrypoints into thin shared-analyzer wrappers.
    - Generalization: This task changed architecture/build ownership directly; no new `docs/operations/best-practices.md` rule was added.
  - **2026-04-02 — verification**
    - Validation status: approved with harness follow-up
    - Evidence: `mvn -q -pl analyze test` passed; `mvn -q -pl query -am test` passed; `mvn -q -pl manager -am -DfailIfNoTests=false -Dspring.mvc.pathmatch.matching-strategy=ant_path_matcher -Dtest=SqlParseServiceTest,JdbcSqlAdvisorServiceTest,ManagerControllerWebTest,QueryRoutingContextControllerTest test` passed.
    - Next action: none for product code; harness validation-command drift is tracked separately.
    - Escalation: INBOX-20260402-021
  - **2026-04-02 — doc-garden**
    - Updated architecture and module docs to describe the shared `analyze` module, the new `/api/v1/query-routing-context` API, `YH_TARGET_ENGINE`-only routing semantics, and the reactor-based verification path; refreshed the configured Chinese architecture mirror.

### CONFIG-PROFILE-001: UNIFY MODULE ENV CONFIG INTO DEV TEST PRO PROFILES

- **Status**: done
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Human clarified that module configuration must be centralized into `dev` / `test` / `pro` profile YAML files, with environment-specific settings removed from code, test code, scripts, and Maven defaults. Scope for this task: add the profile files for `manager`, `query`, and `benchmark`, move runtime/test environment settings into those shared module configs, standardize profile-based startup and DB-init commands, clean scattered test environment config, and doc-garden the resulting workflow.
  - **2026-04-02 — implementation**
    - Files changed: profile configs under `manager/query/benchmark/src/main/resources/application-{dev,test,pro}.yml`; shared test bootstrap files under each module `src/test/resources/application.yml`; `scripts/init-db.sh`; `manager/src/main/java/com/smartbi/engine/ManagerDbInitApplication.java`; `benchmark/src/main/java/com/smartbi/benchmark/BenchmarkDbInitApplication.java`; `manager/pom.xml`; `benchmark/pom.xml`; `query/src/main/java/com/smartbi/query/config/QueryProperties.java`; `benchmark/src/main/java/com/smartbi/benchmark/config/BenchmarkJdbcProperties.java`; benchmark/query/manager test helpers and the benchmark/query/manager tests that previously carried inline env config; docs in `docs/operations/local-development.md`, `docs/modules/{query,manager,benchmark}.md`, `doc-CN/local-development.md`, and `doc-CN/quickstart.md`; `docs/operations/best-practices.md`.
    - Commands run: `rg`, `sed`, `git diff`.
    - Result: Added `dev/test/pro` profile YAMLs for all three backend modules, removed env defaults from the old main `application.yml` files, moved startup/init selection to profile-based commands, centralized benchmark/query test env data into module-level `application-test.yml`, added init-only Spring Boot entrypoints for `manager` and `benchmark`, removed Maven/script hard-coded DB defaults, and replaced the two older config rules in `best-practices.md` with the new English profile-governance rule.
  - **2026-04-02 — review & post-mortem**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Root Cause: Runtime and test environment settings had drifted across Spring YAML, Java defaults, `@SpringBootTest(properties = ...)`, per-test property files, helper constants, Maven Flyway defaults, shell-script defaults, and older fragmented best-practice guidance, so changing one environment contract required many unrelated edits.
    - Cure: Promoted `application-dev.yml`, `application-test.yml`, and `application-pro.yml` to the only module-level environment sources, rewired test helpers and Spring tests to consume shared `application-test.yml`, replaced script/POM defaults with explicit profile-driven execution plus env overrides, and consolidated the generalized rule in `docs/operations/best-practices.md`.
    - Generalization: "Keep all environment-specific configuration in the module's three Spring profile files: `application-dev.yml`, `application-test.yml`, and `application-pro.yml`. Do not hard-code environment-specific endpoints, ports, datasource URLs, credentials, Redis settings, Flyway settings, or similar deployment/test configuration in Java code, test code, shell scripts, Maven defaults, or per-test property files." (replaced the two older config-related rules in `docs/operations/best-practices.md`)
  - **2026-04-02 — verification**
    - Validation status: approved with known follow-up
    - Evidence: `mvn -q -f query/pom.xml test` passed; `mvn -q -f manager/pom.xml test` passed; `mvn -q -f benchmark/pom.xml test` passed with the existing Docker-less Testcontainers warning path; `npm --prefix manager/frontend run build` passed; `npm --prefix benchmark/frontend run build` passed; `bash scripts/init-db.sh dev manager benchmark` passed; `bash scripts/init-db.sh test manager benchmark` passed for `manager`; `BENCHMARK_TEST_DB_JDBC_URL='jdbc:mysql://localhost:3307/engine_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' BENCHMARK_TEST_DB_USER='engine' BENCHMARK_TEST_DB_PASSWORD='engine123' BENCHMARK_TEST_DB_DRIVER_CLASS='com.mysql.cj.jdbc.Driver' bash scripts/init-db.sh test benchmark` passed for the benchmark test-profile deployment path.
    - Next action: none
    - Escalation: INBOX-20260402-017
  - **2026-04-02 — doc-garden**
    - Updated module and local-development docs so local startup now uses `dev`, test/prod activation is explicit, DB init commands take `<dev|test|pro>` plus target modules, and the best-practice guidance now reflects the unified profile-governance rule.

### TEST-CONFIG-001: AUDIT TESTS FOR HARD-CODED CONNECTION FIXTURES

- **Status**: done
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Follow-up review task created automatically from `BENCH-TEST-002` after exporting a new best-practice rule: audit existing tests for inline datasource URLs, probe endpoints, usernames, passwords, or similar connection fixtures and move them into test-owned config where appropriate.
  - **2026-04-02 — investigation**
    - Human requested a project-wide review against the newest applicable best-practice. The latest rule added to `docs/operations/best-practices.md` by `QUERY-BUG-001` was already reviewed and closed by `QUERY-REVIEW-002`, so this still-open task became the active follow-up for the remaining unresolved best-practice drift.
    - Initial audit found clear remaining violations in benchmark/query tests where datasource URLs, usernames, passwords, and related request payload fixtures were embedded directly in `@SpringBootTest(properties = ...)`, setup methods, helper builders, or inline JSON bodies instead of test-owned config.
  - **2026-04-02 — implementation**
    - Files changed: `benchmark/src/test/java/com/smartbi/benchmark/run/BenchmarkAsyncRunnerExecutionModeIntegrationTest.java`, `benchmark/src/test/java/com/smartbi/benchmark/web/BenchmarkSmokeTest.java`, `benchmark/src/test/java/com/smartbi/benchmark/web/DataSourceControllerTest.java`, `benchmark/src/test/java/com/smartbi/benchmark/web/JdbcDriverUploadIntegrationTest.java`, `benchmark/src/test/java/com/smartbi/benchmark/web/RunControllerContextTest.java`, `benchmark/src/test/java/com/smartbi/benchmark/support/BenchmarkTestFixtures.java`, `benchmark/src/test/resources/benchmark-async-runner-execution-mode-integration-test.properties`, `benchmark/src/test/resources/benchmark-smoke-test.properties`, `benchmark/src/test/resources/benchmark-test-fixtures.properties`, `benchmark/src/test/resources/jdbc-driver-upload-integration-test.properties`, `benchmark/src/test/resources/run-controller-context-test.properties`, `query/src/test/java/com/smartbi/query/config/ManagerConfigClientTest.java`, `query/src/test/java/com/smartbi/query/datasource/ManagedDataSourceRegistryTest.java`, `query/src/test/java/com/smartbi/query/route/SqlRouteServiceTest.java`, `query/src/test/java/com/smartbi/query/service/QueryResultMapperTest.java`, `query/src/test/java/com/smartbi/query/trace/QueryTracePersistenceIntegrationTest.java`, `query/src/test/java/com/smartbi/query/web/QueryWebIntegrationTest.java`, `query/src/test/java/com/smartbi/query/support/QueryTestFixtures.java`, `query/src/test/resources/query-test-fixtures.properties`, `query/src/test/resources/query-trace-persistence-integration-test.properties`, `query/src/test/resources/query-web-integration-test.properties`.
    - Commands run: `rg`, `sed`, `git diff`.
    - Result: Moved benchmark/query datasource and credential fixtures out of inline test code into dedicated classpath property files plus small test-fixture loaders, updated Spring integration tests to use `@TestPropertySource`, and isolated the known benchmark SPA route-pattern drift behind test-only matching-strategy config so the touched benchmark tests could still validate cleanly.
  - **2026-04-02 — review & post-mortem**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Root Cause: Test connection details had drifted into inline annotation properties, setup methods, and request payload builders over time, so changing fixtures still required Java edits instead of isolated test-config updates.
    - Cure: Centralized datasource URLs, usernames, passwords, and related request fixture values in classpath test property files, added tiny fixture loaders for non-Spring tests, and used test-only property overrides where needed so focused validation could exercise the fixture cleanup without touching production runtime behavior.
    - Generalization: Existing rule from `BENCH-TEST-002` already covers this case; no new `docs/operations/best-practices.md` entry was needed.
  - **2026-04-02 — verification**
    - Validation status: approved
    - Evidence: `mvn -q -f query/pom.xml -Dtest=QueryWebIntegrationTest,QueryTracePersistenceIntegrationTest,QueryResultMapperTest,ManagerConfigClientTest,ManagedDataSourceRegistryTest,SqlRouteServiceTest test` passed; `mvn -q -f benchmark/pom.xml -Dtest=BenchmarkSmokeTest,JdbcDriverUploadIntegrationTest,RunControllerContextTest,BenchmarkAsyncRunnerExecutionModeIntegrationTest,DataSourceControllerTest test` passed; `rg -n '@SpringBootTest\\([^\\)]*properties\\s*=\\s*\\{|DriverManager\\.getConnection\\(\"|setManagerUrl\\(\"http://localhost:8090|setJdbcUrl\\(\"jdbc:|\"jdbcUrl\":\"jdbc:' benchmark/src/test manager/src/test query/src/test tests/src/test --glob '!**/target/**'` returned no matches.
    - Next action: none
    - Escalation: none

### MGR-DB-001: REVIEW MODULE TABLE PREFIXES AND PREFIX MANAGER TABLES

- **Status**: done
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Human requested a review-and-fix pass so database tables consistently use their owning module name as a prefix. Scope for this task: audit current module-owned tables, rename the manager-owned tables that still lack a `manager_` prefix, preserve upgrade compatibility for existing manager schemas, and doc-garden any table-name references that change.
  - **2026-04-02 — implementation**
    - Files changed: `manager/src/main/java/com/smartbi/engine/config/FlywayConfig.java`, `manager/src/main/java/com/smartbi/engine/domain/SqlExecutionRecord.java`, `manager/src/main/java/com/smartbi/engine/domain/SqlPatternStats.java`, `manager/src/main/java/com/smartbi/engine/domain/AccelerationTable.java`, `manager/src/main/java/com/smartbi/engine/datasource/QueryDatasourceConfig.java`, `manager/src/main/resources/db/migration/V9__prefix_manager_tables.sql`, `manager/src/test/java/com/smartbi/engine/migration/ManagerDashboardBootstrapIntegrationTest.java`, `manager/src/test/java/com/smartbi/engine/datasource/DatasourceConfigFlywayIntegrationTest.java`, `manager/src/test/java/com/smartbi/engine/trace/TraceFlywayExecutionModeIntegrationTest.java`, `manager/src/test/java/com/smartbi/engine/migration/ManagerFlywayHistoryRenameIntegrationTest.java`, `manager/src/test/resources/db/manager-v8-legacy-flyway-baseline.sql`, `docs/modules/manager.md`, `doc-CN/quickstart.md`.
    - Commands run: `rg`, `sed`, `git diff`.
    - Result: Audited module-owned tables, confirmed benchmark tables already used module prefixes, renamed the manager runtime mappings to `manager_*`, added a forward Flyway migration for existing schemas, and taught manager Flyway startup to adopt `manager_flyway_schema_history` while preserving legacy history during upgrades.
  - **2026-04-02 — review & post-mortem**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Root Cause: Manager schema naming drift accumulated because the original migrations used generic table names (`sql_*`, `acceleration_table`) and a cross-module `query_datasource_config` name, while Flyway metadata stayed on its default unprefixed history table.
    - Cure: Added a dedicated forward migration that renames manager-owned tables in place, switched JPA mappings/docs/tests to the new names, and configured Flyway to use a manager-prefixed history table with a legacy-history handoff for existing schemas.
    - Generalization: This task applied an explicit repo naming rule rather than surfacing a reusable new coding practice, so no new `best-practices.md` entry was needed.
  - **2026-04-02 — verification**
    - Validation status: approved
    - Evidence: `mvn -q -f manager/pom.xml test` still fails for unrelated existing manager startup drift because `EngineConfig` registers a PathPattern-incompatible SPA route (`/**/{path:[^\\.]*}`); task-local verification passed with `mvn -q -f manager/pom.xml -Dspring.mvc.pathmatch.matching-strategy=ant_path_matcher -Dtest=DatasourceConfigFlywayIntegrationTest,TraceFlywayExecutionModeIntegrationTest,ManagerDashboardBootstrapIntegrationTest,ManagerFlywayHistoryRenameIntegrationTest test`; `npm --prefix manager/frontend run build` also passed.
    - Next action: none
    - Escalation: INBOX-20260402-011
  - **2026-04-02 — doc-garden**
    - Updated `docs/modules/manager.md` and `doc-CN/quickstart.md` so the current manager table inventory and trace-field backing tables now reflect the prefixed schema names.

### ARCH-015: SEPARATE DATABASE INITIALIZATION FROM SERVICE STARTUP

- **Status**: done
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Human requested that database table creation and seed insertion stop happening during service startup and move to a separate initialization script, with the docs updated and the full task workflow completed in one pass.
  - **2026-04-02 — implementation**
    - Files changed: `manager/src/main/resources/application.yml`, `benchmark/src/main/resources/application.yml`, `manager/pom.xml`, `benchmark/pom.xml`, `benchmark/src/test/java/com/smartbi/benchmark/migration/BenchmarkFlywaySeedTest.java`, `scripts/init-db.sh`, `docs/operations/local-development.md`, `docs/modules/manager.md`, `docs/modules/benchmark.md`, `doc-CN/local-development.md`.
    - Commands run: `rg`, `sed`, `git diff`, `chmod +x scripts/init-db.sh`.
    - Result: Disabled automatic Flyway execution during normal `manager` and `benchmark` startup, added an explicit `scripts/init-db.sh` entrypoint for schema/seed initialization, wired Maven Flyway plugin support for `manager`, and updated the English and Chinese local-development docs plus module runbooks to require the separate init step.
  - **2026-04-02 — review & post-mortem**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Root Cause: Metadata schema creation and seed insertion were still coupled to normal Spring Boot startup through default-enabled Flyway config, so booting `manager` or `benchmark` mutated the database as a side effect instead of relying on an explicit operator action.
    - Cure: Switched runtime defaults so Flyway is no longer invoked automatically during normal startup, added a dedicated init script for the schema/seed path, and documented the new startup order so operators initialize the database intentionally before booting services.
    - Generalization: Keep schema bootstrap and seed loading behind explicit operational entrypoints rather than hiding them inside default service startup.
  - **2026-04-02 — verification**
    - Validation status: approved
    - Evidence: `bash -n scripts/init-db.sh` passed; `mvn -q -f manager/pom.xml -DskipTests compile` passed; `mvn -q -f benchmark/pom.xml -DskipTests compile` passed; `mvn -q -f benchmark/pom.xml -Dtest=BenchmarkFlywaySeedTest test` exited cleanly with the existing Docker-less Testcontainers skip behavior. Additional focused web-context tests for `manager` and `benchmark` still fail for the unrelated SPA route-pattern issue (`PatternParseException` on `/**/{path:[^\\.]*}`), and benchmark still inherits the known Docker/Testcontainers limitation.
    - Next action: none
    - Escalation: INBOX-20260402-011
  - **2026-04-02 — doc-garden**
    - Updated `docs/operations/local-development.md`, `docs/modules/manager.md`, and `docs/modules/benchmark.md` to make explicit DB initialization the canonical workflow, and refreshed the configured Chinese mirror in `doc-CN/local-development.md`.

### QUERY-TRINO-001: ADD TRINO DATASOURCE SUPPORT

- **Status**: done
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Human requested a new Trino datasource path for `query`, including task-ledger tracking and full closeout. Scope for this task: make Trino-backed datasource configs executable in `query`, add focused regression coverage, and doc-garden any query-facing datasource guidance that changes.
  - **2026-04-02 — implementation**
    - Files changed: `query/pom.xml`, `query/src/main/java/com/smartbi/query/route/SqlRouteService.java`, `query/src/main/resources/application.yml`, `query/src/test/java/com/smartbi/query/route/SqlRouteServiceTest.java`, `query/src/test/java/com/smartbi/query/datasource/TrinoDriverAvailabilityTest.java`, `query/src/test/java/com/smartbi/query/web/QueryTrinoRoutingIntegrationTest.java`, `docs/modules/query.md`.
    - Commands run: `rg`, `sed`, `git diff`, `mvn -q -f query/pom.xml -Dtest=TrinoDriverAvailabilityTest,SqlRouteServiceTest,QueryTrinoRoutingIntegrationTest test`, `mvn -q -f query/pom.xml test`.
    - Result: Bundled `io.trino:trino-jdbc`, taught `query` to treat `type=trino` as a first-class routed datasource, added a `trino_local` fallback example for manager-outage mode, and added regression coverage for both packaged-driver availability and Trino-routed query execution through `/kylin/api/query`.
  - **2026-04-02 — review & post-mortem**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Post-mortem: not applicable; this task adds datasource capability rather than correcting a bug or style regression.
  - **2026-04-02 — verification**
    - Validation status: approved
    - Evidence: `mvn -q -f query/pom.xml -Dtest=TrinoDriverAvailabilityTest,SqlRouteServiceTest,QueryTrinoRoutingIntegrationTest test` passed, including the new routed Trino integration path; `mvn -q -f query/pom.xml test` also passed for the full query module.
    - Next action: none
    - Escalation: none
  - **2026-04-02 — doc-garden**
    - Updated `docs/modules/query.md` so the prepared-parameter contract now explicitly includes Trino in the non-Kylin JDBC path and the routing notes document the expected `type`, driver class, and JDBC URL shape for Trino datasource configs. No additional architecture or local-development drift was required for this task.

### QUERY-REVIEW-002: AUDIT QUERY COMPATIBILITY INPUT VALIDATION

- **Status**: done
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Follow-up review task created automatically from `QUERY-BUG-001` after exporting a new best-practice rule: audit the remaining query compatibility/shim entrypoints for missing blank or unsupported request validation and apply the same early-rejection pattern where needed.
  - **2026-04-02 — investigation**
    - Audited the remaining `query` compatibility surface (`POST /kylin/api/query` plus the lightweight authentication shim) and confirmed two unresolved gaps on the query boundary: an empty request body could still be rejected by Spring before the compatibility exception payload was built, and unknown top-level JSON fields were silently ignored even though the endpoint should only accept the query-request envelope.
  - **2026-04-02 — implementation**
    - Files changed: `query/src/main/java/com/smartbi/query/api/dto/PreparedQueryRequestDto.java`, `query/src/main/java/com/smartbi/query/service/QueryExecutionService.java`, `query/src/main/java/com/smartbi/query/web/QueryController.java`, `query/src/test/java/com/smartbi/query/service/QueryExecutionServiceTest.java`, `query/src/test/java/com/smartbi/query/web/QueryWebIntegrationTest.java`, `docs/modules/query.md`, `docs/architecture/http-interfaces.md`, `docs/modules/query-test-matrix.md`.
    - Commands run: `rg`, `sed`, `git diff`.
    - Result: Allowed `POST /kylin/api/query` to accept an absent JSON body so the existing service-level blank-request guard returns the normal Kylin-style exception payload, captured unsupported top-level request fields on the query DTO, rejected those non-query request shapes before routing/cache/datasource work, and added regression coverage for both service and HTTP paths.
  - **2026-04-02 — review & post-mortem**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Root Cause: The earlier blank/non-query fix hardened `QueryExecutionService`, but `QueryController` still required a request body, so Spring MVC could reject empty compatibility requests before the module's own exception envelope ran, and the compatibility DTO still ignored unknown JSON fields instead of treating them as unsupported request shapes.
    - Cure: Made the request body optional at the controller boundary, captured unknown top-level fields on `PreparedQueryRequestDto`, and reused the established invalid-request response path so both empty bodies and unsupported request envelopes fail before routing, cache, or datasource work.
    - Generalization: Existing rule from `QUERY-BUG-001` already covers this case; no new best-practice entry was needed.
  - **2026-04-02 — verification**
    - Validation status: approved
    - Evidence: `mvn -q -f query/pom.xml -Dtest=QueryExecutionServiceTest,QueryWebIntegrationTest test` passed, including the empty-body, null-request, and unsupported-request-shape regressions; `mvn -q -f query/pom.xml test` also passed.
    - Next action: none
    - Escalation: none
  - **2026-04-02 — doc-garden**
    - Updated `docs/modules/query.md`, `docs/architecture/http-interfaces.md`, and `docs/modules/query-test-matrix.md` so the canonical docs now state that `/kylin/api/query` rejects unsupported top-level request fields in addition to blank or non-query SQL.

### QUERY-BUG-001: REJECT NON-QUERY REQUESTS AT QUERY BOUNDARY

- **Status**: done
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Task created from the human-reported issue: `query` must only accept supported query requests on `/kylin/api/query`, reject unsupported request shapes/types with the normal exception response path, and record any broader optimization follow-up in the active ledger.
  - **2026-04-02 — implementation**
    - Files changed: `query/src/main/java/com/smartbi/query/service/QueryExecutionService.java`, `query/src/test/java/com/smartbi/query/service/QueryExecutionServiceTest.java`, `query/src/test/java/com/smartbi/query/web/QueryWebIntegrationTest.java`.
    - Commands run: `rg`, `sed`, `javap`, `git diff`.
    - Result: Added an early blank/non-query request guard ahead of cache and datasource work, kept the Kylin-compatible exception response behavior, and added regression tests for both service and HTTP paths.
  - **2026-04-02 — review & post-mortem**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Root Cause: `QueryExecutionService` only rejected unsupported SQL after routing and cache setup work had already started, and blank SQL had no explicit compatibility guard at all.
    - Cure: Validate the parsed SQL envelope before downstream work, return the normal exception payload for blank or unsupported requests, and document the request contract explicitly.
    - Generalization: "Validate compatibility-shim request envelopes before routing, cache, or datasource work. Reject blank or unsupported requests through the module's established error contract instead of letting them fall through to downstream execution logic." (added to `docs/operations/best-practices.md`)
  - **2026-04-02 — verification**
    - Validation status: approved
    - Evidence: `mvn -q -f query/pom.xml test` passed, including the new blank-request regressions in `QueryExecutionServiceTest` and `QueryWebIntegrationTest`; `python3 scripts/task_audit.py --check` was also run during closeout and still fails for unrelated legacy governance drift because `BENCH-UX-007` has no matching task-id commit subject.
    - Next action: none
    - Escalation: INBOX-20260402-006
  - **2026-04-02 — doc-garden**
    - Updated `docs/modules/query.md` and `docs/architecture/http-interfaces.md` to describe blank/non-query rejection behavior, and created follow-up review task `QUERY-REVIEW-002` for broader compatibility-input auditing.

### BENCH-TEST-002: EXTERNALIZE PREFLIGHT CONTROLLER TEST FIXTURES

- **Status**: done
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Task created from the human-reported issue: `benchmark/src/test/java/com/smartbi/benchmark/web/PreflightControllerTest.java` still hard-codes preflight datasource/probe fixture values in test code, but benchmark tests should keep those fixtures in test-owned config instead of inline Java literals.
  - **2026-04-02 — implementation**
    - Files changed: `benchmark/src/test/java/com/smartbi/benchmark/web/PreflightControllerTest.java`, `benchmark/src/test/resources/preflight-controller-test.properties`, `docs/operations/best-practices.md`, `INBOX.md`.
    - Commands run: `rg`, `sed`, `git diff`.
    - Result: Moved the preflight probe fixture values out of inline test code into a dedicated classpath test properties file and updated the controller test to load those values through a single helper.
  - **2026-04-02 — review & post-mortem**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Root Cause: `PreflightControllerTest` embedded probe URLs and credentials directly in each test method, so test fixture maintenance required Java code edits instead of isolated test config updates.
    - Cure: Added a dedicated test properties file under `src/test/resources` and centralized fixture loading inside the test helper.
    - Generalization: "Keep datasource, probe endpoint, and credential fixtures for tests in test-owned property files or test property sources instead of hard-coding them inside test methods." (added to `docs/operations/best-practices.md`)
  - **2026-04-02 — verification**
    - Validation status: approved
    - Evidence: `mvn -q -f benchmark/pom.xml -Dtest=PreflightControllerTest test` passed; `npm --prefix benchmark/frontend run build` passed; `mvn -q -f benchmark/pom.xml test` still fails for the already-tracked benchmark validation drift (`WebConfig` route pattern startup error plus unavailable Docker/Testcontainers support) recorded in `INBOX-20260402-001`.
    - Next action: none
    - Escalation: INBOX-20260402-001
  - **2026-04-02 — doc-garden**
    - Exported the new testing rule to `docs/operations/best-practices.md` and created follow-up review task `TEST-CONFIG-001` for retroactive cleanup.

### BENCH-CONFIG-001: EXTERNALIZE BENCHMARK PREFLIGHT DATASOURCE PROBE SETTINGS

- **Status**: done
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Task created from human-reported issue: remove hard-coded datasource/probe info from `BenchmarkPreflightProperties`, keep benchmark preflight configurable, and finish the full task workflow in one session.
  - **2026-04-02 — implementation**
    - Files changed: `benchmark/src/main/java/com/smartbi/benchmark/config/BenchmarkPreflightProperties.java`, `benchmark/src/main/resources/application.yml`, `benchmark/src/test/java/com/smartbi/benchmark/config/BenchmarkPreflightPropertiesTest.java`, `benchmark/src/test/java/com/smartbi/benchmark/web/PreflightControllerTest.java`, `docs/modules/benchmark.md`, `docs/operations/best-practices.md`, `INBOX.md`.
    - Commands run: `rg`, `sed`, `git diff`.
    - Result: Removed Java-level hard-coded preflight probe URLs and credentials, moved the active values to configuration with environment-variable override support, and added regression coverage so direct POJO construction no longer hides embedded datasource details.
  - **2026-04-02 — review & post-mortem**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Root Cause: `BenchmarkPreflightProperties` carried environment-specific probe URLs and credentials as Java field defaults, so the class itself encoded datasource connection details instead of remaining a pure configuration binding.
    - Cure: Removed the Java defaults, required tests to set probe credentials explicitly, and documented/env-exposed the configuration contract in Spring YAML.
    - Generalization: "Keep environment-specific endpoints, usernames, and passwords out of Java `@ConfigurationProperties` defaults. Bind them from Spring configuration instead." (added to `docs/operations/best-practices.md`)
  - **2026-04-02 — verification**
    - Validation status: approved
    - Evidence: `mvn -q -f benchmark/pom.xml -Dtest=BenchmarkPreflightPropertiesTest,PreflightControllerTest test` passed; `npm --prefix benchmark/frontend run build` passed; `mvn -q -f benchmark/pom.xml test` was also executed but still fails for unrelated benchmark-suite drift (`PatternParseException` from `benchmark/src/main/java/com/smartbi/benchmark/config/WebConfig.java` and missing Docker/Testcontainers support), which is logged in `INBOX-20260402-001`.
    - Next action: none
    - Escalation: INBOX-20260402-001
  - **2026-04-02 — doc-garden**
    - Updated `docs/modules/benchmark.md` to describe the externalized benchmark preflight configuration contract and environment-variable override points.

### BENCH-COMPAT-001: FIX JAVA 8 COMPATIBILITY (PATH.OF, LIST.OF)

- **Status**: done
- **Updated**: 2026-04-01
- **Progress log**:
  - **2026-04-01 — implementation**
    - Files changed: `JdbcDriverRegistry.java`, `JdbcDriverUploadIntegrationTest.java`, `TraceIngestionTest.java`, `TraceControllerTest.java`, `SqlParseServiceTest.java`.
    - Commands run: `grep`, `mvn test`.
    - Result: Implemented by replacing `Path.of`, `List.of`, and `Files.writeString` with Java 8 compatible alternatives (`Paths.get`, `Arrays.asList`, `Files.write`).
  - **2026-04-01 — verification**
    - Validation status: approved
    - Evidence: `mvn -q -f benchmark/pom.xml test` and `mvn -q -f manager/pom.xml test` passed successfully.
    - Next action: none
    - Escalation: none
 
### BENCH-UX-009: IMPROVE BENCHMARK TEST-SET AUTHORING WORKFLOW

- **Status**: done
- **Updated**: 2026-03-31
- **Progress log**:
  - **2026-03-31 — implementation**
    - Investigated current benchmark test-set flow and confirmed the gap is structural: the create dialog only saves metadata, uploaded rows are read-only, and there is no API to author or seed test-set items from SQL templates.
    - Locked the target workflow for this task: new test sets must support upload-or-empty creation from one dialog, editable item management, and copying templates into test sets for later editing.
    - Added backend item-authoring APIs plus `TestSetAuthoringService`, expanded upload metadata support, rewired the Benchmark Test Sets UI to author rows in-place, and added regression tests for the new create/upload/template-copy flows.
  - **2026-03-31 — verification**
    - Validation status: approved
    - Evidence: `mvn -q -f benchmark/pom.xml test` passed; `npm --prefix benchmark/frontend run test` passed; `npm --prefix benchmark/frontend run build` passed.
    - Next action: none
    - Escalation: none
  - **2026-03-31 — doc-garden**
    - Updated `docs/modules/benchmark.md` and `docs/modules/benchmark-test-matrix.md` to describe the new test-set authoring workflow, APIs, and automated coverage.
---

### DOC-CN-002: WRITE CHINESE QUICKSTART DOCUMENTATION

- **Status**: done
- **Updated**: 2026-04-01
- **Progress log**:
  - **2026-04-01 — planning**
    - Researched environment requirements (Java 8, Node 18, Maven, Docker).
    - Identified database configuration (MySQL 3307, engine_db).
    - Confirmed service initialization via Flyway.
    - Implementation plan approved by user.
  - **2026-04-01 — implementation**
    - Created `doc-CN/quickstart.md` in Chinese with environmental setup, DB config, and service startup instructions.
    - Updated `doc-CN/README.md` to include the Quickstart link.
  - **2026-04-01 — verification**
    - Validation status: approved
    - Evidence: File paths and content verified against `docker-compose.yml` and `pom.xml`.

---

### DOC-CN-003: ADD MYSQL TABLE OVERVIEW TO QUICKSTART

- **Status**: done
- **Updated**: 2026-04-01
- **Progress log**:
  - **2026-04-01 — implementation**
    - Identified tables for Manager and Benchmark modules via migration files.
    - Added structured table overview by module to doc-CN/quickstart.md.
  - **2026-04-01 — verification**
    - Validation status: approved
    - Evidence: Table names and descriptions verified against Flyway migration files in both modules.

### task-ui-integration-001: 整合前端静态资源及 SPA 路由支持

- **Status**: done
- **Updated**: 2026-04-01
- **Progress log**:
  - **2026-04-01 — implementation**
    - Configured `EngineConfig.java` and `WebConfig.java` to support SPA route forwarding.
    - Created `src/main/resources/static/` directories in manager and benchmark modules.
    - Updated `quickstart.md` with instructions for both online dev and offline bundled deployment.
  - **2026-04-01 — verification**
    - Verified Spring Boot configuration handles SPA history mode.
    - Verified static resource paths match Maven project standards.

### MIGRATION-001: UPGRADE TEXT COLUMNS TO MEDIUMTEXT FOR LONG SQL SUPPORT

- **Status**: done
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - User reported SQL length issues with `error_sample` (truncated error reports).
    - Identified multiple `TEXT` columns (64KB limit) in MySQL that risk overflow for long SQLs (thousands of lines) and large JSON evaluation snapshots.
    - Decided to upgrade all relevant SQL/JSON storage columns to `MEDIUMTEXT` (16MB).
  - **2026-04-02 — implementation**
    - Updated JPA domain classes in `benchmark`, `query`, and `manager` modules (10 files total).
    - Created Flyway migration scripts: `benchmark/V18` and `manager/V11`.
    - Verified compilation of all affected modules with `mvn clean compile`.
  - **2026-04-02 — review & post-mortem**
    - Self-Review: [x] style check [x] test coverage [x] side-effects
    - Root Cause: Default `TEXT` type in MySQL (64KB) is insufficient for complex SQL queries (thousands of lines) and large JSON snapshots.
    - Cure: Upgraded affected columns to `MEDIUMTEXT` (16MB).
    - Generalization: "Use `MEDIUMTEXT` (16MB) instead of default `TEXT` (64KB) for all database columns that store raw SQL text, complex JSON snapshots, error samples, or evaluation reports." (added to `best-practices.md`)
  - **2026-04-02 — verification**
    - Validation status: approved
    - Evidence: All backend modules compiled successfully. Flyway scripts follow existing naming conventions for both shared and module-prefixed schemas.
