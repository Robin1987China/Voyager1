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

package io.voyager1.controller.system;

import io.voyager1.util.MapUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseAgentController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.model.system.WorkspaceEnvVarModel;
import io.voyager1.service.system.AgentWorkspaceEnvVarService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @since 2022/3/8
 */
@RestController
@RequestMapping(value = "/system/workspace_env")
public class AgentWorkspaceEnvVarController extends BaseAgentController {

    private final AgentWorkspaceEnvVarService agentWorkspaceEnvVarService;

    public AgentWorkspaceEnvVarController(AgentWorkspaceEnvVarService agentWorkspaceEnvVarService) {
        this.agentWorkspaceEnvVarService = agentWorkspaceEnvVarService;
    }

    /**
     * 更新环境变量
     *
     * @param name        名称
     * @param value       值
     * @param description 描述
     * @return json
     */
    @PostMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Object> updateWorkspaceEnvVar(@ValidatorItem(msg = "请填写名称") String name,
                                                      @ValidatorItem(msg = "请填写值") String value,
                                                      @ValidatorItem(msg = "请填写描述") String description,
                                                      Integer privacy) {
        String workspaceId = getWorkspaceId();
        synchronized (AgentWorkspaceEnvVarController.class) {
            WorkspaceEnvVarModel.WorkspaceEnvVarItemModel workspaceEnvVarModel = new WorkspaceEnvVarModel.WorkspaceEnvVarItemModel();
            workspaceEnvVarModel.setName(name);
            workspaceEnvVarModel.setValue(value);
            workspaceEnvVarModel.setDescription(description);
            workspaceEnvVarModel.setPrivacy(privacy);
            //
            WorkspaceEnvVarModel item = agentWorkspaceEnvVarService.getItem(workspaceId);
            if (null == item) {
                item = new WorkspaceEnvVarModel();
                item.setVarData(java.util.Map.of(name, workspaceEnvVarModel));
                item.setName(workspaceId);
                item.setId(workspaceId);
                agentWorkspaceEnvVarService.addItem(item);
            } else {
                item.put(name, workspaceEnvVarModel);
                agentWorkspaceEnvVarService.updateItem(item);
            }
        }
        return ApiResult.success("更新成功");
    }


    /**
     * 删除环境变量
     *
     * @param name 名称
     * @return json
     */
    @PostMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Object> delete(@ValidatorItem String name) {
        String workspaceId = getWorkspaceId();
        synchronized (AgentWorkspaceEnvVarController.class) {
            //
            WorkspaceEnvVarModel item = agentWorkspaceEnvVarService.getItem(workspaceId);
            if (null != item) {
                item.remove(name);
                agentWorkspaceEnvVarService.updateItem(item);
            }
        }
        return ApiResult.success("删除成功");
    }

}
