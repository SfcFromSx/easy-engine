<!-- MIRROR: docs/architecture/overview.md | SOURCE_SHA256: 10b937d347a5 | SYNCED_AT: 2026-03-29T15:13:46Z -->

# 架构概览

Easy Engine 由四个相互协作的模块组成：

1. `query`：只负责查询执行的服务。
2. `manager`：控制面服务。
3. `benchmark`：基准测试编排服务与 UI。
4. `kylin-jdbc-cache`：可位于 `query` 前方的 JDBC 适配器。

标准的 benchmark 端到端链路如下：

```text
benchmark -> kylin-jdbc-cache -> query -> Kylin or Presto
```

## 架构边界

- `query` 接收查询请求，解析 hint 和保留元数据，执行路由，读写缓存，执行只读 SQL，并发送轨迹负载。
- `manager` 消费轨迹负载，将历史持久化，使用 Calcite 解析 SQL，维护模式统计，并暴露控制面 API 供加速元数据使用。
- `benchmark` 管理 benchmark 数据源、模板、测试集和运行。它不负责生产查询执行语义。
- `kylin-jdbc-cache` 是适配层，不是独立的控制面或查询服务器。

## 数据流

1. 客户端或 benchmark run 通过 `kylin-jdbc-cache` 或直接向 `query` 发起查询。
2. `query` 解析注释和保留元数据，例如 `YH_TARGET_ENGINE`。
3. `query` 优先使用保留的路由元数据，其次使用驱动风格的 engine hint，最后回退到默认数据源。
4. `query` 要么从 Redis 返回缓存命中，要么对选定数据源执行 SQL。
5. `query` 将轨迹负载发布到 Redis。
6. `manager` 消费该轨迹负载，将其存入 PostgreSQL，使用 Calcite 解析 SQL，并更新模式统计。

## 运行时默认值

- `manager`: `8090`
- `benchmark`: `8091`
- `query`: `8092`
- Redis: `6380`
- PostgreSQL: `5433`

## 兼容性规则

- `query` 的路由、缓存语义和轨迹负载必须与 `kylin-jdbc-cache` 保持一致。
- `manager` 必须对当前轨迹负载契约保持向后兼容。
- Job server 的职责属于 `manager`；本仓库中不存在单独的 job server。
- 英文文档是规范版本；中文镜像只是精选的便利性产物。
