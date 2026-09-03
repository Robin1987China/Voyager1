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

package io.voyager1.controller;

import io.voyager1.util.ObjectUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.core.api.ApiResult;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.func.assets.server.MachineDockerServer;
import io.voyager1.func.assets.server.MachineNodeServer;
import io.voyager1.func.assets.server.MachineSshServer;
import io.voyager1.func.files.service.FileStorageService;
import io.voyager1.func.files.service.StaticFileStorageService;
import io.voyager1.func.system.service.ClusterInfoService;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.docker.DockerInfoService;
import io.voyager1.service.docker.DockerSwarmInfoService;
import io.voyager1.service.node.NodeService;
import io.voyager1.service.node.ProjectInfoCacheService;
import io.voyager1.service.node.script.NodeScriptServer;
import io.voyager1.service.node.ssh.SshCommandService;
import io.voyager1.service.node.ssh.SshService;
import io.voyager1.service.outgiving.OutGivingServer;
import io.voyager1.service.script.ScriptServer;
import io.voyager1.service.system.WorkspaceService;
import io.voyager1.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 24/1/4 004
 */
@RestController
@RequestMapping(value = "stat")
@Slf4j
public class DataStatController {

    private final NodeService nodeService;
    private final ProjectInfoCacheService projectInfoCacheService;
    private final NodeScriptServer nodeScriptServer;
    private final OutGivingServer outGivingServer;
    private final SshService sshService;
    private final SshCommandService sshCommandService;
    private final ScriptServer scriptServer;
    private final DockerInfoService dockerInfoService;
    private final FileStorageService fileStorageService;
    private final StaticFileStorageService staticFileStorageService;
    private final DockerSwarmInfoService dockerSwarmInfoService;
    private final UserService userService;
    private final WorkspaceService workspaceService;
    private final ClusterInfoService clusterInfoService;
    private final MachineNodeServer machineNodeServer;
    private final MachineSshServer machineSshServer;
    private final MachineDockerServer machineDockerServer;

    public DataStatController(NodeService nodeService,
                              ProjectInfoCacheService projectInfoCacheService,
                              NodeScriptServer nodeScriptServer,
                              OutGivingServer outGivingServer,
                              SshService sshService,
                              SshCommandService sshCommandService,
                              ScriptServer scriptServer,
                              DockerInfoService dockerInfoService,
                              FileStorageService fileStorageService,
                              StaticFileStorageService staticFileStorageService,
                              DockerSwarmInfoService dockerSwarmInfoService,
                              UserService userService,
                              WorkspaceService workspaceService,
                              ClusterInfoService clusterInfoService,
                              MachineNodeServer machineNodeServer,
                              MachineSshServer machineSshServer,
                              MachineDockerServer machineDockerServer) {
        this.nodeService = nodeService;
        this.projectInfoCacheService = projectInfoCacheService;
        this.nodeScriptServer = nodeScriptServer;
        this.outGivingServer = outGivingServer;
        this.sshService = sshService;
        this.sshCommandService = sshCommandService;
        this.scriptServer = scriptServer;
        this.dockerInfoService = dockerInfoService;
        this.fileStorageService = fileStorageService;
        this.staticFileStorageService = staticFileStorageService;
        this.dockerSwarmInfoService = dockerSwarmInfoService;
        this.userService = userService;
        this.workspaceService = workspaceService;
        this.clusterInfoService = clusterInfoService;
        this.machineNodeServer = machineNodeServer;
        this.machineSshServer = machineSshServer;
        this.machineDockerServer = machineDockerServer;
    }

    @RequestMapping(value = "workspace", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Map<String, Number>> workspace(HttpServletRequest request) {
        String workspaceId = nodeService.getCheckUserWorkspace(request);
        Entity entity = Entity.create();
        entity.set("workspaceId", workspaceId);
        Map<String, Number> map = new HashMap<>(10);
        map.put("nodeCount", nodeService.count(entity));
        map.put("projectCount", projectInfoCacheService.count(entity));
        map.put("nodeScriptCount", nodeScriptServer.count(entity));
        map.put("outGivingCount", outGivingServer.count(entity));
        map.put("sshCount", sshService.count(entity));
        map.put("sshCommandCount", sshCommandService.count(entity));
        map.put("scriptCount", scriptServer.count(entity));
        map.put("dockerCount", dockerInfoService.count(entity));
        map.put("dockerSwarmCount", dockerSwarmInfoService.count(entity));
        map.put("fileCount", fileStorageService.count(entity));
        //map.put("staticFileCount", staticFileStorageService.count(entity));
        return ApiResult.success("", map);
    }

    @RequestMapping(value = "system", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @SystemPermission
    public ApiResult<Map<String, Object>> system(HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>(10);
        {
            long count = userService.count();
            map.put("userCount", count);
            {
                UserModel userModel = new UserModel();
                userModel.setSystemUser(1);
                long systemUserCount = userService.count(userModel);
                map.put("systemUserCount", systemUserCount);
            }
            {
                UserModel userModel = new UserModel();
                userModel.setStatus(0);
                long disableUserCount = userService.count(userModel);
                map.put("disableUserCount", disableUserCount);
            }
        }
        {
            long workspaceCount = workspaceService.count();
            long clusterCount = clusterInfoService.count();
            map.put("workspaceCount", workspaceCount);
            map.put("clusterCount", clusterCount);
        }
        {
            String sql = "select status,count(1) as count from " + machineDockerServer.getTableName() + " group by status";
            List<Entity> query = machineDockerServer.query(sql);
            map.put("dockerStat", query);
        }
        {
            String sql = "select status,count(1) as count from " + machineSshServer.getTableName() + " group by status";
            List<Entity> query = machineSshServer.query(sql);
            map.put("sshStat", query);
        }
        {
            String sql = "select status,count(1) as count from " + machineNodeServer.getTableName() + " group by status";
            List<Entity> query = machineNodeServer.query(sql);
            map.put("nodeStat", query);
        }
        return ApiResult.success("", map);
    }
}
