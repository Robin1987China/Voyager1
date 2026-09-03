-- Voyager1 初始 schema（由 sql-view CSV 生成，Flyway V1 基线）
-- H2 MODE=MYSQL; 标识符不加引号（与 InitDb 旧建表行为一致，H2 存储为大写）

CREATE TABLE IF NOT EXISTS AGENT_APPROVAL (
  id VARCHAR(50) NOT NULL,
  workspaceId VARCHAR(50),
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  agentSessionId VARCHAR(50),
  toolName VARCHAR(50),
  arguments TEXT,
  status INTEGER DEFAULT 0,
  operator VARCHAR(50),
  approver VARCHAR(50),
  result TEXT,
  remark VARCHAR(200),
  expireTimeMillis BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS CI_BUILD_LOG (
  id VARCHAR(50) NOT NULL,
  buildDataId VARCHAR(50),
  buildNumberId INTEGER,
  status TINYINT,
  startTime BIGINT,
  endTime BIGINT,
  resultDirFile VARCHAR(200),
  releaseMethod TINYINT,
  name VARCHAR(100),
  buildName VARCHAR(100),
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50),
  triggerBuildType INTEGER DEFAULT 0,
  buildRemark VARCHAR(255),
  extraData TEXT,
  buildEnvCache TEXT,
  resultFileSize BIGINT,
  buildLogFileSize BIGINT,
  statusMsg TEXT,
  fromBuildNumberId INTEGER,
  repositoryLastCommitId VARCHAR(255),
  repositoryLastCommitMsg VARCHAR(255),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS CI_BUILD (
  id VARCHAR(50) NOT NULL,
  repositoryId VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  name VARCHAR(50),
  buildId INTEGER,
  group VARCHAR(50),
  branchName VARCHAR(50),
  script TEXT,
  resultDirFile VARCHAR(200),
  releaseMethod INTEGER,
  modifyUser VARCHAR(50),
  status INTEGER,
  triggerToken VARCHAR(100),
  extraData TEXT,
  releaseMethodDataId TEXT,
  branchTagName VARCHAR(50),
  workspaceId VARCHAR(50),
  webhook VARCHAR(255),
  autoBuildCron VARCHAR(100),
  buildMode INTEGER,
  repositoryLastCommitId VARCHAR(255),
  sortValue FLOAT,
  buildEnvParameter TEXT,
  aliasCode VARCHAR(50),
  statusMsg TEXT,
  resultKeepDay INTEGER,
  createUser VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS SYS_CERTIFICATE (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT NOT NULL,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  createUser VARCHAR(50) NOT NULL,
  workspaceId VARCHAR(50) NOT NULL,
  description VARCHAR(255),
  keyAlias VARCHAR(50),
  keyType VARCHAR(50),
  certPassword VARCHAR(50),
  serialNumberStr VARCHAR(50) NOT NULL,
  issuerDnName VARCHAR(255),
  subjectDnName VARCHAR(255),
  sigAlgOid VARCHAR(255),
  sigAlgName VARCHAR(255),
  expirationTime VARCHAR(255),
  effectiveTime BIGINT,
  certVersion INTEGER,
  fingerprint VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS CLOUD_ACCOUNT (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  name VARCHAR(50) NOT NULL,
  vendor VARCHAR(20) NOT NULL,
  accessKey TEXT,
  secretKey TEXT,
  extraKey TEXT,
  region VARCHAR(50),
  remark VARCHAR(200),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS CLOUD_INSTANCE (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  accountId VARCHAR(50) NOT NULL,
  instanceId VARCHAR(50),
  name VARCHAR(50),
  publicIp VARCHAR(50),
  privateIp VARCHAR(50),
  status VARCHAR(20),
  groupName VARCHAR(50),
  machineId VARCHAR(50),
  regionId VARCHAR(50),
  zoneId VARCHAR(50),
  instanceType VARCHAR(50),
  cpu INTEGER,
  memory INTEGER,
  osName VARCHAR(200),
  expireTime VARCHAR(50),
  chargeType VARCHAR(30),
  tags TEXT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS INFRA_CLUSTER (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  name VARCHAR(50) NOT NULL,
  clusterId VARCHAR(50) NOT NULL,
  url VARCHAR(255),
  linkGroup VARCHAR(500),
  lastHeartbeat BIGINT,
  localHostName VARCHAR(255),
  voyager1Version VARCHAR(255),
  statusMsg TEXT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS OPS_COMMAND_LOG (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  commandId VARCHAR(50) NOT NULL,
  sshId VARCHAR(50) NOT NULL,
  batchId VARCHAR(50) NOT NULL,
  commandName VARCHAR(100) NOT NULL,
  sshName VARCHAR(100) NOT NULL,
  status INTEGER NOT NULL,
  params TEXT,
  triggerExecType INTEGER DEFAULT 0,
  exitCode INTEGER,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS OPS_COMMAND (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  name VARCHAR(100),
  desc VARCHAR(500),
  command TEXT,
  defParams TEXT,
  sshIds TEXT,
  autoExecCron VARCHAR(100),
  triggerToken VARCHAR(200),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS DEPLOYMENT_RECORD (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  buildId VARCHAR(50) NOT NULL,
  versionId VARCHAR(50),
  version VARCHAR(50),
  environment VARCHAR(50),
  mode VARCHAR(20),
  operator VARCHAR(50),
  status INTEGER DEFAULT 0,
  logRef VARCHAR(200),
  remark VARCHAR(200),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS INFRA_DOCKER (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  name VARCHAR(255) NOT NULL,
  host VARCHAR(255),
  tlsVerify TINYINT DEFAULT 0,
  heartbeatTimeout INTEGER,
  lastHeartbeatTime BIGINT,
  tags VARCHAR(255),
  swarmId VARCHAR(50),
  swarmNodeId VARCHAR(50),
  registryUsername VARCHAR(255),
  registryPassword VARCHAR(255),
  registryEmail VARCHAR(255),
  registryUrl VARCHAR(255),
  machineDockerId VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS INFRA_DOCKER_SWARM (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  name VARCHAR(255) NOT NULL,
  swarmId VARCHAR(50) NOT NULL,
  tag VARCHAR(255),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ENVIRONMENT_INFO (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  name VARCHAR(50) NOT NULL,
  sortValue INTEGER DEFAULT 0,
  enabled INTEGER DEFAULT 1,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS OPS_FILE_RELEASE_LOG (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT NOT NULL,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  name VARCHAR(255) NOT NULL,
  taskId VARCHAR(50) NOT NULL,
  fileId VARCHAR(50) NOT NULL,
  taskDataId VARCHAR(50) NOT NULL,
  taskType TINYINT NOT NULL,
  status TINYINT NOT NULL,
  statusMsg TEXT,
  releasePath VARCHAR(255) NOT NULL,
  beforeScript TEXT,
  afterScript TEXT,
  fileType TINYINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS OPS_FILE_RELEASE_TEMPLATE (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT NOT NULL,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  name VARCHAR(255) NOT NULL,
  templateTag VARCHAR(50) NOT NULL,
  fileType TINYINT,
  data TEXT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS STORAGE_FILE (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT NOT NULL,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  createUser VARCHAR(50) NOT NULL,
  workspaceId VARCHAR(50) NOT NULL,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(255),
  size BIGINT NOT NULL,
  source TINYINT NOT NULL,
  validUntil BIGINT NOT NULL,
  path VARCHAR(255) NOT NULL,
  extName VARCHAR(50) NOT NULL,
  status TINYINT,
  progressDesc VARCHAR(255),
  triggerToken VARCHAR(200),
  aliasCode VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS K8S_CLUSTER (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  name VARCHAR(50) NOT NULL,
  kubeconfig TEXT,
  serverUrl VARCHAR(100),
  namespace VARCHAR(50),
  remark VARCHAR(200),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS OPS_LOG_FILE (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  name VARCHAR(50) NOT NULL,
  nodeProject TEXT,
  cacheData TEXT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS INFRA_MACHINE_DOCKER (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  groupName VARCHAR(50) NOT NULL,
  name VARCHAR(255) NOT NULL,
  host VARCHAR(255),
  tlsVerify TINYINT DEFAULT 0,
  status TINYINT DEFAULT 0,
  failureMsg VARCHAR(255),
  heartbeatTimeout INTEGER,
  lastHeartbeatTime BIGINT,
  dockerVersion TEXT,
  swarmId VARCHAR(50),
  swarmNodeId VARCHAR(50),
  registryUsername VARCHAR(255),
  registryPassword VARCHAR(255),
  registryEmail VARCHAR(255),
  registryUrl VARCHAR(255),
  swarmControlAvailable TINYINT,
  swarmCreatedAt BIGINT,
  swarmUpdatedAt BIGINT,
  swarmNodeAddr VARCHAR(50),
  certInfo VARCHAR(100),
  certExist TINYINT,
  enableSsh TINYINT,
  machineSshId VARCHAR(255),
  sshUseSudo TINYINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS INFRA_MACHINE (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  name VARCHAR(50) NOT NULL,
  groupName VARCHAR(50) NOT NULL,
  hostName VARCHAR(255),
  hostIpv4s TEXT,
  osLoadAverage VARCHAR(100),
  osSystemUptime BIGINT,
  osVersion VARCHAR(255),
  osHardwareVersion VARCHAR(255),
  osCpuCores INTEGER,
  osMoneyTotal BIGINT,
  osFileStoreTotal BIGINT,
  osCpuIdentifierName VARCHAR(255),
  osName VARCHAR(50),
  status TINYINT NOT NULL,
  statusMsg TEXT,
  transportMode TINYINT NOT NULL,
  voyager1Url VARCHAR(100),
  voyager1Username VARCHAR(100),
  voyager1Password VARCHAR(100),
  voyager1Protocol VARCHAR(10),
  voyager1Timeout INTEGER,
  voyager1HttpProxy VARCHAR(200),
  voyager1HttpProxyType VARCHAR(20),
  voyager1Version VARCHAR(50),
  voyager1Uptime BIGINT,
  voyager1BuildTime VARCHAR(50),
  voyager1ProjectCount INTEGER,
  voyager1ScriptCount INTEGER,
  networkDelay INTEGER,
  javaVersion VARCHAR(50),
  jvmTotalMemory BIGINT,
  jvmFreeMemory BIGINT,
  osOccupyCpu DOUBLE,
  osOccupyMemory DOUBLE,
  osOccupyDisk DOUBLE,
  templateNode TINYINT,
  installId VARCHAR(50),
  osSwapTotal BIGINT,
  osVirtualMax BIGINT,
  transportEncryption TINYINT,
  extendInfo TEXT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS INFRA_MACHINE_STAT_LOG (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  machineId VARCHAR(50) NOT NULL,
  occupyCpu DOUBLE,
  occupyMemory DOUBLE,
  occupyDisk DOUBLE,
  networkDelay INTEGER DEFAULT 0,
  monitorTime BIGINT NOT NULL,
  netTxBytes BIGINT,
  netRxBytes BIGINT,
  occupySwapMemory DOUBLE,
  occupyVirtualMemory DOUBLE,
  cpuTicks TEXT,
  PRIMARY KEY (id, machineId)
);

CREATE TABLE IF NOT EXISTS INFRA_MACHINE_SSH (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  name VARCHAR(50),
  groupName VARCHAR(50) NOT NULL,
  host VARCHAR(100) NOT NULL,
  port INTEGER NOT NULL,
  user VARCHAR(100) NOT NULL,
  password VARCHAR(100),
  charset VARCHAR(100),
  privateKey TEXT,
  connectType VARCHAR(10),
  timeout INTEGER DEFAULT 0,
  status TINYINT NOT NULL,
  statusMsg TEXT,
  allowEditSuffix TEXT,
  osName VARCHAR(50),
  hostName VARCHAR(255),
  osLoadAverage VARCHAR(100),
  osSystemUptime BIGINT,
  osVersion VARCHAR(255),
  osCpuCores INTEGER,
  osMoneyTotal BIGINT,
  osFileStoreTotal BIGINT,
  osCpuIdentifierName VARCHAR(255),
  osOccupyCpu DOUBLE,
  osOccupyMemory DOUBLE,
  osMaxOccupyDisk DOUBLE,
  osMaxOccupyDiskName VARCHAR(255),
  javaVersion VARCHAR(255),
  voyager1AgentPid INTEGER,
  dockerInfo VARCHAR(255),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS OPS_MONITOR_NOTIFY_LOG (
  id VARCHAR(50) NOT NULL,
  monitorId VARCHAR(50),
  nodeId VARCHAR(50),
  projectId VARCHAR(30),
  createTime BIGINT,
  title VARCHAR(500),
  content TEXT,
  status TINYINT,
  notifyStyle TINYINT,
  notifyStatus TINYINT,
  notifyObject TEXT,
  notifyError TEXT,
  workspaceId VARCHAR(50),
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS OPS_MONITOR (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  name VARCHAR(50) NOT NULL,
  autoRestart TINYINT DEFAULT 0,
  status TINYINT DEFAULT 0,
  alarm TINYINT DEFAULT 0,
  cycle INTEGER DEFAULT 0,
  notifyUser TEXT,
  projects TEXT,
  execCron VARCHAR(100),
  webhook VARCHAR(255),
  useLanguage VARCHAR(20),
  silenceTime INTEGER,
  silenceUnit VARCHAR(20),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS OPS_MONITOR_NOTIFY (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  name VARCHAR(50) NOT NULL,
  monitorUser TEXT,
  status TINYINT DEFAULT 0,
  notifyUser TEXT,
  monitorFeature TEXT,
  monitorOpt TEXT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS INFRA_NODE (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  name VARCHAR(50),
  url VARCHAR(100) NOT NULL,
  loginName VARCHAR(100) NOT NULL,
  loginPwd VARCHAR(100) NOT NULL,
  protocol VARCHAR(10) NOT NULL,
  openStatus INTEGER DEFAULT 0,
  timeOut INTEGER DEFAULT 0,
  sshId VARCHAR(50),
  group VARCHAR(50),
  httpProxy VARCHAR(200),
  httpProxyType VARCHAR(20),
  sortValue FLOAT,
  machineId VARCHAR(50),
  voyager1ProjectCount INTEGER,
  voyager1ScriptCount INTEGER,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS OPS_RELEASE_LOG (
  id VARCHAR(50) NOT NULL,
  outGivingId VARCHAR(50),
  status TINYINT,
  startTime BIGINT,
  endTime BIGINT,
  result TEXT,
  nodeId VARCHAR(100),
  projectId VARCHAR(100),
  workspaceId VARCHAR(50),
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  fileSize BIGINT,
  progressSize BIGINT,
  mode VARCHAR(50),
  modeData VARCHAR(500),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS OPS_RELEASE (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  name VARCHAR(50) NOT NULL,
  afterOpt INTEGER DEFAULT 0,
  clearOld TINYINT DEFAULT 0,
  outGivingProject TINYINT DEFAULT 0,
  outGivingNodeProjectList TEXT,
  intervalTime INTEGER DEFAULT 10,
  status INTEGER DEFAULT 0,
  secondaryDirectory VARCHAR(200),
  uploadCloseFirst TINYINT DEFAULT 0,
  statusMsg VARCHAR(255),
  group VARCHAR(50),
  webhook VARCHAR(255),
  mode VARCHAR(50),
  modeData VARCHAR(500),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS PIPELINE_CONFIG (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  name VARCHAR(50),
  buildId VARCHAR(50) NOT NULL,
  triggers TEXT,
  stages TEXT,
  enabled INTEGER DEFAULT 1,
  remark VARCHAR(200),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS PIPELINE_EXECUTE_RECORD (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  pipelineId VARCHAR(50) NOT NULL,
  triggerType VARCHAR(20),
  status INTEGER DEFAULT 0,
  currentStage VARCHAR(50),
  stages TEXT,
  startTime BIGINT,
  endTime BIGINT,
  operator VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS CI_PROJECT (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  projectId VARCHAR(50) NOT NULL,
  nodeId VARCHAR(50) NOT NULL,
  name VARCHAR(50) NOT NULL,
  mainClass VARCHAR(100),
  lib VARCHAR(100),
  whitelistDirectory VARCHAR(100),
  logPath VARCHAR(100),
  jvm TEXT,
  args TEXT,
  javaCopyItemList TEXT,
  token VARCHAR(255),
  runMode VARCHAR(20),
  outGivingProject TINYINT DEFAULT 0,
  javaExtDirsCp TEXT,
  sortValue FLOAT,
  triggerToken VARCHAR(100),
  group VARCHAR(50),
  dslContent TEXT,
  autoStart TINYINT,
  nodeName VARCHAR(50),
  workspaceName VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS CI_REPOSITORY (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  name VARCHAR(50),
  gitUrl VARCHAR(255),
  repoType INTEGER,
  protocol INTEGER,
  userName VARCHAR(50),
  password VARCHAR(255),
  rsaPub VARCHAR(2048),
  rsaPrv VARCHAR(4096),
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50),
  sortValue FLOAT,
  timeout INTEGER,
  createUser VARCHAR(50),
  group VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS OPS_SCRIPT_LOG (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  nodeId VARCHAR(50) NOT NULL,
  scriptId VARCHAR(50) NOT NULL,
  scriptName VARCHAR(100),
  triggerExecType INTEGER DEFAULT 0,
  nodeName VARCHAR(50),
  workspaceName VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS OPS_SCRIPT (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  nodeId VARCHAR(50) NOT NULL,
  scriptId VARCHAR(50) NOT NULL,
  name VARCHAR(50) NOT NULL,
  lastRunUser VARCHAR(50),
  autoExecCron VARCHAR(100),
  defArgs TEXT,
  description VARCHAR(255),
  scriptType VARCHAR(100),
  triggerToken VARCHAR(200),
  createUser VARCHAR(50),
  nodeName VARCHAR(50),
  workspaceName VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS OPS_SCRIPT_LIBRARY (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  createUser VARCHAR(50),
  tag VARCHAR(50) NOT NULL,
  description VARCHAR(255),
  script TEXT,
  machineIds TEXT,
  version VARCHAR(50) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS OPS_SERVER_SCRIPT_LOG (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  scriptId VARCHAR(50) NOT NULL,
  scriptName VARCHAR(100),
  triggerExecType INTEGER DEFAULT 0,
  exitCode INTEGER,
  status TINYINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS OPS_SERVER_SCRIPT (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  name VARCHAR(50) NOT NULL,
  lastRunUser VARCHAR(50),
  autoExecCron VARCHAR(100),
  defArgs VARCHAR(100),
  context TEXT,
  description VARCHAR(255),
  nodeIds TEXT,
  triggerToken VARCHAR(200),
  createUser VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS INFRA_SSH_SESSION_LOG (
  id VARCHAR(50) NOT NULL,
  ip VARCHAR(80),
  userId VARCHAR(30),
  userAgent TEXT,
  commands TEXT,
  sshId VARCHAR(50),
  sshName VARCHAR(50),
  refuse INTEGER,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  workspaceId VARCHAR(50),
  modifyUser VARCHAR(50),
  machineSshId VARCHAR(50),
  machineSshName VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS INFRA_SSH (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  name VARCHAR(50),
  host VARCHAR(100) NOT NULL,
  port INTEGER NOT NULL,
  user VARCHAR(100) NOT NULL,
  password VARCHAR(100),
  charset VARCHAR(100),
  fileDirs TEXT,
  privateKey TEXT,
  connectType VARCHAR(10),
  notAllowedCommand TEXT,
  allowEditSuffix TEXT,
  timeout INTEGER DEFAULT 0,
  group VARCHAR(50),
  machineSshId VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS STORAGE_STATIC_FILE (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  name VARCHAR(100),
  description VARCHAR(255),
  extName VARCHAR(50),
  absolutePath VARCHAR(300),
  parentAbsolutePath VARCHAR(300),
  staticDir VARCHAR(50),
  status TINYINT,
  type TINYINT,
  scanTaskId BIGINT,
  lastModified BIGINT,
  size BIGINT,
  level INTEGER,
  triggerToken VARCHAR(100),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS SYS_PARAMETER (
  id VARCHAR(100) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  value TEXT,
  description VARCHAR(255),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS TRIGGER_TOKEN_LOG (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  triggerToken VARCHAR(100) NOT NULL,
  type VARCHAR(50) NOT NULL,
  dataId VARCHAR(50) NOT NULL,
  userId VARCHAR(50) NOT NULL,
  triggerCount INTEGER,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS SYS_OPERATION_LOG (
  id VARCHAR(50) NOT NULL,
  ip VARCHAR(80),
  userId VARCHAR(50),
  resultMsg TEXT,
  optStatus INTEGER,
  optTime BIGINT,
  nodeId VARCHAR(50),
  dataId VARCHAR(200),
  userAgent TEXT,
  reqData TEXT,
  workspaceId VARCHAR(50),
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  classFeature VARCHAR(100),
  methodFeature VARCHAR(100),
  dataName VARCHAR(200),
  workspaceName VARCHAR(50),
  username VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS SYS_USER_WORKSPACE (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  userId VARCHAR(50) NOT NULL,
  workspaceId VARCHAR(100) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS SYS_USER (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  parent VARCHAR(50) NOT NULL,
  name VARCHAR(50),
  systemUser INTEGER DEFAULT 0,
  password VARCHAR(100),
  salt VARCHAR(50),
  pwdErrorCount INTEGER DEFAULT 0,
  lastPwdErrorTime BIGINT DEFAULT 0,
  lockTime BIGINT DEFAULT 0,
  email VARCHAR(255),
  dingDing VARCHAR(255),
  workWx VARCHAR(255),
  status TINYINT,
  permissionGroup TEXT,
  source VARCHAR(100),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS SYS_USER_LOGIN_LOG (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  ip VARCHAR(80),
  userAgent TEXT,
  success TINYINT,
  operateCode TINYINT,
  username VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS SYS_PERMISSION_GROUP (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  name VARCHAR(250) NOT NULL,
  description VARCHAR(255),
  prohibitExecute TEXT,
  allowExecute TEXT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS VERSION_INFO (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  buildId VARCHAR(50) NOT NULL,
  buildNumberId INTEGER,
  version VARCHAR(50),
  status INTEGER DEFAULT 0,
  artifactRef VARCHAR(200),
  remark VARCHAR(200),
  groupName VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS SYS_WORKSPACE (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  name VARCHAR(50),
  description VARCHAR(255),
  group VARCHAR(50),
  clusterInfoId VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS WORKSPACE_ENV_VAR (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  workspaceId VARCHAR(50) NOT NULL,
  name VARCHAR(50),
  description VARCHAR(255),
  value TEXT,
  nodeIds TEXT,
  privacy TINYINT DEFAULT 0,
  triggerToken VARCHAR(100),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS COST_BILL (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  accountId VARCHAR(50),
  vendor VARCHAR(20),
  billDate VARCHAR(20),
  serviceName VARCHAR(50),
  resourceId VARCHAR(100),
  region VARCHAR(50),
  tagKey VARCHAR(100),
  tagValue VARCHAR(100),
  projectId VARCHAR(50),
  amount DOUBLE,
  currency VARCHAR(10),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS COST_TAG_RULE (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  vendor VARCHAR(20),
  tagKey VARCHAR(100),
  tagValue VARCHAR(100),
  projectId VARCHAR(50),
  projectName VARCHAR(50),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS COST_BUDGET (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  modifyUser VARCHAR(50),
  name VARCHAR(50),
  scopeType VARCHAR(20),
  scopeValue VARCHAR(100),
  monthlyLimit DOUBLE,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS BACKUP_INFO (
  id VARCHAR(50) NOT NULL,
  createTimeMillis BIGINT,
  modifyTimeMillis BIGINT,
  name VARCHAR(50),
  filePath VARCHAR(200),
  backupType INTEGER,
  fileSize BIGINT,
  sha1Sum VARCHAR(50),
  status INTEGER DEFAULT 0,
  baleTimeStamp BIGINT,
  version VARCHAR(255),
  modifyUser VARCHAR(50),
  PRIMARY KEY (id)
);
