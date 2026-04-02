# Tasks

This is the active task ledger for Easy Engine. The foreman model reads this file, picks a task at human direction, works directly in the current session, and writes progress and outcomes back into each active task entry.

Before any agent picks or starts a task from this ledger, it must read [AGENTS.md](/Users/sfc/Documents/projects/engine/AGENTS.md) first and follow that contract.

Completed task history lives in [tasks-done.md](/Users/sfc/Documents/projects/engine/tasks-done.md).

## Status values

| Status | Meaning |
|--------|---------|
| `todo` | Ready to work, dependencies met |
| `in_progress` | Currently being worked |
| `in_review` | Implemented, awaiting codebase/human code review |
| `done` | Completed, committed, and archived in `tasks-done.md` |
| `blocked` | Failed max attempts, needs human review |

---

## Todo

| ID | Title | Module | Status | Updated | Dependencies |
|----|-------|--------|--------|---------|--------------|
| QUERY-BUG-002 | ACCEPT QUERY SQL AFTER LEADING OPTIMIZER COMMENTS | query | in_progress | 2026-04-02 | none |
| CONFIG-REVIEW-001 | AUDIT REPO FOR PROFILE-YAML-ONLY ENV CONFIG COMPLIANCE | platform | todo | 2026-04-02 | none |

### QUERY-BUG-002: ACCEPT QUERY SQL AFTER LEADING OPTIMIZER COMMENTS

- **Status**: in_progress
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Human asked to verify whether query SQL with hints can incorrectly fail with `Only query SQL is supported by engine-query`, then requested a tracked fix when the bug was confirmed.
    - Investigation confirmed the bug is specific to leading generic optimizer comments such as `/*+ ... */ SELECT ...`: `QueryExecutionService` validates `parsed.cleanSql`, but `CachePolicy#isQuerySql` only checks whether the trimmed string starts with `select`/`with`/`show`/`describe`/`explain`, so a leading non-routing comment causes a false non-query rejection even though the statement body is read-only. Supported Easy Engine metadata hints such as `YH_TARGET_ENGINE` are already stripped before this check and are not affected.

### CONFIG-REVIEW-001: AUDIT REPO FOR PROFILE-YAML-ONLY ENV CONFIG COMPLIANCE

- **Status**: todo
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Follow-up generalization review task created automatically from `CONFIG-PROFILE-001` after exporting the new profile-governance rule to `docs/operations/best-practices.md`: audit the repo for any remaining environment-specific configuration outside module-level `application-dev.yml`, `application-test.yml`, and `application-pro.yml`, and move or remove the remaining drift.

## Archive


Completed tasks are archived in [tasks-done.md](/Users/sfc/Documents/projects/engine/tasks-done.md). Agents should read `tasks.md` for task selection and current progress, and consult `tasks-done.md` only when they need completed-task history or prior done signals.
