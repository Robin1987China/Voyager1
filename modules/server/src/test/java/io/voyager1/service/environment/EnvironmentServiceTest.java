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

import io.voyager1.ApplicationStartTest;
import io.voyager1.model.data.DeploymentRecordModel;
import io.voyager1.model.data.EnvironmentModel;
import io.voyager1.model.data.VersionModel;
import io.voyager1.model.enums.VersionStatus;
import io.voyager1.service.version.VersionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 环境与部署集成测试（预置/部署/自动CD/查询）
 *
 * @since 2026/8/8
 */
public class EnvironmentServiceTest extends ApplicationStartTest {

    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private DeploymentService deploymentService;
    @Autowired
    private VersionService versionService;
    @Autowired
    private io.voyager1.service.node.NodeService nodeService;

    @BeforeEach
    public void reset() {
        io.voyager1.common.BaseServerController.resetInfo(io.voyager1.model.user.UserModel.EMPTY);
    }

    /**
     * 创建真实节点夹具（bindTarget 有工作区数据权限校验，假节点会被拒绝）。
     */
    private String createNode(String nodeId) {
        io.voyager1.model.data.NodeModel node = new io.voyager1.model.data.NodeModel();
        node.setId(nodeId);
        node.setName("环境测试节点-" + nodeId);
        nodeService.insert(node);
        return nodeId;
    }

    @Test
    public void testDefaultEnvironments() {
        environmentService.initDefaultEnvironments();
        List<EnvironmentModel> list = environmentService.listEnabled();
        Assertions.assertTrue(list.size() >= 3);
        Assertions.assertEquals("dev", list.get(0).getName());
        Assertions.assertEquals("test", list.get(1).getName());
        Assertions.assertEquals("prod", list.get(2).getName());
    }

    @Test
    public void testSaveEnvironment() {
        // 测试库跨运行持久化，环境名需唯一（重名校验会拒绝）
        String envName = "stg" + (System.nanoTime() % 100000000);
        String id = environmentService.saveEnvironment(null, envName, 3, true, "staging", EnvironmentService.STRATEGY_CD_ONLY, true);
        Assertions.assertNotNull(id);
        EnvironmentModel model = environmentService.getByKey(id);
        Assertions.assertEquals(envName, model.getName());
        Assertions.assertEquals(1, model.getEnabled());
        Assertions.assertEquals("staging", model.getType());
        Assertions.assertEquals(EnvironmentService.STRATEGY_CD_ONLY, model.getStrategy());
        Assertions.assertEquals(Boolean.TRUE, model.getApprovalRequired());
        // 环境重名应被拦截（部署/自动 CD 按名定位环境）
        Assertions.assertThrows(IllegalStateException.class,
            () -> environmentService.saveEnvironment(null, envName, 4, true, "x", EnvironmentService.STRATEGY_CI_CD, false));
    }

