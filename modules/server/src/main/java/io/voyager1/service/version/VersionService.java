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

package io.voyager1.service.version;

import io.voyager1.core.entity.VersionEntity;
import io.voyager1.core.repository.VersionRepository;
import io.voyager1.model.BaseEnum;
import io.voyager1.model.data.VersionModel;
import io.voyager1.model.enums.VersionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 版本服务（状态机：提测冻结 CI / 打回解锁 / 发布）。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA 仓库（VersionRepository），对外契约不变。
 *
 * @since 2026/8/7
 */
@Service
@Slf4j
public class VersionService {

    private final VersionRepository repository;

    public VersionService(VersionRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建版本（绑定构建产物）。
     */
    @Transactional
    public VersionModel createVersion(String buildId, Integer buildNumberId, String version, String artifactRef, String remark) {
        Assert.hasText(buildId, "buildId 不能为空");
        Assert.hasText(version, "版本号不能为空");
        long now = System.currentTimeMillis();
        VersionEntity entity = new VersionEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTimeMillis(now);
        entity.setModifyTimeMillis(now);
        entity.setBuildId(buildId);
        entity.setBuildNumberId(buildNumberId);
        entity.setVersion(version);
        entity.setArtifactRef(artifactRef);
        entity.setStatus(VersionStatus.Developing.getCode());
        entity.setRemark(remark);
        repository.save(entity);
        log.info("创建版本: buildId={} version={} id={}", buildId, version, entity.getId());
        return toModel(entity);
    }

    /**
     * 提测（冻结 CI）。
     */
    @Transactional
    public void submit(String id, String remark) {
        VersionEntity entity = this.getRequiredVersion(id);
        int current = entity.getStatus() == null ? -1 : entity.getStatus();
        Assert.state(current == VersionStatus.Developing.getCode() || current == VersionStatus.Returned.getCode(),
            "只有开发中（或已打回）的版本才能提测，当前状态：" + BaseEnum.getEnum(VersionStatus.class, entity.getStatus()));
        this.transition(entity, VersionStatus.Submitted, remark);
        // 自动 CD：提测后部署到 test 环境（配置环境变量 VOYAGER1_ENV_AUTO_CD=false 时关闭）
        try {
            String autoCd = (System.getenv("VOYAGER1_ENV_AUTO_CD") != null ? System.getenv("VOYAGER1_ENV_AUTO_CD") : (System.getProperty("VOYAGER1_ENV_AUTO_CD") != null ? System.getProperty("VOYAGER1_ENV_AUTO_CD") : "true"));
            if (io.voyager1.util.ConvertUtil.toBool(autoCd, true)) {
                io.voyager1.common.SpringContextHolder.getBean(io.voyager1.service.environment.DeploymentService.class)
                    .createRecord(id, "test", "auto", "system", 0, "");
            }
        } catch (Exception e) {
            log.warn("自动 CD 部署失败: {}", e.getMessage());
        }
    }

    /**
     * 打回（解锁 CI）。
     */
    @Transactional
    public void returnVersion(String id, String remark) {
        VersionEntity entity = this.getRequiredVersion(id);
        Assert.state(entity.getStatus() == VersionStatus.Submitted.getCode(),
            "只有已提测的版本才能打回，当前状态：" + BaseEnum.getEnum(VersionStatus.class, entity.getStatus()));
        this.transition(entity, VersionStatus.Returned, remark);
    }

    /**
     * 发布（晋升）。
     */
    @Transactional
    public void release(String id, String remark) {
        VersionEntity entity = this.getRequiredVersion(id);
        Assert.state(entity.getStatus() == VersionStatus.Submitted.getCode(),
            "只有已提测的版本才能发布，当前状态：" + BaseEnum.getEnum(VersionStatus.class, entity.getStatus()));
        this.transition(entity, VersionStatus.Released, remark);
    }

    /**
     * 应用是否存在已提测版本（CI 冻结判定）。
     */
    public boolean hasSubmittedVersion(String buildId) {
        return repository.findFirstByBuildIdAndStatus(buildId, VersionStatus.Submitted.getCode()) != null;
    }

    /**
     * 按应用查询版本列表（创建时间倒序）。
     */
    public List<VersionModel> listByBuildId(String buildId) {
        return repository.findByBuildIdOrderByCreateTimeMillisDesc(buildId)
            .stream().map(this::toModel).collect(Collectors.toList());
    }

    /**
     * 按主键删除版本。
     */
    @Transactional
    public void delByKey(String id) {
        repository.deleteById(id);
    }

    /**
     * 按主键查询版本。
     */
    public VersionModel getByKey(String id) {
        VersionEntity entity = repository.findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    private VersionEntity getRequiredVersion(String id) {
        VersionEntity entity = repository.findById(id).orElse(null);
        Assert.notNull(entity, "版本不存在: " + id);
        return entity;
    }

    private void transition(VersionEntity entity, VersionStatus target, String remark) {
        entity.setStatus(target.getCode());
        entity.setRemark(remark);
        entity.setModifyTimeMillis(System.currentTimeMillis());
        repository.save(entity);
        log.info("版本状态流转: id={} version={} -> {} remark={}",
            entity.getId(), entity.getVersion(), target.getDesc(), remark);
    }

    private VersionModel toModel(VersionEntity entity) {
        VersionModel model = VersionModel.builder()
            .buildId(entity.getBuildId())
            .buildNumberId(entity.getBuildNumberId())
            .version(entity.getVersion())
            .status(entity.getStatus())
            .artifactRef(entity.getArtifactRef())
            .remark(entity.getRemark())
            .groupName(entity.getGroupName())
            .build();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        return model;
    }
}
