<!-- MIRROR: docs/operations/local-development.md | SOURCE_SHA256: 1dc9c2f04df7 | SYNCED_AT: 2026-03-29T15:02:11Z -->

# 本地开发

当前文档是英文规范文档 [docs/operations/local-development.md](/Users/sfc/Documents/projects/engine/docs/operations/local-development.md) 的中文镜像摘要。

## 启动顺序

1. `docker compose up -d postgres redis`
2. 如需 Kylin / Presto：`docker compose --profile olap up -d presto kylin`
3. `mvn -f kylin-jdbc-cache/pom.xml install`
4. 依次启动 `query`、`manager`、`benchmark`
5. 需要时启动前端

## 默认端口

- `manager`: `8090`
- `benchmark`: `8091`
- `query`: `8092`
- Redis: `6380`
- PostgreSQL: `5433`

## 说明

- benchmark 的标准路径是 `benchmark -> kylin-jdbc-cache -> query -> Kylin/Presto`
- 需要 query 侧路由时，优先使用 `YH_TARGET_ENGINE` 这类保留元数据
