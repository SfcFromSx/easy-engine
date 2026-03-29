<!-- MIRROR: docs/architecture/overview.md | SOURCE_SHA256: 10b937d347a5 | SYNCED_AT: 2026-03-29T15:02:11Z -->

# 架构概览

Easy Engine 由四个模块组成：

1. `query`：只负责查询执行。
2. `manager`：控制面。
3. `benchmark`：压测编排与前端。
4. `kylin-jdbc-cache`：JDBC 适配器。

## 标准链路

```text
benchmark -> kylin-jdbc-cache -> query -> Kylin 或 Presto
```

## 边界

- `query` 负责查询、路由、缓存和轨迹上报。
- `manager` 负责轨迹消费、SQL 解析、模式统计和加速元数据。
- `benchmark` 负责数据源、模板、测试集和运行管理。
- `kylin-jdbc-cache` 不是独立服务，而是客户端侧适配层。
