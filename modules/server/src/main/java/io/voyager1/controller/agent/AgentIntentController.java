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

import com.alibaba.fastjson2.JSONObject;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.agent.AgentIntentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent 意图解析 API（自然语言 → 工具调用序列）
 *
 * @since 2026/8/25
 */
@RestController
@RequestMapping(value = "/agent/intent")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE)
public class AgentIntentController extends BaseServerController {

    private final AgentIntentService agentIntentService;

    public AgentIntentController(AgentIntentService agentIntentService) {
        this.agentIntentService = agentIntentService;
    }

    @PostMapping(value = "parse", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<List<JSONObject>> parse(String intent) {
        return ApiResult.success("", agentIntentService.parseIntent(intent));
    }
}
