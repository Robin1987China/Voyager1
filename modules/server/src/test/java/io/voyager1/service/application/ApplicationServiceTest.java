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

import io.voyager1.ApplicationStartTest;
import io.voyager1.model.data.ApplicationDetailModel;
import io.voyager1.model.data.ApplicationModel;
import io.voyager1.service.environment.EnvironmentService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

/**
 * 应用服务集成测试（CRUD + 详情聚合）。
 *
 * @since 2026/9/7
 */
public class ApplicationServiceTest extends ApplicationStartTest {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private io.voyager1.service.dblog.BuildInfoService buildInfoService;
    @Autowired
    private io.voyager1.service.dblog.RepositoryService repositoryService;

    @BeforeEach
    public void reset() {
        io.voyager1.common.BaseServerController.resetInfo(io.voyager1.model.user.UserModel.EMPTY);
    }

    /**
     * 创建真实仓库/构建配置夹具（应用保存有外键校验，假 id 会被拒绝）。
     */
    private String createRepository(String id) {
        io.voyager1.model.data.RepositoryModel model = new io.voyager1.model.data.RepositoryModel();
        model.setId(id);
        model.setName("应用测试仓库-" + id);
        model.setGitUrl("file:///tmp/app-test-repo");
        model.setRepoType(0);
        repositoryService.insert(model);
        return id;
    }

    private String createBuild(String id) {
        io.voyager1.model.data.BuildInfoModel model = new io.voyager1.model.data.BuildInfoModel();
        model.setId(id);
        model.setName("应用测试构建-" + id);
        model.setRepositoryId("app-test-repo");
        buildInfoService.insert(model);
        return id;
    }

    @Test
    public void testSaveAndList() {
        String repo1 = createRepository("repo-" + UUID.randomUUID().toString().substring(0, 8));
        String build1 = createBuild("build-" + UUID.randomUUID().toString().substring(0, 8));
        String id = applicationService.save(null, "order-svc", repo1, build1, "订单服务");
        Assertions.assertNotNull(id);
        ApplicationModel model = applicationService.getByKey(id);
        Assertions.assertEquals("order-svc", model.getName());
        Assertions.assertEquals(repo1, model.getRepositoryId());
        Assertions.assertEquals(build1, model.getBuildId());
        Assertions.assertEquals("订单服务", model.getRemark());

        List<ApplicationModel> list = applicationService.list();
        Assertions.assertTrue(list.stream().anyMatch(a -> id.equals(a.getId())));

        // 更新
        String repo2 = createRepository("repo-" + UUID.randomUUID().toString().substring(0, 8));
        String build2 = createBuild("build-" + UUID.randomUUID().toString().substring(0, 8));
        applicationService.save(id, "order-svc", repo2, build2, "更新备注");
        ApplicationModel updated = applicationService.getByKey(id);
        Assertions.assertEquals(repo2, updated.getRepositoryId());
        Assertions.assertEquals("更新备注", updated.getRemark());
    }

    @Test
    public void testSaveRejectsInvalidForeignKey() {
        // 外键完整性：不存在的仓库/构建配置必须拒绝
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> applicationService.save(null, "ghost-svc", "ghost-repo", "ghost-build", null));
        String repo = createRepository("repo-" + UUID.randomUUID().toString().substring(0, 8));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> applicationService.save(null, "ghost-svc2", repo, "ghost-build", null));
    }

    @Test
    public void testSaveRequiresName() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> applicationService.save(null, "", null, null, null));
    }

    @Test
    public void testDetailAggregation() {
        environmentService.initDefaultEnvironments();
        String repo = createRepository("repo-" + UUID.randomUUID().toString().substring(0, 8));
        String build = createBuild("build-" + UUID.randomUUID().toString().substring(0, 8));
        String id = applicationService.save(null, "order-svc", repo, build, "订单服务");
        ApplicationDetailModel detail = applicationService.detail(id);
        Assertions.assertNotNull(detail);
        Assertions.assertEquals("order-svc", detail.getApplication().getName());
        // 预置环境泳道：至少包含 dev/test/prod（按 sort 排序，前三个必为预置环境）
        Assertions.assertTrue(detail.getEnvironments().size() >= 3);
        Assertions.assertEquals("dev", detail.getEnvironments().get(0).getName());
        Assertions.assertEquals("test", detail.getEnvironments().get(1).getName());
        Assertions.assertEquals("prod", detail.getEnvironments().get(2).getName());
        // 无构建产物/部署数据时为空
        Assertions.assertNotNull(detail.getBuildHistory());
        Assertions.assertNotNull(detail.getDeploymentRecords());
    }

    @Test
    public void testDetailNotFound() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> applicationService.detail("not-exist-id"));
    }

    @Test
    public void testDel() {
        String repo = createRepository("repo-" + UUID.randomUUID().toString().substring(0, 8));
        String build = createBuild("build-" + UUID.randomUUID().toString().substring(0, 8));
        String id = applicationService.save(null, "del-svc", repo, build, null);
        applicationService.delByKey(id);
        Assertions.assertNull(applicationService.getByKey(id));
    }
}
