<!-- MIRROR: docs/operations/local-development.md | SOURCE_SHA256: 3b4967f7c5ed | SYNCED_AT: 2026-03-31T10:19:40Z -->

# 本地开发

本文档描述 Easy Engine 的默认本地开发环境。

## 基础设施

先启动共享基础设施：

```bash
docker compose up -d mysql redis
```

如果本地还需要 OLAP 引擎：

```bash
docker compose --profile olap up -d presto kylin
```

## 服务启动顺序

1. 启动 `query`：

```bash
cd /Users/sfc/Documents/projects/engine/query
mvn spring-boot:run
```

2. 启动 `manager`：

```bash
cd /Users/sfc/Documents/projects/engine/manager
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

3. 启动 `benchmark`：

```bash
cd /Users/sfc/Documents/projects/engine/benchmark
mvn spring-boot:run
```

4. 按需启动前端：

```bash
npm --prefix manager/frontend run dev -- --host 127.0.0.1 --port 4173
npm --prefix benchmark/frontend run dev
```

## 操作说明

- benchmark 是执行 benchmark run、预检和结构化运行报告时首选的控制界面。
- benchmark 通过标准 Apache Kylin JDBC 驱动连接到 `query`（`jdbc:kylin://localhost:8092/<project>`）。额外的 JDBC 驱动 JAR 可通过 Benchmark UI 中的 Data Sources > Upload Driver 上传。
- 默认元数据库改为 MySQL，监听 `localhost:3307`，本地默认账号仍为 `engine` / `engine123`。
- SQL 中的保留元数据注释（例如 `YH_TARGET_ENGINE`）用于在 `query` 内部将请求路由到指定后端。
- 如果本地修改了已经应用过的 migration，导致 Flyway 报 checksum mismatch，需要显式修复并重新迁移：

```bash
mvn -f benchmark/pom.xml compile flyway:repair flyway:migrate
```

## 默认端口

- `manager`: `8090`
- `benchmark`: `8091`
- `query`: `8092`
- Redis: `6380`
- MySQL: `3307`
- Kylin: `17070`
- Presto: `18081`
