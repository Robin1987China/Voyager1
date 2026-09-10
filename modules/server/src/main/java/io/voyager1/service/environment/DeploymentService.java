/*
 * Copyright (c) 2026 Voyager1
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.voyager1.service.environment;

import io.voyager1.build.BuildExecutorPoolService;
import io.voyager1.build.BuildExtraModule;
import io.voyager1.build.BuildUtil;
import io.voyager1.build.ReleaseManage;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.core.entity.DeploymentRecordEntity;
import io.voyager1.core.repository.DeploymentRecordRepository;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.data.DeploymentRecordModel;
import io.voyager1.model.data.EnvironmentModel;
import io.voyager1.model.data.EnvironmentTargetModel;
import io.voyager1.model.data.VersionModel;
import io.voyager1.model.enums.BuildReleaseMethod;
import io.voyager1.model.enums.BuildStatus;
import io.voyager1.model.enums.VersionStatus;
import io.voyager1.model.log.BuildHistoryLog;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.dblog.BuildInfoService;
import io.voyager1.service.dblog.DbBuildHistoryLogService;
import io.voyager1.service.k8s.K8sService;
import io.voyager1.service.version.VersionService;
import io.voyager1.util.FileUtil;
import io.voyager1.util.LogRecorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 部署服务（部署记录 + 版本按环境真实发布）。
 * <p>
 * 环境化 CI/CD：{@link #deployPublish} 把版本部署到环境绑定的目标（节点），复用 {@link ReleaseManage} 发布执行。
 * 关键约束：
 * <ul>
 *   <li>部署挂版本状态机：已打回版本不可部署；prod 仅已发布版本、test 仅已提测/已发布版本</li>
 *   <li>每次部署创建独立的发布记录（新 CI_BUILD_LOG），不回写构建配置状态，避免污染构建状态机</li>
 *   <li>发布动作异步执行，HTTP 同步只做校验与落部署记录</li>
 *   <li>需审批环境先落「待审批」记录，经 {@link #approve} 批准后异步执行，拒绝则闭环为「已拒绝」</li>
 * </ul>
 *
 * @since 2026/8/8
 */
@Service
@Slf4j
public class DeploymentService {

    /**
     * 部署记录状态：成功
     */
    public static final int STATUS_SUCCESS = 0;
    /**
     * 部署记录状态：失败
     */
    public static final int STATUS_FAILED = 1;
    /**
     * 部署记录状态：进行中
     */
    public static final int STATUS_RUNNING = 2;
    /**
     * 部署记录状态：待审批
     */
    public static final int STATUS_PENDING_APPROVAL = 3;
    /**
     * 部署记录状态：已拒绝
     */
    public static final int STATUS_REJECTED = 4;

    /**
     * 发布记录触发类型：环境部署（0 手动 1 触发器 2 自动触发 3 手动回滚 4 环境部署）
     */
    private static final int TRIGGER_TYPE_ENV_DEPLOY = 4;

    private final DeploymentRecordRepository repository;
    private final VersionService versionService;
    private final EnvironmentService environmentService;
    private final BuildInfoService buildInfoService;
    private final DbBuildHistoryLogService dbBuildHistoryLogService;
    private final BuildExecutorPoolService buildExecutorPoolService;

    public DeploymentService(DeploymentRecordRepository repository,
                             VersionService versionService,
                             EnvironmentService environmentService,
                             BuildInfoService buildInfoService,
                             DbBuildHistoryLogService dbBuildHistoryLogService,
                             BuildExecutorPoolService buildExecutorPoolService) {
        this.repository = repository;
        this.versionService = versionService;
        this.environmentService = environmentService;
        this.buildInfoService = buildInfoService;
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
        this.buildExecutorPoolService = buildExecutorPoolService;
    }

