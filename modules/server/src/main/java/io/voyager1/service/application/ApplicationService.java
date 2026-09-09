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

package io.voyager1.service.application;

import io.voyager1.core.entity.ApplicationEntity;
import io.voyager1.core.entity.BuildHistoryLogEntity;
import io.voyager1.core.entity.DeploymentRecordEntity;
import io.voyager1.core.repository.ApplicationRepository;
import io.voyager1.core.repository.BuildHistoryLogRepository;
import io.voyager1.core.repository.DeploymentRecordRepository;
import io.voyager1.model.data.ApplicationDetailModel;
import io.voyager1.model.data.ApplicationModel;
import io.voyager1.model.data.DeploymentRecordModel;
import io.voyager1.model.data.EnvironmentLaneModel;
import io.voyager1.model.data.EnvironmentModel;
import io.voyager1.model.log.BuildHistoryLog;
import io.voyager1.service.environment.EnvironmentService;
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
 * 应用服务（逻辑服务，跨环境）。
 * <p>
 * 应用是「应用交付」的第一公民：绑定代码仓库与构建配置，跨 dev/test/prod 复用同一构建产物。
 * 详情聚合环境泳道（每环境当前部署版本）、构建历史与部署记录。
 *
 * @since 2026/9/7
 */
@Service
@Slf4j
public class ApplicationService {

    private final ApplicationRepository repository;
    private final BuildHistoryLogRepository buildHistoryLogRepository;
    private final DeploymentRecordRepository deploymentRecordRepository;
    private final EnvironmentService environmentService;
    private final io.voyager1.service.dblog.BuildInfoService buildInfoService;
    private final io.voyager1.service.dblog.RepositoryService repositoryService;

    public ApplicationService(ApplicationRepository repository,
                              BuildHistoryLogRepository buildHistoryLogRepository,
                              DeploymentRecordRepository deploymentRecordRepository,
                              EnvironmentService environmentService,
                              io.voyager1.service.dblog.BuildInfoService buildInfoService,
                              io.voyager1.service.dblog.RepositoryService repositoryService) {
        this.repository = repository;
        this.buildHistoryLogRepository = buildHistoryLogRepository;
        this.deploymentRecordRepository = deploymentRecordRepository;
        this.environmentService = environmentService;
        this.buildInfoService = buildInfoService;
        this.repositoryService = repositoryService;
    }

    /**
     * 创建/更新应用。
     */
    @Transactional
    public String save(String id, String name, String repositoryId, String buildId, String remark) {
        Assert.hasText(name, "应用名不能为空");
        // 外键完整性：仓库与构建配置必须真实存在，否则泳道/部署链路在使用时才报错
        Assert.hasText(repositoryId, "仓库不能为空");
        Assert.hasText(buildId, "构建配置不能为空");
        Assert.notNull(buildInfoService.getByKey(buildId), "构建配置不存在: " + buildId);
        Assert.notNull(repositoryService.getByKey(repositoryId, false), "仓库不存在: " + repositoryId);
        long now = System.currentTimeMillis();
        ApplicationEntity entity;
        if (id == null || id.isEmpty()) {
            entity = new ApplicationEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setCreateTimeMillis(now);
            entity.setModifyTimeMillis(now);
        } else {
            entity = repository.findById(id).orElse(null);
            Assert.notNull(entity, "应用不存在: " + id);
            entity.setModifyTimeMillis(now);
        }
        entity.setName(name);
        entity.setRepositoryId(repositoryId);
        entity.setBuildId(buildId);
        entity.setRemark(remark);
        repository.save(entity);
        log.info("保存应用: id={} name={} buildId={}", entity.getId(), name, buildId);
        return entity.getId();
    }

