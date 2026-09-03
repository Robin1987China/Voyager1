-- Voyager1 数据库建表脚本（H2，干净合并版）
-- 由 sql-view/table.*.csv + alter.*.csv 合并生成，已去除历史废弃字段/表

CREATE TABLE IF NOT EXISTS PUBLIC.AGENT_APPROVAL
(
    `id` VARCHAR(50) not null comment 'id',
    `workspaceId` VARCHAR(50) comment '工作空间ID',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `agentSessionId` VARCHAR(50) comment 'Agent会话ID',
    `toolName` VARCHAR(50) comment '工具名',
    `arguments` CLOB comment '工具参数',
    `status` INTEGER default '0' comment '状态{0待审批1已批准2已拒绝3已超时}',
    `operator` VARCHAR(50) comment '发起人',
    `approver` VARCHAR(50) comment '审批人',
    `result` CLOB comment '执行结果',
    `remark` VARCHAR(200) comment '备注',
    `expireTimeMillis` BIGINT comment '超时时间',
    CONSTRAINT AGENT_APPROVAL_PK PRIMARY KEY (id)
);
COMMENT ON TABLE AGENT_APPROVAL is 'Agent审批';

CREATE TABLE IF NOT EXISTS PUBLIC.BACKUP_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `name` VARCHAR(50) comment '备份名称',
    `filePath` VARCHAR(200) comment '文件地址',
    `backupType` INTEGER comment '备份类型{0: 全量, 1: 部分}',
    `fileSize` BIGINT comment '文件大小',
    `sha1Sum` VARCHAR(50) comment 'SHA1 信息',
    `status` INTEGER default '0' comment '状态{0: 默认; 1: 成功; 2: 失败}',
    `baleTimeStamp` BIGINT comment '打包时间',
    `version` VARCHAR(255) comment '服务版本',
    `modifyUser` VARCHAR(50) comment '操作人',
    CONSTRAINT BACKUP_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE BACKUP_INFO is '备份数据库信息';

CREATE TABLE IF NOT EXISTS PUBLIC.BUILDHISTORYLOG
(
    `id` VARCHAR(50) not null comment '表id',
    `buildDataId` VARCHAR(50) comment '构建的数据id',
    `buildNumberId` INTEGER comment '构建编号',
    `status` TINYINT comment '构建状态',
    `startTime` BIGINT comment '开始时间',
    `endTime` BIGINT comment '结束时间',
    `resultDirFile` VARCHAR(200) comment '构建产物目录',
    `releaseMethod` TINYINT comment '发布方式',
    `name` VARCHAR(100) comment '构建名称',
    `buildName` VARCHAR(100) comment '构建名称',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '操作人',
    `workspaceId` VARCHAR(50) comment '所属工作空间',
    `triggerBuildType` INTEGER default '0' comment '触发类型{0，手动，1 触发器,2 自动触发}',
    `buildRemark` VARCHAR(255) comment '构建备注',
    `extraData` CLOB comment '额外信息，JSON 字符串格式',
    `buildEnvCache` CLOB comment '构建环境变量',
    `resultFileSize` BIGINT comment '产物文件大小',
    `buildLogFileSize` BIGINT comment '构建日志文件大小',
    `statusMsg` CLOB comment '状态信息',
    `fromBuildNumberId` INTEGER comment '来自的构建编号，回滚时存在',
    `repositoryLastCommitId` VARCHAR(255) comment '仓库代码最后一次变动信息（ID，git 为 commit hash, svn 最后的版本号）',
    `repositoryLastCommitMsg` VARCHAR(255) comment '仓库代码最后一次变动信息',
    CONSTRAINT BUILDHISTORYLOG_PK PRIMARY KEY (id)
);
COMMENT ON TABLE BUILDHISTORYLOG is '构建历史记录';

CREATE TABLE IF NOT EXISTS PUBLIC.BUILD_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `repositoryId` VARCHAR(50) not null comment '仓库 id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `name` VARCHAR(50) comment '构建名称',
    `buildId` INTEGER comment '构建 id',
    `group` VARCHAR(50) comment '分组名称',
    `branchName` VARCHAR(50) comment '分支',
    `script` CLOB comment '构建命令',
    `resultDirFile` VARCHAR(200) comment '构建产物目录',
    `releaseMethod` INTEGER comment '发布方法{0: 不发布, 1: 节点分发, 2: 分发项目, 3: SSH}',
    `modifyUser` VARCHAR(50) comment '修改人',
    `status` INTEGER comment '状态',
    `triggerToken` VARCHAR(100) comment '触发器token',
    `extraData` CLOB comment '额外信息，JSON 字符串格式',
    `releaseMethodDataId` CLOB comment '构建关联的数据ID',
    `branchTagName` VARCHAR(50) comment '标签',
    `workspaceId` VARCHAR(50) comment '所属工作空间',
    `webhook` VARCHAR(255) comment 'webhook',
    `autoBuildCron` VARCHAR(100) comment '自动构建表达式',
    `buildMode` INTEGER comment '构建方式 {0 本地构建, 1 docker 构建}',
    `repositoryLastCommitId` VARCHAR(255) comment '仓库代码最后一次变动信息（ID，git 为 commit hash, svn 最后的版本号）',
    `sortValue` REAL comment '排序值',
    `buildEnvParameter` CLOB comment '构建环境变量',
    `aliasCode` VARCHAR(50) comment '别名码',
    `statusMsg` CLOB comment '状态信息',
    `resultKeepDay` INTEGER comment '产物保留天数',
    `createUser` VARCHAR(50) comment '创建人',
    CONSTRAINT BUILD_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE BUILD_INFO is '构建信息';

CREATE TABLE IF NOT EXISTS PUBLIC.CERTIFICATE_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT not null comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `createUser` VARCHAR(50) not null comment '创建人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `description` VARCHAR(255) comment '描述',
    `keyAlias` VARCHAR(50) comment '别名',
    `keyType` VARCHAR(50) comment '证书类型',
    `certPassword` VARCHAR(50) comment '证书密码',
    `serialNumberStr` VARCHAR(50) not null comment '证书序列号',
    `issuerDnName` VARCHAR(255) comment '颁发者 DN 名称',
    `subjectDnName` VARCHAR(255) comment '主题 DN 名称',
    `sigAlgOid` VARCHAR(255) comment '算法OID',
    `sigAlgName` VARCHAR(255) comment '算法名',
    `expirationTime` VARCHAR(255) comment '证书到期时间',
    `effectiveTime` BIGINT comment '证书生效日期',
    `certVersion` INTEGER comment '版本号',
    `fingerprint` VARCHAR(50) comment '证书指纹',
    CONSTRAINT CERTIFICATE_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE CERTIFICATE_INFO is '证书信息表';

