# Agent Workflow Runbook

The foreman model drives task execution from `tasks.md` and works directly in the current session.

See [AGENTS.md](../../AGENTS.md) for the full foreman workflow contract, and [human-collaboration.md](/Users/sfc/Documents/projects/engine/docs/operations/human-collaboration.md) for the human-side guardrails.

## Stage Shape

Each task runs through up to four stages:

1. **Plan / inspect** — understand the task, code paths, and constraints.
2. **Implement** — make the required code or documentation changes.
3. **Verify** — run validation commands and inspect the changed behavior.
4. **Doc gardening** — update canonical docs when behavior or APIs changed. Optional.

After verification approval: commit, then optionally run doc gardening.

## Progress Logging

After each stage, or after each major milestone, the foreman appends a dated entry to the task's **Progress log** in `tasks.md`. See AGENTS.md for the log format.

## Commit

One verified task = one commit.

```bash
git add -p
git commit -m "<task-id>: <short title>"
```

Do not push automatically. The human reviews and merges.

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

Stage history is recorded in `.agent/history/` as append-only JSONL files. Detailed runner transcripts live under `.agent/runtime/runner-logs/`.
