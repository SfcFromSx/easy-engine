# Easy Engine

Easy Engine is an agent-first workspace for query execution, control-plane analysis, and benchmark orchestration.

Canonical English documentation lives under [docs/README.md](/Users/sfc/Documents/projects/engine/docs/README.md).

Use these entrypoints:

- [AGENTS.md](/Users/sfc/Documents/projects/engine/AGENTS.md) for the agent operating contract.
- [tasks.md](/Users/sfc/Documents/projects/engine/tasks.md) for the canonical task ledger.
- [INBOX.md](/Users/sfc/Documents/projects/engine/INBOX.md) for agent-found issues and suggestions awaiting human decisions.
- [docs/architecture/README.md](/Users/sfc/Documents/projects/engine/docs/architecture/README.md) for architecture docs.
- [docs/operations/README.md](/Users/sfc/Documents/projects/engine/docs/operations/README.md) for runbooks and local development.
- [docs/operations/human-collaboration.md](/Users/sfc/Documents/projects/engine/docs/operations/human-collaboration.md) for detailed human collaboration rules.
- [docs/agent/README.md](/Users/sfc/Documents/projects/engine/docs/agent/README.md) for prompt and schema assets.
- [doc-CN/README.md](/Users/sfc/Documents/projects/engine/doc-CN/README.md) for the selective Chinese mirror set.

## For Humans

- Assign and reprioritize work in [tasks.md](/Users/sfc/Documents/projects/engine/tasks.md).
- The foreman always works directly in the current session; there is no per-task execution-mode switch.
- Stop or resume the foreman by telling it to stop or continue; there is no separate pause file workflow.
- Review diffs, run extra validation, and approve merges before publish.
- Read [INBOX.md](/Users/sfc/Documents/projects/engine/INBOX.md) for issues or suggestions that agents surfaced but did not convert into implementation work.
