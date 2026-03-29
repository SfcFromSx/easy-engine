<!-- MIRROR: README.md | SOURCE_SHA256: cc6fab69c2fc | SYNCED_AT: 2026-03-29T15:13:46Z -->

# Easy Engine

Easy Engine 是一个以 agent 为中心的工程工作区，覆盖查询执行、控制面分析、基准测试编排，以及 `kylin-jdbc-cache` 适配层。仓库结构的目标是让自治编码 agent 可以安全地持续运行优化循环，同时仍然保留清晰的人类暂停、审查与合并控制点。

## 模块

- `query/`：只负责查询执行的服务，包含路由、缓存和轨迹发布。
- `manager/`：控制面服务，负责轨迹摄取、SQL 模式分析和加速元数据。
- `benchmark/`：基准测试服务与 UI，负责数据源、模板、测试集和运行管理。
- `kylin-jdbc-cache/`：缓存 JDBC 适配器，被 benchmark 和其他客户端使用。

## 规范文档入口

- [AGENTS.md](/Users/sfc/Documents/projects/engine/AGENTS.md)：面向 agent 的简要仓库地图和操作契约。
- [HUMAN.MD](/Users/sfc/Documents/projects/engine/HUMAN.MD)：面向人类的协作护栏。
- [docs/README.md](/Users/sfc/Documents/projects/engine/docs/README.md)：完整英文文档索引。
- [docs/architecture/README.md](/Users/sfc/Documents/projects/engine/docs/architecture/README.md)：架构导航。
- [docs/operations/README.md](/Users/sfc/Documents/projects/engine/docs/operations/README.md)：运行手册与开发策略。
- [docs/agent/README.md](/Users/sfc/Documents/projects/engine/docs/agent/README.md)：规范 prompt 与 schema 资产。

## 自治循环

自治 harness 由 [scripts/agent_loop.py](/Users/sfc/Documents/projects/engine/scripts/agent_loop.py) 驱动。

常用命令：

```bash
python3 scripts/agent_loop.py doctor
python3 scripts/agent_loop.py smoke-runner --runner codex
python3 scripts/agent_loop.py step --runner codex
python3 scripts/agent_loop.py run --runner codex --max-iterations 1
python3 scripts/agent_loop.py run --runner claude --max-iterations 8
python3 scripts/agent_loop.py sync-doc-cn
```

在满足以下条件前，循环不会进入完整自治模式：

- 仓库根目录是有效的 Git 仓库。
- 所有嵌套仓库元数据都已经处理或显式归档到活跃模块路径之外。
- 必需的 CLI runner 可用。
- 机器状态文件通过校验。

当前仓库将 Codex 配置为尽力而为的生产 runner：模型固定为 `gpt-5.4`，启用高推理强度，并对瞬时的供应商或网络错误执行重试和退避。

## 中文镜像

英文文档是规范版本。面向人类的精选中文镜像保存在 [doc-CN/](/Users/sfc/Documents/projects/engine/doc-CN/)。

## 旧文档

仓库中不再支持旧的文档树。规范英文文档位于 `docs/`，精选中文镜像位于 `doc-CN/`。