    /**
     * 创建部署记录
     *
     * @param versionId   版本 id
     * @param environment 环境
     * @param mode        auto/manual
     * @param operator    操作者
     * @param status      {@link #STATUS_SUCCESS} 成功 {@link #STATUS_FAILED} 失败 {@link #STATUS_RUNNING} 进行中
     *                    {@link #STATUS_PENDING_APPROVAL} 待审批 {@link #STATUS_REJECTED} 已拒绝
     * @param logRef      日志引用
     * @return 记录 id
     */
    @Transactional
    public String createRecord(String versionId, String environment, String mode, String operator, int status, String logRef) {
        Assert.hasText(versionId, "版本不能为空");
        Assert.hasText(environment, "环境不能为空");
        VersionModel version = versionService.getByKey(versionId);
        Assert.notNull(version, "版本不存在: " + versionId);
        long now = System.currentTimeMillis();
        DeploymentRecordEntity record = new DeploymentRecordEntity();
        record.setId(UUID.randomUUID().toString());
        record.setCreateTimeMillis(now);
        record.setModifyTimeMillis(now);
        record.setBuildId(version.getBuildId());
        record.setVersionId(versionId);
        record.setVersion(version.getVersion());
        record.setEnvironment(environment);
        record.setMode(mode);
        record.setOperator(operator);
        record.setStatus(status);
        record.setLogRef(logRef);
        repository.save(record);
        log.info("部署记录: version={} env={} mode={} operator={} status={}", version.getVersion(), environment, mode, operator, status);
        return record.getId();
    }

    /**
     * 把版本发布到环境（真实 CD）。
     * <p>
     * 同步完成：版本/环境/目标/状态机校验 + 落部署记录；文件发布异步执行，完成后再回写记录状态。
     * 需审批环境只落「待审批」记录，由 {@link #approve} 批准后再执行。
     *
     * @param versionId       版本 id
     * @param environmentName 环境名（dev/test/prod）
     * @param operator        操作者
     * @param workspaceId     操作者工作区（数据权限校验，系统内部调用传 null 跳过）
     * @param promote         是否晋升（晋升要求上一环境已成功部署）
     * @return 部署记录 id
     */
    public String deployPublish(String versionId, String environmentName, String operator, String workspaceId, boolean promote) {
        VersionModel version = versionService.getByKey(versionId);
        Assert.notNull(version, "版本不存在: " + versionId);
        EnvironmentModel environment = environmentService.getByName(environmentName);
        Assert.notNull(environment, "环境不存在: " + environmentName);
        List<EnvironmentTargetModel> targets = environmentService.listTargets(environment.getId());
        Assert.notEmpty(targets, "环境未绑定部署目标: " + environmentName);
        this.checkVersionDeployable(version, environment);
        this.checkWorkspacePermission(version, workspaceId);
        if (promote) {
            this.checkPromotePath(version, environment);
        }

        // 生产等需审批环境：先落待审批记录，审批通过后才真正执行
        if (Boolean.TRUE.equals(environment.getApprovalRequired())) {
            log.info("环境 {} 需要审批，创建待审批部署记录", environmentName);
            return createRecord(versionId, environmentName, "manual", operator, STATUS_PENDING_APPROVAL, "");
        }

        String recordId = createRecord(versionId, environmentName, "manual", operator, STATUS_RUNNING, "");
        this.executeAsync(recordId, version, environmentName, targets, operator);
        return recordId;
    }

    /**
     * 审批待部署记录：批准后异步执行发布，拒绝则闭环为「已拒绝」。
     *
     * @param recordId 部署记录 id
     * @param pass     true 批准 false 拒绝
     * @param operator 审批人
     * @param remark   审批意见
     */
    @Transactional
    public void approve(String recordId, boolean pass, String operator, String remark) {
        Assert.hasText(recordId, "部署记录不能为空");
        DeploymentRecordEntity record = repository.findById(recordId).orElse(null);
        Assert.notNull(record, "部署记录不存在: " + recordId);
        Assert.state(record.getStatus() != null && record.getStatus() == STATUS_PENDING_APPROVAL,
            "该记录不在待审批状态，无需处理");
        if (!pass) {
            record.setStatus(STATUS_REJECTED);
            record.setRemark((remark == null || remark.isEmpty()) ? "审批拒绝" : remark);
            record.setOperator(record.getOperator() + " / 审批人:" + operator);
            record.setModifyTimeMillis(System.currentTimeMillis());
            repository.save(record);
            log.info("部署审批拒绝: record={} operator={}", recordId, operator);
            return;
        }
        VersionModel version = versionService.getByKey(record.getVersionId());
        Assert.notNull(version, "版本不存在: " + record.getVersionId());
        EnvironmentModel environment = environmentService.getByName(record.getEnvironment());
        Assert.notNull(environment, "环境不存在: " + record.getEnvironment());
        List<EnvironmentTargetModel> targets = environmentService.listTargets(environment.getId());
        Assert.notEmpty(targets, "环境未绑定部署目标: " + record.getEnvironment());
        record.setStatus(STATUS_RUNNING);
        record.setRemark((remark == null || remark.isEmpty()) ? "审批通过" : remark);
        record.setOperator(record.getOperator() + " / 审批人:" + operator);
        record.setModifyTimeMillis(System.currentTimeMillis());
        repository.save(record);
        log.info("部署审批通过: record={} operator={}", recordId, operator);
        this.executeAsync(recordId, version, record.getEnvironment(), targets, operator);
    }

