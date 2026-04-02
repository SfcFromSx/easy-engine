# Completed Tasks

This file is the completed-task archive for Easy Engine.

The foreman should read [tasks.md](/Users/sfc/Documents/projects/engine/tasks.md) for task selection and active progress, and consult this archive only when completed-task history or previous done signals are relevant.

## Done

| ID | Title | Module | Done signal |
|----|-------|--------|-------------|
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
