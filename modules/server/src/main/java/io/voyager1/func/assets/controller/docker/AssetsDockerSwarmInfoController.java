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

import io.voyager1.core.api.ApiResult;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.controller.docker.base.BaseDockerSwarmInfoController;
import io.voyager1.func.assets.model.MachineDockerModel;
import io.voyager1.func.assets.server.MachineDockerServer;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @since 2022/2/13
 */
@RestController
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE_DOCKER)
@RequestMapping(value = "/system/assets/docker/swarm")
@Slf4j
public class AssetsDockerSwarmInfoController extends BaseDockerSwarmInfoController {

    private final MachineDockerServer machineDockerServer;

    public AssetsDockerSwarmInfoController(MachineDockerServer machineDockerServer) {
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

    /**
     * @return json
     */
    @GetMapping(value = "list-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<MachineDockerModel>> listAll() {
        MachineDockerModel machineDockerModel = new MachineDockerModel();
        machineDockerModel.setSwarmControlAvailable(true);
        // load list with all
        List<MachineDockerModel> swarmInfoModes = machineDockerServer.listByBean(machineDockerModel);
        return ApiResult.success("", swarmInfoModes);
    }
}