CREATE TABLE IF NOT EXISTS PUBLIC.CLOUD_ACCOUNT
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `name` VARCHAR(50) not null comment '账号名称',
    `vendor` VARCHAR(20) not null comment '云厂商(aliyun/tencent/aws)',
    `accessKey` CLOB comment 'AccessKey(加密)',
    `secretKey` CLOB comment 'SecretKey(加密)',
    `extraKey` CLOB comment '额外凭证(加密)',
    `region` VARCHAR(50) comment '默认区域',
    `remark` VARCHAR(200) comment '备注',
    CONSTRAINT CLOUD_ACCOUNT_PK PRIMARY KEY (id)
);
COMMENT ON TABLE CLOUD_ACCOUNT is '云账号';

CREATE TABLE IF NOT EXISTS PUBLIC.CLOUD_INSTANCE
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `accountId` VARCHAR(50) not null comment '关联云账号',
    `instanceId` VARCHAR(50) comment '实例ID',
    `name` VARCHAR(50) comment '实例名称',
    `publicIp` VARCHAR(50) comment '公网IP',
    `privateIp` VARCHAR(50) comment '内网IP',
    `status` VARCHAR(20) comment '实例状态',
    `groupName` VARCHAR(50) comment '分组',
    `machineId` VARCHAR(50) comment '关联机器SSH id',
    `regionId` VARCHAR(50) comment '区域',
    `zoneId` VARCHAR(50) comment '可用区',
    `instanceType` VARCHAR(50) comment '实例规格',
    `cpu` INTEGER comment 'CPU核数',
    `memory` INTEGER comment '内存(MB)',
    `osName` VARCHAR(200) comment '操作系统',
    `expireTime` VARCHAR(50) comment '到期时间',
    `chargeType` VARCHAR(30) comment '计费类型',
    `tags` CLOB comment '标签(JSON)',
    CONSTRAINT CLOUD_INSTANCE_PK PRIMARY KEY (id)
);
COMMENT ON TABLE CLOUD_INSTANCE is '云实例';

CREATE TABLE IF NOT EXISTS PUBLIC.CLUSTER_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `name` VARCHAR(50) not null comment '集群名称',
    `clusterId` VARCHAR(50) not null comment '集群Id',
    `url` VARCHAR(255) comment '集群地址',
    `linkGroup` VARCHAR(500) comment '集群地址',
    `lastHeartbeat` BIGINT comment '最后心跳',
    `localHostName` VARCHAR(255) comment '主机名',
    `voyager1Version` VARCHAR(255) comment 'voyager1版本',
    `statusMsg` CLOB comment '状态消息',
    CONSTRAINT CLUSTER_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE CLUSTER_INFO is '机器节点信息';

CREATE TABLE IF NOT EXISTS PUBLIC.COMMAND_EXEC_LOG
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `commandId` VARCHAR(50) not null comment '命令ID',
    `sshId` VARCHAR(50) not null comment 'ssh id',
    `batchId` VARCHAR(50) not null comment '批次ID',
    `commandName` VARCHAR(100) not null comment '命令名称',
    `sshName` VARCHAR(100) not null comment 'ssh 名称',
    `status` INTEGER not null comment '状态 {0，执行中，1 执行结束，2 执行错误}',
    `params` CLOB comment '命令参数',
    `triggerExecType` INTEGER default '0' comment '触发类型{0，手动，1 自动触发}',
    `exitCode` INTEGER comment '退出码',
    CONSTRAINT COMMAND_EXEC_LOG_PK PRIMARY KEY (id)
);
COMMENT ON TABLE COMMAND_EXEC_LOG is '命令执行记录';

CREATE TABLE IF NOT EXISTS PUBLIC.COMMAND_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `name` VARCHAR(100) comment '命令名称',
    `desc` VARCHAR(500) comment '命令描述',
    `command` CLOB comment '指令内容',
    `defParams` CLOB comment '命令参数',
    `sshIds` CLOB comment '绑定的ssh id',
    `autoExecCron` VARCHAR(100) comment '自动执行表达式',
    `triggerToken` VARCHAR(200) comment '触发器token',
    CONSTRAINT COMMAND_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE COMMAND_INFO is '命令行信息';

CREATE TABLE IF NOT EXISTS PUBLIC.COST_BILL
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `accountId` VARCHAR(50) comment '云账号',
    `vendor` VARCHAR(20) comment '云厂商',
    `billDate` VARCHAR(20) comment '账单日期',
    `serviceName` VARCHAR(50) comment '服务类型',
    `resourceId` VARCHAR(100) comment '资源ID',
    `region` VARCHAR(50) comment '区域',
    `tagKey` VARCHAR(100) comment '标签key',
    `tagValue` VARCHAR(100) comment '标签value',
    `projectId` VARCHAR(50) comment '分摊项目',
    `amount` DOUBLE comment '金额(元)',
    `currency` VARCHAR(10) comment '币种',
    CONSTRAINT COST_BILL_PK PRIMARY KEY (id)
);
COMMENT ON TABLE COST_BILL is '成本明细';

CREATE TABLE IF NOT EXISTS PUBLIC.COST_BUDGET
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `name` VARCHAR(50) comment '预算名称',
    `scopeType` VARCHAR(20) comment '范围类型(account/tag/project/global)',
    `scopeValue` VARCHAR(100) comment '范围值',
    `monthlyLimit` DOUBLE comment '月预算(元)',
    CONSTRAINT COST_BUDGET_PK PRIMARY KEY (id)
);
COMMENT ON TABLE COST_BUDGET is '成本预算';

CREATE TABLE IF NOT EXISTS PUBLIC.COST_TAG_RULE
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `vendor` VARCHAR(20) comment '云厂商(空=所有)',
    `tagKey` VARCHAR(100) comment '标签key',
    `tagValue` VARCHAR(100) comment '标签value',
    `projectId` VARCHAR(50) comment '项目ID',
    `projectName` VARCHAR(50) comment '项目名称',
    CONSTRAINT COST_TAG_RULE_PK PRIMARY KEY (id)
);
COMMENT ON TABLE COST_TAG_RULE is '标签分摊规则';

