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
- Use a normal task branch or the current working branch as directed by the human workflow.
- Do not auto-push after commit.
- Do not force-push active branches.
- Humans review before push, merge, or publish.
