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

import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.ResourceUtil;
import io.voyager1.util.Tuple;
import io.voyager1.util.BooleanUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.URLUtil;
import io.voyager1.util.SystemUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.core.AppType;
import com.alibaba.fastjson2.JSONObject;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.common.*;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.dblog.BackupInfoService;
import io.voyager1.service.system.SystemParametersServer;
import io.voyager1.system.ServerConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

/**
 * 在线升级
 *
 * @since 2019/7/22
 */
@RestController
@RequestMapping(value = "system")
@Feature(cls = ClassFeature.SYSTEM_UPGRADE)
@SystemPermission(superUser = true)
@Slf4j
public class SystemUpdateController extends BaseServerController implements ILoadEvent {

    private static final String JOIN_VOYAGER1_BETA_RELEASE = "JOIN_VOYAGER1_BETA_RELEASE";
    public static final String VOYAGER1_REMOTE_VERSION_AUTH = "VOYAGER1_REMOTE_VERSION_AUTH";

    private final BackupInfoService backupInfoService;
    private final ServerConfig serverConfig;
    private final SystemParametersServer systemParametersServer;

    public SystemUpdateController(BackupInfoService backupInfoService,
                                  ServerConfig serverConfig,
                                  SystemParametersServer systemParametersServer) {
        this.backupInfoService = backupInfoService;
        this.serverConfig = serverConfig;
        this.systemParametersServer = systemParametersServer;
    }

