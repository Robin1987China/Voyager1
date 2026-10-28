-- Agent 执行审计：每次 MCP 工具调用留痕（会话 / 意图 / 工具 / 入参 / 结果 / 耗时）
-- 对应 openspec/specs/agent-trust.md「Agent 执行审计」要求

CREATE TABLE IF NOT EXISTS AGENT_AUDIT (
  id VARCHAR(50) NOT NULL,
  workspaceId VARCHAR(50),
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  agentSessionId VARCHAR(50),
  intentText TEXT,
  toolName VARCHAR(50),
  arguments TEXT,
  result TEXT,
  success INTEGER DEFAULT 1,
  durationMillis BIGINT,
  operator VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS AGENT_AUDIT_CREATE_TIME_IDX ON PUBLIC.AGENT_AUDIT (createTimeMillis);
CREATE INDEX IF NOT EXISTS AGENT_AUDIT_SESSION_IDX ON PUBLIC.AGENT_AUDIT (agentSessionId);
CREATE INDEX IF NOT EXISTS AGENT_AUDIT_TOOL_IDX ON PUBLIC.AGENT_AUDIT (toolName);
