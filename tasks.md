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
| QUERY-TRINO-003 | ADD TRINO JDBC COMPATIBILITY PORT FOR BENCHMARK | query, benchmark | in_progress | 2026-04-03 | none |
| CONFIG-REVIEW-001 | AUDIT REPO FOR PROFILE-YAML-ONLY ENV CONFIG COMPLIANCE | platform | todo | 2026-04-02 | none |


### QUERY-TRINO-003: ADD TRINO JDBC COMPATIBILITY PORT FOR BENCHMARK

- **Status**: in_progress
- **Updated**: 2026-04-03
- **Progress log**:
  - **2026-04-03 — intake**
    - Human requested a new Trino JDBC-compatible `query` port in addition to the existing Kylin JDBC surface, with benchmark regression coverage proving `benchmark` can use `io.trino.jdbc.TrinoDriver` against the new port.
    - Scope for this task: add a default `8093` Trino JDBC compatibility port to `query`, implement the minimum `/v1/statement` behavior needed for benchmark `Statement` and `PreparedStatement` flows, keep the existing Kylin `8092` path unchanged, add focused query and benchmark regressions, update the canonical docs, and close the work through the required ledger/archive workflow.

### CONFIG-REVIEW-001: AUDIT REPO FOR PROFILE-YAML-ONLY ENV CONFIG COMPLIANCE

- **Status**: todo
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Follow-up generalization review task created automatically from `CONFIG-PROFILE-001` after exporting the new profile-governance rule to `docs/operations/best-practices.md`: audit the repo for any remaining environment-specific configuration outside module-level `application-dev.yml`, `application-test.yml`, and `application-pro.yml`, and move or remove the remaining drift.

## Archive


Completed tasks are archived in [tasks-done.md](/Users/sfc/Documents/projects/engine/tasks-done.md). Agents should read `tasks.md` for task selection and current progress, and consult `tasks-done.md` only when they need completed-task history or prior done signals.
