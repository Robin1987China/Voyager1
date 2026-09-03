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
import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerOpenApi;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.interceptor.NotLogin;
import io.voyager1.model.data.CommandModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.node.ssh.SshCommandService;
import io.voyager1.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ssh 脚本触发器
 *
 * @since 2022/7/25
 */
@RestController
@NotLogin
@Slf4j
public class SshCommandTriggerApiController extends BaseVoyager1Controller {

    private final SshCommandService sshCommandService;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public SshCommandTriggerApiController(SshCommandService sshCommandService,
                                          TriggerTokenLogServer triggerTokenLogServer) {
        this.sshCommandService = sshCommandService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * 执行脚本
     *
     * @param id    构建ID
     * @param token 构建的token
     * @return json
     */
    @RequestMapping(value = ServerOpenApi.SSH_COMMAND_TRIGGER_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> trigger2(@PathVariable String id, @PathVariable String token, HttpServletRequest request) {
        CommandModel item = sshCommandService.getByKey(id);
        Assert.notNull(item, "没有对应数据");
        Assert.state(java.util.Objects.equals(token, item.getTriggerToken()), "触发token错误,或者已经失效");
        //
        Assert.hasText(item.getSshIds(), "当前脚本未绑定 SSH 节点，不能使用触发器执行");
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, sshCommandService.typeName());
        //
        Assert.notNull(userModel, "触发token错误,或者已经失效:-1");
        // 解析参数
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        Map<String, String> newParamMap = new HashMap<>(10);
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            String key = String.format("trigger_%s", entry.getKey());
            key = StrUtil.toUnderlineCase(key);
            newParamMap.put(key, entry.getValue());
        }
        String batchId;
        try {
            BaseServerController.resetInfo(userModel);
            batchId = sshCommandService.executeBatch(item, item.getDefParams(), item.getSshIds(), 2, newParamMap);
        } catch (Exception e) {
            log.error("触发自动执行SSH命令模版异常", e);
            return new ApiResult<>(500, "执行异常：" + e.getMessage());
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("batchId", batchId);
        return ApiResult.success("开始执行", jsonObject);
    }


    /**
     * 构建触发器
     * <p>
     * 参数 <code>[
     * {
     * "id":"1",
     * "token":"a"
     * }
     * ]</code>
     * <p>
     * 响应 <code>[
     * {
     * "id":"1",
     * "token":"a",
     * "batchId":"1",
     * "msg":"没有对应数据",
     * }
     * ]</code>
     *
     * @return json
     */
    @PostMapping(value = ServerOpenApi.SSH_COMMAND_TRIGGER_BATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<Object>> triggerBatch(HttpServletRequest request) {
        try {
            String body = JakartaServletUtil.getBody(request);
            JSONArray jsonArray = JSONArray.parseArray(body);
            List<Object> collect = jsonArray.stream().peek(o -> {
                JSONObject jsonObject = (JSONObject) o;
                String id = jsonObject.getString("id");
                String token = jsonObject.getString("token");
                CommandModel item = sshCommandService.getByKey(id);
                if (item == null) {
                    String value = "没有对应数据";
                    jsonObject.put("msg", value);
                    return;
                }
                UserModel userModel = triggerTokenLogServer.getUserByToken(token, sshCommandService.typeName());
                if (userModel == null) {
                    String value = "对应的用户不存在,触发器已失效";
                    jsonObject.put("msg", value);
                    return;
                }
                //
                if (!java.util.Objects.equals(token, item.getTriggerToken())) {
                    String value = "触发token错误,或者已经失效";
                    jsonObject.put("msg", value);
                    return;
                }
                BaseServerController.resetInfo(userModel);
                String batchId = null;
                try {
                    batchId = sshCommandService.executeBatch(item, item.getDefParams(), item.getSshIds(), 2);
                } catch (Exception e) {
                    log.error("触发自动执行命令模版异常", e);
                    jsonObject.put("msg", "执行异常：" + e.getMessage());
                }
                jsonObject.put("batchId", batchId);
                //
            }).collect(Collectors.toList());
            return ApiResult.success("触发成功", collect);
        } catch (Exception e) {
            log.error("SSH 脚本批量触发异常", e);
            return new ApiResult<>(500, "触发异常" + e.getMessage());
        }
    }
}
