# Git Workflow

The foreman workflow uses the repository root as the canonical Git boundary.

## Required State

- `/Users/sfc/Documents/projects/engine/.git` exists and is the active repository root.
- `git rev-parse --show-toplevel` resolves to `/Users/sfc/Documents/projects/engine`.
- Nested `.git` directories are removed, converted to submodules, or archived outside active module paths.

## Archived Metadata Notes

- Previous nested Git metadata, if archived under `.agent/runtime/archived-git/`, should remain outside active module paths.
- Keep archived Git metadata outside active module paths.
- Do not recreate nested `.git` directories without an explicit structural decision.

## Commit Policy

- One verified task per commit.
- Commit subjects use `<task-id>: <short title>`.
- Use a normal task branch or the current working branch as directed by the human workflow.
- Do not auto-push after commit.
- Do not force-push active branches.
- Humans review before push, merge, or publish.

## Completion Gate

Before a task is considered finished:

1. the implementation and verification evidence must be appended to `tasks.md`
2. doc gardening must be completed if the task changed behavior, APIs, or architecture
3. the task must be moved to the `Done` table in `tasks.md`
4. the single task commit must include the ledger update

If the commit fails, the agent must keep working until the commit succeeds or restore the task to a non-`done` state before stopping.

## Audit Trail

- `tasks.md` plus Git history are the canonical record of task completion.
- `.agent/history/` and `.agent/runtime/runner-logs/` are diagnostic artifacts only.
- Use `python3 scripts/task_audit.py --check` to catch ledger drift before closeout.