CREATE TABLE IF NOT EXISTS PUBLIC.DEPLOYMENT_RECORD
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `buildId` VARCHAR(50) not null comment '关联构建配置id',
    `versionId` VARCHAR(50) comment '版本id',
    `version` VARCHAR(50) comment '版本号快照',
    `environment` VARCHAR(50) comment '目标环境',
    `mode` VARCHAR(20) comment '触发方式 auto/manual',
    `operator` VARCHAR(50) comment '操作者',
    `status` INTEGER default '0' comment '部署状态',
    `logRef` VARCHAR(200) comment '日志引用',
    `remark` VARCHAR(200) comment '备注',
    CONSTRAINT DEPLOYMENT_RECORD_PK PRIMARY KEY (id)
);
COMMENT ON TABLE DEPLOYMENT_RECORD is '部署记录';

CREATE TABLE IF NOT EXISTS PUBLIC.DOCKER_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `name` VARCHAR(255) not null comment '名称',
    `host` VARCHAR(255) comment 'docker host',
    `tlsVerify` TINYINT default '0' comment 'tls 认证{1，启用，0 未启用}',
    `heartbeatTimeout` INTEGER comment '心跳超时时间',
    `lastHeartbeatTime` BIGINT comment '最后心跳时间',
    `tags` VARCHAR(255) comment '容器标签',
    `swarmId` VARCHAR(50) comment '集群ID',
    `swarmNodeId` VARCHAR(50) comment '集群 节点ID',
    `registryUsername` VARCHAR(255) comment '仓库账号',
    `registryPassword` VARCHAR(255) comment '仓库密码',
    `registryEmail` VARCHAR(255) comment '仓库邮箱',
    `registryUrl` VARCHAR(255) comment '仓库地址',
    `machineDockerId` VARCHAR(50) comment '机器id',
    CONSTRAINT DOCKER_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE DOCKER_INFO is 'docker 信息';

CREATE TABLE IF NOT EXISTS PUBLIC.DOCKER_SWARM_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `name` VARCHAR(255) not null comment '名称',
    `swarmId` VARCHAR(50) not null comment 'swarm Id',
    `tag` VARCHAR(255) comment '容器标签',
    CONSTRAINT DOCKER_SWARM_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE DOCKER_SWARM_INFO is 'docker 集群信息';

CREATE TABLE IF NOT EXISTS PUBLIC.ENVIRONMENT_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `name` VARCHAR(50) not null comment '环境名称(dev/test/prod)',
    `sortValue` INTEGER default '0' comment '排序',
    `enabled` INTEGER default '1' comment '是否启用',
    CONSTRAINT ENVIRONMENT_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE ENVIRONMENT_INFO is '环境信息';

CREATE TABLE IF NOT EXISTS PUBLIC.FILE_RELEASE_TASK_LOG
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT not null comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `name` VARCHAR(255) not null comment '名称',
    `taskId` VARCHAR(50) not null comment '任务id',
    `fileId` VARCHAR(50) not null comment '文件 id',
    `taskDataId` VARCHAR(50) not null comment '任务关联的数据id',
    `taskType` TINYINT not null comment '任务类型 0 ssh 1 节点',
    `status` TINYINT not null comment '任务状态， 0 等待开始 1 进行中 2 任务结束 3 失败 4 取消任务',
    `statusMsg` CLOB comment '状态消息',
    `releasePath` VARCHAR(255) not null comment '发布路径',
    `beforeScript` CLOB comment '发布之前的脚本',
    `afterScript` CLOB comment '发布后的脚本',
    `fileType` TINYINT comment '文件类型',
    CONSTRAINT FILE_RELEASE_TASK_LOG_PK PRIMARY KEY (id)
);
COMMENT ON TABLE FILE_RELEASE_TASK_LOG is '文件发布任务记录';

CREATE TABLE IF NOT EXISTS PUBLIC.FILE_RELEASE_TASK_TEMPLATE
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT not null comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `name` VARCHAR(255) not null comment '名称',
    `templateTag` VARCHAR(50) not null comment '模板标记',
    `fileType` TINYINT comment '文件类型',
    `data` CLOB comment '发布之前的脚本',
    CONSTRAINT FILE_RELEASE_TASK_TEMPLATE_PK PRIMARY KEY (id)
);
COMMENT ON TABLE FILE_RELEASE_TASK_TEMPLATE is '文件发布任务记录';

CREATE TABLE IF NOT EXISTS PUBLIC.FILE_STORAGE
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT not null comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `createUser` VARCHAR(50) not null comment '创建人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `name` VARCHAR(255) not null comment '名称',
    `description` VARCHAR(255) comment '描述',
    `size` BIGINT not null comment '文件大小',
    `source` TINYINT not null comment '文件来源',
    `validUntil` BIGINT not null comment '文件有效期',
    `path` VARCHAR(255) not null comment '文件路径',
    `extName` VARCHAR(50) not null comment '文件后缀',
    `status` TINYINT comment '0 下载中 1 下载完成 3 下载异常',
    `progressDesc` VARCHAR(255) comment '进度描述',
    `triggerToken` VARCHAR(200) comment '触发器token',
    `aliasCode` VARCHAR(50) comment '别名码',
    CONSTRAINT FILE_STORAGE_PK PRIMARY KEY (id)
);
COMMENT ON TABLE FILE_STORAGE is '文件管理中心';

CREATE TABLE IF NOT EXISTS PUBLIC.K8S_CLUSTER
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `name` VARCHAR(50) not null comment '集群名称',
    `kubeconfig` CLOB comment 'kubeconfig内容',
    `serverUrl` VARCHAR(100) comment '集群服务地址',
    `namespace` VARCHAR(50) comment '默认命名空间',
    `remark` VARCHAR(200) comment '备注',
    CONSTRAINT K8S_CLUSTER_PK PRIMARY KEY (id)
);
COMMENT ON TABLE K8S_CLUSTER is 'K8s集群';

CREATE TABLE IF NOT EXISTS PUBLIC.LOG_READ
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `name` VARCHAR(50) not null comment '日志项目名称',
    `nodeProject` CLOB comment '节点下的项目列表',
    `cacheData` CLOB comment '缓存操作数据',
    CONSTRAINT LOG_READ_PK PRIMARY KEY (id)
);
COMMENT ON TABLE LOG_READ is '日志阅读';

