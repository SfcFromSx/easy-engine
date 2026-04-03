# Inbox

This is the canonical repo-root inbox for agent-found issues and suggestions that require human review before they become implementation work.

## Rules

- Record items here only when they are not already tracked in `tasks.md`.
- Keep entries evidence-based and concise.
- Use this inbox for defects, harness/process concerns, and improvement suggestions that need human judgment.
- Task-specific failures that are already fully captured in a task's progress log do not need a duplicate inbox entry.
- Systemic harness, tooling, process, or documentation issues discovered while working another task must still be logged here, even if the triggering task is already tracked in `tasks.md`.
- **Do not** log generalized coding practices, codebase styling rules, or post-mortem fixes here. These belong in task progress logs and `docs/operations/best-practices.md`.
- When a task blocks at max attempts, its latest rejected or revalidation note in `tasks.md` must include `Escalation: none` or `Escalation: INBOX-...` so humans can tell whether the issue stayed task-local or was escalated here.
- Do not convert inbox entries directly into implementation tasks without explicit human confirmation.
- For tooling or infrastructure changes, prefer logging the issue here before changing the harness.

### INBOX-20260403-013
- Area: Harness audit trail
- Related task(s): `UI-BUNDLE-001`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-03 to archive the completion of `UI-BUNDLE-001` after cleaning temporary artifacts and refreshing embedded frontend bundles.
- Evidence: The active ledger row and section for `UI-BUNDLE-001` were removed from `tasks.md`, its full progress log and done signal were archived under `tasks-done.md`, and the archived evidence records both frontend builds plus the static-resource sync into `manager` and `benchmark`.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger edits as ad hoc bookkeeping rather than the required closeout workflow for this bundle-refresh task.
- Suggested next step: Keep the archived task entry and task commit aligned; unrelated local config changes should remain outside this commit.
- Human decision: pending

### INBOX-20260403-012
- Area: Harness audit trail
- Related task(s): `UI-BUNDLE-001`
- Summary: `tasks.md` was updated on 2026-04-03 to intake a human-requested cleanup and frontend-bundle refresh task for the embedded Java static assets.
- Evidence: The active ledger now includes `UI-BUNDLE-001` with intake notes limiting scope to temporary-artifact cleanup, rebuilding `manager` and `benchmark` frontends, and syncing their `dist` outputs into Java `resources/static`.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger edit as ad hoc bookkeeping instead of the required intake for this bundle-refresh pass.
- Suggested next step: Keep the task ledger, archive, and eventual task commit aligned while `UI-BUNDLE-001` is implemented and verified.
- Human decision: pending

### INBOX-20260403-011
- Area: Harness audit trail
- Related task(s): `MGR-QA-001`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-03 to archive the completion of `MGR-QA-001` after the full manager Redis cache review, validation, and browser QA pass.
- Evidence: The active ledger row and section for `MGR-QA-001` were removed from `tasks.md`, its full progress log and done signal were archived under `tasks-done.md`, and the archived evidence records the passed `analyze+manager` reactor validation, passed manager frontend validation, and Playwright QA evidence paths under `/tmp/engine-cache-qa/`.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger edits as ad hoc bookkeeping rather than the required closeout workflow for the cache review/signoff task.
- Suggested next step: Keep the archived task entry, QA evidence references, and task commit aligned; unrelated dirty artifacts such as coverage output should remain outside this task commit.
- Human decision: pending

### INBOX-20260403-010
- Area: Harness audit trail
- Related task(s): `MGR-QA-001`
- Summary: `tasks.md` was updated on 2026-04-03 to intake a human-requested full review and signoff task for the manager Redis cache feature.
- Evidence: The active ledger now includes `MGR-QA-001` with intake notes covering manager cache UI/API review, Playwright-based QA evidence capture, manager-scoped validation, and task-scoped closeout.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger edit as ad hoc bookkeeping instead of the required intake for this review-and-signoff pass.
- Suggested next step: Keep the task ledger, archive, QA evidence, and eventual task commit aligned while `MGR-QA-001` is implemented and verified.
- Human decision: pending

### INBOX-20260403-009
- Area: Harness audit trail
- Related task(s): `MGR-TEST-002`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-03 to archive the completion of `MGR-TEST-002` after committing the leftover manager test/config follow-up from the `ENGINE` routing work.
- Evidence: The active ledger row and section for `MGR-TEST-002` were removed from `tasks.md`, its progress log and done signal were archived under `tasks-done.md`, and the archived evidence records that focused validation is still blocked by the unrelated `JdbcSqlAdvisorService` constructor issue.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger edits as ad hoc bookkeeping rather than the required closeout workflow for this narrow follow-up task.
- Suggested next step: Keep the archived task entry and task commit aligned, and leave the separate manager bean-instantiation defect outside this task's staged scope.
- Human decision: pending

