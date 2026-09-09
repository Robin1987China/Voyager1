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

import io.voyager1.common.SpringContextHolder;
import io.voyager1.core.entity.EnvironmentEntity;
import io.voyager1.core.entity.EnvironmentTargetEntity;
import io.voyager1.core.repository.EnvironmentRepository;
import io.voyager1.core.repository.EnvironmentTargetRepository;
import io.voyager1.model.data.EnvironmentModel;
import io.voyager1.model.data.EnvironmentTargetModel;
import io.voyager1.service.k8s.K8sService;
import io.voyager1.service.node.ssh.SshService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 环境服务（dev/test/prod 定义、策略与目标绑定）。
 * <p>
 * 环境化 CI/CD：环境具备 type/strategy/approvalRequired 策略，并绑定部署目标（节点/集群/SSH）。
 *
 * @since 2026/8/8
 */
@Service
@Slf4j
public class EnvironmentService {

    public static final List<String> DEFAULT_ENVIRONMENTS = Arrays.asList("dev", "test", "prod");

    /**
     * 部署策略：开发环境（可构建可部署）
     */
    public static final String STRATEGY_CI_CD = "CI_CD";

    /**
     * 部署策略：测试/生产环境（仅部署，不构建）
     */
    public static final String STRATEGY_CD_ONLY = "CD_ONLY";

    public static final String TARGET_TYPE_NODE = "NODE";
    public static final String TARGET_TYPE_K8S = "K8S";
    public static final String TARGET_TYPE_SSH = "SSH";

    /**
     * 支持绑定的目标类型：节点 / K8S 集群 / SSH 主机。
     */
    private static final List<String> SUPPORTED_TARGET_TYPES = Arrays.asList(TARGET_TYPE_NODE, TARGET_TYPE_K8S, TARGET_TYPE_SSH);

    private final EnvironmentRepository repository;
    private final EnvironmentTargetRepository targetRepository;
    private final io.voyager1.service.node.NodeService nodeService;

    public EnvironmentService(EnvironmentRepository repository, EnvironmentTargetRepository targetRepository,
                              io.voyager1.service.node.NodeService nodeService) {
        this.repository = repository;
        this.targetRepository = targetRepository;
        this.nodeService = nodeService;
    }

    /**
     * 初始化预置环境（首次启动），带默认策略。
     */
    @Transactional
    public void initDefaultEnvironments() {
        if (!this.listEnabled().isEmpty()) {
            return;
        }
        this.saveEnvironment(null, "dev", 0, true, "dev", STRATEGY_CI_CD, false);
        this.saveEnvironment(null, "test", 1, true, "test", STRATEGY_CD_ONLY, false);
        this.saveEnvironment(null, "prod", 2, true, "prod", STRATEGY_CD_ONLY, true);
        log.info("预置环境: {}", DEFAULT_ENVIRONMENTS);
    }

    /**
     * 创建/更新环境（含策略字段）。
     */
    @Transactional
    public String saveEnvironment(String id, String name, Integer sortValue, Boolean enabled,
                                  String type, String strategy, Boolean approvalRequired) {
        Assert.hasText(name, "环境名称不能为空");
        // 环境名全局唯一（部署/自动 CD 均按名定位环境，重名会导致目标环境不确定）
        EnvironmentEntity sameName = repository.findFirstByName(name);
        Assert.state(sameName == null || sameName.getId().equals(id), "环境名称已存在: " + name);
        long now = System.currentTimeMillis();
        EnvironmentEntity entity;
        if (id == null || id.isEmpty()) {
            entity = new EnvironmentEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setCreateTimeMillis(now);
            entity.setModifyTimeMillis(now);
        } else {
            entity = repository.findById(id).orElse(null);
            Assert.notNull(entity, "环境不存在: " + id);
            entity.setModifyTimeMillis(now);
        }
        entity.setName(name);
        entity.setSortValue(sortValue);
        entity.setEnabled(enabled == null || enabled ? 1 : 0);
        entity.setType(type);
        entity.setStrategy(strategy);
        entity.setApprovalRequired(Boolean.TRUE.equals(approvalRequired) ? 1 : 0);
        repository.save(entity);
        return entity.getId();
    }

