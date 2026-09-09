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
import io.voyager1.core.entity.BuildHistoryLogEntity;
import io.voyager1.core.repository.BuildHistoryLogRepository;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.data.VersionModel;
import io.voyager1.model.enums.BuildStatus;
import io.voyager1.model.enums.VersionStatus;
import io.voyager1.service.dblog.BuildInfoService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/**
 * 「版本在部署时从构建记录自动生成」集成测试。
 * <p>
 * 覆盖 {@link VersionService#createVersionFromBuild}：从 CI_BUILD_LOG 构建记录派生版本号（tag/commit/回退），
 * 产物引用取自构建记录 resultDirFile，不依赖真实构建执行。
 *
 * @since 2026/9/8
 */
public class CreateVersionFromBuildTest extends ApplicationStartTest {

    @Autowired
    private BuildInfoService buildInfoService;
    @Autowired
    private BuildHistoryLogRepository buildHistoryLogRepository;
    @Autowired
    private VersionService versionService;

    @BeforeEach
    public void reset() {
        io.voyager1.common.BaseServerController.resetInfo(io.voyager1.model.user.UserModel.EMPTY);
    }

    private static String newBuildId(String prefix) {
        // id 列长度 VARCHAR(50)，前缀 + 8 位随机保证唯一且不超长
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    private BuildInfoModel createBuildInfo(String id, String tag, String commit) {
        BuildInfoModel model = new BuildInfoModel();
        model.setId(id);
        model.setName("从构建生成版本测试-" + id);
        model.setRepositoryId("test-repo");
        model.setBuildId(100);
        model.setBranchTagName(tag);
        model.setRepositoryLastCommitId(commit);
        buildInfoService.insert(model);
        return model;
    }

    private void createBuildHistory(String buildId, int buildNumberId, String resultDirFile) {
        BuildHistoryLogEntity log = new BuildHistoryLogEntity();
        log.setId(UUID.randomUUID().toString());
        log.setBuildDataId(buildId);
        log.setBuildNumberId(buildNumberId);
        log.setBuildName("test-build");
        log.setResultDirFile(resultDirFile);
        log.setStatus(BuildStatus.Success.getCode());
        long now = System.currentTimeMillis();
        log.setStartTime(now);
        log.setEndTime(now);
        log.setCreateTimeMillis(now);
        log.setModifyTimeMillis(now);
        buildHistoryLogRepository.save(log);
    }

    @Test
    public void testVersionFromTag() {
        String buildId = newBuildId("fb-tag-");
        createBuildInfo(buildId, "2.0.0", null);
        createBuildHistory(buildId, 5, "/tmp/artifact.jar");
        VersionModel version = versionService.createVersionFromBuild(buildId, 5, "从 tag 生成");
        Assertions.assertEquals("v2.0.0", version.getVersion());
        Assertions.assertEquals("/tmp/artifact.jar", version.getArtifactRef());
        Assertions.assertEquals(VersionStatus.Developing.getCode(), version.getStatus());
        Assertions.assertEquals(buildId, version.getBuildId());
    }

    @Test
    public void testVersionFromCommit() {
        String buildId = newBuildId("fb-com-");
        createBuildInfo(buildId, null, "abcdef1234567890");
        createBuildHistory(buildId, 3, "/tmp/artifact2.jar");
        VersionModel version = versionService.createVersionFromBuild(buildId, 3, "从 commit 生成");
        Assertions.assertEquals("v100.3.abcdef12", version.getVersion());
        Assertions.assertEquals("/tmp/artifact2.jar", version.getArtifactRef());
    }

    @Test
    public void testVersionFallback() {
        String buildId = newBuildId("fb-fb-");
        createBuildInfo(buildId, null, null);
        createBuildHistory(buildId, 1, "/tmp/artifact3.jar");
        VersionModel version = versionService.createVersionFromBuild(buildId, 1, "回退生成");
        Assertions.assertNotNull(version.getVersion());
        Assertions.assertTrue(version.getVersion().startsWith("v100.1."));
        Assertions.assertEquals("/tmp/artifact3.jar", version.getArtifactRef());
    }

    @Test
    public void testMissingBuildRecord() {
        String buildId = newBuildId("fb-miss-");
        createBuildInfo(buildId, "9.9.9", null);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> versionService.createVersionFromBuild(buildId, 99, "构建记录不存在"));
    }
}