CREATE TABLE IF NOT EXISTS PUBLIC.MACHINE_DOCKER_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `groupName` VARCHAR(50) not null comment '分组名称',
    `name` VARCHAR(255) not null comment '名称',
    `host` VARCHAR(255) comment 'docker host',
    `tlsVerify` TINYINT default '0' comment 'tls 认证{1，启用，0 未启用}',
    `status` TINYINT default '0' comment '状态{1，启用，0 未启用}',
    `failureMsg` VARCHAR(255) comment '错误消息',
    `heartbeatTimeout` INTEGER comment '心跳超时时间',
    `lastHeartbeatTime` BIGINT comment '最后心跳时间',
    `dockerVersion` CLOB comment 'docker 版本信息',
    `swarmId` VARCHAR(50) comment '集群ID',
    `swarmNodeId` VARCHAR(50) comment '集群 节点ID',
    `registryUsername` VARCHAR(255) comment '仓库账号',
    `registryPassword` VARCHAR(255) comment '仓库密码',
    `registryEmail` VARCHAR(255) comment '仓库邮箱',
    `registryUrl` VARCHAR(255) comment '仓库地址',
    `swarmControlAvailable` TINYINT comment '集群管理员',
    `swarmCreatedAt` BIGINT comment '集群的创建时间',
    `swarmUpdatedAt` BIGINT comment '集群的更新时间',
    `swarmNodeAddr` VARCHAR(50) comment '节点 地址',
    `certInfo` VARCHAR(100) comment '证书信息',
    `certExist` TINYINT comment '证书是否存在',
    `enableSsh` TINYINT comment '是否开启SSH连接',
    `machineSshId` VARCHAR(255) comment 'SSH连接信息',
    `sshUseSudo` TINYINT comment '是否使用 sudo 执行命令',
    CONSTRAINT MACHINE_DOCKER_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE MACHINE_DOCKER_INFO is '机器DOCKER信息';

CREATE TABLE IF NOT EXISTS PUBLIC.MACHINE_NODE_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `name` VARCHAR(50) not null comment '机器名称',
    `groupName` VARCHAR(50) not null comment '分组名称',
    `hostName` VARCHAR(255) comment '机器主机名',
    `hostIpv4s` CLOB comment '机器所有 IP',
    `osLoadAverage` VARCHAR(100) comment '系统负载',
    `osSystemUptime` BIGINT comment '系统运行时间（自启动以来的时间）',
    `osVersion` VARCHAR(255) comment '系统版本',
    `osHardwareVersion` VARCHAR(255) comment '硬件版本',
    `osCpuCores` INTEGER comment 'CPU数',
    `osMoneyTotal` BIGINT comment '总内存',
    `osFileStoreTotal` BIGINT comment '硬盘大小',
    `osCpuIdentifierName` VARCHAR(255) comment 'CPU 型号',
    `osName` VARCHAR(50) comment '系统名称',
    `status` TINYINT not null comment '节点连接状态：0 未连接，1 连接中',
    `statusMsg` CLOB comment '状态消息',
    `transportMode` TINYINT not null comment '传输方式。0 服务器拉取，1 节点机器推送',
    `voyager1Url` VARCHAR(100) comment '节点 url IP:PORT',
    `voyager1Username` VARCHAR(100) comment '节点登录名',
    `voyager1Password` VARCHAR(100) comment '节点密码',
    `voyager1Protocol` VARCHAR(10) comment '协议 http https',
    `voyager1Timeout` INTEGER comment '节点超时时间',
    `voyager1HttpProxy` VARCHAR(200) comment 'http 代理',
    `voyager1HttpProxyType` VARCHAR(20) comment 'http 代理类型',
    `voyager1Version` VARCHAR(50) comment 'voyager1版本号',
    `voyager1Uptime` BIGINT comment 'voyager1运行时间',
    `voyager1BuildTime` VARCHAR(50) comment 'Voyager1 打包时间',
    `voyager1ProjectCount` INTEGER comment 'voyager1项目数',
    `voyager1ScriptCount` INTEGER comment 'voyager1脚本数',
    `networkDelay` INTEGER comment '网络耗时（延迟）',
    `javaVersion` VARCHAR(50) comment 'java版本',
    `jvmTotalMemory` BIGINT comment 'jvm 总内存',
    `jvmFreeMemory` BIGINT comment 'jvm 剩余内存',
    `osOccupyCpu` DOUBLE comment '占用cpu',
    `osOccupyMemory` DOUBLE comment '占用内存',
    `osOccupyDisk` DOUBLE comment '占用磁盘',
    `templateNode` TINYINT comment '模板节点 ，1 模板节点 0 非模板节点',
    `installId` VARCHAR(50) comment '机器安装 id',
    `osSwapTotal` BIGINT comment '虚拟总内存',
    `osVirtualMax` BIGINT comment '交互总内存',
    `transportEncryption` TINYINT comment '传输加密方式 0 不加密 1 BASE64 2 AES',
    `extendInfo` CLOB comment '扩展信息',
    CONSTRAINT MACHINE_NODE_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE MACHINE_NODE_INFO is '机器节点信息';

CREATE TABLE IF NOT EXISTS PUBLIC.MACHINE_NODE_STAT_LOG
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `machineId` VARCHAR(50) not null comment '机器id',
    `occupyCpu` DOUBLE comment '占用cpu',
    `occupyMemory` DOUBLE comment '占用内存',
    `occupyDisk` DOUBLE comment '占用磁盘',
    `networkDelay` INTEGER default '0' comment '网络耗时',
    `monitorTime` BIGINT not null comment '监控的时间',
    `netTxBytes` BIGINT comment '每秒发送的KB数,rxkB/s',
    `netRxBytes` BIGINT comment '每秒接收的KB数,rxkB/s',
    `occupySwapMemory` DOUBLE comment '交互内存',
    `occupyVirtualMemory` DOUBLE comment '虚拟内存',
    `cpuTicks` CLOB comment 'CPU负载信息',
    CONSTRAINT MACHINE_NODE_STAT_LOG_PK PRIMARY KEY (id, machineId)
);
COMMENT ON TABLE MACHINE_NODE_STAT_LOG is '资产机器节点统计';

