# Agent Loop Runbook

The autonomous loop is driven by [scripts/agent_loop.py](/Users/sfc/Documents/projects/engine/scripts/agent_loop.py).

## Commands

```bash
python3 scripts/agent_loop.py doctor
python3 scripts/agent_loop.py smoke-runner --runner codex
python3 scripts/agent_loop.py step --runner codex
python3 scripts/agent_loop.py run --runner codex
python3 scripts/agent_loop.py run --runner codex --max-iterations 8
python3 scripts/agent_loop.py run --runner claude --max-iterations 8
python3 scripts/agent_loop.py sync-doc-cn
```

## Loop Shape

`step` is the single-task primitive. `run` is the default long-lived mode and keeps draining the next eligible `todo` task until the queue is empty, `.agent/PAUSE` exists, or a hard failure occurs. An explicit `--max-iterations` value acts as a temporary cap for debugging.

Each successful polling cycle follows:

1. `doctor`
2. select the next eligible task from `tasks.json`
3. run the orchestrator prompt
4. run the implementer prompt
5. run the verifier prompt
6. update task state
7. commit verified work if Git is ready
8. push the current branch when auto-push is enabled
9. run best-effort local refresh commands for affected modules
10. confirm affected local services and frontends are healthy, starting configured frontend dev servers when needed
11. trigger doc gardening when required

If another valid loop currently holds `.agent/lock.json`, `run` waits and polls using the configured `lock_poll_seconds` interval instead of failing immediately.

## Halt Conditions

- `doctor` fails
- `.agent/PAUSE` exists
- no eligible tasks remain
- schema validation fails
- verifier retry ceiling is hit
- a stray `agent_loop.py` process is detected while `.agent/lock.json` is unlocked
- `--max-iterations` is exhausted when that cap is provided

## State Files

- `tasks.json`: task graph and retry ledger
- `.agent/config.json`: runtime policy
- `.agent/lock.json`: active-loop lock
- `.agent/history/*.jsonl`: iteration logs
- `.agent/runtime/quarantine/`: parked patch bundles and manifests for interrupted mixed task work

## Safety Notes

- The harness must never mutate `tasks.json` without recording timestamps and last results.
- The harness must never continue when root Git is invalid.
- The harness should prefer deterministic task selection over free-form prioritization.
- Runner timeouts and runner logs under `.agent/runtime/runner-logs/` should make stalled model calls diagnosable instead of silent.
- Codex runs are launched through `scripts/codex_harness.py`, which builds an isolated local Codex home for project runs, strips inherited plugin and MCP configuration, and pins Codex to `gpt-5.4` with high reasoning via harness config overrides.
- Codex currently runs in best-effort mode for production work on unstable networks: long stage timeouts, automatic retries, backoff, and explicit runner logs are enabled by default.
- For Codex on unstable provider paths, prefer fewer but longer attempts over many short retries. The default profile uses long single-stage windows before giving up.
- The Codex verifier stage uses a longer timeout window than orchestrator and implementer, because the verification payload is larger and provider latency is higher in practice.
- When `auto_push_after_commit` is enabled, successful task commits are pushed to the configured remote immediately after the task is marked `done`.
- After successful task completion, the harness can run best-effort local refresh commands for affected modules so local compiled/backend/frontend artifacts stay close to the newest committed logic.
- For changed frontend modules, the harness should also verify the local dev surface is available and start the configured frontend dev server if it is missing.
- When the todo queue drops to the configured warning threshold, the harness emits a warning with the remaining task IDs so humans can decide whether to add more work.
- When a mixed task worktree must be parked before harness maintenance, capture the patches and manifest under `.agent/runtime/quarantine/` before restoring the repo to a clean `HEAD`.
- `doctor` and `run` now treat an unlocked `.agent/lock.json` plus another live `agent_loop.py` process as a recovery error. Resolve or quarantine that stale process state before starting a new loop.
- Prefer `smoke-runner` and `step` for debugging one stage in isolation. For normal autonomous work, `run --runner codex` is the supported default.
