# Agent Workflow Runbook

The foreman model drives task execution from `tasks.md` and works directly in the current session.

See [AGENTS.md](../../AGENTS.md) for the full foreman workflow contract, and [human-collaboration.md](/Users/sfc/Documents/projects/engine/docs/operations/human-collaboration.md) for the human-side guardrails.

## Stage Shape

Each task runs through up to five stages:

1. **Plan / inspect** — understand the task, code paths, and constraints.
2. **Implement** — make the required code or documentation changes.
3. **Verify** — run validation commands and inspect the changed behavior.
4. **Doc gardening** — update canonical docs when behavior or APIs changed. Optional.
5. **Ledger closeout + commit** — append progress evidence, move the task to `Done`, and create the task commit.

Completion order is mandatory: implement, verify, doc-garden if needed, append progress evidence, update `tasks.md`, then commit. A task is not finished until that commit succeeds.

## Progress Logging

After each stage, or after each major milestone, the foreman appends a dated entry to the task's **Progress log** in `tasks.md`. See AGENTS.md for the log format.

If a task is blocked at max attempts, the latest rejected or revalidation entry must include both `Next action:` and `Escalation:`. Use `Escalation: none` for task-local blockers that are already fully captured in the task log, or `Escalation: INBOX-...` when the blocker exposed a systemic harness, tooling, process, or documentation issue that also needs human review.

## Commit

One verified task = one commit.

```bash
git add -p
git commit -m "<task-id>: <short title>"
```

Do not push automatically. The human reviews and merges.

Run `python3 scripts/task_audit.py --check` before closeout when task-ledger or harness-governance files changed.

## Validation Commands Reference

See `.agent/config.json` `validation_commands` for the authoritative per-module command set.

| Module | Commands |
|--------|----------|
| manager | `mvn -q -f manager/pom.xml test` · `npm --prefix manager/frontend run build` |
| query | `mvn -q -f query/pom.xml test` |
| benchmark | `mvn -q -f benchmark/pom.xml test` · `npm --prefix benchmark/frontend run build` |
| smoke | `bash scripts/benchmark-smoke.sh` |

## Stop Conditions

- Human says to stop.
- Verifier rejects and attempts ≥ 3 → mark task `blocked`, stop.
- No unblocked `todo` tasks remain.
- Git state is invalid (nested .git hazard, dirty tree that cannot be attributed to the active task).

## Retry Policy

- On transient failures (network, timeout, process crash): retry up to 2 times with 30-second backoff. Do not count against task attempts.
- On permanent failures (wrong output, schema error, test failure): increment task attempts. Block after 3 permanent failures.

## Doc Gardening

Run the doc-gardener stage after any task that changes behavior, APIs, or architecture. Refresh Chinese mirrors only for the configured human-facing document set (see `.agent/config.json` `mirror_docs`).

## History Logs

`tasks.md` plus Git history are the canonical audit trail. `.agent/history/` and `.agent/runtime/runner-logs/` are best-effort diagnostics from earlier loop tooling and must not be treated as authoritative completion state unless an in-repo loop implementation is restored. The repository does not currently ship `scripts/agent_loop.py`.
