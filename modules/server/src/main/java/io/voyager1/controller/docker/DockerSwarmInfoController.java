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

package io.voyager1.controller.docker;

import io.voyager1.util.CollUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.controller.docker.base.BaseDockerSwarmInfoController;
import io.voyager1.func.assets.model.MachineDockerModel;
import io.voyager1.func.assets.server.MachineDockerServer;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.docker.DockerInfoModel;
import io.voyager1.model.docker.DockerSwarmInfoMode;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.docker.DockerInfoService;
import io.voyager1.service.docker.DockerSwarmInfoService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 2022/2/13
 */
@RestController
@Feature(cls = ClassFeature.DOCKER_SWARM)
@RequestMapping(value = "/docker/swarm")
@Slf4j
public class DockerSwarmInfoController extends BaseDockerSwarmInfoController {

    private final DockerSwarmInfoService dockerSwarmInfoService;
    private final MachineDockerServer machineDockerServer;
    private final DockerInfoService dockerInfoService;

    public DockerSwarmInfoController(DockerSwarmInfoService dockerSwarmInfoService,
                                     MachineDockerServer machineDockerServer,
                                     DockerInfoService dockerInfoService) {
        this.dockerSwarmInfoService = dockerSwarmInfoService;
        this.machineDockerServer = machineDockerServer;
        this.dockerInfoService = dockerInfoService;
    }

    /**
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<DockerSwarmInfoMode>> list(HttpServletRequest request) {
        // load list with page
        PageResultDto<DockerSwarmInfoMode> resultDto = dockerSwarmInfoService.listPage(request);
        resultDto.each(dockerSwarmInfoMode -> {
            String swarmId = dockerSwarmInfoMode.getSwarmId();
            MachineDockerModel machineDocker = machineDockerServer.tryMachineDockerBySwarmId(swarmId);
            dockerSwarmInfoMode.setMachineDocker(machineDocker);
        });
        return ApiResult.success("", resultDto);
    }

    /**
     * @return json
     */
    @GetMapping(value = "list-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<DockerSwarmInfoMode>> listAll(HttpServletRequest request) {
        // load list with all
        List<DockerSwarmInfoMode> swarmInfoModes = dockerSwarmInfoService.listByWorkspace(request);
        return ApiResult.success("", swarmInfoModes);
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Object> edit(@ValidatorItem String id,
                                    @ValidatorItem String name,
                                    @ValidatorItem String tag,
                                    HttpServletRequest request) throws Exception {
        String workspaceId = dockerSwarmInfoService.getCheckUserWorkspace(request);
        DockerSwarmInfoMode dockerSwarmInfoMode1 = dockerSwarmInfoService.getByKey(id, request);
        Assert.notNull(dockerSwarmInfoMode1, "对应的集群不存在");
        // 更新集群信息
        DockerSwarmInfoMode dockerSwarmInfoMode = new DockerSwarmInfoMode();
        dockerSwarmInfoMode.setId(id);
        dockerSwarmInfoMode.setName(name);
        dockerSwarmInfoMode.setTag(tag);
        dockerSwarmInfoService.updateById(dockerSwarmInfoMode);
        // 更新集群关联的 docker 工作空间的 tag
        MachineDockerModel dockerModel = new MachineDockerModel();
        dockerModel.setSwarmId(dockerSwarmInfoMode1.getSwarmId());
        List<MachineDockerModel> machineDockerModels = machineDockerServer.listByBean(dockerModel);
        Assert.notEmpty(machineDockerModels, "当前集群未找到 docker 信息");
        for (MachineDockerModel machineDockerModel : machineDockerModels) {
            DockerInfoModel queryWhere = new DockerInfoModel();
            queryWhere.setMachineDockerId(machineDockerModel.getId());
            queryWhere.setWorkspaceId(workspaceId);
            List<DockerInfoModel> dockerInfoModels = dockerInfoService.listByBean(queryWhere);
            for (DockerInfoModel dockerInfoModel : dockerInfoModels) {
                // 处理标签
                Collection<String> allTag = StrUtil.splitTrim(dockerInfoModel.getTags(), ":");
                allTag = (allTag != null ? allTag : new ArrayList<>());
                if (!allTag.contains(tag)) {
                    allTag.add(tag);
                }
                allTag = allTag.stream().filter(StrUtil::isNotEmpty).collect(Collectors.toSet());
                String newTags = ":" + allTag.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(":")) + ":";
                //
                DockerInfoModel update = new DockerInfoModel();
                update.setId(dockerInfoModel.getId());
                update.setTags(newTags);
                dockerInfoService.updateById(update);
            }
        }
        //
        return ApiResult.success("修改成功");
    }

    @GetMapping(value = "del", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> del(@ValidatorItem String id, HttpServletRequest request) throws Exception {
        dockerSwarmInfoService.delByKey(id, request);
        return ApiResult.success("删除成功");
    }


    @Override
    protected Map<String, Object> toDockerParameter(String id) {
        return machineDockerServer.dockerParameter(id);
    }
}
