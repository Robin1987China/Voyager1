# Flyway 迁移目录

Voyager1 schema 采用「V1 基线 + 增量迁移」策略：`V1__init.sql` 包含全量建表 + 索引（表名/列名均为 Voyager1 新命名：领域前缀 + 大写下划线），后续演进通过 `V2__xxx`、`V3__xxx` 增量迁移。

当前迁移一览：

| 版本 | 内容 |
|---|---|
| `V1__init.sql` | 全量建表基线 |
| `V2__environment_cicd.sql` | 环境化 CI/CD：环境策略字段（type/strategy/approvalRequired）+ CI_BUILD.environment + ENVIRONMENT_TARGET 表 |
| `V3__application.sql` | 应用管理：APPLICATION 表 |

## 跨库兼容约定（H2/MySQL/MariaDB/PostgreSQL 均可执行）

1. 标识符**不加引号**（反引号仅 MySQL 系支持，PostgreSQL 非法）；
2. **不使用 `ADD COLUMN IF NOT EXISTS`**（MySQL 8 不支持）。项目 `hbm2ddl.auto=none`，列只能由 Flyway 创建，重复添加不会发生；
3. 新增迁移后，需至少在默认 H2 上启动验证一次；涉及方言特性时需在 MySQL/PG profile 下各验证一次。

## 新命名约定

| 前缀 | 领域 | 示例 |
|---|---|---|
| `SYS_` | 系统（用户/工作空间/证书/权限/参数） | `SYS_USER`、`SYS_CERTIFICATE` |
| `INFRA_` | 基础设施（机器/节点/SSH/Docker） | `INFRA_MACHINE`、`INFRA_SSH` |
| `CI_` | 构建发布（构建/仓库/版本/Pipeline） | `CI_BUILD`、`CI_REPOSITORY` |
| `OPS_` | 运维（分发/监控/脚本/命令） | `OPS_MONITOR`、`OPS_SCRIPT` |
| `STORAGE_` | 文件存储 | `STORAGE_FILE`、`STORAGE_STATIC_FILE` |

## 职责划分

- **schema 唯一权威是 Flyway**（`db/migration`）。Hibernate 的 `hbm2ddl.auto=none`，不参与建表/校验。
- `sql-view/*.csv` 仅作为生成 `V1__init.sql` 的**原料**（历史遗留，逐步废弃），运行时不再被读取建表。

## 执行顺序

Flyway 通过 `FlywayRunner`（实现 `ILoadEvent`，`getOrder() = HIGHEST_PRECEDENCE + 1`）手动编排：

1. `FlywayRunner`（`HIGHEST_PRECEDENCE + 1`）执行全部未应用的迁移建表/变更；
2. `DataInitEvent`（`HIGHEST_PRECEDENCE + 2`）触发 `statusRecover` 等业务初始化，此时表结构已就绪。

> 若 Flyway 晚于业务初始化执行，会报 `Table "XXX" not found`。

## 演进规则

- `V1__init.sql` **一旦在某环境落地就不可再修改**，只能新增 `V2__xxx`、`V3__xxx` ...（SQL 或 Java 迁移均可）。
- 因尚未发布，若需要调整未发布的迁移，直接改对应文件并**清空本地 dev 库重建**（删除数据目录下的 H2 文件，或 `flyway repair` 修复校验和）。

## 测试库注意事项

持久化测试库（`$TMPDIR/voyager1-test-data`）跨运行复用。若修改了 V1 后遇到 Flyway 校验失败（checksum mismatch），说明测试库仍残留旧迁移记录，删除对应 H2 数据文件重建即可。
