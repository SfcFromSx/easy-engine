# Tasks

This is the active task ledger for Easy Engine. The foreman model reads this file, picks a task at human direction, works directly in the current session, and writes progress and outcomes back into each active task entry.

Before any agent picks or starts a task from this ledger, it must read [AGENTS.md](/Users/sfc/Documents/projects/engine/AGENTS.md) first and follow that contract.

Completed task history lives in [tasks-done.md](/Users/sfc/Documents/projects/engine/tasks-done.md).

## Status values

| Status | Meaning |
|--------|---------|
| `todo` | Ready to work, dependencies met |
| `in_progress` | Currently being worked |
| `done` | Completed, committed, and archived in `tasks-done.md` |
| `blocked` | Failed max attempts, needs human review |

---

## Todo

### ARCH-010

- **Status:** blocked
- **Module:** tests | **Type:** backend | **Priority:** 15
- **Title:** UPDATE E2E TEST SUITE FOR MYSQL DATABASE
- **Attempts:** 3

**Context files**

- `tests/pom.xml`
- `tests/src/test/java/com/smartbi/e2e/E2EConfig.java`
- `tests/src/test/java/com/smartbi/e2e/E2ETestBase.java`
- `tests/src/test/java/com/smartbi/e2e/PostgresInfraE2ETest.java`
- `tests/src/test/java/com/smartbi/e2e/TraceRecordE2ETest.java`
- `tests/src/test/java/com/smartbi/e2e/CacheE2ETest.java`
- `tests/src/test/java/com/smartbi/e2e/PrestoRoutingE2ETest.java`
- `tests/src/test/java/com/smartbi/e2e/KylinJdbcE2ETest.java`
- `tests/src/test/java/com/smartbi/e2e/ManagerApiE2ETest.java`

**Acceptance criteria**

1. `tests/pom.xml` replaces `org.postgresql:postgresql` with `com.mysql:mysql-connector-j` (same version used in manager/benchmark pom.xml).
2. `E2EConfig.java` default JDBC URL updated to `jdbc:mysql://localhost:3307/engine_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`; user/password updated to match the MySQL docker-compose service.
3. `E2EConfig.java` adds MySQL host/port properties (`e2e.mysql.host`, `e2e.mysql.port`) overridable via `-D` flags.
4. `PostgresInfraE2ETest` renamed or updated to `MysqlInfraE2ETest`; Flyway history table updated to match the MySQL schema history table name used by each module.
5. All `pgConnection()` usages in test classes updated to use the MySQL JDBC URL.
6. Any PostgreSQL-specific SQL syntax in tests replaced with MySQL-compatible equivalents.
7. `tests/README.md` updated: docker-compose command uses the MySQL service, port references updated.
8. `mvn -f tests/pom.xml test` passes against a running MySQL + all other services.
9. E2E API assertions align to the current manager public contract (`/api/v1/query-datasources`, `/api/v1/stats/summary`, `/api/v1/acceleration-tables`, `DELETE` returns `204`).
10. E2E schema assertions align to the current MySQL schema (`received_at`, `clean_sql_sample`, `name`/`schema_name`) and use the correct Flyway history table per module.

**Validation commands**

```bash
mvn -f tests/pom.xml test
```

**Progress log**

**2026-03-31 — implementation**
Files changed: `tests/README.md`, `tests/pom.xml`, `tests/src/test/java/com/smartbi/e2e/*`
Commands run: `mvn -f tests/pom.xml test`
Result: E2E suite updated for MySQL naming, host/port overrides, manager API route changes, and current schema field names.

**2026-03-31 — verification**
Validation status: rejected
Evidence: `QueryHttpE2ETest` still fails because Kylin-backed requests return `isException=true`; `bash scripts/benchmark-smoke.sh` exits with `Kylin 不可用: http://127.0.0.1:17070`.
Next action: resolve local Kylin readiness in `ARCH-014`, then rerun `mvn -f tests/pom.xml test`.

**2026-03-31 — revalidation**
Validation status: rejected
Evidence: `mvn -f tests/pom.xml test` still fails with `QueryHttpE2ETest#testPreparedStatementQuerySuccess` (`isException=true`) plus 5 `KylinJdbcE2ETest` errors returning HTTP `404` from `/kylin/api/user/authentication`; direct `curl -X POST http://127.0.0.1:17070/kylin/api/user/authentication` now returns `200` and `docker compose ps --all` shows `kylin` `healthy`, so the original container-readiness blocker is cleared but E2E validation still fails on the query/Kylin path.
Next action: investigate the Kylin JDBC/query integration mismatch and rerun `mvn -f tests/pom.xml test` after that path is fixed.
Escalation: none

## Archive

Completed tasks are archived in [tasks-done.md](/Users/sfc/Documents/projects/engine/tasks-done.md). Agents should read `tasks.md` for task selection and current progress, and consult `tasks-done.md` only when they need completed-task history or prior done signals.