    /**
     * 异步执行发布：逐个目标复用 ReleaseManage 发布，全部结束后回写部署记录状态。
     */
    private void executeAsync(String recordId, VersionModel version, String environmentName,
                              List<EnvironmentTargetModel> targets, String operator) {
        UserModel userModel = this.currentUser();
        buildExecutorPoolService.execute(() -> {
            boolean success = true;
            StringBuilder logRefs = new StringBuilder();
            for (EnvironmentTargetModel target : targets) {
                try {
                    String logRef = this.deployToTarget(version, target, userModel);
                    if (logRef != null) {
                        logRefs.append(logRefs.length() == 0 ? "" : ",").append(logRef);
                    }
                } catch (Exception e) {
                    log.error("部署到目标失败: env={} target={}/{}", environmentName, target.getTargetType(), target.getTargetId(), e);
                    success = false;
                }
            }
            this.finishRecord(recordId, success, logRefs.toString());
        });
    }

    /**
     * 回写部署记录最终状态（异步任务完成后调用）。
     */
    @Transactional
    public void finishRecord(String recordId, boolean success, String logRef) {
        DeploymentRecordEntity record = repository.findById(recordId).orElse(null);
        if (record == null) {
            log.warn("部署记录不存在，无法回写状态: {}", recordId);
            return;
        }
        record.setStatus(success ? STATUS_SUCCESS : STATUS_FAILED);
        if (logRef != null && !logRef.isEmpty()) {
            record.setLogRef(logRef);
        }
        record.setModifyTimeMillis(System.currentTimeMillis());
        repository.save(record);
    }

    /**
     * 版本状态机约束：已打回不可部署；prod 仅已发布；test 仅已提测/已发布。
     */
    private void checkVersionDeployable(VersionModel version, EnvironmentModel environment) {
        Integer status = version.getStatus();
        Assert.state(status != null && status != VersionStatus.Returned.getCode(),
            "已打回的版本不能部署，请先重新提测: " + version.getVersion());
        String envType = (environment.getType() == null || environment.getType().isEmpty())
            ? environment.getName() : environment.getType();
        if ("prod".equalsIgnoreCase(envType)) {
            Assert.state(status == VersionStatus.Released.getCode(),
                "生产环境仅允许部署已发布的版本，请先在版本管理中执行发布: " + version.getVersion());
        } else if ("test".equalsIgnoreCase(envType)) {
            Assert.state(status == VersionStatus.Submitted.getCode() || status == VersionStatus.Released.getCode(),
                "测试环境仅允许部署已提测/已发布的版本: " + version.getVersion());
        }
    }

    /**
     * 工作区数据权限：版本对应构建配置必须在操作者工作区内（系统内部调用传 null 跳过）。
     */
    private void checkWorkspacePermission(VersionModel version, String workspaceId) {
        if (workspaceId == null || workspaceId.isEmpty()) {
            return;
        }
        BuildInfoModel buildInfo = buildInfoService.getByKey(version.getBuildId());
        Assert.notNull(buildInfo, "构建配置不存在: " + version.getBuildId());
        String buildWorkspace = buildInfo.getWorkspaceId();
        Assert.state(workspaceId.equals(buildWorkspace), "没有该应用在当前工作区的数据权限");
    }

    /**
     * 晋升路径约束：上一环境（按排序紧邻）必须已成功部署过该版本。
     */
    private void checkPromotePath(VersionModel version, EnvironmentModel targetEnv) {
        List<EnvironmentModel> enabled = environmentService.listEnabled();
        int index = -1;
        for (int i = 0; i < enabled.size(); i++) {
            if (enabled.get(i).getId().equals(targetEnv.getId())) {
                index = i;
                break;
            }
        }
        Assert.state(index > 0, "首个环境无需晋升，请直接部署: " + targetEnv.getName());
        String prevEnvName = enabled.get(index - 1).getName();
        boolean deployed = repository.findByVersionIdOrderByCreateTimeMillisDesc(version.getId())
            .stream()
            .anyMatch(record -> prevEnvName.equals(record.getEnvironment())
                && record.getStatus() != null && record.getStatus() == STATUS_SUCCESS);
        Assert.state(deployed, "请先成功部署到上一环境后再晋升: " + prevEnvName);
    }

