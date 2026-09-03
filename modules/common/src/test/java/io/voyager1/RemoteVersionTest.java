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

package io.voyager1;

import io.voyager1.util.FileUtil;
import io.voyager1.util.SystemUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * RemoteVersion 版本检查行为契约测试
 *
 * @since 2026/8/3
 */
public class RemoteVersionTest {

    @AfterEach
    public void restore() {
        RemoteVersion.setRemoteVersionUrl(null);
        System.clearProperty("VOYAGER1_REMOTE_VERSION_CACHE_FILE");
        System.clearProperty("VOYAGER1_IS_DEBUG");
        System.clearProperty("VOYAGER1_TYPE");
        System.clearProperty("VOYAGER1_VERSION");
        RemoteVersion.changeBetaRelease("false");
    }

    @Test
    public void testLoadRemoteInfoEmptyUrl() {
        // 未配置 URL 时安全降级：返回 null 且不抛异常
        RemoteVersion remoteVersion = RemoteVersion.loadRemoteInfo();
        Assertions.assertNull(remoteVersion);
    }

    @Test
    public void testInvalidUrlFallsBackToEmpty() {
        RemoteVersion.setRemoteVersionUrl("not-a-url");
        RemoteVersion remoteVersion = RemoteVersion.loadRemoteInfo();
        Assertions.assertNull(remoteVersion);
    }

    @Test
    public void testBetaRelease() {
        Assertions.assertFalse(RemoteVersion.betaRelease());
        RemoteVersion.changeBetaRelease("true");
        Assertions.assertTrue(RemoteVersion.betaRelease());
        RemoteVersion.changeBetaRelease("false");
        Assertions.assertFalse(RemoteVersion.betaRelease());
    }

    @Test
    public void testCacheInfoNoFile() {
        // 未设置缓存文件路径时安全返回（或由环境变量断言保护）
        File tempFile = FileUtil.file(FileUtil.getTmpDir(), "voyager1-remote-version-" + System.currentTimeMillis() + ".json");
        System.setProperty("VOYAGER1_REMOTE_VERSION_CACHE_FILE", tempFile.getAbsolutePath());
        Assertions.assertNull(RemoteVersion.cacheInfo());
        FileUtil.del(tempFile);
    }

    @Test
    public void testGetDownloadSource() {
        RemoteVersion remoteVersion = new RemoteVersion();
        remoteVersion.setDownloadSource("https://example.com/source");
        // 未配置 auth 时返回默认值
        Assertions.assertEquals("https://example.com/source", remoteVersion.getDownloadSource());
    }
}
