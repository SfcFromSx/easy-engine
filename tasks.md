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
| MGR-DDL-REBUILD-002 | REPLACE PATCH-STYLE COLUMN UPGRADES IN MANAGER MIGRATIONS | manager | todo | 2026-04-04 | none |

### MGR-DDL-REBUILD-002: REPLACE PATCH-STYLE COLUMN UPGRADES IN MANAGER MIGRATIONS

- **Status**: todo
- **Updated**: 2026-04-04
- **Module**: manager
- **Dependencies**: none
- **Scope**:
  - Keep `manager/src/main/resources/db/migration/V2__engine_core.sql` and `manager/src/main/resources/db/migration/V7__datasource_config.sql` as the canonical final table definitions.
  - Replace `manager/src/main/resources/db/migration/V11__upgrade_text_to_mediumtext.sql` with an explicit rebuild-or-copy-forward compatibility path instead of `ALTER TABLE ... MODIFY COLUMN`.
  - Preserve only the minimum legacy-upgrade behavior needed for existing schemas; do not add fresh-schema structural drift back into later migrations.
  - Leave `V9__prefix_manager_tables` alone unless the rebuild work proves it must change for compatibility.
- **Progress log**:
  - **2026-04-04 — intake**
    - `BP-AUDIT-001` confirmed that audited `ADD COLUMN` drift is already gone, but manager still keeps one structure-shaping compatibility migration in `V11__upgrade_text_to_mediumtext.sql`.
    - The follow-up task is therefore narrowly scoped to replacing that residual column-patch chain with a rebuild-first migration path and re-verifying manager Flyway upgrade coverage.

## Archive


Completed tasks are archived in [tasks-done.md](/Users/sfc/Documents/projects/engine/tasks-done.md). Agents should read `tasks.md` for task selection and current progress, and consult `tasks-done.md` only when they need completed-task history or prior done signals.
