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

### INBOX-20260331-001
- Area: Harness governance and audit trail
- Related task(s): `HARNESS-GOV-001`, `ARCH-010`
- Summary: Task completion and escalation hygiene drifted from the documented contract: some completed tasks stayed under `## Todo`, multiple `Done` rows have no matching task-id commit subject in Git history, blocked tasks were not recording `Escalation: ...`, and legacy harness artifacts (`.agent/history/`, prompt docs, stale `agent_loop.pyc` files) could be mistaken for authoritative state even though the in-repo loop source is absent.
- Evidence: `tasks.md` previously carried completed items in `## Todo`; `git log --all --format=%s` currently misses task-id commit subjects for `BENCH-REVIEW-001`, `MGR-DASH-002`, `MGR-UX-003`, `BENCH-DATA-001`, `BENCH-ACTIVE-001`, `BENCH-RUNS-API-001`, `BENCH-UX-005`, `BENCH-TEST-001`, `ARCH-014`, `ARCH-013`, `ARCH-012`, `ARCH-011`, `MGR-TEST-001`, `MGR-BUG-001`, `MGR-REVIEW-001`, and `QUERY-REVIEW-001`; `.agent/history/` stops at `20260330.jsonl` while `tasks.md` contains `2026-03-31` progress entries; the repo no longer contains `scripts/agent_loop.py` even though older artifacts reference it.
- Impact/Risk: Humans cannot reliably infer whether a task is actually complete, committed, or merely verified; blocked-task escalation is ambiguous; legacy diagnostics can be mistaken for the source of truth during recovery.
- Suggested next step: Keep `tasks.md`, `tasks-done.md`, and Git history as the canonical audit trail, grandfather the known legacy missing-commit IDs in the audit script, and enforce the stricter rules for all new completions.
- Human decision: pending

### INBOX-20260402-001
- Area: Benchmark validation drift
- Related task(s): `BENCH-CONFIG-001`
- Summary: The standard benchmark backend validation command currently fails for reasons unrelated to the preflight-config task: Spring MVC rejects the benchmark SPA forward pattern in `WebConfig`, and Docker-backed coverage is still unavailable in this environment.
- Evidence: `mvn -q -f benchmark/pom.xml test` failed on 2026-04-02 with `PatternParseException: Invalid mapping pattern detected: /**/{path:[^\\.]*}` from `benchmark/src/main/java/com/smartbi/benchmark/config/WebConfig.java`, plus Testcontainers reported `Could not find a valid Docker environment`.
- Impact/Risk: Benchmark tasks cannot rely on the module's default Maven test command as a clean verification signal, so unrelated suite drift can mask task-local regressions.
- Suggested next step: Open a dedicated benchmark validation task to fix or isolate the SPA route mapping for test startup and decide how Docker-dependent coverage should run locally versus CI.
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

### INBOX-20260402-009
- Area: Harness audit trail
- Related task(s): `QUERY-TRINO-001`
- Summary: `tasks-done.md` was updated on 2026-04-02 to archive the completion of `QUERY-TRINO-001`, while preserving a concurrently introduced `tasks.md` intake for unrelated task `ARCH-015`.
- Evidence: The Trino task now has a full archived progress log and done signal in `tasks-done.md`; `tasks.md` was left aligned with the separately in-progress `ARCH-015` entry instead of being overwritten during closeout.
- Impact/Risk: Without an explicit note, later recovery could misread the ledger state and assume the missing active-row history for `QUERY-TRINO-001` was accidental rather than the result of concurrent harness edits being preserved.
- Suggested next step: Keep the archived Trino entry and its task commit aligned, and let the active ledger continue tracking `ARCH-015` independently.
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
