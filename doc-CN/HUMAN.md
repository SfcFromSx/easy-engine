<!-- MIRROR: HUMAN.MD | SOURCE_SHA256: f4d4dd9d42ec | SYNCED_AT: 2026-03-29T15:13:46Z -->

# HUMAN 操作规则

这个仓库被设计为以自治 agent 驱动开发为主。人类仍然负责目标、审批和恢复，但不应进行会破坏 agent 连续性的临时修改。

## 允许的人类操作

- 通过 [tasks.json](/Users/sfc/Documents/projects/engine/tasks.json) 和 [docs/exec-plans/](/Users/sfc/Documents/projects/engine/docs/exec-plans/) 下的计划文档调整目标与优先级。
- 使用 `.agent/PAUSE` 暂停或恢复自治循环。
- 提供凭据、环境变量和基础设施。
- 审查 diff、运行额外验证、批准合并，并归档已完成计划。
- 解决 Git 拓扑问题，例如初始化根仓库或清理嵌套仓库。

## 活跃循环期间禁止的事项

在循环运行期间，不要执行以下操作：

- 在当前任务范围之外手工修改代码。
- 改写分支历史或强制推送。
- 临时移动文件或重命名目录。
- 直接修改 `.agent/config.json`、`.agent/history/`、`.agent/lock.json` 或 prompt 模板。
- 修改 `docs/generated/` 下的生成文档，除非循环已经暂停。
- 静默删除任务文件、schema 文件或验证脚本。

## 手工修改前先暂停

如果人类需要修改代码、prompt、任务状态或仓库布局：

1. 创建 `.agent/PAUSE`。
2. 等待当前循环停止。
3. 完成手工修改。
4. 在相关计划或 backlog 文档中记录原因。
5. 仅在工作区恢复稳定后再移除 `.agent/PAUSE`。

## 紧急恢复

### 工作树不干净

1. 暂停循环。
2. 检查未提交变更。
3. 决定保留、回滚，或将其转入新任务。
4. 仅在 `doctor` 通过后恢复。

### 验证失败

1. 如果失败重复出现，先暂停循环。
2. 保留 `.agent/history/` 中的日志。
3. 新增或更新任务，并附上失败证据。
4. 在故障模式被记录后再恢复。

### Git 冲突或拓扑漂移

1. 暂停循环。
2. 修复仓库根目录的 Git 状态。
3. 在恢复自治工作之前，审查已归档或新出现的嵌套仓库元数据。
4. 重新运行 `python3 scripts/agent_loop.py doctor`。

## 有意的约束

- 英文文档是规范版本。
- 中文镜像仅用于方便人类阅读。
- agent 必须始终能够从 `docs/`、`tasks.json` 和 `.agent/config.json` 恢复上下文。
- 人类便利性不能凌驾于 agent 的连续性之上。