### INBOX-20260403-008
- Area: Harness audit trail
- Related task(s): `MGR-TEST-002`
- Summary: `tasks.md` was updated on 2026-04-03 to intake a follow-up manager test/config cleanup task for the leftover files that did not land in `QUERY-ENGINE-001`.
- Evidence: The active ledger now includes `MGR-TEST-002` with intake notes limiting scope to the remaining manager Redis dependency/profile wiring and the three migration/bootstrap tests that still needed alignment.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger edit as ad hoc bookkeeping instead of the required intake for this narrow follow-up commit.
- Suggested next step: Keep the task ledger, archive, and eventual task commit aligned while `MGR-TEST-002` is implemented and verified.
- Human decision: pending

### INBOX-20260403-005
- Area: Harness audit trail
- Related task(s): `MGR-CACHE-001`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-03 to archive the completion of `MGR-CACHE-001` after finishing the manager Redis cache CRUD work.
- Evidence: The active ledger row and section for `MGR-CACHE-001` were removed from `tasks.md`, the full progress log and done signal were archived under `tasks-done.md`, and the archived evidence records both the focused cache regressions and the unrelated stock-manager-suite `JdbcSqlAdvisorService` blocker note.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger edits as ad hoc bookkeeping rather than the required task closeout workflow.
- Suggested next step: Keep the archived task entry and its task commit aligned, and leave the unrelated manager-suite constructor issue outside this cache task's staged scope.
- Human decision: pending

### INBOX-20260403-007
- Area: Harness audit trail
- Related task(s): `QUERY-ENGINE-001`
- Summary: `tasks-done.md` was updated on 2026-04-03 to archive the completion of the human-requested routing-contract change that switches runtime routing to `ENGINE` and adds Redis-backed report overrides keyed by `YH_RPTID`.
- Evidence: The archive now includes `QUERY-ENGINE-001` with the implementation, review, verification, and doc-garden evidence for the `ENGINE` routing migration plus the Redis override behavior, and the corresponding task commit stages only the routing/parser/runtime/test/doc files touched by this change.
- Impact/Risk: Without an explicit audit note, later recovery could misread the archive edit as ad hoc bookkeeping rather than the intended closeout record for this cross-module routing change.
- Suggested next step: Keep the archived task entry and its focused task commit aligned; leave unrelated pre-existing frontend/config dirty worktree changes outside this commit.
- Human decision: pending

### INBOX-20260403-003
- Area: Harness audit trail
- Related task(s): `QUERY-TRINO-003`
- Summary: `tasks.md` was updated on 2026-04-03 to intake a human-requested Trino JDBC compatibility task covering a new `query` port plus benchmark regression coverage.
- Evidence: The active ledger now includes `QUERY-TRINO-003` with intake notes describing the new `8093` Trino JDBC surface, the minimum `/v1/statement` compatibility scope, and the required benchmark verification.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger edit as ad hoc bookkeeping instead of the required task intake for this cross-module implementation.
- Suggested next step: Keep the task ledger, archive, and eventual task commit aligned while `QUERY-TRINO-003` is implemented and verified.
- Human decision: pending

### INBOX-20260403-006
- Area: Harness audit trail
- Related task(s): `QUERY-TRINO-003`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-03 to archive the completion of `QUERY-TRINO-003` after adding the new Trino JDBC compatibility port and benchmark regression coverage.
- Evidence: The active ledger row/section for `QUERY-TRINO-003` were removed from `tasks.md`, its full progress log and done signal were archived under `tasks-done.md`, and the archived evidence records the `8093` Trino `/v1/statement` surface plus the completed query/benchmark validation runs.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger edits as ad hoc bookkeeping rather than the required task closeout workflow.
- Suggested next step: Keep the archived task entry and the eventual task commit aligned; the active ledger should continue to track only `CONFIG-REVIEW-001`.
- Human decision: pending

### INBOX-20260403-007
- Area: Benchmark/JDK tooling compatibility
- Related task(s): `QUERY-TRINO-003`
- Summary: The new benchmark Trino JDBC regression passes, but running it on the current JDK 25 environment triggers noisy JaCoCo instrumentation warnings from Trino-driven JDK security/provider classes before the test suite exits successfully.
- Evidence: `mvn -q -f benchmark/pom.xml test` on 2026-04-03 completed with exit code `0`, but stderr included repeated `IllegalClassFormatException` / `Unsupported class file major version 69` warnings while JaCoCo attempted to instrument `org/ietf/jgss/*` and `org/jcp/xml/dsig/internal/dom/*` classes loaded indirectly by `io.trino.jdbc.TrinoDriver`.
- Impact/Risk: The benchmark suite is green today, but the warning noise can obscure real failures and signals a JDK 25 versus JaCoCo instrumentation gap that may become a hard failure in stricter environments or future Trino/JDK upgrades.
- Suggested next step: Decide whether to upgrade or reconfigure JaCoCo for JDK 25, or explicitly exclude the affected JDK security/provider classes from instrumentation in benchmark test runs.
- Human decision: pending