    /**
     * 按主键查询环境。
     */
    public EnvironmentModel getByKey(String id) {
        EnvironmentEntity entity = repository.findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    /**
     * 按名称查询环境（部署时通常用 dev/test/prod 名称）。
     */
    public EnvironmentModel getByName(String name) {
        EnvironmentEntity entity = repository.findFirstByName(name);
        return entity == null ? null : toModel(entity);
    }

    /**
     * 查询启用环境列表（按排序、创建时间）。
     */
    public List<EnvironmentModel> listEnabled() {
        return repository.findByEnabledOrderBySortValueAscCreateTimeMillisAsc(1)
            .stream()
            .map(this::toModel)
            .collect(Collectors.toList());
    }

    /**
     * 给环境绑定部署目标（节点/集群/SSH）。
     */
    @Transactional
    public String bindTarget(String environmentId, String targetType, String targetId, String projectId, String workspaceId) {
        Assert.hasText(environmentId, "环境不能为空");
        Assert.hasText(targetType, "目标类型不能为空");
        Assert.hasText(targetId, "目标不能为空");
        Assert.state(SUPPORTED_TARGET_TYPES.contains(targetType), "暂不支持的目标类型: " + targetType);
        Assert.notNull(repository.findById(environmentId).orElse(null), "环境不存在: " + environmentId);
        // 目标存在性校验：节点/集群/SSH 主机必须存在（防绑定无效目标）
        if (TARGET_TYPE_NODE.equals(targetType)) {
            Assert.hasText(workspaceId, "工作区不能为空");
            Assert.notNull(nodeService.getByKey(targetId, workspaceId), "节点不存在或没有该工作区的数据权限: " + targetId);
            Assert.hasText(projectId, "NODE 目标缺少 projectId");
        } else if (TARGET_TYPE_K8S.equals(targetType)) {
            K8sService k8sService = SpringContextHolder.getBean(K8sService.class);
            Assert.notNull(k8sService.getByKey(targetId), "K8S 集群不存在: " + targetId);
        } else if (TARGET_TYPE_SSH.equals(targetType)) {
            Assert.hasText(workspaceId, "工作区不能为空");
            SshService sshService = SpringContextHolder.getBean(SshService.class);
            Assert.notNull(sshService.getByKey(targetId, workspaceId), "SSH 主机不存在或没有该工作区的数据权限: " + targetId);
        }
        Assert.state(!targetRepository.existsByEnvironmentIdAndTargetTypeAndTargetId(environmentId, targetType, targetId),
            "该目标已绑定到当前环境");
        long now = System.currentTimeMillis();
        EnvironmentTargetEntity entity = new EnvironmentTargetEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTimeMillis(now);
        entity.setModifyTimeMillis(now);
        entity.setEnvironmentId(environmentId);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setProjectId(projectId);
        entity.setWorkspaceId(workspaceId);
        entity.setEnabled(1);
        entity.setSortValue(0);
        targetRepository.save(entity);
        return entity.getId();
    }

    /**
     * 解绑环境目标。
     */
    @Transactional
    public void unbindTarget(String id) {
        Assert.hasText(id, "绑定 id 不能为空");
        targetRepository.deleteById(id);
    }

    /**
     * 查询环境的启用目标列表。
     */
    public List<EnvironmentTargetModel> listTargets(String environmentId) {
        return targetRepository.findByEnvironmentIdAndEnabledOrderBySortValueAscCreateTimeMillisAsc(environmentId, 1)
            .stream()
            .map(this::toTargetModel)
            .collect(Collectors.toList());
    }

    private EnvironmentModel toModel(EnvironmentEntity entity) {
        EnvironmentModel model = EnvironmentModel.builder()
            .name(entity.getName())
            .sortValue(entity.getSortValue())
            .enabled(entity.getEnabled())
            .type(entity.getType())
            .strategy(entity.getStrategy())
            .approvalRequired(entity.getApprovalRequired() != null && entity.getApprovalRequired() == 1)
            .build();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        return model;
    }

    private EnvironmentTargetModel toTargetModel(EnvironmentTargetEntity entity) {
        EnvironmentTargetModel model = EnvironmentTargetModel.builder()
            .environmentId(entity.getEnvironmentId())
            .targetType(entity.getTargetType())
            .targetId(entity.getTargetId())
            .projectId(entity.getProjectId())
            .workspaceId(entity.getWorkspaceId())
            .enabled(entity.getEnabled())
            .sortValue(entity.getSortValue())
            .build();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        return model;
    }
}
