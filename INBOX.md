# Inbox

This is the canonical repo-root inbox for agent-found issues and suggestions that still require human review, judgment, approval, or prioritization before they become implementation work.

## Rules

- Record items here only when they are not already tracked in `tasks.md`.
- Keep entries evidence-based and concise.
- Use this inbox for defects, harness/process concerns, and improvement suggestions that still need human judgment.
- Task-specific failures that are already fully captured in a task's progress log do not need a duplicate inbox entry.
- Systemic harness, tooling, process, or documentation issues discovered while working another task belong here only when they still need human judgment, approval, prioritization, or task shaping.
- **Do not** log generalized coding practices, codebase styling rules, or post-mortem fixes here. These belong in task progress logs and `docs/operations/best-practices.md`.
- Do not add audit-only entries for deterministic task intake, task closeout, harness-document edits, or best-practice bookkeeping when the human has already requested the work and no further decision is needed.
- When a task blocks at max attempts, its latest rejected or revalidation note in `tasks.md` must include `Escalation: none` or `Escalation: INBOX-...` so humans can tell whether the issue stayed task-local or was escalated here.
- Do not convert inbox entries directly into implementation tasks without explicit human confirmation.
- For tooling or infrastructure changes that still need approval, prefer logging the issue here before changing the harness.
- Legacy harness audit-trail entries below are historical records; do not keep creating new entries in that style.

### INBOX-20260404-003
- Area: Benchmark test validation
- Related task(s): `TEST-CONFIG-002`
- Summary: Task-local benchmark validation is currently blocked by an unrelated compile failure in `benchmark/src/test/java/com/smartbi/benchmark/run/BenchmarkAsyncRunnerTrinoJdbcIntegrationTest.java`.
- Evidence: `bash scripts/with-java8.sh mvn -q -f benchmark/pom.xml test` fails during `testCompile` with five `incompatible types: java.nio.charset.Charset cannot be converted to java.lang.String` errors in `BenchmarkAsyncRunnerTrinoJdbcIntegrationTest`.
- Impact/Risk: The benchmark module cannot complete module-level regression validation for unrelated work until that test source is repaired or excluded, which makes future task verification noisier and easier to misread.
- Suggested next step: Queue a focused benchmark test-repair task to fix the incorrect method calls in `BenchmarkAsyncRunnerTrinoJdbcIntegrationTest` so `benchmark` regains a usable baseline.
- Human decision: pending

### INBOX-20260403-007
- Area: Benchmark/JDK tooling compatibility
- Related task(s): `QUERY-TRINO-003`
- Summary: The new benchmark Trino JDBC regression passes, but running it on the current JDK 25 environment triggers noisy JaCoCo instrumentation warnings from Trino-driven JDK security/provider classes before the test suite exits successfully.
- Evidence: `mvn -q -f benchmark/pom.xml test` on 2026-04-03 completed with exit code `0`, but stderr included repeated `IllegalClassFormatException` / `Unsupported class file major version 69` warnings while JaCoCo attempted to instrument `org/ietf/jgss/*` and `org/jcp/xml/dsig/internal/dom/*` classes loaded indirectly by `io.trino.jdbc.TrinoDriver`.
- Impact/Risk: The benchmark suite is green today, but the warning noise can obscure real failures and signals a JDK 25 versus JaCoCo instrumentation gap that may become a hard failure in stricter environments or future Trino/JDK upgrades.
- Suggested next step: Decide whether to upgrade or reconfigure JaCoCo for JDK 25, or explicitly exclude the affected JDK security/provider classes from instrumentation in benchmark test runs.
- Human decision: pending

### INBOX-20260331-001
- Area: Harness governance and audit trail
- Related task(s): `HARNESS-GOV-001`, `ARCH-010`
- Summary: Task completion and escalation hygiene drifted from the documented contract: some completed tasks stayed under `## Todo`, multiple `Done` rows have no matching task-id commit subject in Git history, blocked tasks were not recording `Escalation: ...`, and legacy harness artifacts (`.agent/history/`, prompt docs, stale `agent_loop.pyc` files) could be mistaken for authoritative state even though the in-repo loop source is absent.
- Evidence: `tasks.md` previously carried completed items in `## Todo`; `git log --all --format=%s` currently misses task-id commit subjects for `BENCH-REVIEW-001`, `MGR-DASH-002`, `MGR-UX-003`, `BENCH-DATA-001`, `BENCH-ACTIVE-001`, `BENCH-RUNS-API-001`, `BENCH-UX-005`, `BENCH-TEST-001`, `ARCH-014`, `ARCH-013`, `ARCH-012`, `ARCH-011`, `MGR-TEST-001`, `MGR-BUG-001`, `MGR-REVIEW-001`, and `QUERY-REVIEW-001`; `.agent/history/` stops at `20260330.jsonl` while `tasks.md` contains `2026-03-31` progress entries; the repo no longer contains `scripts/agent_loop.py` even though older artifacts reference it.
- Impact/Risk: Humans cannot reliably infer whether a task is actually complete, committed, or merely verified; blocked-task escalation is ambiguous; legacy diagnostics can be mistaken for the source of truth during recovery.
- Suggested next step: Keep `tasks.md`, `tasks-done.md`, and Git history as the canonical audit trail, grandfather the known legacy missing-commit IDs in the audit script, and enforce the stricter rules for all new completions.
- Human decision: pending

