# AGENTS

This file is intentionally short. It is the fast-start contract for autonomous agents working in Easy Engine.

## Mission

- Improve the repository safely through small, verified, reviewable changes.
- Treat English docs in `docs/` as the canonical source of truth.
- Keep agent loops resumable, deterministic, and easy to audit.

## Repo Map

- `query/`: query execution service.
- `manager/`: control-plane service.
- `benchmark/`: benchmark backend and frontend.
- `kylin-jdbc-cache/`: JDBC adapter.
- `scripts/agent_loop.py`: autonomous harness entrypoint.
- `tasks.json`: canonical machine-readable task ledger.
- `.agent/config.json`: runner, validation, and policy defaults.
- `.agent/history/`: iteration logs and runner transcripts.

## Read This First

1. [docs/README.md](/Users/sfc/Documents/projects/engine/docs/README.md)
2. [docs/architecture/README.md](/Users/sfc/Documents/projects/engine/docs/architecture/README.md)
3. [docs/operations/README.md](/Users/sfc/Documents/projects/engine/docs/operations/README.md)
4. [docs/product/backlog.md](/Users/sfc/Documents/projects/engine/docs/product/backlog.md)
5. [HUMAN.MD](/Users/sfc/Documents/projects/engine/HUMAN.MD)

## Hard Rules

- Do not recreate a parallel legacy English documentation tree. Canonical English docs live in `docs/`.
- Do not change `.agent/` files manually during an active loop unless the loop is paused.
- Do not bypass `tasks.json` when selecting work for autonomous runs.
- Do not make broad multi-module changes in one task unless the task explicitly says to do so.
- Do not remove human review checkpoints from Git or documentation workflows.

## Task Selection Contract

- Eligible task statuses: `todo`.
- Ignore tasks whose dependencies are not `done`.
- Prefer lower numeric `priority`.
- Break ties by oldest `updated_at`.
- Mark `blocked` after three verifier rejections.

## Documentation Contract

- Keep `AGENTS.md` short.
- Put durable explanations in `docs/`.
- Update architecture or interface docs whenever behavior, boundaries, or APIs change.
- Refresh Chinese mirrors only for the configured human-facing document set.

## Validation Contract

Default validation commands are defined in `.agent/config.json`:

- `manager`: Maven tests and frontend build.
- `query`: Maven tests.
- `benchmark`: Maven tests and frontend build.
- optional smoke: `scripts/benchmark-smoke.sh`

Codex-specific operation:

- Codex runs are best-effort by default under unstable networks.
- Expect retries, backoff, and runner logs before a task is marked as failed.
- Prefer `smoke-runner` and single-step execution before longer Codex loops.

## Git Contract

- Root repository is the intended canonical Git boundary.
- The loop must refuse full autonomous execution if root Git is invalid.
- Nested `.git` directories must be treated as hazards unless explicitly resolved.
- One verified task maps to one commit.

## Stop Conditions

- `doctor` fails.
- `.agent/PAUSE` exists.
- schema validation fails.
- retry ceiling is exceeded.
- no unblocked tasks remain.

## Human Collaboration

- Humans may reprioritize `tasks.json`, pause or resume the loop, review diffs, and merge.
- Humans must pause the loop before manual edits outside an explicitly assigned task.
- If the harness itself needs changes, collect the issue into `docs/product/issues-inbox.md` first unless a human explicitly authorizes a harness modification task.
