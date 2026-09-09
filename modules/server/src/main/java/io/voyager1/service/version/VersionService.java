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
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.data.VersionModel;
import io.voyager1.model.enums.VersionStatus;
import io.voyager1.model.log.BuildHistoryLog;
import io.voyager1.service.dblog.BuildInfoService;
import io.voyager1.service.dblog.DbBuildHistoryLogService;
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
     * 从构建记录生成版本（版本号/产物由构建自动派生，不再手动乱填）。
     */
    @Transactional
    public VersionModel createVersionFromBuild(String buildId, Integer buildNumberId, String remark) {
        Assert.hasText(buildId, "buildId 不能为空");
        Assert.notNull(buildNumberId, "buildNumberId 不能为空");
        BuildInfoModel buildInfo = io.voyager1.common.SpringContextHolder.getBean(BuildInfoService.class).getByKey(buildId);
        Assert.notNull(buildInfo, "构建配置不存在: " + buildId);
        BuildHistoryLog historyLog = this.getHistoryLog(buildId, buildNumberId);
        Assert.notNull(historyLog, "构建记录不存在: " + buildId + " #" + buildNumberId);
        Assert.state(historyLog.getStatus() != null && historyLog.getStatus() == io.voyager1.model.enums.BuildStatus.Success.getCode(),
            "只有构建成功的记录才能生成版本: " + buildId + " #" + buildNumberId);
        String version = this.resolveVersion(buildInfo, buildNumberId);
        String artifactRef = historyLog.getResultDirFile();
        return this.createVersion(buildId, buildNumberId, version, artifactRef, remark);
    }

    /**
     * 从构建记录派生版本号（优先 tag，其次 commit 前缀，最后 buildId#buildNumberId）。
     */
    private String resolveVersion(BuildInfoModel buildInfo, int buildNumberId) {
        String tagName = buildInfo.getBranchTagName();
        if ((tagName != null && !tagName.isEmpty())) {
            return tagName.startsWith("v") ? tagName : "v" + tagName;
        }
        // 构建从未执行过时 buildId 为 null，回退 0，避免生成 "vnull.x.x"
        Object buildSeq = (buildInfo.getBuildId() != null ? buildInfo.getBuildId() : 0);
        String commit = buildInfo.getRepositoryLastCommitId();
        if ((commit != null && !commit.isEmpty())) {
            String shortCommit = commit.length() > 8 ? commit.substring(0, 8) : commit;
            return String.format("v%s.%s.%s", buildSeq, buildNumberId, shortCommit);
        }
        // 末段用完整时间戳，避免同构建同编号重复生成时版本号碰撞
        return String.format("v%s.%s.%s", buildSeq, buildNumberId, System.currentTimeMillis());
    }

    private BuildHistoryLog getHistoryLog(String buildId, Integer buildNumberId) {
        io.voyager1.core.db.Entity where = io.voyager1.core.db.Entity.create("CI_BUILD_LOG");
        where.set("buildDataId", buildId);
        where.set("buildNumberId", buildNumberId);
        List<BuildHistoryLog> list = io.voyager1.common.SpringContextHolder.getBean(DbBuildHistoryLogService.class).queryList(where, 1);
        return (list == null || list.isEmpty() ? null : list.get(0));
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
        // 提测 = 部署到 test 环境（环境化 CD；test 未绑定目标或构建产物缺失时仅记录失败）
        try {
            String autoCd = (System.getenv("VOYAGER1_ENV_AUTO_CD") != null ? System.getenv("VOYAGER1_ENV_AUTO_CD") : (System.getProperty("VOYAGER1_ENV_AUTO_CD") != null ? System.getProperty("VOYAGER1_ENV_AUTO_CD") : "true"));
            if (io.voyager1.util.ConvertUtil.toBool(autoCd, true)) {
                io.voyager1.common.SpringContextHolder.getBean(io.voyager1.service.environment.DeploymentService.class)
                    .deployPublish(id, "test", "system", null, false);
            }
        } catch (Exception e) {
            log.warn("提测自动部署 test 失败: {}", e.getMessage());
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
        // 发布 = 部署到 prod 环境（prod 需审批时，deployPublish 内部落待审批记录，审批通过后执行）
        try {
            io.voyager1.common.SpringContextHolder.getBean(io.voyager1.service.environment.DeploymentService.class)
                .deployPublish(id, "prod", "system", null, false);
        } catch (Exception e) {
            log.warn("发布部署 prod 失败: {}", e.getMessage());
        }
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
        List<VersionEntity> entities = (buildId == null || buildId.isEmpty())
            ? repository.findAllByOrderByCreateTimeMillisDesc()
            : repository.findByBuildIdOrderByCreateTimeMillisDesc(buildId);
        return entities.stream().map(this::toModel).collect(Collectors.toList());
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
