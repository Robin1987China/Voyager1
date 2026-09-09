-- 环境化 CI/CD（Phase 1）：环境策略字段 + 环境-目标绑定表
-- strategy: CI_CD=开发环境可构建可部署, CD_ONLY=测试/生产仅部署
-- approvalRequired: 1=部署前需审批（prod）
--
-- 跨库兼容约定（H2/MySQL/MariaDB/PostgreSQL 均可执行）：
--   1. 标识符不加引号（反引号仅 MySQL 系支持，PG 非法）；
--   2. 不使用 ADD COLUMN IF NOT EXISTS（MySQL 8 不支持）。
--      项目 ddl-auto=none，列只能由 Flyway 创建，重复添加不会发生。

ALTER TABLE ENVIRONMENT_INFO ADD COLUMN type VARCHAR(20);
ALTER TABLE ENVIRONMENT_INFO ADD COLUMN strategy VARCHAR(20);
ALTER TABLE ENVIRONMENT_INFO ADD COLUMN approvalRequired INTEGER DEFAULT 0;
ALTER TABLE CI_BUILD ADD COLUMN environment VARCHAR(20);

CREATE TABLE ENVIRONMENT_TARGET (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  environmentId VARCHAR(50) NOT NULL,
  targetType VARCHAR(20) NOT NULL,
  targetId VARCHAR(100) NOT NULL,
  projectId VARCHAR(100),
  workspaceId VARCHAR(50),
  enabled INTEGER DEFAULT 1,
  sortValue INTEGER DEFAULT 0,
  PRIMARY KEY (id)
);

-- 回填既有环境策略：type 缺省用 name；test/prod 为 CD_ONLY，prod 需审批
UPDATE ENVIRONMENT_INFO SET type = name WHERE type IS NULL;
UPDATE ENVIRONMENT_INFO SET strategy = 'CI_CD', approvalRequired = 0 WHERE strategy IS NULL;
UPDATE ENVIRONMENT_INFO SET strategy = 'CD_ONLY' WHERE name = 'test';
UPDATE ENVIRONMENT_INFO SET strategy = 'CD_ONLY', approvalRequired = 1 WHERE name = 'prod';
