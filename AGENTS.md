# AGENTS

This file is the fast-start contract for the foreman model working in Easy Engine.

The foreman reads `tasks.md`, receives a task assignment from the human, does the work directly in the current session, and writes progress back into `tasks.md`.

## Repo Map

- `query/`: query execution service.
- `manager/`: control-plane service.
- `benchmark/`: benchmark backend and frontend.
- `tasks.md`: canonical task ledger — human-readable, foreman-writable.
- `INBOX.md`: repo-root inbox for agent-found issues and suggestions awaiting human review.
- `.agent/config.json`: harness policy, validation commands, mirror policy, and service health checks.
- `.agent/history/`: best-effort JSONL diagnostics from earlier loop tooling; detailed runner logs live under `.agent/runtime/runner-logs/`.

## Read This First

1. [README.md](README.md)
2. [docs/README.md](docs/README.md)
3. [docs/architecture/README.md](docs/architecture/README.md)
4. [docs/operations/README.md](docs/operations/README.md)
5. [docs/operations/human-collaboration.md](docs/operations/human-collaboration.md)
6. [docs/product/backlog.md](docs/product/backlog.md)

## Workflow

### 1. Select a task

The human tells the foreman which task to run. The foreman reads that task entry from `tasks.md` and marks it `in_progress`.

Eligibility rules (apply when the human has not named a specific task):

- Status must be `todo`.
- All `depends_on` tasks must be `done`.
- Prefer lower `priority` number. Break ties by oldest update timestamp.

### 2. Investigate and implement

The foreman plans, implements, and validates the task directly in the current session. There is no per-stage or per-task model switching in the harness contract.

Append concrete implementation notes to the task's **Progress log** section in `tasks.md`.

### 3. Validate

Run the task's validation commands when feasible, inspect the changed behavior, and record evidence in the task's **Progress log**.

### 4. Run doc-gardener (when docs changed)

Run after a task that changes behavior, APIs, or architecture. The foreman performs doc gardening itself and records any doc updates or remaining drift in the task log.

### 5. Close the task and commit

One verified task = one commit.

```bash
git add -p   # stage only task-related changes
git commit -m "<task-id>: <short title>"
```

Completion order is mandatory:

1. implement
2. verify
3. doc-garden if needed
4. append progress evidence to `tasks.md`
5. update the task to `done` and move it to the Done table
6. create the single task commit, including the ledger update
7. treat the task as finished only after the commit succeeds

If the commit fails, keep working until it succeeds or restore the task to a non-`done` state before stopping.

## Writing Progress Back to tasks.md

After each major milestone, append a dated entry under the task's **Progress log** heading:

```markdown
**2026-03-30 — implementation**
Files changed: ...
Commands run: ...
Result: implemented / failed

**2026-03-30 — verification**
Validation status: approved / rejected
Evidence: ...
Next action: ...
Escalation: none / INBOX-...
```

When a task is completed, update its **Status** line to `done`, move it to the Done table at the bottom of `tasks.md`, and include that ledger update in the task's commit.

## Hard Rules

- Do the work directly in the current session.
- Do not switch between external coding runners by stage or by task.
- Do not edit `.agent/config.json` during non-harness task execution. Harness-policy changes must be their own explicit task.
- Do not make broad multi-module changes in one task unless the task explicitly says so.
- Do not remove human review checkpoints from Git workflows.
- Collect harness or tooling issues into `INBOX.md` first, even when they are discovered during another tracked task; do not change infrastructure without explicit human approval.
- Always read `AGENTS.md` first when picking up a new task to ensure alignment with the latest project contract.
- All task state lives in `tasks.md`. Do not create JSON, YAML, or other machine state files for task tracking.

## Git Contract

- Root repository is the intended canonical Git boundary.
- Nested `.git` directories are hazards unless explicitly resolved.
- One verified task maps to one commit.
- Commit subjects use `<task-id>: <short title>`.
- Do not force-push or rewrite published history.

## Audit Trail

- `tasks.md` plus Git history are the canonical audit trail for task state and completion.
- `.agent/history/` and `.agent/runtime/runner-logs/` are best-effort diagnostics only unless an in-repo loop implementation is restored.

## Validation Commands Reference

See `.agent/config.json` `validation_commands` for the current per-module command set.

| Module | Commands |
|--------|----------|
| manager | `mvn -q -f manager/pom.xml test` · `npm --prefix manager/frontend run build` |
| query | `mvn -q -f query/pom.xml test` |
| benchmark | `mvn -q -f benchmark/pom.xml test` · `npm --prefix benchmark/frontend run build` |
| smoke | `bash scripts/benchmark-smoke.sh` |

## Documentation Contract

- Keep `AGENTS.md` short.
- Put durable explanations in `docs/`.
- Update architecture or interface docs whenever behavior, boundaries, or APIs change.
- Refresh Chinese mirrors only for the configured human-facing document set.

## Human Collaboration

- Humans assign tasks, review diffs, approve merges, and reprioritize `tasks.md`.
- Humans pause work by telling the foreman to stop; no lock file required.
- If the foreman is uncertain, ask the human before proceeding.