CREATE TABLE IF NOT EXISTS PUBLIC.MACHINE_SSH_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `name` VARCHAR(50) comment '名称',
    `groupName` VARCHAR(50) not null comment '分组名称',
    `host` VARCHAR(100) not null comment 'ssh host IP',
    `port` INTEGER not null comment '端口',
    `user` VARCHAR(100) not null comment '用户',
    `password` VARCHAR(100) comment '密码',
    `charset` VARCHAR(100) comment '编码格式',
    `privateKey` CLOB comment '私钥',
    `connectType` VARCHAR(10) comment '连接方式',
    `timeout` INTEGER default '0' comment '节点超时时间',
    `status` TINYINT not null comment '状态{0，无法连接，1 正常}',
    `statusMsg` CLOB comment '状态消息',
    `allowEditSuffix` CLOB comment '允许编辑的后缀文件',
    `osName` VARCHAR(50) comment '系统名称',
    `hostName` VARCHAR(255) comment '机器主机名',
    `osLoadAverage` VARCHAR(100) comment '系统负载',
    `osSystemUptime` BIGINT comment '系统运行时间（自启动以来的时间）',
    `osVersion` VARCHAR(255) comment '系统版本',
    `osCpuCores` INTEGER comment 'CPU数',
    `osMoneyTotal` BIGINT comment '总内存',
    `osFileStoreTotal` BIGINT comment '硬盘大小',
    `osCpuIdentifierName` VARCHAR(255) comment 'CPU 型号',
    `osOccupyCpu` DOUBLE comment '占用cpu',
    `osOccupyMemory` DOUBLE comment '占用内存',
    `osMaxOccupyDisk` DOUBLE comment '占用磁盘',
    `osMaxOccupyDiskName` VARCHAR(255) comment '占用磁盘 分区名',
    `javaVersion` VARCHAR(255) comment 'java版本',
    `voyager1AgentPid` INTEGER comment 'voyager1 agent进程号',
    `dockerInfo` VARCHAR(255) comment '服务器中的 docker 信息',
    CONSTRAINT MACHINE_SSH_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE MACHINE_SSH_INFO is 'ssh信息表';

CREATE TABLE IF NOT EXISTS PUBLIC.MONITORNOTIFYLOG
(
    `id` VARCHAR(50) not null comment '记录id',
    `monitorId` VARCHAR(50) comment '监控id',
    `nodeId` VARCHAR(50) comment '节点ID',
    `projectId` VARCHAR(30) comment '项目id',
    `createTime` BIGINT comment '异常时间',
    `title` VARCHAR(500) comment '异常描述',
    `content` CLOB comment '异常内容',
    `status` TINYINT comment '当前状态',
    `notifyStyle` TINYINT comment '通知方式',
    `notifyStatus` TINYINT comment '通知状态',
    `notifyObject` CLOB comment '通知对象',
    `notifyError` CLOB comment '通知异常内容',
    `workspaceId` VARCHAR(50) comment '所属工作空间',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '操作人',
    CONSTRAINT MONITORNOTIFYLOG_PK PRIMARY KEY (id)
);
COMMENT ON TABLE MONITORNOTIFYLOG is '监控异常日志记录';

CREATE TABLE IF NOT EXISTS PUBLIC.MONITOR_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `name` VARCHAR(50) not null comment '名称',
    `autoRestart` TINYINT default '0' comment '是否自动重启{1，是，0 否}',
    `status` TINYINT default '0' comment '启用状态{1，启用，0 未启用}',
    `alarm` TINYINT default '0' comment '报警状态{1，报警中，0 未报警}',
    `cycle` INTEGER default '0' comment '监控周期',
    `notifyUser` CLOB comment '报警联系人',
    `projects` CLOB comment '监控的项目',
    `execCron` VARCHAR(100) comment '自动执行表达式',
    `webhook` VARCHAR(255) comment 'webhook',
    `useLanguage` VARCHAR(20) comment '使用语言',
    `silenceTime` INTEGER comment '沉默时间',
    `silenceUnit` VARCHAR(20) comment '沉默单位',
    CONSTRAINT MONITOR_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE MONITOR_INFO is '监控信息';

CREATE TABLE IF NOT EXISTS PUBLIC.MONITOR_USER_OPT
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `name` VARCHAR(50) not null comment '名称',
    `monitorUser` CLOB comment '监控的人',
    `status` TINYINT default '0' comment '启用状态{1，启用，0 未启用}',
    `notifyUser` CLOB comment '报警联系人',
    `monitorFeature` CLOB comment '监控的项目',
    `monitorOpt` CLOB comment '监控的项目',
    CONSTRAINT MONITOR_USER_OPT_PK PRIMARY KEY (id)
);
COMMENT ON TABLE MONITOR_USER_OPT is '监控信息';

CREATE TABLE IF NOT EXISTS PUBLIC.NODE_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `name` VARCHAR(50) comment '名称',
    `url` VARCHAR(100) not null comment '节点 url IP:PORT',
    `loginName` VARCHAR(100) not null comment '节点登录名',
    `loginPwd` VARCHAR(100) not null comment '节点密码',
    `protocol` VARCHAR(10) not null comment '协议 http https',
    `openStatus` INTEGER default '0' comment '启用状态{1，启用，0 未启用)}',
    `timeOut` INTEGER default '0' comment '节点超时时间',
    `sshId` VARCHAR(50) comment '关联的sshid',
    `group` VARCHAR(50) comment '分组名称',
    `httpProxy` VARCHAR(200) comment 'http 代理',
    `httpProxyType` VARCHAR(20) comment 'http 代理类型',
    `sortValue` REAL comment '排序值',
    `machineId` VARCHAR(50) comment '机器id',
    `voyager1ProjectCount` INTEGER comment 'voyager1项目数',
    `voyager1ScriptCount` INTEGER comment 'voyager1脚本数',
    CONSTRAINT NODE_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE NODE_INFO is '节点信息表';

CREATE TABLE IF NOT EXISTS PUBLIC.OUTGIVINGLOG
(
    `id` VARCHAR(50) not null comment 'id',
    `outGivingId` VARCHAR(50) comment '分发id',
    `status` TINYINT comment '状态',
    `startTime` BIGINT comment '开始时间',
    `endTime` BIGINT comment '结束时间',
    `result` CLOB comment '消息',
    `nodeId` VARCHAR(100) comment '节点id',
    `projectId` VARCHAR(100) comment '项目id',
    `workspaceId` VARCHAR(50) comment '所属工作空间',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '操作人',
    `fileSize` BIGINT comment '文件大小',
    `progressSize` BIGINT comment '当前进度',
    `mode` VARCHAR(50) comment '分发方式',
    `modeData` VARCHAR(500) comment '分发方式数据',
    CONSTRAINT OUTGIVINGLOG_PK PRIMARY KEY (id)
);
COMMENT ON TABLE OUTGIVINGLOG is '分发日志';

