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

package io.voyager1.func.assets.controller.docker;

import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.controller.docker.base.BaseDockerSwarmServiceController;
import io.voyager1.func.assets.model.MachineDockerModel;
import io.voyager1.func.assets.server.MachineDockerServer;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.system.ServerConfig;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @since 2022/2/14
 */
@RestController
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE_DOCKER)
@RequestMapping(value = "/system/assets/docker/swarm-service")
@Slf4j
public class AssetsDockerSwarmServiceController extends BaseDockerSwarmServiceController {

    private final MachineDockerServer machineDockerServer;

    public AssetsDockerSwarmServiceController(ServerConfig serverConfig,
                                              MachineDockerServer machineDockerServer) {
        super(serverConfig);
        this.machineDockerServer = machineDockerServer;
    }

    @Override
    protected Map<String, Object> toDockerParameter(String id) {
        MachineDockerModel machineDockerModel = machineDockerServer.getByKey(id, false);
        Assert.notNull(machineDockerModel, "没有对应的 docker 信息");
        if (machineDockerModel.isControlAvailable()) {
            // 管理节点
            return machineDockerServer.toParameter(machineDockerModel);
        }
        // 非管理节点
        MachineDockerModel bySwarmId = machineDockerServer.getMachineDockerBySwarmId(machineDockerModel.getSwarmId());
        return machineDockerServer.toParameter(bySwarmId);
    }
}
