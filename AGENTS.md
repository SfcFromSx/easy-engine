# AGENTS

This file is the fast-start contract for the foreman model working in Easy Engine.

The foreman does not write code directly. It reads `tasks.md`, receives a task assignment from the human, then drives execution by calling `claude` and `codex` from the command line. All progress is written back into `tasks.md`.

## Repo Map

- `query/`: query execution service.
- `manager/`: control-plane service.
- `benchmark/`: benchmark backend and frontend.
- `kylin-jdbc-cache/`: JDBC adapter.
- `tasks.md`: canonical task ledger — human-readable, foreman-writable.
- `INBOX.md`: repo-root inbox for agent-found issues and suggestions awaiting human review.
- `.agent/config.json`: runner binaries, models, validation commands, and service health checks.
- `.agent/history/`: append-only JSONL stage history; detailed runner logs live under `.agent/runtime/runner-logs/`.

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

### 2. Run the orchestrator

The orchestrator reads the task and produces a precise implementation brief. It does not write code.

**Default runner: claude**

```bash
claude -p --model opus4.6 \
  "You are the Easy Engine Orchestrator. Read the task below and produce an implementation brief for the implementer. Follow docs/agent/prompts/orchestrator.md.\n\n$(cat tasks.md)\n\nTask ID: <TASK_ID>"
```

Append the orchestrator output to the task's **Progress log** section in `tasks.md`.

### 3. Run the implementer

The implementer receives the orchestrator brief and makes the code changes.

**Default runner: codex** (frontend tasks: claude)

```bash
# codex (backend/test/architecture/documentation tasks)
codex exec --sandbox danger-full-access \
  --model gpt-5.4 \
  -c 'model_reasoning_effort="high"' \
  "You are the Easy Engine Implementer. Follow docs/agent/prompts/implementer.md.\n\nOrchestrator brief:\n<ORCHESTRATOR_OUTPUT>\n\nTask:\n<TASK_ENTRY>"

# claude (frontend tasks)
claude -p --model opus4.6 \
  "You are the Easy Engine Implementer. Follow docs/agent/prompts/implementer.md.\n\nOrchestrator brief:\n<ORCHESTRATOR_OUTPUT>\n\nTask:\n<TASK_ENTRY>"
```

Append the implementer output and a list of modified files to the task's **Progress log**.

### 4. Run the verifier

The verifier reviews the implementation against the acceptance criteria.

**Default runner: codex**

```bash
codex exec --sandbox danger-full-access \
  --model gpt-5.4 \
  -c 'model_reasoning_effort="high"' \
  "You are the Easy Engine Verifier. Follow docs/agent/prompts/verifier.md.\n\nOrchestrator brief:\n<ORCHESTRATOR_OUTPUT>\n\nImplementer output:\n<IMPLEMENTER_OUTPUT>\n\nTask:\n<TASK_ENTRY>"
```

- **Approved:** mark task `done` in `tasks.md`, append evidence to **Progress log**, commit.
- **Rejected:** append the rejection reason to **Progress log**, increment attempts. If attempts ≥ 3, mark `blocked`.

### 5. Run doc-gardener (when docs changed)

Run after a task that changes behavior, APIs, or architecture.

**Default runner: codex**

```bash
codex exec --sandbox danger-full-access \
  --model gpt-5.4 \
  -c 'model_reasoning_effort="high"' \
  "You are the Easy Engine Doc Gardener. Follow docs/agent/prompts/doc-gardener.md.\n\nCompleted task:\n<TASK_ENTRY>\n\nImplementer output:\n<IMPLEMENTER_OUTPUT>"
```

### 6. Commit

One verified task = one commit.

```bash
git add -p   # stage only task-related changes
git commit -m "<task-id>: <short title>"
```

## Writing Progress Back to tasks.md

After each stage, append a dated entry under the task's **Progress log** heading:

```markdown
**2026-03-30 — orchestrator**
Brief: <one-line summary of the brief produced>

**2026-03-30 — implementer**
Files changed: ...
Commands run: ...
Result: implemented / failed

**2026-03-30 — verifier**
Validation status: approved / rejected
Evidence: ...
Next action: ...
```

When a task is completed, update its **Status** line to `done` and move it to the Done table at the bottom of `tasks.md`.

## Codex isolation note

Codex runs are isolated: MCP servers and plugins are disabled. Pass model overrides via `-c` flags as shown above. The `.agent/config.json` `runners.codex` section is the reference for current model and flag values.

## Hard Rules

- Do not write code yourself — delegate to claude or codex.
- Do not edit `.agent/config.json` during active task execution.
- Do not make broad multi-module changes in one task unless the task explicitly says so.
- Do not remove human review checkpoints from Git workflows.
- Collect harness or tooling issues into `INBOX.md` first; do not change infrastructure without explicit human approval.
- All task state lives in `tasks.md`. Do not create JSON, YAML, or other machine state files for task tracking.

## Git Contract

- Root repository is the intended canonical Git boundary.
- Nested `.git` directories are hazards unless explicitly resolved.
- One verified task maps to one commit.
- Do not force-push or rewrite published history.

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
