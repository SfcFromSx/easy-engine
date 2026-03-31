# Agent Docs

This section contains legacy prompt and schema assets kept for historical reference from earlier harness designs. The active harness workflow is direct and does not switch external models by stage or task.

The authoritative completion, commit, and escalation rules live in [AGENTS.md](/Users/sfc/Documents/projects/engine/AGENTS.md) and [docs/operations/agent-loop-runbook.md](/Users/sfc/Documents/projects/engine/docs/operations/agent-loop-runbook.md), not in these legacy prompt files. The repository does not currently ship an active in-repo loop implementation such as `scripts/agent_loop.py`.

## Prompt Templates

- [prompts/orchestrator.md](/Users/sfc/Documents/projects/engine/docs/agent/prompts/orchestrator.md)
- [prompts/implementer.md](/Users/sfc/Documents/projects/engine/docs/agent/prompts/implementer.md)
- [prompts/verifier.md](/Users/sfc/Documents/projects/engine/docs/agent/prompts/verifier.md)
- [prompts/doc-gardener.md](/Users/sfc/Documents/projects/engine/docs/agent/prompts/doc-gardener.md)

## Schemas

- [schemas/orchestrator-output.schema.json](/Users/sfc/Documents/projects/engine/docs/agent/schemas/orchestrator-output.schema.json)
- [schemas/implementer-output.schema.json](/Users/sfc/Documents/projects/engine/docs/agent/schemas/implementer-output.schema.json)
- [schemas/verifier-output.schema.json](/Users/sfc/Documents/projects/engine/docs/agent/schemas/verifier-output.schema.json)
- [schemas/doc-gardener-output.schema.json](/Users/sfc/Documents/projects/engine/docs/agent/schemas/doc-gardener-output.schema.json)
