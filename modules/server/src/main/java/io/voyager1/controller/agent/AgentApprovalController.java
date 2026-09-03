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

import io.voyager1.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.model.data.AgentApprovalModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.agent.AgentApprovalService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent 审批 API（危险工具的批准/拒绝/列表）
 *
 * @since 2026/8/25
 */
@RestController
@RequestMapping(value = "/agent/approval")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE)
public class AgentApprovalController extends BaseServerController {

    private final AgentApprovalService agentApprovalService;

    public AgentApprovalController(AgentApprovalService agentApprovalService) {
        this.agentApprovalService = agentApprovalService;
    }

    @PostMapping(value = "list", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<AgentApprovalModel>> list() {
        return ApiResult.success("", agentApprovalService.listPending());
    }

    @PostMapping(value = "approve", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<JSONObject> approve(String id) {
        return ApiResult.success("已批准并执行", agentApprovalService.approve(id, this.operator()));
    }

    @PostMapping(value = "reject", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> reject(String id, String remark) {
        agentApprovalService.reject(id, this.operator(), remark);
        return ApiResult.success("已拒绝");
    }

    private String operator() {
        UserModel user = this.getUserModel();
        return user == null ? "system" : ((user.getName() != null && !user.getName().isEmpty()) ? user.getName() : user.getId());
    }
}
