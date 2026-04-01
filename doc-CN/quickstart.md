# 快速入门 (Quickstart)

本文档旨在指导新开发者在本地环境中快速搭建并运行 Easy Engine。

## 1. 运行环境要求

在开始之前，请确保您的本地机器已安装以下软件：

- **Java**: 1.8 (Java 8)
- **Node.js**: >= 18.0.0 (推荐使用最新的 LTS 版本)
- **Maven**: 3.6+
- **Docker & Docker Compose**: 用于启动 MySQL 和 Redis 等基础设施
- **Git**: 用于代码版本管理

## 2. 启动基础设施

Easy Engine 使用 Docker Compose 来管理共享的中间件。

```bash
# 启动 MySQL (元数据库) 和 Redis (缓存)
docker compose up -d mysql redis
```

> [!NOTE]
> - **MySQL 端口**: `3307` (主机端口) 映射到容器内的 `3306`。
> - **默认数据库**: `engine_db`
> - **默认账号/密码**: `engine` / `engine123`
> - **Redis 端口**: `6380` (主机端口) 映射到容器内的 `6379`。

## 3. 数据库初始化与配置

### 数据库表创建
Easy Engine 使用 **Flyway** 进行自动化的数据库迁移。
- **无需手动执行 SQL 脚本**: 当后端服务（如 `manager` 或 `benchmark`）启动时，它们会自动检测并应用 `src/main/resources/db/migration` 下的 `.sql` 脚本。
- **查看脚本**: 您可以在各个模块的 `src/main/resources/db/migration` 目录下找到 DDL 和种子数据脚本。

### 3.1 MySQL 表结构概览
系统启动后，Flyway 会在 `engine_db` 中创建以下主要表结构，按模块分类如下：

#### Manager (管理模块)
- `query_datasource_config`: 存储各个查询引擎（如 Kylin, Presto）的连接配置。
- `sql_execution_record`: 存储从 Query 服务摄取的原始 SQL 执行轨迹（Traces）。
- `sql_pattern_stats`: 存储经过脱敏和聚合后的 SQL 特征统计信息。
- `acceleration_table`: 存储建议或已创建的加速表（物化视图）元数据。

#### Benchmark (基准测试模块)
- `benchmark_data_source`: 用于测试执行的目标数据源连接信息。
- `benchmark_job`: 定义基准测试作业（包含执行策略、频率等）。
- `benchmark_run`: 记录每次作业运行的详细历史、耗时及结果报告集。
- `benchmark_sql_template`: 存储可复用的测试 SQL 模板。
- `benchmark_test_set`: 将多个 SQL 模板组合成逻辑测试集。
- `benchmark_test_set_item`: 测试集与 SQL 模板的多对多映射关系。

### 修改配置
如果您需要修改数据库连接、端口或其他配置，可以通过以下两种方式：

1.  **修改环境变量**:
    大部分配置支持通过环境变量覆盖，例如：
    - `MYSQL_HOST`: 数据库地址 (默认 `localhost`)
    - `MYSQL_PORT`: 数据库端口 (默认 `3307`)
    - `SERVER_PORT`: 服务启动端口

2.  **修改配置文件**:
    直接编辑各模块下的 `src/main/resources/application.yml`。

## 4. 启动后端服务

按照以下顺序启动三个核心服务：

1.  **启动 Query (执行服务)**:
    ```bash
    cd query
    mvn spring-boot:run
    ```
    - 端口: `8092`

2.  **启动 Manager (控制台 API)**:
    ```bash
    cd manager
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
    ```
    - 端口: `8090`

3.  **启动 Benchmark (测试编排服务)**:
    ```bash
    cd benchmark
    mvn spring-boot:run
    ```
    - 端口: `8091`

## 5. 启动前端页面

针对您的开发环境（是否有外网或私服），可以选择以下两种方式之一启动前端。

### 方案 1：实时编译模式（推荐，适合有网络环境）
这种方式支持热更新（HMR），适合前端逻辑开发。

```bash
# 启动 Manager 控制台
npm --prefix manager/frontend install
npm --prefix manager/frontend run dev

# 启动 Benchmark 管理后台
npm --prefix benchmark/frontend install
npm --prefix benchmark/frontend run dev
```
- **访问地址**: Manager (http://localhost:5173), Benchmark (http://localhost:5174)

### 方案 2：预编译托管模式（适合内网/隔离环境）
如果您在内网环境无法运行 `npm install`，可以使用此方案。由 Spring Boot 后端直接提供前端静态资源服务。

1.  **在外网环境构建**：执行 `npm run build` 生成 `dist` 目录。
2.  **拷贝至内网**：将 `dist` 目录下的所有文件拷贝到后端的静态资源目录：
    - `manager/frontend/dist/*` -> `manager/src/main/resources/static/`
    - `benchmark/frontend/dist/*` -> `benchmark/src/main/resources/static/`
3.  **启动后端**：正常执行 `mvn spring-boot:run`。
4.  **访问地址**：直接访问后端端口即可看到 UI。
    - **Manager**: http://localhost:8090/
    - **Benchmark**: http://localhost:8091/

> [!NOTE]
> 在方案 2 下，前后端共用同一个端口，无需处理跨域问题，系统启动更加轻量。

## 6. 后续步骤：配置数据源

系统启动后，您通常需要添加实际的计算引擎（如 Kylin 或 Presto）作为数据源：

1.  **访问 UI**: 打开浏览器访问 Benchmark UI (默认 `http://localhost:5174`)。
2.  **上传驱动**: 在 "Data Sources" -> "Upload Driver" 页面上传对应的 JDBC 驱动 JAR 包。
3.  **添加数据源**: 在 "Data Sources" 页面配置您的引擎连接信息。
4.  **验证连接**: 使用 UI 提供的测试工具确保连接畅通。

---

> [!TIP]
> 更多详细信息请参考 [本地开发指南](local-development.md) 或 [架构概览](architecture-overview.md)。
