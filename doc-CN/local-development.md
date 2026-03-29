<!-- MIRROR: docs/operations/local-development.md | SOURCE_SHA256: 1dc9c2f04df7 | SYNCED_AT: 2026-03-29T15:13:46Z -->

# 本地开发

本文档描述 Easy Engine 的默认本地开发环境。

## 基础设施

先启动共享基础设施：

```bash
docker compose up -d postgres redis
```

如果本地还需要 OLAP 引擎：

```bash
docker compose --profile olap up -d presto kylin
```

## 服务启动顺序

1. 当 benchmark 依赖缓存 JDBC 适配器时，先安装该适配器：

```bash
mvn -f kylin-jdbc-cache/pom.xml install
```

2. 启动 `query`：

```bash
cd /Users/sfc/Documents/projects/engine/query
mvn spring-boot:run
```

3. 启动 `manager`：

```bash
cd /Users/sfc/Documents/projects/engine/manager
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

4. 启动 `benchmark`：

```bash
cd /Users/sfc/Documents/projects/engine/benchmark
mvn spring-boot:run
```

5. 按需启动前端：

```bash
npm --prefix manager/frontend run dev -- --host 127.0.0.1 --port 4173
npm --prefix benchmark/frontend run dev
```

## 操作说明

- benchmark 是执行 benchmark run、预检和结构化运行报告时首选的控制界面。
- benchmark 路径在常规作业执行时会禁用驱动侧路由，因此需要为 `query` 路由场景保留元数据注释。
- 如果本地修改了已经应用过的 migration，导致 Flyway 报 checksum mismatch，需要显式修复并重新迁移：

```bash
mvn -f benchmark/pom.xml compile flyway:repair flyway:migrate
```

## 默认端口

- `manager`: `8090`
- `benchmark`: `8091`
- `query`: `8092`
- Redis: `6380`
- PostgreSQL: `5433`
- Kylin: `17070`
- Presto: `18081`