### INBOX-20260403-004
- Area: Harness audit trail
- Related task(s): `MGR-CACHE-001`
- Summary: `tasks.md` was updated on 2026-04-03 to intake a human-requested manager Redis cache CRUD task covering the missing cache detail, create, and update flows.
- Evidence: The active ledger now includes `MGR-CACHE-001` with intake notes describing the current delete-only manager cache surface and the planned `/cache` page plus `/api/v1/cache/keys*` expansion.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger edit as ad hoc bookkeeping instead of the required task intake for this manager implementation.
- Suggested next step: Keep the task ledger, archive, and eventual task commit aligned while `MGR-CACHE-001` is implemented and verified.
- Human decision: pending

### INBOX-20260403-001
- Area: Harness audit trail
- Related task(s): `BENCH-BUG-002`
- Summary: `tasks.md` was updated on 2026-04-03 to intake a human-requested benchmark recovery task covering the renewed local Kylin outage plus the leftover active-run state in benchmark.
- Evidence: The active ledger now includes `BENCH-BUG-002` with intake notes capturing the unhealthy `kylin-standalone` container, failing benchmark preflight probe, and surviving `benchmark_run` row `#8` still marked `RUNNING` after service restart.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger edit as ad hoc bookkeeping instead of the required task intake for this bugfix.
- Suggested next step: Keep the task ledger, archive, and eventual task commit aligned while `BENCH-BUG-002` is implemented and verified.
- Human decision: pending

### INBOX-20260403-002
- Area: Harness audit trail
- Related task(s): `BENCH-BUG-002`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-03 to archive the completion of `BENCH-BUG-002` after restoring local Kylin readiness and closing the lingering benchmark active-run defect.
- Evidence: The active ledger row/section for `BENCH-BUG-002` were removed from `tasks.md`, its full progress log and done signal were archived under `tasks-done.md`, and the archived evidence records the recovered Kylin health plus `benchmark_run.id=8` being moved from `RUNNING` to `FAILED`.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger edits as ad hoc bookkeeping rather than the required task closeout workflow.
- Suggested next step: Keep the archived task entry and the task commit aligned; the remaining active ledger should continue to track only `CONFIG-REVIEW-001`.
- Human decision: pending

### INBOX-20260331-001
- Area: Harness governance and audit trail
- Related task(s): `HARNESS-GOV-001`, `ARCH-010`
- Summary: Task completion and escalation hygiene drifted from the documented contract: some completed tasks stayed under `## Todo`, multiple `Done` rows have no matching task-id commit subject in Git history, blocked tasks were not recording `Escalation: ...`, and legacy harness artifacts (`.agent/history/`, prompt docs, stale `agent_loop.pyc` files) could be mistaken for authoritative state even though the in-repo loop source is absent.
- Evidence: `tasks.md` previously carried completed items in `## Todo`; `git log --all --format=%s` currently misses task-id commit subjects for `BENCH-REVIEW-001`, `MGR-DASH-002`, `MGR-UX-003`, `BENCH-DATA-001`, `BENCH-ACTIVE-001`, `BENCH-RUNS-API-001`, `BENCH-UX-005`, `BENCH-TEST-001`, `ARCH-014`, `ARCH-013`, `ARCH-012`, `ARCH-011`, `MGR-TEST-001`, `MGR-BUG-001`, `MGR-REVIEW-001`, and `QUERY-REVIEW-001`; `.agent/history/` stops at `20260330.jsonl` while `tasks.md` contains `2026-03-31` progress entries; the repo no longer contains `scripts/agent_loop.py` even though older artifacts reference it.
- Impact/Risk: Humans cannot reliably infer whether a task is actually complete, committed, or merely verified; blocked-task escalation is ambiguous; legacy diagnostics can be mistaken for the source of truth during recovery.
- Suggested next step: Keep `tasks.md`, `tasks-done.md`, and Git history as the canonical audit trail, grandfather the known legacy missing-commit IDs in the audit script, and enforce the stricter rules for all new completions.
- Human decision: pending


### INBOX-20260402-002
- Area: Harness audit trail
- Related task(s): `BENCH-TEST-002`
- Summary: `tasks.md` was updated on 2026-04-02 to intake a human-requested benchmark test-fix task for `PreflightControllerTest`, and this inbox note records that core harness-document modification per the repo contract.
- Evidence: Active ledger row and task section for `BENCH-TEST-002` were added to `tasks.md` before implementation began.
- Impact/Risk: Without an explicit audit note, core harness-document changes are harder to track during recovery or review.
- Suggested next step: No implementation decision needed beyond keeping the task ledger and archive aligned with the eventual task commit.
- Human decision: pending

