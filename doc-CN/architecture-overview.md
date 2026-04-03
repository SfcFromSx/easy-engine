<!-- MIRROR: docs/architecture/overview.md | SOURCE_SHA256: 93ff3ad74308e27e2ab59e6921276fbc856481292972be9dd4d199984d0af204 | SYNCED_AT: 2026-04-02T14:35:00Z -->

# 架构概览

Easy Engine 由三个相互协作的模块组成：

1. `query`：查询执行服务，也是执行语义的事实来源。
2. `manager`：控制面服务，管理数据源配置、加速表和轨迹历史。
3. `benchmark`：基准测试编排服务与 UI。

代码仓库中还新增了一个共享的进程内模块 `analyze`，但它不是第四个运行时服务。`query` 和 `manager` 都通过它复用 SQL 注释解析、路由改写分析和 Calcite 结构分析能力。

标准的 benchmark 端到端链路如下：

```text
benchmark (Kylin JDBC) -> query -> Kylin / Presto / Hive
                               |-> 写入 MySQL 中的 SqlExecutionRecord
manager <-- 读取 MySQL，并管理数据源配置
```

## 架构边界

- `query` 通过标准 Apache Kylin JDBC 协议接收查询请求，借助共享的 `analyze` 模块解析注释和保留元数据，路由到配置后的后端（Kylin、Presto、Hive），读写 Redis 结果缓存，执行只读 SQL，并将轨迹记录直接写入 MySQL。
- `manager` 拥有数据源配置（通过 `/api/v1/query-datasources` 和 `/api/v1/query-routing-context` 暴露），从 MySQL 读取轨迹记录，借助共享的 `analyze` 模块解析 SQL，维护模式统计，并暴露控制面 API 供加速元数据使用。
- `benchmark` 管理 benchmark 数据源、模板、测试集和运行。它通过标准 Apache Kylin JDBC 驱动连接到 `query`，并支持在 UI 中上传任意 JDBC 驱动 JAR 供数据源测试使用。

## 数据流

1. `benchmark`（或任意 JDBC 客户端）通过 `jdbc:kylin://query-host:8092/<project>` 连接。
2. `query` 解析注释和保留元数据，例如 `ENGINE`。
3. `query` 从 `manager` 拉取缓存的路由上下文（`GET /api/v1/query-routing-context`），其中包含数据源定义和已启用的加速规则。
4. `query` 会先按 `YH_RPTID` 查 Redis 覆盖 `ENGINE`，否则直接使用 `ENGINE`；如果仍然没有可用路由值，则回退到默认数据源，并把最终执行 SQL 规范化为带有前置 `ENGINE` 注释的形式。
5. `query` 要么从 Redis 返回缓存命中，要么对选定数据源执行 SQL。
6. `query` 将 `SqlExecutionRecord` 直接写入 MySQL，其中包含 `executionMode`；对失败的预编译执行，还会写入可读的 `parameterPayload`。
7. `manager` 从 MySQL 读取 `SqlExecutionRecord`，通过 `/api/v1/traces` 暴露轨迹记录，并通过共享的 `analyze` 模块完成 SQL 解析与模式统计更新。

## 运行时默认值

- `manager`: `8090`
- `benchmark`: `8091`
- `query`: `8092`
- Redis: `6380`
- MySQL: `3307`

## 所有权规则

- `query` 是执行语义的唯一所有者：路由、缓存、SQL 改写和轨迹写入都由它负责。
- `manager` 是数据源配置和控制面状态的唯一所有者。
- `benchmark` 不拥有生产查询执行语义。
- `analyze` 只是共享库，不拥有独立运行时状态，也不暴露网络接口。
- 英文文档是规范版本；中文镜像只是精选的便利性产物。
