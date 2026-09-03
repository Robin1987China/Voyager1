# Flyway 迁移目录（Phase 1/2）

- 已存在的 schema 通过 `baselineOnMigrate` 基线化为版本 `1`（不重建表）。
- 后续新增/修改表结构使用 `V2__描述`、`V3__描述` ... 命名（SQL 或 Java 迁移均可）。
- 旧 `sql-view/*.csv`（InitDb CSV 建表）在 Phase 2 数据模型重构时逐步废弃，由这里的迁移脚本取代。

## Phase 2 新命名约定

领域前缀 + 大写下划线（与原表名区分，作为原创 schema 设计）：

| 前缀 | 领域 | 示例 |
|---|---|---|
| `SYS_` | 系统（用户/工作空间/证书/权限/参数） | `SYS_USER`、`SYS_CERTIFICATE` |
| `INFRA_` | 基础设施（机器/节点/SSH/Docker） | `INFRA_MACHINE`、`INFRA_SSH` |
| `CI_` | 构建发布（构建/仓库/版本/Pipeline） | `CI_BUILD`、`CI_REPOSITORY` |
| `OPS_` | 运维（分发/监控/脚本/命令） | `OPS_MONITOR`、`OPS_SCRIPT` |
| `STORAGE_` | 文件存储 | `STORAGE_FILE` |

已迁移（Phase 2）：
- `CERTIFICATE_INFO` → `SYS_CERTIFICATE`（V2，Java 迁移，见 `db.migration.V2__rename_sys_certificate`）。
- 其余 38 张承继表批量重命名（V3，见 `db.migration.V3__rename_core_tables`，由 `script/rename-tables.mjs` 依据 `script/table-rename-map.json` 生成）。
- 存量库残留列名 `PEGASUS*` → `VOYAGER1*`（V4，见 `db.migration.V4__rename_pegasus_columns`，修复集群/节点心跳 `voyager1Version` 等列不存在）。

## 执行顺序（重要）

Flyway 通过 `FlywayRunner`（实现 `ILoadEvent`，`getOrder() = HIGHEST_PRECEDENCE + 1`）手动编排，保证：

1. `InitDb`（`HIGHEST_PRECEDENCE`）用 CSV 建表（迁移后的表已按新名建）；
2. `FlywayRunner`（`HIGHEST_PRECEDENCE + 1`）执行 V2+ 迁移；
3. `DataInitEvent`（`HIGHEST_PRECEDENCE + 2`）触发 `statusRecover` 等业务初始化，此时模型 `@TableName` 已指向新表名。

> 若 Flyway 晚于业务初始化执行，会报 `Table "SYS_CERTIFICATE" not found`（业务用新表名、但表尚未改名）。

## 单表重命名迁移配方（Phase 2 逐表复用）

每迁移一张表，三步联动：

1. **改 CSV**：`sql-view/table.all.v1.0.csv` 首列旧表名 → 新表名（InitDb 后续在全新库上直接建新名）。
2. **加 Java 迁移**：`db.migration.V(n)__rename_<xxx>.java`，条件重命名——
   - 旧表存在 → 删除 InitDb 刚建的空白新表（如有）→ `ALTER TABLE 旧表 RENAME TO 新表`（保留数据）；
   - 旧表不存在（全新库）→ 跳过。
3. **改模型**：对应 Model 的 `@TableName` → 新表名。

参考实现：`db.migration.V2__rename_sys_certificate.java`。回归测试：`io.voyager1.core.db.FlywayRenameMigrationTest`（覆盖存量库改名保留数据 + 全新库跳过两种场景）。

## 测试库注意事项

采用“CSV 新名建表 + 条件重命名”后，全新库与存量库均幂等，持久化测试库（`$TMPDIR/voyager1-test-data`）可跨运行复用，无需手动清理。若遇 Flyway 校验失败（版本冲突），用 `mvn clean test` 清除 `target/` 里残留的旧迁移产物后重试。
