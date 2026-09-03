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

import io.voyager1.core.entity.DeploymentRecordEntity;
import io.voyager1.core.repository.DeploymentRecordRepository;
import io.voyager1.model.data.DeploymentRecordModel;
import io.voyager1.model.data.VersionModel;
import io.voyager1.service.version.VersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 部署服务（部署记录 + 按版本/环境查询）。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA 仓库（DeploymentRecordRepository），对外契约不变。
 *
 * @since 2026/8/8
 */
@Service
@Slf4j
public class DeploymentService {

    private final DeploymentRecordRepository repository;
    private final VersionService versionService;

    public DeploymentService(DeploymentRecordRepository repository, VersionService versionService) {
        this.repository = repository;
        this.versionService = versionService;
    }

    /**
     * 创建部署记录
     *
     * @param versionId   版本 id
     * @param environment 环境
     * @param mode        auto/manual
     * @param operator    操作者
     * @param status      0 成功 1 失败 2 进行中
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
        log.info("部署记录: version={} env={} mode={} operator={}", version.getVersion(), environment, mode, operator);
        return record.getId();
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
     * 查询环境当前部署的版本（最新一条记录）
     */
    public DeploymentRecordModel currentVersion(String environment) {
        DeploymentRecordEntity entity = repository.findFirstByEnvironmentAndStatusOrderByCreateTimeMillisDesc(environment, 0);
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
