# Git Workflow

The autonomous loop uses the repository root as the canonical Git boundary.

## Required State for Full Autonomous Mode

- `/Users/sfc/Documents/projects/engine/.git` exists and is the active repository root.
- `git rev-parse --show-toplevel` resolves to `/Users/sfc/Documents/projects/engine`.
- Nested `.git` directories are removed, converted to submodules, or archived outside active module paths.

## Current Handling

The previous nested `kylin-jdbc-cache/.git` metadata has been archived under `.agent/runtime/archived-git/` so the harness can treat the root as the active repository.

## Archived Metadata Notes

- Keep archived Git metadata outside active module paths.
- Do not recreate nested `.git` directories without an explicit structural decision.
- If `kylin-jdbc-cache` later needs to become a submodule or external dependency, update this doc and the harness expectations together.

## Commit Policy

- One verified task per commit.
- Use long-lived run branches such as `codex/autoloop/<timestamp>`.
- Do not force-push active autonomous branches.
