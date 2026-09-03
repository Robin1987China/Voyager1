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

package io.voyager1.func.openapi.controller;

import io.voyager1.util.StrUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseVoyager1Controller;
import io.voyager1.common.ServerOpenApi;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.interceptor.NotLogin;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.node.ProjectInfoCacheModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.node.NodeService;
import io.voyager1.service.node.ProjectInfoCacheService;
import io.voyager1.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目触发器
 *
 * @since 2022/12/18
 */
@RestController
@RequestMapping
@NotLogin
@Slf4j
public class ProjectTriggerApiController extends BaseVoyager1Controller {

    private final ProjectInfoCacheService projectInfoCacheService;
    private final TriggerTokenLogServer triggerTokenLogServer;
    private final NodeService nodeService;

    public ProjectTriggerApiController(ProjectInfoCacheService projectInfoCacheService,
                                       TriggerTokenLogServer triggerTokenLogServer,
                                       NodeService nodeService) {
        this.projectInfoCacheService = projectInfoCacheService;
        this.triggerTokenLogServer = triggerTokenLogServer;
        this.nodeService = nodeService;
    }

    private NodeUrl resolveAction(String action) {
        if ((action != null && action.equalsIgnoreCase("stop"))) {
            return NodeUrl.Manage_Operate;
        }
        if ((action != null && action.equalsIgnoreCase("start"))) {
            return NodeUrl.Manage_Operate;
        }
        if ((action != null && action.equalsIgnoreCase("restart"))) {
            return NodeUrl.Manage_Operate;
        }
        if ((action != null && action.equalsIgnoreCase("reload"))) {
            return NodeUrl.Manage_Operate;
        }
        return NodeUrl.Manage_GetProjectStatus;
    }

    private ApiResult<Object> execAction(ProjectInfoCacheModel item, String action) {
        NodeUrl resolveAction = this.resolveAction(action);
        //
        NodeModel nodeModel = nodeService.getByKey(item.getNodeId());
        return NodeForward.request(nodeModel, resolveAction,
            "id", item.getProjectId(), "opt", action);
    }

    /**
     * 执行脚本
     *
     * @param id    构建ID
     * @param token 构建的token
     * @return json
     */
    @RequestMapping(value = ServerOpenApi.SERVER_PROJECT_TRIGGER_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Object> trigger(@PathVariable String id, @PathVariable String token, String action) {
        ProjectInfoCacheModel item = projectInfoCacheService.getByKey(id);
        Assert.notNull(item, "没有对应数据");
        Assert.state(java.util.Objects.equals(token, item.getTriggerToken()), "触发token错误,或者已经失效");
        //
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, projectInfoCacheService.typeName());
        //
        Assert.notNull(userModel, "触发token错误,或者已经失效:-1");

        try {
            return this.execAction(item, action);
        } catch (Exception e) {
            log.error("触发自动执行服务器脚本异常", e);
            return new ApiResult<>(500, "执行异常：" + e.getMessage());
        }
    }


    /**
     * 构建触发器
     * <p>
     * 参数 <code>[
     * {
     * "id":"1",
     * "token":"a",
     * "action":"status"
     * }
     * ]</code>
     * <p>
     * 响应 <code>[
     * {
     * "id":"1",
     * "token":"a",
     * "code":"1",
     * "data":{},
     * "msg":"没有对应数据",
     * }
     * ]</code>
     *
     * @return json
     */
    @PostMapping(value = ServerOpenApi.SERVER_PROJECT_TRIGGER_BATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<JSONObject>> triggerBatch(HttpServletRequest request) {
        try {
            String body = JakartaServletUtil.getBody(request);
            JSONArray jsonArray = JSONArray.parseArray(body);
            List<JSONObject> collect = jsonArray.stream().map(o -> {
                JSONObject jsonObject = (JSONObject) o;
                String id = jsonObject.getString("id");
                String token = jsonObject.getString("token");
                String action = jsonObject.getString("action");
                ProjectInfoCacheModel item = projectInfoCacheService.getByKey(id);
                if (item == null) {
                    String value = "没有对应数据";
                    jsonObject.put("msg", value);
                    return jsonObject;
                }
                UserModel userModel = triggerTokenLogServer.getUserByToken(token, projectInfoCacheService.typeName());
                if (userModel == null) {
                    String value = "对应的用户不存在,触发器已失效";
                    jsonObject.put("msg", value);
                    return jsonObject;
                }
                //
                if (!java.util.Objects.equals(token, item.getTriggerToken())) {
                    String value = "触发token错误,或者已经失效";
                    jsonObject.put("msg", value);
                    return jsonObject;
                }
                ApiResult<Object> message = this.execAction(item, action);
                jsonObject.put("msg", message.getMsg());
                jsonObject.put("data", message.getData());
                jsonObject.put("code", message.getCode());
                return jsonObject;
            }).collect(Collectors.toList());
            return ApiResult.success("触发成功", collect);
        } catch (Exception e) {
            log.error("项目批量触发异常", e);
            return new ApiResult<>(500, "触发异常" + e.getMessage());
        }
    }
}