    /**
     * 按目标类型分发到对应部署实现（NODE 节点 / K8S 集群 / SSH 主机）。
     */
    private String deployToTarget(VersionModel version, EnvironmentTargetModel target, UserModel userModel) throws Exception {
        String targetType = target.getTargetType();
        if (EnvironmentService.TARGET_TYPE_NODE.equals(targetType)) {
            return this.deployToNode(version, target, userModel);
        } else if (EnvironmentService.TARGET_TYPE_K8S.equals(targetType)) {
            return this.deployToK8s(version, target, userModel);
        } else if (EnvironmentService.TARGET_TYPE_SSH.equals(targetType)) {
            return this.deployToSsh(version, target, userModel);
        }
        throw new IllegalStateException("暂不支持的目标类型: " + targetType);
    }

    /**
     * 校验构建记录可用，并复制一条独立的发布记录（新 CI_BUILD_LOG 行），避免复用源构建记录导致状态互相覆盖。
     */
    private BuildHistoryLog createDeployLog(VersionModel version, EnvironmentTargetModel target) {
        BuildInfoModel buildInfo = buildInfoService.getByKey(version.getBuildId());
        Assert.notNull(buildInfo, "构建配置不存在: " + version.getBuildId());
        BuildHistoryLog historyLog = this.getHistoryLog(buildInfo.getId(), version.getBuildNumberId());
        Assert.notNull(historyLog, "构建记录不存在: " + version.getBuildId() + " #" + version.getBuildNumberId());
        Assert.state(historyLog.getStatus() != null && historyLog.getStatus() == BuildStatus.Success.getCode(),
            "只有构建成功的记录才能部署: " + version.getBuildId() + " #" + version.getBuildNumberId());
        BuildHistoryLog deployLog = historyLog.toJson().to(BuildHistoryLog.class);
        deployLog.setId(null);
        deployLog.setCreateUser(null);
        deployLog.setCreateTimeMillis(null);
        deployLog.setModifyUser(null);
        deployLog.setModifyTimeMillis(null);
        deployLog.setStatus(BuildStatus.PubIng.getCode());
        deployLog.setStatusMsg("环境部署中: " + target.getTargetId());
        deployLog.setTriggerBuildType(TRIGGER_TYPE_ENV_DEPLOY);
        deployLog.setFromBuildNumberId(historyLog.getBuildNumberId());
        deployLog.setStartTime(System.currentTimeMillis());
        deployLog.setEndTime(null);
        dbBuildHistoryLogService.insert(deployLog);
        return deployLog;
    }

    /**
     * 构建 {@link ReleaseManage}（复用发布引擎）。
     */
    private ReleaseManage buildReleaseManage(VersionModel version, EnvironmentTargetModel target,
                                             BuildExtraModule buildExtraModule, BuildHistoryLog historyLog,
                                             BuildHistoryLog deployLog, UserModel userModel) {
        return ReleaseManage.builder()
            .buildExtraModule(buildExtraModule)
            .logId(deployLog.getId())
            .userModel(userModel)
            .buildNumberId(version.getBuildNumberId())
            .fromBuildNumberId(version.getBuildNumberId())
            .syncBuildStatus(false)
            .logRecorder(LogRecorder.builder()
                .file(FileUtil.file("logs/deploy", "deploy-" + sanitizeFileName(target.getTargetId()) + ".log")).build())
            .buildEnv(historyLog.toEnvironmentMapBuilder())
            .build();
    }

    /**
     * 把版本发布到单个 NODE 目标（复用 ReleaseManage，覆盖发布目标为环境的 node:project）。
     */
    private String deployToNode(VersionModel version, EnvironmentTargetModel target, UserModel userModel) throws Exception {
        Assert.hasText(target.getProjectId(), "NODE 目标缺少 projectId");
        BuildInfoModel buildInfo = buildInfoService.getByKey(version.getBuildId());
        BuildHistoryLog deployLog = this.createDeployLog(version, target);
        BuildHistoryLog historyLog = this.getHistoryLog(buildInfo.getId(), version.getBuildNumberId());

        BuildExtraModule buildExtraModule = BuildExtraModule.build(historyLog);
        buildExtraModule.setReleaseMethod(BuildReleaseMethod.Project.getCode());
        buildExtraModule.setReleaseMethodDataId(target.getTargetId() + ":" + target.getProjectId());
        ReleaseManage manage = this.buildReleaseManage(version, target, buildExtraModule, historyLog, deployLog, userModel);
        String msg = manage.start(null, buildInfo);
        Assert.isTrue((msg == null || msg.isEmpty()), "发布失败: " + msg);
        return deployLog.getId();
    }

