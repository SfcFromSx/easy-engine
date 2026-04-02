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
| CONFIG-REVIEW-001 | AUDIT REPO FOR PROFILE-YAML-ONLY ENV CONFIG COMPLIANCE | platform | todo | 2026-04-02 | none |

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

### CONFIG-REVIEW-001: AUDIT REPO FOR PROFILE-YAML-ONLY ENV CONFIG COMPLIANCE

- **Status**: todo
- **Updated**: 2026-04-02
- **Progress log**:
  - **2026-04-02 — intake**
    - Follow-up generalization review task created automatically from `CONFIG-PROFILE-001` after exporting the new profile-governance rule to `docs/operations/best-practices.md`: audit the repo for any remaining environment-specific configuration outside module-level `application-dev.yml`, `application-test.yml`, and `application-pro.yml`, and move or remove the remaining drift.

## Archive


Completed tasks are archived in [tasks-done.md](/Users/sfc/Documents/projects/engine/tasks-done.md). Agents should read `tasks.md` for task selection and current progress, and consult `tasks-done.md` only when they need completed-task history or prior done signals.