CREATE TABLE IF NOT EXISTS PUBLIC.OUT_GIVING
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `name` VARCHAR(50) not null comment '名称',
    `afterOpt` INTEGER default '0' comment '分发后的操作',
    `clearOld` TINYINT default '0' comment '是否清空旧包发布',
    `outGivingProject` TINYINT default '0' comment '是否为单独创建的分发项目',
    `outGivingNodeProjectList` CLOB comment '分发项目信息',
    `intervalTime` INTEGER default '10' comment '分发间隔时间',
    `status` INTEGER default '0' comment '状态{0: 未分发; 1: 分发中; 2: 分发结束}',
    `secondaryDirectory` VARCHAR(200) comment '二级目录',
    `uploadCloseFirst` TINYINT default '0' comment '是否清空旧包发布',
    `statusMsg` VARCHAR(255) comment '分发状态信息',
    `group` VARCHAR(50) comment '项目分组',
    `webhook` VARCHAR(255) comment 'webhook',
    `mode` VARCHAR(50) comment '分发方式',
    `modeData` VARCHAR(500) comment '分发方式数据',
    CONSTRAINT OUT_GIVING_PK PRIMARY KEY (id)
);
COMMENT ON TABLE OUT_GIVING is '节点分发信息';

CREATE TABLE IF NOT EXISTS PUBLIC.PIPELINE_CONFIG
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `name` VARCHAR(50) comment '名称',
    `buildId` VARCHAR(50) not null comment '关联构建配置id',
    `triggers` CLOB comment '触发配置JSON',
    `stages` CLOB comment '阶段配置JSON',
    `enabled` INTEGER default '1' comment '是否启用',
    `remark` VARCHAR(200) comment '备注',
    CONSTRAINT PIPELINE_CONFIG_PK PRIMARY KEY (id)
);
COMMENT ON TABLE PIPELINE_CONFIG is 'Pipeline配置';

CREATE TABLE IF NOT EXISTS PUBLIC.PIPELINE_EXECUTE_RECORD
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `pipelineId` VARCHAR(50) not null comment '关联Pipeline配置id',
    `triggerType` VARCHAR(20) comment '触发类型',
    `status` INTEGER default '0' comment '执行状态',
    `currentStage` VARCHAR(50) comment '当前阶段id',
    `stages` CLOB comment '阶段状态快照JSON',
    `startTime` BIGINT comment '开始时间',
    `endTime` BIGINT comment '结束时间',
    `operator` VARCHAR(50) comment '触发人',
    CONSTRAINT PIPELINE_EXECUTE_RECORD_PK PRIMARY KEY (id)
);
COMMENT ON TABLE PIPELINE_EXECUTE_RECORD is 'Pipeline执行记录';

CREATE TABLE IF NOT EXISTS PUBLIC.PROJECT_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `projectId` VARCHAR(50) not null comment '项目ID',
    `nodeId` VARCHAR(50) not null comment '节点ID',
    `name` VARCHAR(50) not null comment '名称',
    `mainClass` VARCHAR(100) comment 'mainClas',
    `lib` VARCHAR(100) comment 'lib',
    `whitelistDirectory` VARCHAR(100) comment 'whitelistDirectory',
    `logPath` VARCHAR(100) comment 'logPath',
    `jvm` CLOB comment 'jvm',
    `args` CLOB comment 'args',
    `javaCopyItemList` CLOB comment 'javaCopyItemList',
    `token` VARCHAR(255) comment 'token',
    `runMode` VARCHAR(20) comment '连接方式',
    `outGivingProject` TINYINT default '0' comment '分发项目{1，分发，0 独立项目}',
    `javaExtDirsCp` CLOB comment 'javaExtDirsCp',
    `sortValue` REAL comment '排序值',
    `triggerToken` VARCHAR(100) comment '触发器token',
    `group` VARCHAR(50) comment '项目分组',
    `dslContent` CLOB comment 'dslContent',
    `autoStart` TINYINT comment '在启动',
    `nodeName` VARCHAR(50) comment '节点名称',
    `workspaceName` VARCHAR(50) comment '工作空间名称',
    CONSTRAINT PROJECT_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE PROJECT_INFO is '项目信息表';

CREATE TABLE IF NOT EXISTS PUBLIC.REPOSITORY
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `name` VARCHAR(50) comment '仓库名称',
    `gitUrl` VARCHAR(255) comment '仓库地址',
    `repoType` INTEGER comment '仓库类型{0: GIT, 1: SVN}',
    `protocol` INTEGER comment '拉取代码的协议{0: http, 1: ssh}',
    `userName` VARCHAR(50) comment '登录用户',
    `password` VARCHAR(255) comment '登录密码',
    `rsaPub` VARCHAR(2048) comment 'SSH RSA 公钥',
    `rsaPrv` VARCHAR(4096) comment 'SSH RSA 私钥',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) comment '所属工作空间',
    `sortValue` REAL comment '排序值',
    `timeout` INTEGER comment '仓库超时连接',
    `createUser` VARCHAR(50) comment '创建人',
    `group` VARCHAR(50) comment '分组',
    CONSTRAINT REPOSITORY_PK PRIMARY KEY (id)
);
COMMENT ON TABLE REPOSITORY is '仓库信息';

CREATE TABLE IF NOT EXISTS PUBLIC.SCRIPT_EXECUTE_LOG
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `nodeId` VARCHAR(50) not null comment '节点ID',
    `scriptId` VARCHAR(50) not null comment '脚本ID',
    `scriptName` VARCHAR(100) comment '脚本名称',
    `triggerExecType` INTEGER default '0' comment '触发类型{0，手动，1 自动触发}',
    `nodeName` VARCHAR(50) comment '节点名称',
    `workspaceName` VARCHAR(50) comment '工作空间名称',
    CONSTRAINT SCRIPT_EXECUTE_LOG_PK PRIMARY KEY (id)
);
COMMENT ON TABLE SCRIPT_EXECUTE_LOG is '节点脚本模版执行记录';

CREATE TABLE IF NOT EXISTS PUBLIC.SCRIPT_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `nodeId` VARCHAR(50) not null comment '节点ID',
    `scriptId` VARCHAR(50) not null comment '脚本ID',
    `name` VARCHAR(50) not null comment '名称',
    `lastRunUser` VARCHAR(50) comment '最后执行人',
    `autoExecCron` VARCHAR(100) comment '自动执行表达式',
    `defArgs` CLOB comment '默认参数',
    `description` VARCHAR(255) comment '描述',
    `scriptType` VARCHAR(100) comment '脚本类型',
    `triggerToken` VARCHAR(200) comment '触发器token',
    `createUser` VARCHAR(50) comment '创建人',
    `nodeName` VARCHAR(50) comment '节点名称',
    `workspaceName` VARCHAR(50) comment '工作空间名称',
    CONSTRAINT SCRIPT_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE SCRIPT_INFO is '节点脚本模版';