### INBOX-20260402-003
- Area: Test config governance
- Related task(s): `BENCH-TEST-002`, `TEST-CONFIG-001`
- Summary: `BENCH-TEST-002` exported a new testing rule to keep datasource/probe fixtures in test-owned config, and a follow-up review task was added so the rest of the codebase can be checked for the same pattern.
- Evidence: `docs/operations/best-practices.md` now includes the test-fixture rule, and `tasks.md` now includes `TEST-CONFIG-001` to audit remaining inline connection fixtures.
- Impact/Risk: Similar hard-coded test connection details may still exist elsewhere, which makes fixture updates harder to review and maintain.
- Suggested next step: Schedule `TEST-CONFIG-001` and convert remaining inline test connection fixtures to test-owned config where appropriate.
- Human decision: pending

### INBOX-20260402-004
- Area: Harness audit trail
- Related task(s): `QUERY-BUG-001`
- Summary: `tasks.md` was updated on 2026-04-02 to intake a human-requested query-boundary hardening task so the work can follow the repo's single-task audit trail and completion workflow.
- Evidence: Active ledger row and task section for `QUERY-BUG-001` were added to `tasks.md` before implementation began.
- Impact/Risk: Without an explicit audit note, later recovery or review could miss why the active ledger changed during the session.
- Suggested next step: Keep the task ledger, archive, and eventual commit aligned when `QUERY-BUG-001` closes.
- Human decision: pending

### INBOX-20260402-005
- Area: Query request validation governance
- Related task(s): `QUERY-BUG-001`, `QUERY-REVIEW-002`
- Summary: `QUERY-BUG-001` exported a new compatibility-validation rule to `docs/operations/best-practices.md`, and a follow-up review task was added so the rest of the query compatibility surface can be checked for the same early-rejection pattern.
- Evidence: `docs/operations/best-practices.md` now requires compatibility shims to validate blank/unsupported requests before routing/cache/datasource work, and `tasks.md` now includes `QUERY-REVIEW-002` to audit the remaining query entrypoints against that rule.
- Impact/Risk: Similar delayed validation may still exist in other compatibility/shim paths, which can waste downstream work or blur the intended error contract.
- Suggested next step: Schedule `QUERY-REVIEW-002` and apply the same validation pattern anywhere else the query shim still lets unsupported request envelopes travel too far.
- Human decision: pending

### INBOX-20260402-006
- Area: Harness audit trail
- Related task(s): `QUERY-BUG-001`, `BENCH-UX-007`
- Summary: Running `scripts/task_audit.py --check` during `QUERY-BUG-001` closeout revealed unrelated governance drift: `BENCH-UX-007` still has a `tasks-done.md` entry with no matching task-id commit subject in Git history.
- Evidence: `python3 scripts/task_audit.py --check` on 2026-04-02 reported `BENCH-UX-007: tasks-done.md entry has no git commit subject containing the task id`; the same run also flagged the in-progress `QUERY-BUG-001` row before its commit existed.
- Impact/Risk: The repo-level task audit cannot return green after unrelated query work until the older `BENCH-UX-007` audit trail is reconciled or explicitly grandfathered.
- Suggested next step: Decide whether `BENCH-UX-007` should be grandfathered into the audit script like the other legacy gaps in `INBOX-20260331-001` or whether a recoverable task-id commit link still exists and should be restored.
- Human decision: pending

### INBOX-20260402-007
- Area: Harness audit trail
- Related task(s): `QUERY-REVIEW-002`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-02 to track and archive the completion of `QUERY-REVIEW-002` in the canonical task ledger.
- Evidence: The active ledger row for `QUERY-REVIEW-002` was removed from `tasks.md`, its full progress log was archived under `tasks-done.md`, and the closeout will be included in the task's single commit.
- Impact/Risk: Without an explicit inbox note, later recovery or review could misread the harness-document edits as ad hoc bookkeeping instead of the required task workflow.
- Suggested next step: Keep the ledger archive and task commit aligned for `QUERY-REVIEW-002` closeout.
- Human decision: pending

### INBOX-20260402-008
- Area: Harness audit trail
- Related task(s): `ARCH-015`
- Summary: `tasks.md` was updated on 2026-04-02 to intake a human-requested database-initialization workflow change so the work can follow the repo's required task lifecycle.
- Evidence: Active ledger row and task section for `ARCH-015` were added to `tasks.md` before implementation began.
- Impact/Risk: Without an explicit audit note, later recovery or review could miss why the active ledger changed during the session.
- Suggested next step: Keep the task ledger, archive, and eventual commit aligned when `ARCH-015` closes.
- Human decision: pending

### INBOX-20260402-009
- Area: Harness audit trail
- Related task(s): `QUERY-TRINO-001`
- Summary: `tasks-done.md` was updated on 2026-04-02 to archive the completion of `QUERY-TRINO-001`, while preserving a concurrently introduced `tasks.md` intake for unrelated task `ARCH-015`.
- Evidence: The Trino task now has a full archived progress log and done signal in `tasks-done.md`; `tasks.md` was left aligned with the separately in-progress `ARCH-015` entry instead of being overwritten during closeout.
- Impact/Risk: Without an explicit note, later recovery could misread the ledger state and assume the missing active-row history for `QUERY-TRINO-001` was accidental rather than the result of concurrent harness edits being preserved.
- Suggested next step: Keep the archived Trino entry and its task commit aligned, and let the active ledger continue tracking `ARCH-015` independently.
- Human decision: pending

