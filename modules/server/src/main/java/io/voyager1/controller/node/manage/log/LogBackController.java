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

package io.voyager1.controller.node.manage.log;

import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.NodeDataPermission;
import io.voyager1.service.node.ProjectInfoCacheService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 控制台日志备份管理
 *
 * @since 2019/3/7
 */
@Controller
@RequestMapping(value = "node/manage/log")
@Feature(cls = ClassFeature.PROJECT_LOG)
@NodeDataPermission(cls = ProjectInfoCacheService.class)
public class LogBackController extends BaseServerController {

    private final ProjectInfoCacheService projectInfoCacheService;

    public LogBackController(ProjectInfoCacheService projectInfoCacheService) {
        this.projectInfoCacheService = projectInfoCacheService;
    }

    @RequestMapping(value = "export", method = RequestMethod.GET)
    @ResponseBody
    @Feature(method = MethodFeature.DOWNLOAD)
    public void export(HttpServletRequest request, HttpServletResponse response) {
        NodeForward.requestDownload(getNode(), request, response, NodeUrl.Manage_Log_export);
    }

    /**
     * get log back list
     * 日志备份列表接口
     *
     * @return json
     */
    @RequestMapping(value = "log-back-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> logBackList(HttpServletRequest request) {
        JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.Manage_Log_logBack, request, JSONObject.class);
        return ApiResult.success("", jsonObject);
    }

    @RequestMapping(value = "logBack_download", method = RequestMethod.GET)
    @ResponseBody
    @Feature(method = MethodFeature.DOWNLOAD)
    public void download(HttpServletResponse response, HttpServletRequest request) {
        NodeForward.requestDownload(getNode(), request, response, NodeUrl.Manage_Log_logBack_download);
    }

    @RequestMapping(value = "logBack_delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> clear(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_Log_logBack_delete);
    }

    @RequestMapping(value = "logSize", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ApiResult<JSONObject> logSize(String id) {

        return NodeForward.request(getNode(), NodeUrl.Manage_Log_LogSize, "id", id);
    }

    /**
     * 重置日志
     *
     * @return json
     */
    @RequestMapping(value = "resetLog", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> resetLog(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_Log_ResetLog);
    }
}
