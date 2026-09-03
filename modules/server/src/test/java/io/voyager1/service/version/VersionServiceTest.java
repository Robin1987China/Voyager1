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

import io.voyager1.ApplicationStartTest;
import io.voyager1.model.data.VersionModel;
import io.voyager1.model.enums.VersionStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 版本状态机测试（真实 H2 上下文）
 *
 * @since 2026/8/7
 */
public class VersionServiceTest extends ApplicationStartTest {

    @Autowired
    private VersionService versionService;

    @BeforeEach
    public void clean() {
        // 清理测试数据（共用真实 H2，避免残留污染）
        for (String buildId : new String[]{"test-build-id", "test-return-build", "test-release-build", "test-illegal-build"}) {
            for (io.voyager1.model.data.VersionModel v : versionService.listByBuildId(buildId)) {
                versionService.delByKey(v.getId());
            }
        }
    }

    private VersionModel createVersion() {
        return createVersion("test-build-id");
    }

    private VersionModel createVersion(String buildId) {
        return versionService.createVersion(buildId, 1, "v1.2.3", "/tmp/artifact.jar", "单元测试");
    }

    @Test
    public void testCreateVersion() {
        VersionModel model = createVersion();
        Assertions.assertNotNull(model.getId());
        Assertions.assertEquals(VersionStatus.Developing.getCode(), model.getStatus());
        Assertions.assertEquals("v1.2.3", model.getVersion());
    }

    @Test
    public void testSubmitFreezeCi() {
        VersionModel model = createVersion();
        versionService.submit(model.getId(), "提测");
        VersionModel updated = versionService.getByKey(model.getId());
        Assertions.assertEquals(VersionStatus.Submitted.getCode(), updated.getStatus());
        // 冻结判定
        Assertions.assertTrue(versionService.hasSubmittedVersion("test-build-id"));
    }

    @Test
    public void testReturnUnlockCi() {
        VersionModel model = createVersion("test-return-build");
        versionService.submit(model.getId(), "提测");
        versionService.returnVersion(model.getId(), "测试不通过");
        VersionModel updated = versionService.getByKey(model.getId());
        Assertions.assertEquals(VersionStatus.Returned.getCode(), updated.getStatus());
        // 解锁判定
        Assertions.assertFalse(versionService.hasSubmittedVersion("test-build-id"));
    }

    @Test
    public void testRelease() {
        VersionModel model = createVersion("test-release-build");
        versionService.submit(model.getId(), "提测");
        versionService.release(model.getId(), "发布");
        VersionModel updated = versionService.getByKey(model.getId());
        Assertions.assertEquals(VersionStatus.Released.getCode(), updated.getStatus());
    }

    @Test
    public void testIllegalTransition() {
        VersionModel model = createVersion("test-illegal-build");
        // 开发中直接打回 -> 拒绝
        Assertions.assertThrows(IllegalStateException.class, () -> versionService.returnVersion(model.getId(), "非法流转"));
        // 已发布再次提测 -> 拒绝
        versionService.submit(model.getId(), "提测");
        versionService.release(model.getId(), "发布");
        Assertions.assertThrows(IllegalStateException.class, () -> versionService.submit(model.getId(), "再次提测"));
    }

    @Test
    public void testListByBuildId() {
        String buildId = "test-list-" + System.currentTimeMillis();
        createVersion(buildId);
        createVersion(buildId);
        List<VersionModel> list = versionService.listByBuildId(buildId);
        Assertions.assertEquals(2, list.size());
    }
}
