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

package io.voyager1.func.files.controller;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.DateTime;
import io.voyager1.util.FileUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.Validator;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerConst;
import io.voyager1.common.ServerOpenApi;
import io.voyager1.common.UrlRedirectUtil;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.controller.outgiving.OutGivingWhitelistService;
import io.voyager1.func.files.model.FileStorageModel;
import io.voyager1.func.files.service.FileStorageService;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.ServerWhitelist;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.user.TriggerTokenLogServer;
import io.voyager1.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 2023/3/16
 */
@RestController
@RequestMapping(value = "/file-storage")
@Feature(cls = ClassFeature.FILE_STORAGE)
public class FileStorageController extends BaseServerController {
    private final ServerConfig serverConfig;
    private final FileStorageService fileStorageService;
    private final OutGivingWhitelistService outGivingWhitelistService;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public FileStorageController(ServerConfig serverConfig,
                                 FileStorageService fileStorageService,
                                 OutGivingWhitelistService outGivingWhitelistService,
                                 TriggerTokenLogServer triggerTokenLogServer) {
        this.serverConfig = serverConfig;
        this.fileStorageService = fileStorageService;
        this.outGivingWhitelistService = outGivingWhitelistService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * 分页列表
     *
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<FileStorageModel>> list(HttpServletRequest request) {
        File storageSavePath = serverConfig.fileStorageSavePath();
        //
        PageResultDto<FileStorageModel> listPage = fileStorageService.listPage(request);
        listPage.each(fileStorageModel -> {
            File file = FileUtil.file(storageSavePath, fileStorageModel.getPath());
            fileStorageModel.setExists(FileUtil.isFile(file));
        });
        return ApiResult.success("", listPage);
    }

    /**
     * 判断是否存在文件
     *
     * @param fileSumMd5 文件 md5
     * @return json
     */
    @GetMapping(value = "has-file", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<FileStorageModel> hasFile(@ValidatorItem String fileSumMd5) {
        FileStorageModel storageModel = fileStorageService.getByKey(fileSumMd5);
        return ApiResult.success("", storageModel);
    }

    /**
     * 上传分片
     *
     * @param file       文件对象
     * @param sliceId    分片id
     * @param totalSlice 总分片
     * @param nowSlice   当前分片
     * @param fileSumMd5 文件 md5
     * @return json
     * @throws IOException io
     */
    @PostMapping(value = "upload-sharding", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD, log = false)
    public ApiResult<String> uploadSharding(MultipartFile file,
                                               String sliceId,
                                               Integer totalSlice,
                                               Integer nowSlice,
                                               String fileSumMd5) throws IOException {
        File userTempPath = serverConfig.getUserTempPath();
        this.uploadSharding(file, userTempPath.getAbsolutePath(), sliceId, totalSlice, nowSlice, fileSumMd5);
        return ApiResult.success("上传成功");
    }

    /**
     * 合并文件分片
     *
     * @param sliceId    分片id
     * @param totalSlice 增分片数
     * @param fileSumMd5 文件 md5
     * @return json
     * @throws IOException 异常
     */
    @PostMapping(value = "upload-sharding-merge", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public ApiResult<String> uploadMerge(String sliceId,
                                            Integer totalSlice,
                                            String fileSumMd5,
                                            Integer keepDay,
                                            String description,
                                            String aliasCode,
                                            HttpServletRequest request) throws IOException {
        Opt.ofBlankAble(aliasCode).ifPresent(s -> Validator.validateGeneral(s, "别名码只能是英文、数字"));
        File storageSavePath = serverConfig.fileStorageSavePath();
        // 验证文件
        FileStorageModel fileStorageModel1 = fileStorageService.getByKey(fileSumMd5);
        if (fileStorageModel1 != null) {
            // 如果存在记录，判断文件是否存在
            File file = FileUtil.file(storageSavePath, fileStorageModel1.getPath());
            Assert.state(!FileUtil.exist(file), "当前文件已经存在啦，请勿重复上传");
        }
        // 合并文件
        File userTempPath = serverConfig.getUserTempPath();
        File successFile = this.shardingTryMerge(userTempPath.getAbsolutePath(), sliceId, totalSlice, fileSumMd5);
        String extName = FileUtil.extName(successFile);
        String path = String.format("/%s/%s.%s", DateTime.now().toString("yyyyMMdd"), fileSumMd5, extName);

        File fileStorageFile = FileUtil.file(storageSavePath, path);
        FileUtil.mkParentDirs(fileStorageFile);
        FileUtil.move(successFile, fileStorageFile, true);
        // 保存
        FileStorageModel fileStorageModel = new FileStorageModel();
        fileStorageModel.setId(fileSumMd5);
        fileStorageModel.setName(successFile.getName());
        fileStorageModel.setDescription(description);
        fileStorageModel.setAliasCode(aliasCode);
        fileStorageModel.setExtName(extName);
        fileStorageModel.setPath(path);
        fileStorageModel.setSize(FileUtil.size(fileStorageFile));
        fileStorageModel.setSource(0);
        //
        fileStorageModel.setWorkspaceId(fileStorageService.covertGlobalWorkspace(request));
        fileStorageModel.validUntil(keepDay, null);
        //
        fileStorageService.insert(fileStorageModel);
        return ApiResult.success("上传成功");
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> edit(@ValidatorItem String id,
                                     @ValidatorItem String name,
                                     Integer keepDay,
                                     String description,
                                     String aliasCode,
                                     HttpServletRequest request) throws IOException {
        Opt.ofBlankAble(aliasCode).ifPresent(s -> Validator.validateGeneral(s, "别名码只能是英文、数字"));
        FileStorageModel storageModel = fileStorageService.getByKeyAndGlobal(id, request);

        FileStorageModel fileStorageModel = new FileStorageModel();
        fileStorageModel.setId(id);
        fileStorageModel.setName(name);
        fileStorageModel.setAliasCode(aliasCode);
        fileStorageModel.setDescription(description);
        //
        fileStorageModel.setWorkspaceId(fileStorageService.covertGlobalWorkspace(request));
        //
        fileStorageModel.validUntil(keepDay, storageModel.getCreateTimeMillis());
        fileStorageService.updateById(fileStorageModel);
        return ApiResult.success("修改成功");
    }

    @GetMapping(value = "del", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> del(String id, String ids, HttpServletRequest request) throws IOException {
        this.delItem(id, request);
        List<String> list = io.voyager1.util.ConvertUtil.splitTrim(ids, ",");
        for (String s : list) {
            this.delItem(s, request);
        }
        return ApiResult.success("删除成功");
    }

    private void delItem(String id, HttpServletRequest request) {
        if ((id == null || id.isEmpty())) {
            return;
        }
        FileStorageModel storageModel = fileStorageService.getByKeyAndGlobal(id, request);
        if (storageModel == null) {
            return;
        }
        //
        File storageSavePath = serverConfig.fileStorageSavePath();
        File fileStorageFile = FileUtil.file(storageSavePath, storageModel.getPath());
        FileUtil.del(fileStorageFile);
        //
        fileStorageService.delByKey(id);
    }

    /**
     * 远程下载
     *
     * @param url         远程 url
     * @param keepDay     保留天数
     * @param description 描述
     * @param global      是否全局共享
     * @param request     请求
     * @return json
     * @throws IOException io
     */
    @PostMapping(value = "remote-download", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.REMOTE_DOWNLOAD)
    public ApiResult<String> download(
        @ValidatorItem String url,
        Integer keepDay,
        String description,
        String aliasCode,
        Boolean global,
        HttpServletRequest request) throws IOException {
        Opt.ofBlankAble(aliasCode).ifPresent(s -> Validator.validateGeneral(s, "别名码只能是英文、数字"));
        // 验证远程 地址
        ServerWhitelist whitelist = outGivingWhitelistService.getServerWhitelistData(request);
        whitelist.checkAllowRemoteDownloadHost(url);
        String workspace = fileStorageService.getCheckUserWorkspace(request);
        fileStorageService.download(url, global, workspace, keepDay, description, aliasCode);
        return ApiResult.success("开始异步下载");
    }

    /**
     * get a trigger url
     *
     * @param id id
     * @return json
     */
    @GetMapping(value = "trigger-url", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Map<String, String>> getTriggerUrl(@ValidatorItem String id, String rest, HttpServletRequest request) {
        UserModel user = getUser();
        // 查询当前工作空间
        FileStorageModel item = fileStorageService.getByKey(id, request);
        Assert.notNull(item, "没有对应的文件信息");
        //
        FileStorageModel updateInfo;
        if ((item.getTriggerToken() == null || item.getTriggerToken().isEmpty()) || (rest != null && !rest.isEmpty())) {
            updateInfo = new FileStorageModel();
            updateInfo.setId(id);
            updateInfo.setTriggerToken(triggerTokenLogServer.restToken(item.getTriggerToken(), fileStorageService.typeName(),
                item.getId(), user.getId()));
            fileStorageService.updateById(updateInfo);
            // 避免无法查看发片下载地址
            updateInfo.setAliasCode(item.getAliasCode());
        } else {
            updateInfo = item;
        }
        Map<String, String> map = this.getBuildToken(updateInfo, request);
        String string = "重置成功";
        return ApiResult.success((rest == null || rest.isEmpty()) ? "ok" : string, map);
    }

    private Map<String, String> getBuildToken(FileStorageModel item, HttpServletRequest request) {
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
        Map<String, String> map = new HashMap<>(10);
        {
            String url = ServerOpenApi.FILE_STORAGE_DOWNLOAD.
                replace("{id}", item.getId()).
                replace("{token}", item.getTriggerToken());
            String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
            map.put("triggerDownloadUrl", FileUtil.normalize(triggerBuildUrl));
        }
        if ((item.getAliasCode() != null && !item.getAliasCode().isEmpty())) {
            String url = ServerOpenApi.FILE_STORAGE_DOWNLOAD.
                replace("{id}", item.getAliasCode()).
                replace("{token}", item.getTriggerToken());
            String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
            map.put("triggerAliasDownloadUrl", FileUtil.normalize(triggerBuildUrl));
        }
        map.put("id", item.getId());
        map.put("token", item.getTriggerToken());
        return map;
    }
}
