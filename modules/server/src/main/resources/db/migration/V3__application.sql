-- 应用交付重构（Phase 1）：应用(Application)为第一公民
-- Application = 逻辑服务（跨环境），绑定代码仓库 + 构建配置

CREATE TABLE IF NOT EXISTS APPLICATION (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  name VARCHAR(100) NOT NULL,
  repositoryId VARCHAR(50),
  buildId VARCHAR(50),
  remark VARCHAR(200),
  PRIMARY KEY (id)
);