    /**
     * 把版本发布到单个 SSH 目标（复用 ReleaseManage 的 SSH 发布：SCP 产物 + 远程执行发布命令）。
     * <p>
     * projectId 可选：用于覆盖发布目录（发布脚本通过 SSH_RELEASE_PATH 读取）。
     */
    private String deployToSsh(VersionModel version, EnvironmentTargetModel target, UserModel userModel) throws Exception {
        BuildInfoModel buildInfo = buildInfoService.getByKey(version.getBuildId());
        BuildHistoryLog deployLog = this.createDeployLog(version, target);
        BuildHistoryLog historyLog = this.getHistoryLog(buildInfo.getId(), version.getBuildNumberId());

        BuildExtraModule buildExtraModule = BuildExtraModule.build(historyLog);
        buildExtraModule.setReleaseMethod(BuildReleaseMethod.Ssh.getCode());
        buildExtraModule.setReleaseMethodDataId(target.getTargetId());
        if (target.getProjectId() != null && !target.getProjectId().isEmpty()) {
            buildExtraModule.setReleasePath(target.getProjectId());
        }
        ReleaseManage manage = this.buildReleaseManage(version, target, buildExtraModule, historyLog, deployLog, userModel);
        String msg = manage.start(null, buildInfo);
        Assert.isTrue((msg == null || msg.isEmpty()), "发布失败: " + msg);
        return deployLog.getId();
    }

    /**
     * 把版本部署到单个 K8S 目标。
     * <p>
     * 支持两条交付路径（可叠加）：
     * <ol>
     *   <li>容器交付：构建配置含 Dockerfile 时，先构建并推送镜像（供集群拉取）；</li>
     *   <li>manifest 交付：apply 构建产物中的 Kubernetes manifest（支持多文档/多文件）。</li>
     * </ol>
     * projectId 可选：用于覆盖目标命名空间（缺省使用集群配置的默认命名空间）。
     */
    private String deployToK8s(VersionModel version, EnvironmentTargetModel target, UserModel userModel) throws Exception {
        BuildInfoModel buildInfo = buildInfoService.getByKey(version.getBuildId());
        BuildHistoryLog deployLog = this.createDeployLog(version, target);
        BuildHistoryLog historyLog = this.getHistoryLog(buildInfo.getId(), version.getBuildNumberId());

        K8sService k8sService = SpringContextHolder.getBean(K8sService.class);
        String namespace = (target.getProjectId() == null || target.getProjectId().isEmpty())
            ? null : target.getProjectId();
        BuildExtraModule extra = BuildExtraModule.build(historyLog);
        boolean dockerBuild = extra.getDockerfile() != null && !extra.getDockerfile().isEmpty();

        try {
            // 1. 容器交付：构建并推送镜像
            if (dockerBuild) {
                BuildExtraModule dockerExtra = BuildExtraModule.build(historyLog);
                dockerExtra.setReleaseMethod(BuildReleaseMethod.DockerImage.getCode());
                ReleaseManage manage = this.buildReleaseManage(version, target, dockerExtra, historyLog, deployLog, userModel);
                String msg = manage.start(null, buildInfo);
                Assert.isTrue((msg == null || msg.isEmpty()), "镜像构建/推送失败: " + msg);
            }
            // 2. manifest 交付：apply 构建产物中的 Kubernetes manifest（可能多个）
            List<String> manifests = this.readManifests(buildInfo.getId(), version.getBuildNumberId(), historyLog);
            if (!manifests.isEmpty()) {
                for (String manifest : manifests) {
                    k8sService.applyManifest(target.getTargetId(), namespace, manifest);
                }
            } else if (!dockerBuild) {
                throw new IllegalStateException("K8S 部署失败：构建产物中既无 Kubernetes manifest(.yaml/.yml)，构建配置也未提供 Dockerfile，无法交付到集群: "
                    + target.getTargetId());
            }
            deployLog.setStatus(BuildStatus.PubSuccess.getCode());
            deployLog.setStatusMsg("K8S 部署成功: " + target.getTargetId());
            deployLog.setEndTime(System.currentTimeMillis());
            dbBuildHistoryLogService.updateById(deployLog);
        } catch (Exception e) {
            deployLog.setStatus(BuildStatus.PubError.getCode());
            deployLog.setStatusMsg("K8S 部署失败: " + e.getMessage());
            deployLog.setEndTime(System.currentTimeMillis());
            dbBuildHistoryLogService.updateById(deployLog);
            throw e;
        }
        return deployLog.getId();
    }

