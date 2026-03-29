# Terminology and Invariants

## Terms

- `Canonical docs`: English docs under `docs/`.
- `Mirror docs`: selected Chinese human-facing translations under `doc-CN/`.
- `Loop`: one orchestrator -> implementer -> verifier cycle.
- `Doc gardening`: refreshing canonical docs and mirrors after behavior changes.
- `Root Git boundary`: `/Users/sfc/Documents/projects/engine` as the intended single repository root.

## Invariants

- `query` is query-only.
- `manager` is control-plane only.
- `benchmark` owns benchmark orchestration, not production query semantics.
- `kylin-jdbc-cache` remains a client-side adapter.
- `tasks.json` is the task ledger for autonomous runs.
- `AGENTS.md` must stay short.
