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
import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerConst;
import io.voyager1.common.ServerOpenApi;
import io.voyager1.common.UrlRedirectUtil;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.func.files.model.StaticFileStorageModel;
import io.voyager1.func.files.service.FileStorageService;
import io.voyager1.func.files.service.StaticFileStorageService;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 23/12/28 028
 */
@RestController
@RequestMapping(value = "/file-storage/static")
@Feature(cls = ClassFeature.STATIC_FILE_STORAGE)
public class StaticFileStorageController extends BaseServerController {

    private final StaticFileStorageService staticFileStorageService;
    private final FileStorageService fileStorageService;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public StaticFileStorageController(StaticFileStorageService staticFileStorageService,
                                       FileStorageService fileStorageService,
                                       TriggerTokenLogServer triggerTokenLogServer) {
        this.staticFileStorageService = staticFileStorageService;
        this.fileStorageService = fileStorageService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * 分页列表
     *
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<StaticFileStorageModel>> list(HttpServletRequest request) {
        //
        String workspace = fileStorageService.getCheckUserWorkspace(request);
        PageResultDto<StaticFileStorageModel> listPage = staticFileStorageService.listPage(request, workspace);
        return ApiResult.success("", listPage);
    }

    @GetMapping(value = "del", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> del(String id, String ids, @ValidatorItem Boolean thorough, HttpServletRequest request) throws IOException {
        this.delItem(id, thorough, request);
        List<String> list = io.voyager1.util.ConvertUtil.splitTrim(ids, ",");
        for (String s : list) {
            this.delItem(s, thorough, request);
        }
        return ApiResult.success("删除成功");
    }

    /**
     * 删除数据
     *
     * @param id       id
     * @param thorough 是否彻底删除
     * @param request  请求
     */
    private void delItem(String id, Boolean thorough, HttpServletRequest request) {
        if ((id == null || id.isEmpty())) {
            return;
        }
        StaticFileStorageModel storageModel = staticFileStorageService.getByKey(id);
        if (storageModel == null) {
            return;
        }
        this.checkStaticDir(storageModel, request);
        //
        if (thorough != null && thorough) {
            FileUtil.del(storageModel.getAbsolutePath());
        }
        //
        staticFileStorageService.delByKey(id);
    }

    /**
     * 判断是否有权限操作
     *
     * @param storageModel 静态文件
     */
    private void checkStaticDir(StaticFileStorageModel storageModel, HttpServletRequest request) {
        String workspace = fileStorageService.getCheckUserWorkspace(request);
        staticFileStorageService.checkStaticDir(storageModel, workspace);
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
        StaticFileStorageModel item = staticFileStorageService.getByKey(id);

        this.checkStaticDir(item, request);
        //
        StaticFileStorageModel updateInfo;
        if ((item.getTriggerToken() == null || item.getTriggerToken().isEmpty()) || (rest != null && !rest.isEmpty())) {
            updateInfo = new StaticFileStorageModel();
            updateInfo.setId(id);
            updateInfo.setTriggerToken(triggerTokenLogServer.restToken(item.getTriggerToken(), staticFileStorageService.typeName(),
                item.getId(), user.getId()));
            staticFileStorageService.updateById(updateInfo);
        } else {
            updateInfo = item;
        }
        Map<String, String> map = this.getBuildToken(updateInfo, request);
        String string = "重置成功";
        return ApiResult.success((rest == null || rest.isEmpty()) ? "ok" : string, map);
    }

    private Map<String, String> getBuildToken(StaticFileStorageModel item, HttpServletRequest request) {
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
        Map<String, String> map = new HashMap<>(10);
        {
            String url = ServerOpenApi.STATIC_FILE_STORAGE_DOWNLOAD.
                replace("{id}", item.getId()).
                replace("{token}", item.getTriggerToken());
            String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
            map.put("triggerDownloadUrl", FileUtil.normalize(triggerBuildUrl));
        }
        map.put("id", item.getId());
        map.put("token", item.getTriggerToken());
        return map;
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> edit(@ValidatorItem String id,
                                     String description,
                                     HttpServletRequest request) throws IOException {
        StaticFileStorageModel storageModel = staticFileStorageService.getByKey(id);
        this.checkStaticDir(storageModel, request);
        StaticFileStorageModel fileStorageModel = new StaticFileStorageModel();
        fileStorageModel.setId(id);
        fileStorageModel.setDescription(description);
        staticFileStorageService.updateById(fileStorageModel);
        return ApiResult.success("修改成功");
    }

    /**
     * 判断是否存在文件
     *
     * @param fileId 文件id
     * @return json
     */
    @GetMapping(value = "has-file", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<StaticFileStorageModel> hasFile(@ValidatorItem String fileId, HttpServletRequest request) {
        StaticFileStorageModel storageModel = staticFileStorageService.getByKey(fileId);
        this.checkStaticDir(storageModel, request);
        return ApiResult.success("", storageModel);
    }

    /**
     * 重新扫描
     *
     * @return json
     */
    @GetMapping(value = "scanner", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<String> scanner(HttpServletRequest request) {
        boolean scanning = staticFileStorageService.isScanning();
        Assert.state(!scanning, "当前正在扫描中");
        String workspace = fileStorageService.getCheckUserWorkspace(request);
        staticFileStorageService.scanByWorkspace(workspace);
        return ApiResult.success("扫描成功");
    }
}
