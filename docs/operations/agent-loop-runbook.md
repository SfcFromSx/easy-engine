# Agent Loop Runbook

The autonomous loop is driven by [scripts/agent_loop.py](/Users/sfc/Documents/projects/engine/scripts/agent_loop.py).

## Commands

```bash
python3 scripts/agent_loop.py doctor
python3 scripts/agent_loop.py step --runner codex
python3 scripts/agent_loop.py run --runner codex --max-iterations 8
python3 scripts/agent_loop.py run --runner claude --max-iterations 8
python3 scripts/agent_loop.py sync-doc-cn
```

## Loop Shape

Each iteration follows:

1. `doctor`
2. select the next eligible task from `tasks.json`
3. run the orchestrator prompt
4. run the implementer prompt
5. run the verifier prompt
6. update task state
7. commit verified work if Git is ready
8. trigger doc gardening when required

## Halt Conditions

- `doctor` fails
- `.agent/PAUSE` exists
- no eligible tasks remain
- schema validation fails
- verifier retry ceiling is hit
- max iterations are exhausted

## State Files

- `tasks.json`: task graph and retry ledger
- `.agent/config.json`: runtime policy
- `.agent/lock.json`: active-loop lock
- `.agent/history/*.jsonl`: iteration logs

## Safety Notes

- The harness must never mutate `tasks.json` without recording timestamps and last results.
- The harness must never continue when root Git is invalid.
- The harness should prefer deterministic task selection over free-form prioritization.