### INBOX-20260402-003
- Area: Test config governance
- Related task(s): `BENCH-TEST-002`, `TEST-CONFIG-001`
- Summary: `BENCH-TEST-002` exported a new testing rule to keep datasource/probe fixtures in test-owned config, and a follow-up review task was added so the rest of the codebase can be checked for the same pattern.
- Evidence: `docs/operations/best-practices.md` now includes the test-fixture rule, and `tasks.md` now includes `TEST-CONFIG-001` to audit remaining inline connection fixtures.
- Impact/Risk: Similar hard-coded test connection details may still exist elsewhere, which makes fixture updates harder to review and maintain.
- Suggested next step: Schedule `TEST-CONFIG-001` and convert remaining inline test connection fixtures to test-owned config where appropriate.
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
- Suggested next step: Grandfather the remaining missing task-id commit links (`BENCH-UX-007`, `MYSQL-ONLY-001`, and `TEST-CONFIG-002`) into the audit allowlist instead of trying to reconstruct non-recoverable historical commit subjects.
- Human decision: approved 2026-04-04 — use the grandfather path for `BENCH-UX-007`, `MYSQL-ONLY-001`, and `TEST-CONFIG-002`; do not spend more time reconstructing historical commit links.

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
- Suggested next step: Keep the benchmark cleanup on a `MYSQL-only` checked-in test-fixture path and execute the existing follow-up task `BENCH-MYSQL-TEST-002` to remove the remaining H2 defaults instead of introducing any new H2 fallback or MySQL-first compromise.
- Human decision: approved 2026-04-04 — keep the benchmark follow-up on a `MYSQL-only` path by executing `BENCH-MYSQL-TEST-002`; do not pursue an H2 fallback or a mixed `MySQL-first` approach.

### INBOX-20260402-018
- Area: Config governance
- Related task(s): `CONFIG-PROFILE-001`, `CONFIG-REVIEW-001`
- Summary: `CONFIG-PROFILE-001` exported a new English best-practice rule that all environment-specific configuration must live in each module's `application-dev.yml`, `application-test.yml`, and `application-pro.yml`, and a follow-up review task was added to audit the rest of the repo against that rule.
- Evidence: `docs/operations/best-practices.md` now replaces the two older config-related rules with the new profile-governance rule, and `tasks.md` now includes `CONFIG-REVIEW-001` for retroactive cleanup.
- Impact/Risk: Without the follow-up audit, older files may still carry environment-specific drift outside the three profile YAMLs even though the new standard is now documented.
- Suggested next step: Schedule `CONFIG-REVIEW-001` and remove any remaining non-compliant environment config from the repo.
- Human decision: pending

### INBOX-20260402-021
- Area: Harness validation-command drift
- Related task(s): `QUERY-ARCH-003`
- Summary: The shared `analyze` module means the module-local validation commands in `.agent/config.json` for `query` and `manager` are now stale, because standalone `mvn -q -f query/pom.xml test` and `mvn -q -f manager/pom.xml test` no longer resolve the sibling artifact without a reactor build or a prior local install.
- Evidence: During `QUERY-ARCH-003` verification on 2026-04-02, `mvn -q -pl query -am test` and `mvn -q -pl manager -am ... test` passed, while standalone `mvn -q -f query/pom.xml test` and `mvn -q -f manager/pom.xml ... test` failed dependency resolution for `com.smartbi:analyze:1.0.0-SNAPSHOT`.
- Impact/Risk: Harness automation and humans following the existing validation matrix can get false-negative failures even when the code is healthy, because the documented and automated commands no longer match the repo's build topology.
- Suggested next step: Open a harness-policy task to update `.agent/config.json`, the validation matrix, and any related automation to use reactor builds for modules that now depend on `analyze`.
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
