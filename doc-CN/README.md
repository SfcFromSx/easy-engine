<!-- MIRROR: README.md | SOURCE_SHA256: cc6fab69c2fc | SYNCED_AT: 2026-03-29T15:02:11Z -->

# Easy Engine

Easy Engine 是一个以 agent 为主的一体化工程工作区，包含查询执行、控制面分析、压测编排以及 `kylin-jdbc-cache` 适配层。英文文档是唯一规范版本，当前目录只保留少量面向人类的中文镜像，方便阅读和交接。

## 模块

- `query/`：查询执行服务。
- `manager/`：控制面服务。
- `benchmark/`：压测服务与前端。
- `kylin-jdbc-cache/`：缓存 JDBC 适配器。

## 关键入口

- [AGENTS.md](/Users/sfc/Documents/projects/engine/AGENTS.md)
- [HUMAN.MD](/Users/sfc/Documents/projects/engine/HUMAN.MD)
- [docs/README.md](/Users/sfc/Documents/projects/engine/docs/README.md)
- [scripts/agent_loop.py](/Users/sfc/Documents/projects/engine/scripts/agent_loop.py)

## 自循环命令

```bash
python3 scripts/agent_loop.py doctor
python3 scripts/agent_loop.py step --runner codex
python3 scripts/agent_loop.py run --runner codex --max-iterations 8
python3 scripts/agent_loop.py run --runner claude --max-iterations 8
python3 scripts/agent_loop.py sync-doc-cn
```

## 说明

- 英文文档为准。
- 规范英文文档只保留在 `docs/` 中。
