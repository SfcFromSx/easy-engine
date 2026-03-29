# Easy Engine

Easy Engine is an agent-first workspace for query execution, control-plane analysis, benchmark orchestration, and the `kylin-jdbc-cache` adapter. The repository is organized so autonomous coding agents can operate safely over long-running optimization loops while humans retain clear pause, review, and merge control.

## Modules

- `query/`: query-only execution service with routing, caching, and trace publishing.
- `manager/`: control-plane service for trace ingestion, SQL pattern analysis, and acceleration metadata.
- `benchmark/`: benchmark service and UI for datasource, template, test-set, and run management.
- `kylin-jdbc-cache/`: cached JDBC adapter used by benchmark and other clients.

## Canonical Documentation

- [AGENTS.md](/Users/sfc/Documents/projects/engine/AGENTS.md): short repo map and operating contract for agents.
- [HUMAN.MD](/Users/sfc/Documents/projects/engine/HUMAN.MD): human guardrails for agent-first operation.
- [docs/README.md](/Users/sfc/Documents/projects/engine/docs/README.md): full documentation index.
- [docs/architecture/README.md](/Users/sfc/Documents/projects/engine/docs/architecture/README.md): architecture navigation.
- [docs/operations/README.md](/Users/sfc/Documents/projects/engine/docs/operations/README.md): runbooks and development policy.
- [docs/agent/README.md](/Users/sfc/Documents/projects/engine/docs/agent/README.md): canonical prompt and schema assets.

## Autonomous Loop

The autonomous harness is driven from [scripts/agent_loop.py](/Users/sfc/Documents/projects/engine/scripts/agent_loop.py).

Common commands:

```bash
python3 scripts/agent_loop.py doctor
python3 scripts/agent_loop.py smoke-runner --runner codex
python3 scripts/agent_loop.py step --runner codex
python3 scripts/agent_loop.py run --runner codex --max-iterations 1
python3 scripts/agent_loop.py run --runner claude --max-iterations 8
python3 scripts/agent_loop.py sync-doc-cn
```

The loop refuses to enter full autonomous mode until:

- the repository root is a valid Git repository,
- any nested repository metadata is resolved or explicitly archived outside active module paths,
- the required CLI runners are available,
- machine-state files validate successfully.

Codex is configured as a best-effort production runner in this repository: it is pinned to `gpt-5.4` with high reasoning, and the harness applies retries and backoff for transient provider or network failures.

## Chinese Mirrors

English is canonical. Selected human-facing mirrors live under [doc-CN/](/Users/sfc/Documents/projects/engine/doc-CN/).

## Legacy Docs

There is no supported legacy documentation tree anymore. Use `docs/` for canonical English docs and `doc-CN/` for the selective Chinese mirror set.