    /**
     * 读取构建产物中的 Kubernetes manifest 列表（递归收集结果目录下的 .yaml/.yml 文件，缺省返回空列表）。
     */
    private List<String> readManifests(String buildId, Integer buildNumberId, BuildHistoryLog historyLog) throws Exception {
        BuildExtraModule extra = BuildExtraModule.build(historyLog);
        String resultFile = extra.getResultDirFile();
        java.io.File packageFile = BuildUtil.getHistoryPackageFile(buildId, buildNumberId, resultFile);
        if (packageFile == null || !packageFile.exists()) {
            return Collections.emptyList();
        }
        List<java.io.File> manifests = new ArrayList<>();
        this.collectManifests(packageFile, manifests);
        List<String> contents = new ArrayList<>();
        for (java.io.File manifest : manifests) {
            contents.add(new String(java.nio.file.Files.readAllBytes(manifest.toPath()), java.nio.charset.StandardCharsets.UTF_8));
        }
        return contents;
    }

    /**
     * 递归收集结果目录下的 .yaml/.yml（包级可见，便于单元测试）。
     */
    void collectManifests(java.io.File file, List<java.io.File> out) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isFile()) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".yaml") || name.endsWith(".yml")) {
                out.add(file);
            }
            return;
        }
        if (file.isDirectory()) {
            java.io.File[] children = file.listFiles();
            if (children != null) {
                for (java.io.File child : children) {
                    this.collectManifests(child, out);
                }
            }
        }
    }

    /**
     * 目标 id 消毒为安全文件名（防路径穿越）。
     */
    private static String sanitizeFileName(String targetId) {
        if (targetId == null || targetId.isEmpty()) {
            return "unknown";
        }
        String sanitized = targetId.replaceAll("[^a-zA-Z0-9._-]", "_");
        // 剥离前导点，防 ".."
        while (sanitized.startsWith(".")) {
            sanitized = sanitized.substring(1);
        }
        return sanitized.isEmpty() ? "unknown" : sanitized;
    }

    private BuildHistoryLog getHistoryLog(String buildId, Integer buildNumberId) {
        io.voyager1.core.db.Entity where = io.voyager1.core.db.Entity.create("CI_BUILD_LOG");
        where.set("buildDataId", buildId);
        where.set("buildNumberId", buildNumberId);
        List<BuildHistoryLog> list = dbBuildHistoryLogService.queryList(where, 1);
        return (list == null || list.isEmpty() ? null : list.get(0));
    }

    private UserModel currentUser() {
        UserModel user = BaseServerController.getUserByThreadLocal();
        return user == null ? new UserModel() : user;
    }

    /**
     * 按版本查询部署记录
     */
    public List<DeploymentRecordModel> listByVersionId(String versionId) {
        return repository.findByVersionIdOrderByCreateTimeMillisDesc(versionId)
            .stream().map(this::toModel).collect(Collectors.toList());
    }

    /**
     * 按环境查询部署记录
     */
    public List<DeploymentRecordModel> listByEnvironment(String environment) {
        return repository.findByEnvironmentOrderByCreateTimeMillisDesc(environment)
            .stream().map(this::toModel).collect(Collectors.toList());
    }

    /**
     * 查询环境当前部署的版本（最新一条成功记录）
     */
    public DeploymentRecordModel currentVersion(String environment) {
        DeploymentRecordEntity entity = repository.findFirstByEnvironmentAndStatusOrderByCreateTimeMillisDesc(environment, STATUS_SUCCESS);
        return entity == null ? null : toModel(entity);
    }

    private DeploymentRecordModel toModel(DeploymentRecordEntity entity) {
        DeploymentRecordModel model = DeploymentRecordModel.builder()
            .buildId(entity.getBuildId())
            .versionId(entity.getVersionId())
            .version(entity.getVersion())
            .environment(entity.getEnvironment())
            .mode(entity.getMode())
            .operator(entity.getOperator())
            .status(entity.getStatus())
            .logRef(entity.getLogRef())
            .remark(entity.getRemark())
            .build();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        return model;
    }
}
