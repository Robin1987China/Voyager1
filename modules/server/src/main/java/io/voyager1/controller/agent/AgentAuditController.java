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

import io.voyager1.common.BaseServerController;
import io.voyager1.core.api.ApiResult;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.AgentAuditModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.agent.AgentAuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent 执行审计 API（分页查询 / 按会话查轨迹）。
 *
 * @since 2026/10/27
 */
@RestController
@RequestMapping(value = "/agent/audit")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE)
public class AgentAuditController extends BaseServerController {

    private final AgentAuditService agentAuditService;

    public AgentAuditController(AgentAuditService agentAuditService) {
        this.agentAuditService = agentAuditService;
    }

    @PostMapping(value = "list", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<AgentAuditModel>> list(HttpServletRequest request) {
        return ApiResult.success("", agentAuditService.listPage(request));
    }

    @PostMapping(value = "session", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<AgentAuditModel>> session(String agentSessionId) {
        return ApiResult.success("", agentAuditService.listBySession(agentSessionId));
    }
}
