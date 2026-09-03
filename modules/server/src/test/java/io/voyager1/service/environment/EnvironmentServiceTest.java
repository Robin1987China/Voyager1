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

    @BeforeEach
    public void reset() {
        io.voyager1.common.BaseServerController.resetInfo(io.voyager1.model.user.UserModel.EMPTY);
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
        String id = environmentService.saveEnvironment(null, "staging", 3, true);
        Assertions.assertNotNull(id);
        EnvironmentModel model = environmentService.getByKey(id);
        Assertions.assertEquals("staging", model.getName());
        Assertions.assertEquals(1, model.getEnabled());
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
        VersionModel version = versionService.createVersion("env-auto-cd", 1, "v1.1.1", "/tmp/a.jar", "测试");
        versionService.submit(version.getId(), "提测（自动CD）");
        // submit 自动部署到 test（VOYAGER1_ENV_AUTO_CD 默认 true）
        List<DeploymentRecordModel> records = deploymentService.listByVersionId(version.getId());
        Assertions.assertEquals(1, records.size());
        Assertions.assertEquals("test", records.get(0).getEnvironment());
        Assertions.assertEquals("auto", records.get(0).getMode());
        VersionModel updated = versionService.getByKey(version.getId());
        Assertions.assertEquals(VersionStatus.Submitted.getCode(), updated.getStatus());
    }
}