### INBOX-20260402-010
- Area: Harness audit trail
- Related task(s): `TEST-CONFIG-001`
- Summary: `tasks.md` was updated on 2026-04-02 to start the existing project-wide test-fixture audit task instead of creating a duplicate task for the same best-practice follow-up.
- Evidence: `TEST-CONFIG-001` is now marked `in_progress`, and its progress log records that the remaining project-wide best-practice drift is inline test connection fixtures in benchmark/query tests.
- Impact/Risk: Without an explicit audit note, later recovery or review could misread the ledger change as ad hoc bookkeeping instead of the required task workflow.
- Suggested next step: Keep the task ledger, archive, and eventual commit aligned when `TEST-CONFIG-001` closes.
- Human decision: pending


### INBOX-20260402-012
- Area: Harness audit trail
- Related task(s): `ARCH-015`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-02 to archive the completion of `ARCH-015` in the canonical task ledger.
- Evidence: The active ledger row for `ARCH-015` was removed from `tasks.md`, its full progress log was archived under `tasks-done.md`, and the closeout is included in the task's single commit.
- Impact/Risk: Without an explicit audit note, later recovery or review could misread the harness-document edits as ad hoc bookkeeping instead of the required task workflow.
- Suggested next step: Keep the ledger archive and task commit aligned for `ARCH-015` closeout.
- Human decision: pending

### INBOX-20260402-013
- Area: Harness audit trail
- Related task(s): `MGR-DB-001`
- Summary: `tasks-done.md` was updated on 2026-04-02 to archive the completion of `MGR-DB-001` while preserving the concurrently active `TEST-CONFIG-001` entry in `tasks.md`.
- Evidence: The manager table-prefix task now has a full archived progress log and done signal in `tasks-done.md`; `tasks.md` was left aligned with the separately in-progress `TEST-CONFIG-001` work instead of being overwritten during closeout.
- Impact/Risk: Without an explicit note, later recovery could misread the ledger state and assume the missing active-row history for `MGR-DB-001` was accidental rather than the result of concurrent harness edits being preserved.
- Suggested next step: Keep the archived manager-prefix entry and its task commit aligned, and let the active ledger continue tracking `TEST-CONFIG-001` independently.
- Human decision: pending

### INBOX-20260402-014
- Area: Harness audit trail
- Related task(s): `TEST-CONFIG-001`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-02 to archive the completion of `TEST-CONFIG-001` in the canonical task ledger.
- Evidence: The active ledger row for `TEST-CONFIG-001` was removed from `tasks.md`, its full progress log was archived under `tasks-done.md`, and the closeout will be included in the task's single commit.
- Impact/Risk: Without an explicit inbox note, later recovery or review could misread the harness-document edits as ad hoc bookkeeping instead of the required task workflow.
- Suggested next step: Keep the ledger archive and task commit aligned for `TEST-CONFIG-001` closeout.
- Human decision: pending

### INBOX-20260402-015
- Area: Harness audit trail
- Related task(s): `CONFIG-PROFILE-001`
- Summary: `tasks.md` was updated on 2026-04-02 to intake a human-requested repo-wide configuration-governance task so the profile unification work can follow the required task lifecycle.
- Evidence: Active ledger row and task section for `CONFIG-PROFILE-001` were added to `tasks.md` before implementation began.
- Impact/Risk: Without an explicit audit note, later recovery or review could miss why the active ledger changed during this session.
- Human decision: pending

### INBOX-20260402-016
- Area: Harness audit trail (Git Rules)
- Related task(s): None
- Summary: The core `AGENTS.md` contract was updated to strictly prohibit `git add .` or global commit flags.
- Evidence: Appended `ONLY stage and commit the specific files modified during the current task...` rule under the `Git Contract` section.
- Impact/Risk: Prevents agents from accidentally absorbing concurrently active changes from other tasks into a single commit.
- Suggested next step: Acknowledge the rule update.
- Human decision: pending

