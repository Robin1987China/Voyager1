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

package io.voyager1.controller.outgiving;

import io.voyager1.util.CollUtil;
import io.voyager1.util.RegexPool;
import io.voyager1.util.ReUtil;
import io.voyager1.util.ReflectUtil;
import io.voyager1.core.api.ApiResult;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.i18n.I18nThreadUtil;
import io.voyager1.func.files.service.StaticFileStorageService;
import io.voyager1.model.data.AgentWhitelist;
import io.voyager1.model.data.ServerWhitelist;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.system.SystemParametersServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点授权
 *
 * @since 2019/4/22
 */
@RestController
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING_CONFIG_WHITELIST)
@Slf4j
public class OutGivingWhitelistController extends BaseServerController {

    private final SystemParametersServer systemParametersServer;
    private final OutGivingWhitelistService outGivingWhitelistService;
    private final StaticFileStorageService staticFileStorageService;

    public OutGivingWhitelistController(SystemParametersServer systemParametersServer,
                                        OutGivingWhitelistService outGivingWhitelistService,
                                        StaticFileStorageService staticFileStorageService) {
        this.systemParametersServer = systemParametersServer;
        this.outGivingWhitelistService = outGivingWhitelistService;
        this.staticFileStorageService = staticFileStorageService;
    }


    /**
     * get whiteList data
     * 授权数据接口
     *
     * @return json
     */
    @RequestMapping(value = "white-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Map<String, Object>> whiteList(HttpServletRequest request) {
        ServerWhitelist serverWhitelist = outGivingWhitelistService.getServerWhitelistData(request);
        Field[] fields = ReflectUtil.getFields(ServerWhitelist.class);
        Map<String, Object> map = new HashMap<>(8);
        for (Field field : fields) {
            Object value = ReflectUtil.getFieldValue(serverWhitelist, field);
            if (value instanceof Collection) {
                Collection<String> fieldValue = (Collection<String>) value;
                map.put(field.getName(), AgentWhitelist.convertToLine(fieldValue));
                map.put(field.getName() + "Array", fieldValue);
            }
        }
        return ApiResult.success("", map);
    }


    /**
     * 保存节点授权
     *
     * @param outGiving 数据
     * @return json
     */
    @RequestMapping(value = "whitelistDirectory_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @SystemPermission
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> whitelistDirectorySubmit(String outGiving,
                                                         String allowRemoteDownloadHost,
                                                         String staticDir,
                                                         HttpServletRequest request) {
        String workspaceId = nodeService.getCheckUserWorkspace(request);
        return this.whitelistDirectorySubmit(outGiving, staticDir, allowRemoteDownloadHost, workspaceId);
    }


    private ApiResult<String> whitelistDirectorySubmit(String outGiving,
                                                          String staticDir,
                                                          String allowRemoteDownloadHost,
                                                          String workspaceId) {
        List<String> list = AgentWhitelist.parseToList(outGiving, true, "授权目录不能为空");
        list = AgentWhitelist.covertToArray(list, "授权目录不能位于Voyager1目录下");
        String error = AgentWhitelist.findStartsWith(list);
        Assert.isNull(error, "授权目录中不能存在包含关系：" + error);
        //
        List<String> staticDirList = AgentWhitelist.parseToList(staticDir, false, "静态目录授权不能为空");
        staticDirList = AgentWhitelist.covertToArray(staticDirList, 100, "静态目录授权不能位于Voyager1目录下");
        error = AgentWhitelist.findStartsWith(staticDirList);
        Assert.isNull(error, "静态目录中不能存在包含关系：" + error);

        ServerWhitelist serverWhitelist = outGivingWhitelistService.getServerWhitelistData(workspaceId);
        serverWhitelist.setOutGiving(list);
        serverWhitelist.setStaticDir(staticDirList);
        //
        List<String> allowRemoteDownloadHostList = AgentWhitelist.parseToList(allowRemoteDownloadHost, "运行远程下载的 host 不能配置为空");
        //
        if ((allowRemoteDownloadHostList != null && !allowRemoteDownloadHostList.isEmpty())) {
            for (String s : allowRemoteDownloadHostList) {
                Assert.state(ReUtil.isMatch(RegexPool.URL_HTTP, s), "配置的远程地址不规范,请重新填写：" + s);
            }
        }
        java.util.Set<String> allowHosts = allowRemoteDownloadHostList == null ? null : new java.util.HashSet<>(allowRemoteDownloadHostList);
        serverWhitelist.setAllowRemoteDownloadHost(allowHosts);
        //

        String id = ServerWhitelist.workspaceId(workspaceId);
        systemParametersServer.upsert(id, serverWhitelist, id);

        String resultData = AgentWhitelist.convertToLine(list);
        // 重新检查静态目录任务状态
        I18nThreadUtil.execute(() -> {
            try {
                staticFileStorageService.startLoad();
            } catch (Exception e) {
                log.error("静态文件任务加载失败", e);
            }
        });

        return ApiResult.success("保存成功", resultData);
    }
}