CREATE TABLE IF NOT EXISTS PUBLIC.SCRIPT_LIBRARY
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `createUser` VARCHAR(50) comment '创建人',
    `tag` VARCHAR(50) not null comment '标签',
    `description` VARCHAR(255) comment '描述',
    `script` CLOB comment '描述',
    `machineIds` CLOB comment '关联的机器节点',
    `version` VARCHAR(50) not null comment '版本',
    CONSTRAINT SCRIPT_LIBRARY_PK PRIMARY KEY (id)
);
COMMENT ON TABLE SCRIPT_LIBRARY is '静态文件管理';

CREATE TABLE IF NOT EXISTS PUBLIC.SERVER_SCRIPT_EXECUTE_LOG
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `scriptId` VARCHAR(50) not null comment '脚本ID',
    `scriptName` VARCHAR(100) comment '脚本名称',
    `triggerExecType` INTEGER default '0' comment '触发类型{0，手动，1 自动触发}',
    `exitCode` INTEGER comment '退出码',
    `status` TINYINT comment '执行状态',
    CONSTRAINT SERVER_SCRIPT_EXECUTE_LOG_PK PRIMARY KEY (id)
);
COMMENT ON TABLE SERVER_SCRIPT_EXECUTE_LOG is '脚本模版执行记录';

CREATE TABLE IF NOT EXISTS PUBLIC.SERVER_SCRIPT_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `name` VARCHAR(50) not null comment '名称',
    `lastRunUser` VARCHAR(50) comment '最后执行人',
    `autoExecCron` VARCHAR(100) comment '自动执行表达式',
    `defArgs` VARCHAR(100) comment '默认参数',
    `context` CLOB comment '内容',
    `description` VARCHAR(255) comment '描述',
    `nodeIds` CLOB comment '绑定的节点 id',
    `triggerToken` VARCHAR(200) comment '触发器token',
    `createUser` VARCHAR(50) comment '创建人',
    CONSTRAINT SERVER_SCRIPT_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE SERVER_SCRIPT_INFO is '脚本模版';

CREATE TABLE IF NOT EXISTS PUBLIC.SSHTERMINALEXECUTELOG
(
    `id` VARCHAR(50) not null comment 'id',
    `ip` VARCHAR(80) comment '客户端IP地址',
    `userId` VARCHAR(30) comment '操作的用户ID',
    `userAgent` CLOB comment '浏览器标识',
    `commands` CLOB comment '操作的命令',
    `sshId` VARCHAR(50) comment '操作的sshid',
    `sshName` VARCHAR(50) comment '操作的ssh name',
    `refuse` INTEGER comment '拒绝执行',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `workspaceId` VARCHAR(50) comment '所属工作空间',
    `modifyUser` VARCHAR(50) comment '操作人',
    `machineSshId` VARCHAR(50) comment '机器sshid',
    `machineSshName` VARCHAR(50) comment '机器ssh 名称',
    CONSTRAINT SSHTERMINALEXECUTELOG_PK PRIMARY KEY (id)
);
COMMENT ON TABLE SSHTERMINALEXECUTELOG is 'ssh 终端操作记录表';

CREATE TABLE IF NOT EXISTS PUBLIC.SSH_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `name` VARCHAR(50) comment '名称',
    `host` VARCHAR(100) not null comment 'ssh host IP',
    `port` INTEGER not null comment '端口',
    `user` VARCHAR(100) not null comment '用户',
    `password` VARCHAR(100) comment '密码',
    `charset` VARCHAR(100) comment '编码格式',
    `fileDirs` CLOB comment '文件目录',
    `privateKey` CLOB comment '私钥',
    `connectType` VARCHAR(10) comment '连接方式',
    `notAllowedCommand` CLOB comment '不允许执行的命令',
    `allowEditSuffix` CLOB comment '允许编辑的后缀文件',
    `timeout` INTEGER default '0' comment '节点超时时间',
    `group` VARCHAR(50) comment '分组',
    `machineSshId` VARCHAR(50) comment '机器sshid',
    CONSTRAINT SSH_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE SSH_INFO is 'ssh信息表';

CREATE TABLE IF NOT EXISTS PUBLIC.STATIC_FILE_STORAGE
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `name` VARCHAR(100) comment '文件名',
    `description` VARCHAR(255) comment '描述',
    `extName` VARCHAR(50) comment '扩展名',
    `absolutePath` VARCHAR(300) comment '文件路径',
    `parentAbsolutePath` VARCHAR(300) comment '父级文件路径',
    `staticDir` VARCHAR(50) comment '配置的静态路径',
    `status` TINYINT comment '状态 0 不存在 1 存在',
    `type` TINYINT comment '类型 0 文件夹 1 文件',
    `scanTaskId` BIGINT comment '扫描任务id',
    `lastModified` BIGINT comment '最后修改时间',
    `size` BIGINT comment '文件大小',
    `level` INTEGER comment '层级',
    `triggerToken` VARCHAR(100) comment '触发器token',
    CONSTRAINT STATIC_FILE_STORAGE_PK PRIMARY KEY (id)
);
COMMENT ON TABLE STATIC_FILE_STORAGE is '静态文件管理';

CREATE TABLE IF NOT EXISTS PUBLIC.SYSTEM_PARAMETERS
(
    `id` VARCHAR(100) not null comment 'ID',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `value` CLOB comment '参数值，JSON 字符串格式',
    `description` VARCHAR(255) comment '参数描述',
    CONSTRAINT SYSTEM_PARAMETERS_PK PRIMARY KEY (id)
);
COMMENT ON TABLE SYSTEM_PARAMETERS is '系统参数表';

CREATE TABLE IF NOT EXISTS PUBLIC.TRIGGER_TOKEN_LOG
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `triggerToken` VARCHAR(100) not null comment 'triggerToken',
    `type` VARCHAR(50) not null comment 'token 类型',
    `dataId` VARCHAR(50) not null comment '关联数据ID',
    `userId` VARCHAR(50) not null comment '用户ID',
    `triggerCount` INTEGER comment '触发次数',
    CONSTRAINT TRIGGER_TOKEN_LOG_PK PRIMARY KEY (id)
);
COMMENT ON TABLE TRIGGER_TOKEN_LOG is '触发器 token';

