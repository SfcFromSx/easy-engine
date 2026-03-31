# Terminology and Invariants

## Terms

- `Canonical docs`: English docs under `docs/`.
- `Mirror docs`: selected Chinese human-facing translations under `doc-CN/`.
- `Loop`: one plan -> implement -> verify cycle under the foreman workflow.
- `Direct workflow`: the foreman does the work itself in the current session.
- `Doc gardening`: refreshing canonical docs and mirrors after behavior changes.
- `Root Git boundary`: `/Users/sfc/Documents/projects/engine` as the intended single repository root.

## Invariants

- `query` is query-only.
- `manager` is control-plane only.
- `benchmark` owns benchmark orchestration, not production query semantics.
- `kylin-jdbc-cache` remains a client-side adapter.
- `tasks.md` is the task ledger — human-readable Markdown, written by the foreman model.
- `AGENTS.md` must stay short.
