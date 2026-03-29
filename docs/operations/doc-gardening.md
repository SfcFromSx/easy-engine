# Documentation Gardening

Doc gardening keeps the canonical docs current without turning `AGENTS.md` into an encyclopedia.

## Rules

- Keep `AGENTS.md` short and operational.
- Move durable detail into `docs/`.
- Update architecture and interface docs whenever public behavior changes.
- Refresh generated context files when layout or module boundaries change.
- Refresh selected Chinese mirrors only after English source docs stabilize.

## Trigger Points

- After every two successful autonomous tasks.
- Whenever architecture or interface files change.
- Whenever a verifier rejection reveals missing or stale docs.

## Canonical vs Legacy

- `docs/` is canonical.
- `doc-CN/` is a selective mirror for human-facing docs.
- Do not introduce a parallel legacy English doc tree.