    @PostMapping(value = "info", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> info(HttpServletRequest request, String machineId) {
        ApiResult<JSONObject> message = this.tryRequestMachine(machineId, request, NodeUrl.Info);
        return Optional.ofNullable(message).orElseGet(() -> {
            Voyager1Manifest instance = Voyager1Manifest.getInstance();
            io.voyager1.RemoteVersion remoteVersion = RemoteVersion.cacheInfo();
            //
            JSONObject jsonObject = new JSONObject();
            String auth = systemParametersServer.getConfig(VOYAGER1_REMOTE_VERSION_AUTH, String.class);
            jsonObject.put("auth", auth);
            jsonObject.put("manifest", instance);
            jsonObject.put("remoteVersion", remoteVersion);
            jsonObject.put("joinBetaRelease", RemoteVersion.betaRelease());
            return ApiResult.success("", jsonObject);
        });
    }

    @GetMapping(value = "change-beta-release", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<String> changeBetaRelease(String beta) {
        boolean betaBool = this.changeBetaRelease2(beta);
        RemoteVersion.loadRemoteInfo();
        String isBeta = "成功加入 beta 计划";
        String closeBeta = "关闭 beta 计划成功";
        return ApiResult.success(betaBool ? isBeta : closeBeta);
    }

    @GetMapping(value = "change-download-auth", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<String> changeDownloadAuth(String auth) {
        String data = (auth == null || auth.isEmpty() ? "" : auth);
        systemParametersServer.upsert(VOYAGER1_REMOTE_VERSION_AUTH, data, "下载授权码");
        System.setProperty(VOYAGER1_REMOTE_VERSION_AUTH, data);
        return ApiResult.success("更新成功");
    }

    private boolean changeBetaRelease2(String beta) {
        boolean betaBool = Boolean.parseBoolean(beta);
        systemParametersServer.upsert(JOIN_VOYAGER1_BETA_RELEASE, String.valueOf(betaBool), "是否加入 beta 计划");
        RemoteVersion.changeBetaRelease(String.valueOf(betaBool));
        return betaBool;
    }

    @PostMapping(value = "upload-jar-sharding", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE, log = false)
    public ApiResult<String> uploadJarSharding(MultipartFile file,
                                                  String machineId,
                                                  String sliceId,
                                                  Integer totalSlice,
                                                  Integer nowSlice,
                                                  String fileSumMd5, MultipartHttpServletRequest multiRequest) throws IOException {
        if ((machineId != null && !machineId.isEmpty())) {
            MachineNodeModel model = machineNodeServer.getByKey(machineId);
            Assert.notNull(model, "没有找到对应的机器");
            return NodeForward.requestMultipart(model, multiRequest, NodeUrl.SystemUploadJar);
        }
        String absolutePath = serverConfig.getUserTempPath().getAbsolutePath();
        this.uploadSharding(file, absolutePath, sliceId, totalSlice, nowSlice, fileSumMd5, "jar", "zip");
        return ApiResult.success("上传成功");
    }

    @PostMapping(value = "upload-jar-sharding-merge", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> uploadJar(String sliceId,
                                          Integer totalSlice,
                                          String fileSumMd5,
                                          HttpServletRequest request,
                                          String machineId) throws IOException {
        ApiResult<String> message = this.tryRequestMachine(machineId, request, NodeUrl.SystemUploadJarMerge);
        if (message != null) {
            // 判断-删除分片id
            BaseServerController.SHARDING_IDS.remove(sliceId);
            return message;
        }
        //
        String absolutePath = serverConfig.getUserTempPath().getAbsolutePath();
        File successFile = this.shardingTryMerge(absolutePath, sliceId, totalSlice, fileSumMd5);
        Objects.requireNonNull(Voyager1Manifest.getScriptFile());
        String path = FileUtil.getAbsolutePath(successFile);
        // 解析压缩包
        File file = Voyager1Manifest.zipFileFind(path, AppType.Server, absolutePath);
        path = FileUtil.getAbsolutePath(file);
        // 基础检查
        ApiResult<Tuple> error = Voyager1Manifest.checkVoyager1Jar(path, AppType.Server);
        if (!error.success()) {
            return new ApiResult<>(error.getCode(), error.getMsg());
        }
        Tuple data = error.getData();
        String version = data.get(0);
        Voyager1Manifest.releaseJar(path, version);
        //
        backupInfoService.autoBackup();
        //
        Voyager1Application.restart();
        return ApiResult.success(Const.UPGRADE_MSG.get());
    }

    /**
     * 检查是否存在新版本
     *
     * @return json
     * @see RemoteVersion
     */
    @PostMapping(value = "check_version.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<io.voyager1.RemoteVersion> checkVersion(HttpServletRequest request,
                                                                   String machineId) {
        ApiResult<io.voyager1.RemoteVersion> message = this.tryRequestMachine(machineId, request, NodeUrl.CHECK_VERSION);
        return Optional.ofNullable(message).orElseGet(() -> {
            io.voyager1.RemoteVersion remoteVersion = RemoteVersion.loadRemoteInfo();
            return ApiResult.success("", remoteVersion);
        });
    }

    /**
     * 远程下载升级
     *
     * @return json
     * @see RemoteVersion
     */
    @GetMapping(value = "remote_upgrade.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DOWNLOAD)
    public ApiResult<String> upgrade(HttpServletRequest request,
                                        String machineId) {

        ApiResult<String> message = this.tryRequestMachine(machineId, request, NodeUrl.REMOTE_UPGRADE);
        return Optional.ofNullable(message).orElseGet(() -> {
            try {
                RemoteVersion.upgrade(Voyager1Application.getInstance().getTempPath().getAbsolutePath(), objects -> backupInfoService.autoBackup());
            } catch (IOException e) {
                throw Lombok.sneakyThrow(e);
            }
            return ApiResult.success(Const.UPGRADE_MSG.get());
        });
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        String config = systemParametersServer.getConfig(JOIN_VOYAGER1_BETA_RELEASE, String.class);
        boolean release2 = this.changeBetaRelease2(config);
        String auth = systemParametersServer.getConfig(VOYAGER1_REMOTE_VERSION_AUTH, String.class);
        String msg = "版本配置信息";
        log.debug("{} beta:{} auth:{}", msg, release2, auth);
        if (auth != null) {
            System.setProperty(VOYAGER1_REMOTE_VERSION_AUTH, auth);
        } else {
            System.clearProperty(VOYAGER1_REMOTE_VERSION_AUTH);
        }
    }
}
