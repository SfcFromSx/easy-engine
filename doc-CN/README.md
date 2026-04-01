<!-- MIRROR: README.md | SOURCE_SHA256: 50c084f4d761 | SYNCED_AT: 2026-03-31T08:06:10Z -->

# Easy Engine

Easy Engine 是一个以 agent 为中心的工程工作区，覆盖查询执行、控制面分析和基准测试编排。

## 模块

- `query/`：只负责查询执行的服务，包含路由、缓存和轨迹发布。
- `manager/`：控制面服务，负责轨迹摄取、SQL 模式分析和加速元数据。
- `benchmark/`：基准测试服务与 UI，负责数据源、模板、测试集和运行管理。

## 规范文档入口

- [AGENTS.md](/Users/sfc/Documents/projects/engine/AGENTS.md)：面向 agent 的简要仓库地图和操作契约。
- [tasks.md](/Users/sfc/Documents/projects/engine/tasks.md)：规范任务台账。
- [INBOX.md](/Users/sfc/Documents/projects/engine/INBOX.md)：等待人类决定的 issue 与建议收件箱。
- [docs/README.md](/Users/sfc/Documents/projects/engine/docs/README.md)：完整英文文档索引。
- [docs/architecture/README.md](/Users/sfc/Documents/projects/engine/docs/architecture/README.md)：架构导航。
- [docs/operations/README.md](/Users/sfc/Documents/projects/engine/docs/operations/README.md)：运行手册与开发策略。
- [docs/operations/human-collaboration.md](/Users/sfc/Documents/projects/engine/docs/operations/human-collaboration.md)：详细的人类协作规则。
- [docs/agent/README.md](/Users/sfc/Documents/projects/engine/docs/agent/README.md)：规范 prompt 与 schema 资产。

## 任务驱动工作流

任务由 [tasks.md](/Users/sfc/Documents/projects/engine/tasks.md) 管理。监工模型读取任务，在人类指定任务后，直接在当前会话内完成工作，并将进展写回 `tasks.md`。

详见 [AGENTS.md](/Users/sfc/Documents/projects/engine/AGENTS.md)。

## 给人类的提示

- 在 [tasks.md](/Users/sfc/Documents/projects/engine/tasks.md) 里分配或调整任务优先级。
- 直接告诉监工停止或继续，不再依赖单独的暂停文件。
- 在推送、合并或发布前先审查 diff 并补充验证。
- 在 [INBOX.md](/Users/sfc/Documents/projects/engine/INBOX.md) 查看 agent 发现但需要人类判断的问题与建议。

## 关于入门 (Getting Started)

- [doc-CN/quickstart.md](/Users/sfc/Documents/projects/engine/doc-CN/quickstart.md)：快速本地环境搭建与服务启动。

## 中文镜像

英文文档是规范版本。面向人类的精选中文镜像保存在 [doc-CN/](/Users/sfc/Documents/projects/engine/doc-CN/)。

## 旧文档

仓库中不再支持旧的文档树。规范英文文档位于 `docs/`，精选中文镜像位于 `doc-CN/`。
