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

package io.voyager1.controller.system;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.ResourceUtil;
import io.voyager1.util.Tuple;
import io.voyager1.util.StrUtil;
import io.voyager1.util.URLUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.core.AppType;
import io.voyager1.Voyager1Application;
import io.voyager1.common.BaseAgentController;
import io.voyager1.common.Const;
import io.voyager1.common.Voyager1Manifest;
import io.voyager1.common.RemoteVersion;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.AgentConfig;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

/**
 * 在线升级
 *
 * @since 2019/7/22
 */
@RestController
@RequestMapping(value = "system")
public class SystemUpdateController extends BaseAgentController {

    private final AgentConfig agentConfig;
    private final Voyager1Application configBean;

    public SystemUpdateController(AgentConfig agentConfig,
                                  Voyager1Application configBean) {
        this.agentConfig = agentConfig;
        this.configBean = configBean;
    }

    @PostMapping(value = "upload-jar-sharding", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> uploadJarSharding(MultipartFile file, String sliceId,
                                                  Integer totalSlice,
                                                  Integer nowSlice,
                                                  String fileSumMd5) throws IOException {
        //
        String tempPathName = agentConfig.getFixedTempPathName();
        this.uploadSharding(file, tempPathName, sliceId, totalSlice, nowSlice, fileSumMd5, "jar", "zip");
        return ApiResult.success("上传成功");
    }

    @PostMapping(value = "upload-jar-sharding-merge", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> uploadJarShardingMerge(String sliceId,
                                                      Integer totalSlice,
                                                      String fileSumMd5) throws IOException {
        //
        String tempPathName = agentConfig.getFixedTempPathName();
        File successFile = this.shardingTryMerge(tempPathName, sliceId, totalSlice, fileSumMd5);
        Objects.requireNonNull(Voyager1Manifest.getScriptFile());
        String absolutePath = agentConfig.getTempPath().getAbsolutePath();
        String path = FileUtil.getAbsolutePath(successFile);
        // 解析压缩包
        File file = Voyager1Manifest.zipFileFind(path, AppType.Agent, absolutePath);
        path = FileUtil.getAbsolutePath(file);
        // 基础检查
        ApiResult<Tuple> error = Voyager1Manifest.checkVoyager1Jar(path, AppType.Agent);
        if (!error.success()) {
            return new ApiResult<>(error.getCode(), error.getMsg());
        }
        Tuple data = error.getData();
        String version = data.get(0);
        Voyager1Manifest.releaseJar(path, version);
        //
        Voyager1Application.restart();
        return ApiResult.success(Const.UPGRADE_MSG.get());
    }

    @PostMapping(value = "change_log", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> changeLog(String beta) {
        //
        boolean betaBool = ConvertUtil.toBool(beta, false);
        boolean betaRelease = RemoteVersion.betaRelease();
        URL resource = ResourceUtil.getResource((betaRelease || betaBool) ? "CHANGELOG-BETA.md" : "CHANGELOG.md");
        String log = "";
        if (resource != null) {
            InputStream stream = URLUtil.getStream(resource);
            log = IoUtil.readUtf8(stream);
        }
        return ApiResult.success("", log);
    }

    /**
     * 检查是否存在新版本
     *
     * @return json
     * @see RemoteVersion
     */
    @PostMapping(value = "check_version.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<io.voyager1.RemoteVersion> checkVersion() {
        io.voyager1.RemoteVersion remoteVersion = RemoteVersion.loadRemoteInfo();
        return ApiResult.success("", remoteVersion);
    }

    /**
     * 远程下载升级
     *
     * @return json
     * @see RemoteVersion
     */
    @PostMapping(value = "remote_upgrade.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Object> upgrade() throws IOException {
        RemoteVersion.upgrade(configBean.getTempPath().getAbsolutePath());
        return ApiResult.success(Const.UPGRADE_MSG.get());
    }
}
