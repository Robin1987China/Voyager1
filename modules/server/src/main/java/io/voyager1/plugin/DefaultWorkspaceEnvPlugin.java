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

package io.voyager1.plugin;

import io.voyager1.common.SpringContextHolder;
import io.voyager1.plugin.PluginConfig;
import io.voyager1.common.Const;
import io.voyager1.service.system.WorkspaceEnvVarService;

import java.util.Map;


@PluginConfig(name = IWorkspaceEnvPlugin.PLUGIN_NAME)
public class DefaultWorkspaceEnvPlugin implements IWorkspaceEnvPlugin {

    @Override
    public Object execute(Object main, Map<String, Object> parameter) throws Exception {
        WorkspaceEnvVarService workspaceEnvVarService = SpringContextHolder.getBean(WorkspaceEnvVarService.class);
        String workspaceId = (String) parameter.get(Const.WORKSPACE_ID_REQ_HEADER);
        String value = (String) parameter.get("value");
        return workspaceEnvVarService.convertRefEnvValue(workspaceId, value);
    }
}