    /**
     * 按主键查询应用。
     */
    public ApplicationModel getByKey(String id) {
        ApplicationEntity entity = repository.findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    /**
     * 查询全部应用（创建时间倒序）。
     */
    public List<ApplicationModel> list() {
        return repository.findAllByOrderByCreateTimeMillisDesc()
            .stream()
            .map(this::toModel)
            .collect(Collectors.toList());
    }

    /**
     * 按主键删除应用（仅删应用定义，不级联删除构建/版本/部署记录）。
     */
    @Transactional
    public void delByKey(String id) {
        Assert.hasText(id, "应用 id 不能为空");
        repository.deleteById(id);
    }

    /**
     * 应用详情聚合：应用信息 + 环境泳道 + 构建历史 + 部署记录。
     */
    public ApplicationDetailModel detail(String id) {
        ApplicationModel application = this.getByKey(id);
        Assert.notNull(application, "应用不存在: " + id);
        String buildId = application.getBuildId();
        return ApplicationDetailModel.builder()
            .application(application)
            .environments(this.environmentLanes(buildId))
            .buildHistory(this.buildHistory(buildId))
            .deploymentRecords(this.deploymentRecords(buildId))
            .build();
    }

    /**
     * 环境泳道：每个启用环境拼接该应用当前部署版本。
     */
    private List<EnvironmentLaneModel> environmentLanes(String buildId) {
        List<EnvironmentLaneModel> lanes = new ArrayList<>();
        for (EnvironmentModel env : environmentService.listEnabled()) {
            // 泳道"当前版本"只取最后一次部署成功的记录（与 DeploymentService.currentVersion 语义一致），
            // 失败/待审批记录不应显示为环境当前版本
            DeploymentRecordEntity current = (buildId == null || buildId.isEmpty())
                ? null
                : deploymentRecordRepository.findFirstByBuildIdAndEnvironmentAndStatusOrderByCreateTimeMillisDesc(
                    buildId, env.getName(), io.voyager1.service.environment.DeploymentService.STATUS_SUCCESS);
            lanes.add(EnvironmentLaneModel.builder()
                .name(env.getName())
                .type(env.getType())
                .strategy(env.getStrategy())
                .approvalRequired(env.getApprovalRequired())
                .current(current == null ? null : toRecordModel(current))
                .build());
        }
        return lanes;
    }

    /**
     * 构建历史（构建编号倒序）。
     */
    private List<BuildHistoryLog> buildHistory(String buildId) {
        if (buildId == null || buildId.isEmpty()) {
            return Collections.emptyList();
        }
        return buildHistoryLogRepository.findByBuildDataIdOrderByBuildNumberIdDesc(buildId)
            .stream()
            .map(this::toHistoryLog)
            .collect(Collectors.toList());
    }

    /**
     * 部署记录（创建时间倒序）。
     */
    private List<DeploymentRecordModel> deploymentRecords(String buildId) {
        if (buildId == null || buildId.isEmpty()) {
            return Collections.emptyList();
        }
        return deploymentRecordRepository.findByBuildIdOrderByCreateTimeMillisDesc(buildId)
            .stream()
            .map(this::toRecordModel)
            .collect(Collectors.toList());
    }

    private ApplicationModel toModel(ApplicationEntity entity) {
        ApplicationModel model = ApplicationModel.builder()
            .name(entity.getName())
            .repositoryId(entity.getRepositoryId())
            .buildId(entity.getBuildId())
            .remark(entity.getRemark())
            .build();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        return model;
    }

    private DeploymentRecordModel toRecordModel(DeploymentRecordEntity entity) {
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

    /**
     * 构建历史实体转视图模型（剥离隐私环境变量字段，不泄露 buildEnvCache/extraData）。
     */
    private BuildHistoryLog toHistoryLog(BuildHistoryLogEntity entity) {
        BuildHistoryLog log = new BuildHistoryLog();
        log.setId(entity.getId());
        log.setCreateTimeMillis(entity.getCreateTimeMillis());
        log.setModifyTimeMillis(entity.getModifyTimeMillis());
        log.setBuildDataId(entity.getBuildDataId());
        log.setBuildNumberId(entity.getBuildNumberId());
        log.setBuildName(entity.getBuildName());
        log.setStatus(entity.getStatus());
        log.setStartTime(entity.getStartTime());
        log.setEndTime(entity.getEndTime());
        log.setStatusMsg(entity.getStatusMsg());
        log.setBuildRemark(entity.getBuildRemark());
        log.setResultDirFile(entity.getResultDirFile());
        log.setTriggerBuildType(entity.getTriggerBuildType());
        log.setFromBuildNumberId(entity.getFromBuildNumberId());
        log.setRepositoryLastCommitId(entity.getRepositoryLastCommitId());
        log.setRepositoryLastCommitMsg(entity.getRepositoryLastCommitMsg());
        log.setResultFileSize(entity.getResultFileSize());
        return log;
    }
}
