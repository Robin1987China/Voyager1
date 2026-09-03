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

import io.voyager1.util.StrUtil;
import io.voyager1.ApplicationStartTest;
import io.voyager1.build.BuildExecuteService;
import io.voyager1.core.api.ApiResult;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.data.VersionModel;
import io.voyager1.service.dblog.BuildInfoService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * CI 冻结钩子集成测试（自动触发拦截 / 手动放行）
 *
 * @since 2026/8/7
 */
public class CiFreezeIntegrationTest extends ApplicationStartTest {

    @Autowired
    private BuildExecuteService buildExecuteService;
    @Autowired
    private BuildInfoService buildInfoService;
    @Autowired
    private VersionService versionService;

    private BuildInfoModel createBuildInfo(String name) {
        BuildInfoModel model = new BuildInfoModel();
        model.setId("test-freeze-" + java.util.UUID.randomUUID().toString());
        model.setName(name);
        model.setRepositoryId("test-repo");
        buildInfoService.insert(model);
        return model;
    }

    @BeforeEach
    public void clean() {
        io.voyager1.common.BaseServerController.resetInfo(io.voyager1.model.user.UserModel.EMPTY);
        for (io.voyager1.model.data.VersionModel v : versionService.listByBuildId("test-freeze-build")) {
            versionService.delByKey(v.getId());
        }
    }

    @Test
    public void testAutoTriggerFrozenWhenSubmitted() {
        BuildInfoModel buildInfo = createBuildInfo("冻结测试");
        // 创建版本并提测
        VersionModel version = versionService.createVersion(buildInfo.getId(), 1, "v1.0.0", "/tmp/x.jar", "测试");
        versionService.submit(version.getId(), "提测");
        // 自动触发（triggerBuildType=1 WebHook）应被冻结
        ApiResult<Integer> result = buildExecuteService.start(buildInfo.getId(), null, null, 1, "auto trigger");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(405, result.getCode());
        Assertions.assertTrue(result.getMsg().contains("已提测"));
    }

    @Test
    public void testCronTriggerFrozenWhenSubmitted() {
        BuildInfoModel buildInfo = createBuildInfo("冻结测试2");
        VersionModel version = versionService.createVersion(buildInfo.getId(), 1, "v1.0.0", "/tmp/x.jar", "测试");
        versionService.submit(version.getId(), "提测");
        // cron 触发（triggerBuildType=2）应被冻结
        ApiResult<Integer> result = buildExecuteService.start(buildInfo.getId(), null, null, 2, "cron trigger");
        Assertions.assertEquals(405, result.getCode());
    }

    @Test
    public void testManualTriggerNotFrozen() {
        BuildInfoModel buildInfo = createBuildInfo("冻结测试3");
        VersionModel version = versionService.createVersion(buildInfo.getId(), 1, "v1.0.0", "/tmp/x.jar", "测试");
        versionService.submit(version.getId(), "提测");
        // 手动触发（triggerBuildType=0）不应被冻结（走到下一步：checkStatus/构建中检查）
        // 后续步骤可能因构建配置不完整而失败（"构建信息缺失"等），但绝不能命中冻结分支
        try {
            ApiResult<Integer> result = buildExecuteService.start(buildInfo.getId(), null, null, 0, "manual");
            Assertions.assertNotEquals(405, result.getCode());
            Assertions.assertFalse(result.getMsg().contains("已提测"));
        } catch (IllegalArgumentException e) {
            Assertions.assertFalse(e.getMessage().contains("已提测"), "手动触发不应被 CI 冻结拦截");
        }
    }

    @Test
    public void testReturnedUnfreezes() {
        BuildInfoModel buildInfo = createBuildInfo("冻结测试4");
        VersionModel version = versionService.createVersion(buildInfo.getId(), 1, "v1.0.0", "/tmp/x.jar", "测试");
        versionService.submit(version.getId(), "提测");
        // 打回
        versionService.returnVersion(version.getId(), "测试不通过");
        // 自动触发恢复（不应被冻结）
        try {
            ApiResult<Integer> result = buildExecuteService.start(buildInfo.getId(), null, null, 1, "auto after return");
            Assertions.assertNotEquals(405, result.getCode());
        } catch (IllegalArgumentException e) {
            Assertions.assertFalse(e.getMessage().contains("已提测"), "打回后自动触发不应被冻结拦截");
        }
    }
}
