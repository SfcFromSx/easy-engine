<!-- MIRROR: docs/operations/local-development.md | SOURCE_SHA256: 914f8b6d8b4278126d3727078a71409f8a8c128bab7e773c17d365ba521f0d88 | SYNCED_AT: 2026-04-03T09:10:24Z -->

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

本地 Kylin 镜像会把 engine 和 query Spark master 都固定为 `local[2]`。
这样可以让真实的 Kylin 容器在 benchmark smoke 和 Kylin 专用 E2E 场景里保持可用，
避免被独立容器内嵌 YARN 的 `sparder_on_docker` 启动路径卡住。
Easy Engine 自身的 `query` 服务仍然只暴露 `POST /kylin/api/query`。

## 数据库初始化

在启动 `manager` 或 `benchmark` 之前，需要先显式初始化元数据库 schema：

```bash
bash scripts/init-db.sh dev
```

这个脚本会按所选 profile 的元数据库配置依次执行 `manager` 和
`benchmark` 的 Flyway migration。正常服务启动过程不再自动建表或写入种子数据。

## 服务启动顺序

1. 启动 `query`：

```bash
cd /Users/sfc/Documents/projects/engine/query
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

2. 启动 `manager`：

```bash
cd /Users/sfc/Documents/projects/engine/manager
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

3. 启动 `benchmark`：

```bash
cd /Users/sfc/Documents/projects/engine/benchmark
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

4. 按需启动前端：

```bash
npm --prefix manager/frontend run dev -- --host 127.0.0.1 --port 5173
npm --prefix benchmark/frontend run dev
```

## 操作说明

- benchmark 是执行 benchmark run、预检和结构化运行报告时首选的控制界面。
- benchmark 通过标准 Apache Kylin JDBC 驱动连接到 `query`（`jdbc:kylin://localhost:8092/<project>`）。额外的 JDBC 驱动 JAR 可通过 Benchmark UI 中的 Data Sources > Upload Driver 上传。
- 每个后端模块现在都维护 `application-dev.yml`、`application-test.yml`、`application-pro.yml` 三套配置。本地启动统一用 `dev`，自动化测试统一用 `test`，测试环境容器应设置 `SPRING_PROFILES_ACTIVE=test`，生产环境设置 `SPRING_PROFILES_ACTIVE=pro`。
- 三套 profile YAML（`dev` / `test` / `pro`）现在都以 MySQL 为默认元数据库配置；任何内存 H2 仅保留在 `src/test/resources` 下的测试专用覆盖文件里。
- `manager` 和 `benchmark` 现在默认要求元数据库已提前初始化；如果跳过 `bash scripts/init-db.sh`，服务会因为缺少表而快速失败，而不是在启动阶段直接修改数据库。
- `bash scripts/init-db.sh <profile> ...` 现在只读取模块运行时 classpath，因此 `test` profile 的 schema 初始化会遵循模块 `application-test.yml` 中的 MySQL 配置，而不会误读测试专用 H2 覆盖。
- SQL 中的保留元数据注释（例如 `ENGINE`）用于在 `query` 内部将请求路由到指定后端。
- 如果本地修改了已经应用过的 migration，导致 Flyway 报 checksum mismatch，需要显式修复并重新迁移：

```bash
SPRING_PROFILES_ACTIVE=dev mvn -f manager/pom.xml -Dflyway.url="$MANAGER_DB_JDBC_URL" -Dflyway.user="$MANAGER_DB_USER" -Dflyway.password="$MANAGER_DB_PASSWORD" flyway:repair
SPRING_PROFILES_ACTIVE=dev mvn -f benchmark/pom.xml -Dflyway.url="$BENCHMARK_DB_JDBC_URL" -Dflyway.user="$BENCHMARK_DB_USER" -Dflyway.password="$BENCHMARK_DB_PASSWORD" flyway:repair
bash scripts/init-db.sh dev
```

## 默认端口

- `manager`: `8090`
- `benchmark`: `8091`
- `query`: `8092`
- Redis: `6380`
- MySQL: `3307`
- Kylin: `17070`
- Presto: `18081`