### INBOX-20260402-017
- Area: Benchmark test-profile initialization drift
- Related task(s): `CONFIG-PROFILE-001`
- Summary: The new shared `benchmark` `application-test.yml` works for automated tests and for MySQL-backed test-environment deployment overrides, but its raw H2 default cannot replay the full benchmark Flyway chain because migration `V17__dedupe_seeded_benchmark_datasources.sql` uses MySQL-specific multi-table `UPDATE ... JOIN` syntax.
- Evidence: `bash scripts/init-db.sh test manager benchmark` on 2026-04-02 succeeded for `manager` but failed for `benchmark` on H2 with `Syntax error in SQL statement ... expected "SET"` inside `V17__dedupe_seeded_benchmark_datasources.sql`; the same path passed when the benchmark test-profile datasource was overridden to local MySQL via `BENCHMARK_TEST_DB_*` environment variables.
- Impact/Risk: Benchmark's unified `test` profile currently needs DB env overrides for deployment-like DB initialization, so the out-of-the-box H2 defaults are only suitable for automated benchmark tests that keep Flyway disabled.
- Suggested next step: Decide whether benchmark `test` profile should keep H2-first defaults for automated tests or move to MySQL-first defaults with a separate shared test bootstrap override for local automated runs.
- Human decision: pending

### INBOX-20260402-018
- Area: Config governance
- Related task(s): `CONFIG-PROFILE-001`, `CONFIG-REVIEW-001`
- Summary: `CONFIG-PROFILE-001` exported a new English best-practice rule that all environment-specific configuration must live in each module's `application-dev.yml`, `application-test.yml`, and `application-pro.yml`, and a follow-up review task was added to audit the rest of the repo against that rule.
- Evidence: `docs/operations/best-practices.md` now replaces the two older config-related rules with the new profile-governance rule, and `tasks.md` now includes `CONFIG-REVIEW-001` for retroactive cleanup.
- Impact/Risk: Without the follow-up audit, older files may still carry environment-specific drift outside the three profile YAMLs even though the new standard is now documented.
- Suggested next step: Schedule `CONFIG-REVIEW-001` and remove any remaining non-compliant environment config from the repo.
- Human decision: pending

### INBOX-20260402-019
- Area: Harness audit trail
- Related task(s): `CONFIG-PROFILE-001`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-02 to archive the completion of `CONFIG-PROFILE-001`, while preserving the follow-up audit task `CONFIG-REVIEW-001` in the active ledger.
- Evidence: The active ledger row and section for `CONFIG-PROFILE-001` were removed from `tasks.md`, its full progress log and done signal were archived under `tasks-done.md`, and the new best-practice replacement plus review-task creation are reflected in the same closeout.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger change as ad hoc bookkeeping instead of the required task finalization workflow.
- Suggested next step: Keep the archived `CONFIG-PROFILE-001` entry and its commit aligned, and let `CONFIG-REVIEW-001` remain the active follow-up.
- Human decision: pending

### INBOX-20260402-020
- Area: Harness audit trail
- Related task(s): `QUERY-ARCH-003`
- Summary: `tasks.md` was updated on 2026-04-02 to intake a human-requested shared `analyze` extraction task so the cross-module build, query, and manager changes can follow the repo's required task lifecycle.
- Evidence: Active ledger row and task section for `QUERY-ARCH-003` were added to `tasks.md` before implementation began.
- Impact/Risk: Without an explicit audit note, later recovery or review could miss why the active ledger changed during this session.
- Suggested next step: Keep the task ledger, archive, and eventual commit aligned when `QUERY-ARCH-003` closes.
- Human decision: pending

### INBOX-20260402-021
- Area: Harness validation-command drift
- Related task(s): `QUERY-ARCH-003`
- Summary: The shared `analyze` module means the module-local validation commands in `.agent/config.json` for `query` and `manager` are now stale, because standalone `mvn -q -f query/pom.xml test` and `mvn -q -f manager/pom.xml test` no longer resolve the sibling artifact without a reactor build or a prior local install.
- Evidence: During `QUERY-ARCH-003` verification on 2026-04-02, `mvn -q -pl query -am test` and `mvn -q -pl manager -am ... test` passed, while standalone `mvn -q -f query/pom.xml test` and `mvn -q -f manager/pom.xml ... test` failed dependency resolution for `com.smartbi:analyze:1.0.0-SNAPSHOT`.
- Impact/Risk: Harness automation and humans following the existing validation matrix can get false-negative failures even when the code is healthy, because the documented and automated commands no longer match the repo's build topology.
- Suggested next step: Open a harness-policy task to update `.agent/config.json`, the validation matrix, and any related automation to use reactor builds for modules that now depend on `analyze`.
- Human decision: pending

### INBOX-20260402-022
- Area: Harness audit trail
- Related task(s): `QUERY-ARCH-003`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-02 to archive the completion of `QUERY-ARCH-003`, while preserving the separately active `CONFIG-REVIEW-001` task in the current ledger.
- Evidence: The active ledger row and section for `QUERY-ARCH-003` were removed from `tasks.md`, its full progress log and done signal were archived under `tasks-done.md`, and the closeout is included in the task's single commit.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger change as ad hoc bookkeeping instead of the required task finalization workflow.
- Suggested next step: Keep the archived `QUERY-ARCH-003` entry and its task commit aligned, and let `CONFIG-REVIEW-001` remain the active follow-up task in `tasks.md`.
- Human decision: pending

