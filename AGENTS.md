# AGENTS

This file is the fast-start contract for the foreman model working in Easy Engine.

The foreman reads `tasks.md`, receives a task assignment from the human, does the work directly in the current session, and writes progress back into the active ledger. Completed-task history lives in `tasks-done.md`.

## Repo Map

- `query/`: query execution service.
- `manager/`: control-plane service.
- `benchmark/`: benchmark backend and frontend.
- `tasks.md`: active task ledger — human-readable, foreman-writable.
- `tasks-done.md`: completed task archive and done-signal history.
- `INBOX.md`: repo-root inbox for agent-found issues and suggestions that still need human review or judgment.
- `.agent/config.json`: harness policy, validation commands, mirror policy, and service health checks.

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

### 3. Self-Review and Post-mortem

Run a self-review checklist against the implementation (checking style consistency, test coverage, and side-effects). If human code review is required, update the task status to `in_review` and pause.
For any task correcting a bug or code style issue, append a Post-mortem block (Root Cause, Cure, and Generalization) to the task's **Progress log**. Persistent generalization rules should be curated into `docs/operations/best-practices.md`.

### 4. Validate

Run the task's validation commands when feasible, inspect the changed behavior, and record evidence in the task's **Progress log**.

### 5. Run doc-gardener (when docs changed)

Run after a task that changes behavior, APIs, or architecture. The foreman performs doc gardening itself and records any doc updates or remaining drift in the task log.

### 6. Close the task and commit

One verified task = one commit.

```bash
git add -p   # stage only task-related changes
git commit -m "<task-id>: <short title>"
```

Completion order is mandatory:

1. implement
2. self-review and post-mortem
3. verify
4. doc-garden if needed
5. append progress evidence to `tasks.md`
6. update the task to `done` and move it from `tasks.md` into `tasks-done.md`
7. create the single task commit, including the ledger update
8. treat the task as finished only after the commit succeeds

If the commit fails, keep working until it succeeds or restore the task to a non-`done` state before stopping.

## Writing Progress Back to tasks.md

After each major milestone, append a dated entry under the task's **Progress log** heading:

```markdown
**2026-03-30 — implementation**
Files changed: ...
Commands run: ...
Result: implemented / failed

**2026-04-02 — review & post-mortem**
- Self-Review: [x] style check [x] test coverage [x] side-effects
- Root Cause (if bug): ...
- Cure: ...
- Generalization: "Always use X over Y..." (added to best-practices.md)

**2026-03-30 — verification**
Validation status: approved / rejected
Evidence: ...
Next action: ...
Escalation: none / INBOX-...
```

When a task is completed, update its **Status** line to `done`, move it out of `tasks.md` and into `tasks-done.md`, and include that ledger update in the task's commit.
If a task is awaiting human approval to proceed, update its status to `in_review`.

## Hard Rules

- Do the work directly in the current session.
- Do not switch between external coding runners by stage or by task.
- Do not edit `.agent/config.json` during non-harness task execution. Harness-policy changes must be their own explicit task.
- Do not make broad multi-module changes in one task unless the task explicitly says so.
- Do not remove human review checkpoints from Git workflows.
- Use `INBOX.md` only for issues that still need human judgment, approval, prioritization, or task-shaping. Do not create audit-only inbox entries for deterministic work the human already requested directly. Do not change infrastructure without explicit human approval.
- Generalized coding practices and code style rules derived from post-mortems belong in `docs/operations/best-practices.md`, not `INBOX.md`.
- Always read `AGENTS.md` first when picking up a new task to ensure alignment with the latest project contract.
- Task state lives in `tasks.md` and `tasks-done.md`. Do not create JSON, YAML, or other machine state files for task tracking.
- If the foreman is uncertain, ask the human before proceeding.
- Before changing `docs/operations/best-practices.md`, review the existing rules first. If the new lesson overlaps with an existing rule, merge, rewrite, or replace the current wording instead of adding a near-duplicate entry, and record that consolidation in the task log.
- When a best-practice update introduces new repo-wide cleanup work, create a "generalization review" task in `tasks.md`. Use `INBOX.md` for that follow-up only if the scope or priority still needs human judgment.
- Core harness document changes (for example `AGENTS.md`, `tasks.md`, `INBOX.md`, navigation trees) must be captured in the active task ledger and reported back to the human, but they do not require a mirror `INBOX.md` entry unless a human decision is still pending.

## Git Contract

- Root repository is the intended canonical Git boundary.
- Nested `.git` directories are hazards unless explicitly resolved.
- One verified task maps to one commit.
- ONLY stage and commit the specific files modified during the current task. NEVER use global tracking commands like `git add .`, `git add -A`, or `git commit -a` to prevent committing unrelated changes.
- Commit subjects use `<task-id>: <short title>`.
- Do not force-push or rewrite published history.

## Audit Trail

- `tasks.md`, `tasks-done.md`, and Git history are the unified, canonical audit trail for task state and completion.

## Validation Commands Reference

See `.agent/config.json` `validation_commands` for the current per-module command set.

| Module | Commands |
|--------|----------|
| manager | `bash scripts/with-java8.sh mvn -q -pl analyze,manager -am test -Dspring.mvc.pathmatch.matching-strategy=ant_path_matcher` · `npm --prefix manager/frontend run build` |
| query | `bash scripts/with-java8.sh mvn -q -pl analyze,query -am test` |
| benchmark | `bash scripts/with-java8.sh mvn -q -f benchmark/pom.xml test` · `npm --prefix benchmark/frontend run build` |
| smoke | `bash scripts/benchmark-smoke.sh` |

## Documentation Contract

- Keep `AGENTS.md` short.
- Put durable explanations in `docs/`.
- Update architecture or interface docs whenever behavior, boundaries, or APIs change.
- Refresh Chinese mirrors only for the configured human-facing document set.
