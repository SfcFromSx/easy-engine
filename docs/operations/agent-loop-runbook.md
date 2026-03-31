# Agent Workflow Runbook

The foreman model drives task execution manually from `tasks.md`. It receives a task assignment from the human, then calls `claude` and `codex` from the command line.

See [AGENTS.md](../../AGENTS.md) for the full foreman workflow and CLI command templates, and [human-collaboration.md](/Users/sfc/Documents/projects/engine/docs/operations/human-collaboration.md) for the human-side guardrails.

## Stage Shape

Each task runs through up to four stages:

1. **Orchestrator** — reads the task, produces an implementation brief. Runner: claude.
2. **Implementer** — makes code changes per the brief. Runner: codex (backend/test/docs) or claude (frontend).
3. **Verifier** — challenges the implementation against acceptance criteria. Runner: codex.
4. **Doc-gardener** — updates canonical docs when behavior or APIs changed. Runner: codex. Optional.

After verifier approval: commit, then optionally run doc-gardener.

## Runner Commands

### claude

```bash
claude -p --model opus4.6 "<prompt>"
```

### codex

```bash
codex exec --sandbox danger-full-access \
  --model gpt-5.4 \
  -c 'model_reasoning_effort="high"' \
  "<prompt>"
```

Codex runs with MCP servers and plugins disabled. Model and flag values are in `.agent/config.json` under `runners.codex`.

## Stage Routing

Default routing is defined in `tasks.md` header and `.agent/config.json`. Per-task overrides are noted in the task entry.

| Task type | Orchestrator | Implementer | Verifier | Doc-gardener |
|-----------|-------------|-------------|----------|--------------|
| backend / test / docs / architecture | claude | codex | codex | codex |
| frontend | claude | claude | codex | codex |

## Progress Logging

After each stage the foreman appends a dated entry to the task's **Progress log** in `tasks.md`. See AGENTS.md for the log format.

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

## Codex Best Practices

- Prefer single-stage execution for debugging.
- Codex runs are best-effort under unstable networks — expect retries.
- Do not run codex with MCP or plugin flags; the isolation is intentional.

## Doc Gardening

Run the doc-gardener stage after any task that changes behavior, APIs, or architecture. Refresh Chinese mirrors only for the configured human-facing document set (see `.agent/config.json` `mirror_docs`).

## History Logs

Stage history is recorded in `.agent/history/` as append-only JSONL files. Detailed runner transcripts live under `.agent/runtime/runner-logs/`.
