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
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseVoyager1Controller;
import io.voyager1.common.ServerOpenApi;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.interceptor.NotLogin;
import io.voyager1.model.data.WorkspaceEnvVarModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.system.WorkspaceEnvVarService;
import io.voyager1.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @since 23/12/19 019
 */
@RestController
@NotLogin
@Slf4j
public class WorkspaceEnvVarApiController extends BaseVoyager1Controller {

    private final WorkspaceEnvVarService workspaceEnvVarService;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public WorkspaceEnvVarApiController(WorkspaceEnvVarService workspaceEnvVarService,
                                        TriggerTokenLogServer triggerTokenLogServer) {
        this.workspaceEnvVarService = workspaceEnvVarService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * 参数获取并验证变量
     *
     * @param id       变量id
     * @param token    token
     * @param response 响应
     * @return data
     */
    private WorkspaceEnvVarModel get(String id, String token, HttpServletResponse response) {
        WorkspaceEnvVarModel item = workspaceEnvVarService.getByKey(id);
        if (item == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            JakartaServletUtil.write(response, "没有对应数据", MediaType.TEXT_PLAIN_VALUE);
            return null;
        }
        if (!java.util.Objects.equals(token, item.getTriggerToken())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            JakartaServletUtil.write(response, "触发token错误,或者已经失效", MediaType.TEXT_PLAIN_VALUE);
            return null;
        }
        //
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, workspaceEnvVarService.typeName());
        if (userModel == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            JakartaServletUtil.write(response, "触发token错误,或者已经失效:-1", MediaType.TEXT_PLAIN_VALUE);
            return null;
        }
        Integer privacy = item.getPrivacy();
        if (privacy == null || privacy != 0) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            JakartaServletUtil.write(response, "非明文变量不能查看", MediaType.TEXT_PLAIN_VALUE);
            return null;
        }
        return item;
    }


    /**
     * 获取变量值
     *
     * @param id    变量ID
     * @param token 变量的token
     */
    @GetMapping(value = ServerOpenApi.SERVER_ENV_VAR_TRIGGER_URL, produces = MediaType.TEXT_PLAIN_VALUE)
    public void trigger(@PathVariable String id, @PathVariable String token, HttpServletResponse response) {
        WorkspaceEnvVarModel item = this.get(id, token, response);
        if (item != null) {
            JakartaServletUtil.write(response, item.getValue(), MediaType.TEXT_PLAIN_VALUE);
        }
    }

    /**
     * 修改变量值
     *
     * @param id    变量ID
     * @param token 变量的token
     */
    @PostMapping(value = ServerOpenApi.SERVER_ENV_VAR_TRIGGER_URL, produces = MediaType.TEXT_PLAIN_VALUE)
    public void trigger(@PathVariable String id, @PathVariable String token, String value, HttpServletResponse response, HttpServletRequest request) {
        this.update(id, token, value, response);
    }

    /**
     * 修改变量值
     *
     * @param id    变量ID
     * @param token 变量的token
     */
    @PutMapping(value = ServerOpenApi.SERVER_ENV_VAR_TRIGGER_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public void triggerPut(@PathVariable String id, @PathVariable String token, HttpServletResponse response, HttpServletRequest request) {
        String value = JakartaServletUtil.getBody(request);
        this.update(id, token, value, response);
    }

    /**
     * 修改变量操作
     *
     * @param id       变量id
     * @param token    变量token
     * @param value    变量值
     * @param response 响应
     */
    private void update(String id, String token, String value, HttpServletResponse response) {
        if ((value == null || value.isEmpty())) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JakartaServletUtil.write(response, "修改的值为空", MediaType.TEXT_PLAIN_VALUE);
            return;
        }
        WorkspaceEnvVarModel item = this.get(id, token, response);
        if (item != null) {
            WorkspaceEnvVarModel update = new WorkspaceEnvVarModel();
            update.setId(item.getId());
            update.setValue(value);
            workspaceEnvVarService.updateById(update);
            JakartaServletUtil.write(response, "success", MediaType.TEXT_PLAIN_VALUE);
        }
    }
}