    @Test
    public void testBindTarget() {
        environmentService.initDefaultEnvironments();
        EnvironmentModel dev = environmentService.listEnabled().get(0);
        String nodeId = createNode("node-" + System.nanoTime());
        String bindId = environmentService.bindTarget(dev.getId(), EnvironmentService.TARGET_TYPE_NODE, nodeId, "proj-001", "DEFAULT");
        Assertions.assertNotNull(bindId);
        // 重复绑定应被拦截
        String nodeIdFinal = nodeId;
        Assertions.assertThrows(IllegalStateException.class,
            () -> environmentService.bindTarget(dev.getId(), EnvironmentService.TARGET_TYPE_NODE, nodeIdFinal, "proj-001", "DEFAULT"));
        // 不存在于当前工作区的节点应被拒绝（工作区数据权限）
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> environmentService.bindTarget(dev.getId(), EnvironmentService.TARGET_TYPE_NODE, "ghost-node", "proj-001", "DEFAULT"));
        List<io.voyager1.model.data.EnvironmentTargetModel> targets = environmentService.listTargets(dev.getId());
        Assertions.assertEquals(1, targets.size());
        Assertions.assertEquals(nodeId, targets.get(0).getTargetId());
        Assertions.assertEquals("proj-001", targets.get(0).getProjectId());
        environmentService.unbindTarget(bindId);
        Assertions.assertEquals(0, environmentService.listTargets(dev.getId()).size());
    }

    @Test
    public void testManualDeploy() {
        VersionModel version = versionService.createVersion("env-test-build", 1, "v9.9.9", "/tmp/e.jar", "测试");
        String recordId = deploymentService.createRecord(version.getId(), "prod", "manual", "tester", 0, "");
        Assertions.assertNotNull(recordId);
        List<DeploymentRecordModel> records = deploymentService.listByVersionId(version.getId());
        Assertions.assertEquals(1, records.size());
        Assertions.assertEquals("prod", records.get(0).getEnvironment());
        Assertions.assertEquals("manual", records.get(0).getMode());
        // 环境当前版本
        DeploymentRecordModel current = deploymentService.currentVersion("prod");
        Assertions.assertNotNull(current);
        Assertions.assertEquals("v9.9.9", current.getVersion());
    }

    @Test
    public void testAutoCdOnSubmit() {
        environmentService.initDefaultEnvironments();
        EnvironmentModel test = environmentService.getByName("test");
        environmentService.bindTarget(test.getId(), EnvironmentService.TARGET_TYPE_NODE, createNode("node-" + System.nanoTime()), "proj-test", "DEFAULT");
        VersionModel version = versionService.createVersion("env-auto-cd", 1, "v1.1.1", "/tmp/a.jar", "测试");
        versionService.submit(version.getId(), "提测（自动CD）");
        // 提测 = 部署到 test（状态迁移到「已提测」）
        VersionModel updated = versionService.getByKey(version.getId());
        Assertions.assertEquals(VersionStatus.Submitted.getCode(), updated.getStatus());
    }

    @Test
    public void testDeployPublishNoTarget() {
        environmentService.initDefaultEnvironments();
        // 新建一个无绑定的环境（唯一短名），部署应报「未绑定目标」
        String envName = "emp" + (System.nanoTime() % 100000000);
        String envId = environmentService.saveEnvironment(null, envName, 9, true, envName, EnvironmentService.STRATEGY_CD_ONLY, false);
        EnvironmentModel env = environmentService.getByKey(envId);
        VersionModel version = versionService.createVersion("deploy-no-target", 1, "v2.0.0", "/tmp/e.jar", "测试");
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> deploymentService.deployPublish(version.getId(), env.getName(), "tester", null, false));
    }

    @Test
    public void testDeployPublishApprovalRequired() {
        environmentService.initDefaultEnvironments();
        EnvironmentModel prod = environmentService.getByName("prod");
        Assertions.assertEquals(Boolean.TRUE, prod.getApprovalRequired());
        environmentService.bindTarget(prod.getId(), EnvironmentService.TARGET_TYPE_NODE, createNode("node-" + System.nanoTime()), "proj-prod", "DEFAULT");
        VersionModel version = versionService.createVersion("deploy-approval-" + System.nanoTime(), 1, "v3.0.0", "/tmp/e.jar", "测试");
        // 状态机约束：开发中的版本不能部署到 prod（需先提测+发布）
        Assertions.assertThrows(IllegalStateException.class,
            () -> deploymentService.deployPublish(version.getId(), "prod", "tester", null, false));
        // 提测（自动 CD 到 test，无绑定时内部吞掉）→ 发布（自动部署 prod，落待审批记录）
        versionService.submit(version.getId(), "提测");
        versionService.release(version.getId(), "发布");
        String recordId = deploymentService.deployPublish(version.getId(), "prod", "tester", null, false);
        Assertions.assertNotNull(recordId);
        // prod 需审批 → 待审批记录（status=3）；release/submit 的自动 CD 也会落记录，数量随环境绑定情况变化，按 id 断言
        List<DeploymentRecordModel> records = deploymentService.listByVersionId(version.getId());
        Assertions.assertTrue(records.size() >= 2);
        DeploymentRecordModel pending = records.stream().filter(r -> recordId.equals(r.getId())).findFirst().orElse(null);
        Assertions.assertNotNull(pending);
        Assertions.assertEquals(DeploymentService.STATUS_PENDING_APPROVAL, pending.getStatus().intValue());
        // 审批拒绝闭环为「已拒绝」
        deploymentService.approve(recordId, false, "admin", "本期不上");
        List<DeploymentRecordModel> afterReject = deploymentService.listByVersionId(version.getId());
        DeploymentRecordModel rejected = afterReject.stream().filter(r -> recordId.equals(r.getId())).findFirst().orElse(null);
        Assertions.assertNotNull(rejected);
        Assertions.assertEquals(DeploymentService.STATUS_REJECTED, rejected.getStatus().intValue());
        // 已处理的记录不能重复审批
        Assertions.assertThrows(IllegalStateException.class,
            () -> deploymentService.approve(recordId, true, "admin", null));
    }
}
