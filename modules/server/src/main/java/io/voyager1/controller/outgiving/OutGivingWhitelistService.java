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

package io.voyager1.controller.outgiving;

import io.voyager1.model.data.ServerWhitelist;
import io.voyager1.service.node.NodeService;
import io.voyager1.service.system.SystemParametersServer;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @since 2022/1/23
 */
@Service
public class OutGivingWhitelistService {

    private final SystemParametersServer systemParametersServer;
    private final NodeService nodeService;

    public OutGivingWhitelistService(SystemParametersServer systemParametersServer,
                                     NodeService nodeService) {
        this.systemParametersServer = systemParametersServer;
        this.nodeService = nodeService;
    }


    public ServerWhitelist getServerWhitelistData(HttpServletRequest request) {
        String workspaceId = nodeService.getCheckUserWorkspace(request);
        return this.getServerWhitelistData(workspaceId);
    }

    public ServerWhitelist getServerWhitelistData(String workspaceId) {
        String id = ServerWhitelist.workspaceId(workspaceId);
        ServerWhitelist serverWhitelist = systemParametersServer.getConfigDefNewInstance(id, ServerWhitelist.class);
        if (serverWhitelist == null) {
            // 兼容旧数据
            serverWhitelist = systemParametersServer.getConfigDefNewInstance(ServerWhitelist.ID, ServerWhitelist.class);
        }
        return serverWhitelist;
    }
}