CREATE TABLE IF NOT EXISTS PUBLIC.USEROPERATELOGV1
(
    `id` VARCHAR(50) not null comment 'id',
    `ip` VARCHAR(80) comment '客户端IP地址',
    `userId` VARCHAR(50) comment '操作的用户ID',
    `resultMsg` CLOB comment '操作的结果信息',
    `optStatus` INTEGER comment '操作状态 成功/失败',
    `optTime` BIGINT comment '操作时间',
    `nodeId` VARCHAR(50) comment '节点ID',
    `dataId` VARCHAR(200) comment '操作的数据ID',
    `userAgent` CLOB comment '浏览器标识',
    `reqData` CLOB comment '用户请求参数',
    `workspaceId` VARCHAR(50) comment '所属工作空间',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '操作人',
    `classFeature` VARCHAR(100) comment '操作的功能',
    `methodFeature` VARCHAR(100) comment '操作的方法',
    `dataName` VARCHAR(200) comment '数据名称',
    `workspaceName` VARCHAR(50) comment '工作空间名',
    `username` VARCHAR(50) comment '用户名',
    CONSTRAINT USEROPERATELOGV1_PK PRIMARY KEY (id)
);
COMMENT ON TABLE USEROPERATELOGV1 is '操作日志';

CREATE TABLE IF NOT EXISTS PUBLIC.USER_BIND_WORKSPACE
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `userId` VARCHAR(50) not null comment '用户ID',
    `workspaceId` VARCHAR(100) not null comment '工作空间ID',
    CONSTRAINT USER_BIND_WORKSPACE_PK PRIMARY KEY (id)
);
COMMENT ON TABLE USER_BIND_WORKSPACE is '用户工作空间绑定表';

CREATE TABLE IF NOT EXISTS PUBLIC.USER_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `parent` VARCHAR(50) not null comment '创建人',
    `name` VARCHAR(50) comment '昵称',
    `systemUser` INTEGER default '0' comment '是否为系统管理员 {1，是，0 否(默认)}',
    `password` VARCHAR(100) comment '密码',
    `salt` VARCHAR(50) comment '盐值',
    `pwdErrorCount` INTEGER default '0' comment '密码错误次数',
    `lastPwdErrorTime` BIGINT default '0' comment '最后登录失败时间',
    `lockTime` BIGINT default '0' comment '锁定时长',
    `email` VARCHAR(255) comment '邮箱地址',
    `dingDing` VARCHAR(255) comment '钉钉地址',
    `workWx` VARCHAR(255) comment '企业微信地址',
    `status` TINYINT comment '状态 0 禁用  null、1 启用',
    `permissionGroup` CLOB comment '权限组',
    `source` VARCHAR(100) comment '账号来源',
    CONSTRAINT USER_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE USER_INFO is '用户信息表';

CREATE TABLE IF NOT EXISTS PUBLIC.USER_LOGIN_LOG
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `ip` VARCHAR(80) comment '客户端IP地址',
    `userAgent` CLOB comment '浏览器标识',
    `success` TINYINT comment '是否登录成功',
    `operateCode` TINYINT comment '操作状态码（备注码）',
    `username` VARCHAR(50) comment '昵称',
    CONSTRAINT USER_LOGIN_LOG_PK PRIMARY KEY (id)
);
COMMENT ON TABLE USER_LOGIN_LOG is '用户登录日志';

CREATE TABLE IF NOT EXISTS PUBLIC.USER_PERMISSION_GROUP
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `name` VARCHAR(250) not null comment '名称',
    `description` VARCHAR(255) comment '描述',
    `prohibitExecute` CLOB comment '禁止执行的配置',
    `allowExecute` CLOB comment '允许执行的配置',
    CONSTRAINT USER_PERMISSION_GROUP_PK PRIMARY KEY (id)
);
COMMENT ON TABLE USER_PERMISSION_GROUP is '用户权限组';

CREATE TABLE IF NOT EXISTS PUBLIC.VERSION_INFO
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `buildId` VARCHAR(50) not null comment '关联构建配置id（应用）',
    `buildNumberId` INTEGER comment '构建记录编号',
    `version` VARCHAR(50) comment '版本号',
    `status` INTEGER default '0' comment '状态（VersionStatus）',
    `artifactRef` VARCHAR(200) comment '产物引用',
    `remark` VARCHAR(200) comment '备注',
    `groupName` VARCHAR(50) comment '归属分组',
    CONSTRAINT VERSION_INFO_PK PRIMARY KEY (id)
);
COMMENT ON TABLE VERSION_INFO is '版本信息';

CREATE TABLE IF NOT EXISTS PUBLIC.WORKSPACE
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `name` VARCHAR(50) comment '名称',
    `description` VARCHAR(255) comment '描述',
    `group` VARCHAR(50) comment '分组',
    `clusterInfoId` VARCHAR(50) comment '绑定集群id',
    CONSTRAINT WORKSPACE_PK PRIMARY KEY (id)
);
COMMENT ON TABLE WORKSPACE is '工作空间表';

CREATE TABLE IF NOT EXISTS PUBLIC.WORKSPACE_ENV_VAR
(
    `id` VARCHAR(50) not null comment 'id',
    `createTimeMillis` BIGINT comment '数据创建时间',
    `modifyTimeMillis` BIGINT comment '数据修改时间',
    `modifyUser` VARCHAR(50) comment '修改人',
    `workspaceId` VARCHAR(50) not null comment '所属工作空间',
    `name` VARCHAR(50) comment '名称',
    `description` VARCHAR(255) comment '参数描述',
    `value` CLOB comment '值',
    `nodeIds` CLOB comment '绑定的节点 id',
    `privacy` TINYINT default '0' comment '隐私变量{1，隐私变量，0 非隐私变量（明文回显）}',
    `triggerToken` VARCHAR(100) comment '触发器token',
    CONSTRAINT WORKSPACE_ENV_VAR_PK PRIMARY KEY (id)
);
COMMENT ON TABLE WORKSPACE_ENV_VAR is '用户信息表';
CREATE UNIQUE INDEX IF NOT EXISTS USER_INF_SALT_INDEX1 ON PUBLIC.USER_INFO (salt);
CREATE INDEX IF NOT EXISTS DIR_TASK_ID ON PUBLIC.STATIC_FILE_STORAGE (staticDir, scanTaskId);
CREATE INDEX IF NOT EXISTS DIR_ABS_PATH ON PUBLIC.STATIC_FILE_STORAGE (staticDir, absolutePath);
CREATE INDEX IF NOT EXISTS DIR_PARENT_PATH ON PUBLIC.STATIC_FILE_STORAGE (staticDir, parentAbsolutePath);
CREATE INDEX IF NOT EXISTS TRIGGER_TOKEN_TYPE ON PUBLIC.TRIGGER_TOKEN_LOG (type);
CREATE INDEX IF NOT EXISTS TRIGGER_TOKEN_USER_ID ON PUBLIC.TRIGGER_TOKEN_LOG (userId);
