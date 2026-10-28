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

package io.voyager1.controller.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.core.api.ApiResult;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.agent.SelfHealService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * AIOps 自愈 API：执行自愈动作 / 人工中止 / 熔断复位。
 *
 * @since 2026/10/27
 */
@RestController
@RequestMapping(value = "/agent/self-heal")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE)
public class AgentSelfHealController extends BaseServerController {

    private final SelfHealService selfHealService;

    public AgentSelfHealController(SelfHealService selfHealService) {
        this.selfHealService = selfHealService;
    }

    /**
     * 执行自愈动作（危险工具会停在待审批）。
     *
     * @param arguments 工具入参 JSON 字符串，如 {"nodeId":"n1","command":"..."}
     */
    @PostMapping(value = "execute", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<JSONObject> execute(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "请提供告警类型") String alertType,
                                         String target,
                                         String agentSessionId,
                                         String arguments) {
        String sessionId = (agentSessionId == null || agentSessionId.isEmpty())
            ? UUID.randomUUID().toString() : agentSessionId;
        JSONObject args = (arguments == null || arguments.trim().isEmpty())
            ? new JSONObject() : JSON.parseObject(arguments);
        return ApiResult.success("", selfHealService.execute(alertType, target, sessionId, args));
    }

    /**
     * 人工中止某会话的自愈。
     */
    @PostMapping(value = "abort", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> abort(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "请提供会话 ID") String agentSessionId) {
        selfHealService.abort(agentSessionId);
        return ApiResult.success("已中止");
    }

    /**
     * 复位熔断（运维手动恢复自愈）。
     */
    @PostMapping(value = "reset", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> reset(String alertType, String target) {
        selfHealService.resetCircuit(alertType, target);
        return ApiResult.success("已复位");
    }
}