### INBOX-20260402-023
- Area: Harness audit trail
- Related task(s): `QUERY-CACHE-001`
- Summary: `tasks.md` was updated on 2026-04-02 to intake a human-requested query cache-write task so the Redis async change can follow the repo's required task lifecycle.
- Evidence: Active ledger row and task section for `QUERY-CACHE-001` were added to `tasks.md` before implementation began.
- Impact/Risk: Without an explicit audit note, later recovery or review could miss why the active ledger changed during this session.
- Suggested next step: Keep the task ledger, archive, and eventual commit aligned when `QUERY-CACHE-001` closes.
- Human decision: pending

### INBOX-20260402-024
- Area: Harness audit trail
- Related task(s): `QUERY-CACHE-001`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-02 to archive the completion of `QUERY-CACHE-001`, while preserving the separately active `CONFIG-REVIEW-001` task in the current ledger.
- Evidence: The active ledger row and section for `QUERY-CACHE-001` were removed from `tasks.md`, its full progress log and done signal were archived under `tasks-done.md`, and the closeout will be included in the task's single commit.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger change as ad hoc bookkeeping instead of the required task finalization workflow.
- Suggested next step: Keep the archived `QUERY-CACHE-001` entry and its task commit aligned, and let `CONFIG-REVIEW-001` remain the active follow-up task in `tasks.md`.
- Human decision: pending

### INBOX-20260402-025
- Area: Harness audit trail
- Related task(s): `BENCH-UX-010`
- Summary: `tasks.md` was updated on 2026-04-02 to intake a human-requested benchmark SQL Lib workflow task so the schema, API, UI, and ledger changes can follow the repo's required task lifecycle.
- Evidence: Active ledger row and task section for `BENCH-UX-010` were added to `tasks.md` before implementation began.
- Impact/Risk: Without an explicit audit note, later recovery or review could miss why the active ledger changed during this session.
- Suggested next step: Keep the task ledger, archive, and eventual commit aligned when `BENCH-UX-010` closes.
- Human decision: pending

### INBOX-20260402-026
- Area: Harness audit trail
- Related task(s): `BENCH-UX-010`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-02 to archive the completion of `BENCH-UX-010`, while preserving the separately active `CONFIG-REVIEW-001` task in the current ledger.
- Evidence: The active ledger row and section for `BENCH-UX-010` were removed from `tasks.md`, its full progress log and done signal were archived under `tasks-done.md`, and the closeout is included in the task's single commit.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger change as ad hoc bookkeeping instead of the required task finalization workflow.
- Suggested next step: Keep the archived `BENCH-UX-010` entry and its task commit aligned, and let `CONFIG-REVIEW-001` remain the active follow-up task in `tasks.md`.
- Human decision: pending

### INBOX-20260402-027
- Area: Harness audit trail
- Related task(s): `QUERY-BUG-002`
- Summary: `tasks.md` was updated on 2026-04-02 to intake a human-requested query bugfix task so the leading-comment query detection fix can follow the repo's required task lifecycle.
- Evidence: Active ledger row and task section for `QUERY-BUG-002` were added to `tasks.md` before implementation began.
- Impact/Risk: Without an explicit audit note, later recovery or review could miss why the active ledger changed during this session.
- Suggested next step: Keep the task ledger, archive, and eventual commit aligned when `QUERY-BUG-002` closes.
- Human decision: pending

### INBOX-20260402-028
- Area: Harness audit trail
- Related task(s): `QUERY-BUG-002`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-02 to archive the completion of `QUERY-BUG-002`, while preserving the separately active `CONFIG-REVIEW-001` task in the current ledger.
- Evidence: The active ledger row and section for `QUERY-BUG-002` were removed from `tasks.md`, its full progress log and done signal were archived under `tasks-done.md`, and the closeout will be included in the task's single commit.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger change as ad hoc bookkeeping instead of the required task finalization workflow.
- Suggested next step: Keep the archived `QUERY-BUG-002` entry and its task commit aligned, and let `CONFIG-REVIEW-001` remain the active follow-up task in `tasks.md`.
- Human decision: pending

### INBOX-20260403-001
- Area: Harness audit trail
- Related task(s): `BENCH-BUG-001`
- Summary: `tasks.md` was updated on 2026-04-03 to intake a human-requested benchmark runtime bugfix task so the dispatch-path repair can follow the repo's required task lifecycle.
- Evidence: Active ledger row and task section for `BENCH-BUG-001` were added to `tasks.md` before implementation began.
- Impact/Risk: Without an explicit audit note, later recovery or review could miss why the active ledger changed during this session.
- Suggested next step: Keep the task ledger, archive, and eventual commit aligned when `BENCH-BUG-001` closes.
- Human decision: pending

