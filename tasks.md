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
| QUERY-MYSQL-TEST-002 | REMOVE REMAINING H2 TEST FIXTURES FROM QUERY | query | todo | 2026-04-04 | none |
| MANAGER-MYSQL-TEST-002 | REMOVE REMAINING H2 TEST FIXTURES FROM MANAGER | manager | todo | 2026-04-04 | none |
| BENCH-MYSQL-TEST-002 | REMOVE REMAINING H2 TEST FIXTURES FROM BENCHMARK | benchmark | todo | 2026-04-04 | none |
| MGR-DDL-REBUILD-002 | REPLACE PATCH-STYLE COLUMN UPGRADES IN MANAGER MIGRATIONS | manager | todo | 2026-04-04 | none |

### QUERY-MYSQL-TEST-002: REMOVE REMAINING H2 TEST FIXTURES FROM QUERY

- **Status**: todo
- **Updated**: 2026-04-04
- **Module**: query
- **Dependencies**: none
- **Scope**:
  - Remove the remaining H2 test dependency from `query/pom.xml`.
  - Replace H2 defaults in `query/src/test/resources/test-fixtures.yml` with MySQL-first or engine-appropriate explicit fixtures.
  - Rewrite remaining `query` tests and helpers that still hard-code `org.h2.Driver`, `jdbc:h2:`, or `type=h2`.
  - Update any query documentation that still describes H2 as an allowed checked-in regression fixture.
- **Progress log**:
  - **2026-04-04 — intake**
    - `BP-AUDIT-001` re-audited the current working tree and found remaining H2 references in `query/pom.xml`, `query/src/test/resources/test-fixtures.yml`, multiple query tests, and `docs/modules/query.md`.
    - Scope is limited to the query module so the MySQL-only cleanup can be validated and committed independently from manager and benchmark follow-ups.

### MANAGER-MYSQL-TEST-002: REMOVE REMAINING H2 TEST FIXTURES FROM MANAGER

- **Status**: todo
- **Updated**: 2026-04-04
- **Module**: manager
- **Dependencies**: none
- **Scope**:
  - Remove the remaining H2 test dependency from `manager/pom.xml`.
  - Replace H2 defaults in `manager/src/test/resources/test-fixtures.yml` and `ManagerTestFixtures` with MySQL-backed explicit fixtures.
  - Rewrite remaining manager tests and helpers that still hard-code `org.h2.Driver` or `jdbc:h2:`.
  - Update any manager documentation that still describes H2 as an allowed checked-in regression fixture.
- **Progress log**:
  - **2026-04-04 — intake**
    - `BP-AUDIT-001` found manager-side H2 residue in `manager/pom.xml`, `manager/src/test/resources/test-fixtures.yml`, `manager/src/test/java/com/smartbi/engine/support/ManagerTestFixtures.java`, and `docs/modules/manager.md`.
    - The task stays manager-only so fixture cleanup and migration-chain work do not get coupled into one wide commit.

### BENCH-MYSQL-TEST-002: REMOVE REMAINING H2 TEST FIXTURES FROM BENCHMARK

- **Status**: todo
- **Updated**: 2026-04-04
- **Module**: benchmark
- **Dependencies**: none
- **Scope**:
  - Remove the remaining H2 test dependency from `benchmark/pom.xml`.
  - Replace H2 defaults in `benchmark/src/test/resources/test-fixtures.yml` with MySQL-backed explicit fixtures.
  - Rewrite benchmark JDBC upload, query-service, async-runner, and smoke fixtures that still assume H2 driver classes or H2 JDBC URLs.
  - Update any benchmark documentation that still describes H2 as an allowed checked-in regression fixture.
- **Progress log**:
  - **2026-04-04 — intake**
    - `BP-AUDIT-001` found benchmark-side H2 residue in `benchmark/pom.xml`, `benchmark/src/test/resources/test-fixtures.yml`, `JdbcDriverUploadIntegrationTest`, `BenchmarkQueryServiceTest`, and `docs/modules/benchmark.md`.
    - This cleanup is split from query and manager so benchmark-specific fixture rewrites can be verified with the benchmark module test command only.

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
