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

package io.voyager1.service.system;

import io.voyager1.util.CollStreamUtil;
import io.voyager1.common.AgentConst;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.model.system.WorkspaceEnvVarModel;
import io.voyager1.service.BaseOperService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @since 2022/3/8 9:16
 **/
@Service
public class AgentWorkspaceEnvVarService extends BaseOperService<WorkspaceEnvVarModel> {

    public AgentWorkspaceEnvVarService() {
        super(AgentConst.WORKSPACE_ENV_VAR);
    }

    /**
     * 获取指定工作空间的环境变量
     *
     * @param workspaceId 工作空间
     * @return env
     */
    public EnvironmentMapBuilder getEnv(String workspaceId) {
        WorkspaceEnvVarModel item = this.getItem(workspaceId);
        Map<String, EnvironmentMapBuilder.Item> objectMap = Optional.ofNullable(item)
            .map(WorkspaceEnvVarModel::getVarData)
            .map(map -> CollStreamUtil.toMap(map.values(), WorkspaceEnvVarModel.WorkspaceEnvVarItemModel::getName, workspaceEnvVarItemModel -> {
                // 需要考虑兼容之前没有隐私变量字段，默认为隐私字段
                Integer privacy = workspaceEnvVarItemModel.getPrivacy();
                return new EnvironmentMapBuilder.Item(workspaceEnvVarItemModel.getValue(), privacy == null || privacy == 1, false);
            }))
            .orElse(new HashMap<>(1));
        return EnvironmentMapBuilder.builder(objectMap);
    }
}