### INBOX-20260403-002
- Area: Harness audit trail
- Related task(s): `BENCH-BUG-001`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-03 to archive the completion of `BENCH-BUG-001`, while preserving the separately active `CONFIG-REVIEW-001` task in the current ledger.
- Evidence: The active ledger row and section for `BENCH-BUG-001` were removed from `tasks.md`, its full progress log and done signal were archived under `tasks-done.md`, and the closeout will be included in the task's single commit.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger change as ad hoc bookkeeping instead of the required task finalization workflow.
- Suggested next step: Keep the archived `BENCH-BUG-001` entry and its task commit aligned, and let `CONFIG-REVIEW-001` remain the active follow-up task in `tasks.md`.
- Human decision: pending

### INBOX-20260402-031
- Area: Harness audit trail
- Related task(s): `QUERY-TRINO-002`
- Summary: `tasks.md` was updated on 2026-04-02 to intake a human-requested local Trino runtime and E2E validation task so the compose, config, and test changes can follow the repo's required task lifecycle.
- Evidence: Active ledger row and task section for `QUERY-TRINO-002` were added to `tasks.md` before implementation began.
- Impact/Risk: Without an explicit audit note, later recovery or review could miss why the active ledger changed during this session.
- Suggested next step: Keep the task ledger, archive, and eventual commit aligned when `QUERY-TRINO-002` closes.
- Human decision: pending

### INBOX-20260402-029
- Area: Harness audit trail
- Related task(s): `QUERY-BUG-003`
- Summary: `tasks.md` was updated on 2026-04-02 to intake a human-requested parser bugfix task so the clean-SQL comment-normalization fix can follow the repo's required task lifecycle.
- Evidence: Active ledger row and task section for `QUERY-BUG-003` were added to `tasks.md` before implementation began.
- Impact/Risk: Without an explicit audit note, later recovery or review could miss why the active ledger changed during this session.
- Suggested next step: Keep the task ledger, archive, and eventual commit aligned when `QUERY-BUG-003` closes.
- Human decision: pending

### INBOX-20260402-030
- Area: Harness audit trail
- Related task(s): `QUERY-BUG-003`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-02 to archive the completion of `QUERY-BUG-003`, while preserving the separately active `CONFIG-REVIEW-001` task in the current ledger.
- Evidence: The active ledger row and section for `QUERY-BUG-003` were removed from `tasks.md`, its full progress log and done signal were archived under `tasks-done.md`, and the closeout will be included in the task's single commit.
- Impact/Risk: Without an explicit audit note, later recovery could misread the ledger change as ad hoc bookkeeping instead of the required task finalization workflow.
- Suggested next step: Keep the archived `QUERY-BUG-003` entry and its task commit aligned, and let `CONFIG-REVIEW-001` remain the active follow-up task in `tasks.md`.
- Human decision: pending

### INBOX-20260402-032
- Area: Harness audit trail
- Related task(s): `QUERY-TRINO-002`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-02 to archive the completion of `QUERY-TRINO-002`, which added local Trino service support and verified it via E2E tests while resolving repo-wide table prefixing drift in both the `query` module and `tests` module.
- Evidence: The active row for `QUERY-TRINO-002` was removed from `tasks.md`, its full progress log and done signal were archived under `tasks-done.md`, and the fix for `manager_sql_execution_record` drift was verified with a green `TrinoRoutingE2ETest` run.
- Impact/Risk: Without an explicit audit note, later recovery or review could miss the dual-purpose nature of this task's edits across the build surface and the metadata store.
- Suggested next step: Keep the archived Trino entry and its task commit aligned.
### INBOX-20260402-033
- Area: Harness audit trail
- Related task(s): `MIGRATION-001`
- Summary: `tasks.md` was updated on 2026-04-02 to intake a human-requested database migration task to upgrade `TEXT` columns to `MEDIUMTEXT` across all modules to support long SQL queries.
- Evidence: Active ledger row and task section for `MIGRATION-001` were added to `tasks.md` before implementation began.
- Impact/Risk: Without an explicit audit note, later recovery or review could miss why the active ledger changed during this session.
- Suggested next step: Keep the task ledger and eventual commit aligned when `MIGRATION-001` closes.
- Human decision: pending

### INBOX-20260402-034
- Area: Harness audit trail
- Related task(s): `MIGRATION-001`
- Summary: `tasks.md` and `tasks-done.md` were updated on 2026-04-02 to archive the completion of `MIGRATION-001`, which upgraded 10 JPA domain classes and added matched Flyway migrations for large SQL support.
- Evidence: The active row for `MIGRATION-001` was removed from `tasks.md`, its full progress log and done signal were archived under `tasks-done.md`, and a new best practice for `MEDIUMTEXT` was exported.
- Impact/Risk: Without an explicit audit note, later recovery or review could miss the scope of this migration across the benchmark, query, and manager modules.
- Suggested next step: Keep the archived migration entry and its task commit aligned.
- Human decision: pending


## Template

```md
### INBOX-YYYYMMDD-001
- Area:
- Related task(s):
- Summary:
- Evidence:
- Impact/Risk:
- Suggested next step:
- Human decision: pending
```
