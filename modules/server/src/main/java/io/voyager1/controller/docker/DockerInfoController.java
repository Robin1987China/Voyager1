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

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.plugin.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.func.assets.model.MachineDockerModel;
import io.voyager1.func.assets.server.MachineDockerServer;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.docker.DockerInfoModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.docker.DockerInfoService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @since 2022/1/26
 */
@RestController
@Feature(cls = ClassFeature.DOCKER)
@RequestMapping(value = "/docker")
@Slf4j
public class DockerInfoController extends BaseServerController {

    private final DockerInfoService dockerInfoService;
    private final MachineDockerServer machineDockerServer;

    public DockerInfoController(DockerInfoService dockerInfoService,
                                MachineDockerServer machineDockerServer) {
        this.dockerInfoService = dockerInfoService;
        this.machineDockerServer = machineDockerServer;
    }

    /**
     * @return json
     */
    @GetMapping(value = "api-versions", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<JSONObject>> apiVersions() throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        List<JSONObject> data = (List<JSONObject>) plugin.execute("apiVersions");
        return ApiResult.success("", data);
    }

    /**
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<DockerInfoModel>> list(HttpServletRequest request) {
        // load list with page
        PageResultDto<DockerInfoModel> resultDto = dockerInfoService.listPage(request);
        resultDto.each(dockerInfoModel -> {
            MachineDockerModel machineDockerModel = machineDockerServer.getByKey(dockerInfoModel.getMachineDockerId());
            if (machineDockerModel != null) {
                machineDockerModel.setRegistryPassword(null);
            }
            dockerInfoModel.setMachineDocker(machineDockerModel);
        });
        return ApiResult.success("", resultDto);
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Object> edit(@ValidatorItem String id, @ValidatorItem String name, String tags, HttpServletRequest request) throws Exception {
        DockerInfoModel dockerInfoModel = new DockerInfoModel();
        dockerInfoModel.setId(id);
        dockerInfoModel.setName(name);
        Assert.state(!(tags != null && tags.contains(":")), "标签不能包含 ：");
        List<String> tagList = io.voyager1.util.ConvertUtil.splitTrim(tags, ",");
        String newTags = ":" + tagList.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(":")) + ":";
        dockerInfoModel.setTags(newTags);
        dockerInfoService.updateById(dockerInfoModel, request);
        //
        return ApiResult.success("操作成功");
    }


    @GetMapping(value = "del", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> del(@ValidatorItem String id, HttpServletRequest request) throws Exception {
        dockerInfoService.delByKey(id, request);
        return ApiResult.success("删除成功");
    }


    /**
     * 同步到指定工作空间
     *
     * @param ids           节点ID
     * @param toWorkspaceId 分配到到工作空间ID
     * @return msg
     */
    @GetMapping(value = "sync-to-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission()
    public ApiResult<String> syncToWorkspace(@ValidatorItem String ids, @ValidatorItem String toWorkspaceId, HttpServletRequest request) {
        String nowWorkspaceId = dockerInfoService.getCheckUserWorkspace(request);
        //
        dockerInfoService.checkUserWorkspace(toWorkspaceId);
        dockerInfoService.syncToWorkspace(ids, nowWorkspaceId, toWorkspaceId);
        return ApiResult.success("操作成功");
    }

    /**
     * 查询所有的 tag
     *
     * @return msg
     */
    @GetMapping(value = "all-tag", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<String>> allTag(HttpServletRequest request) {
        String workspaceId = dockerInfoService.getCheckUserWorkspace(request);
        //
        List<String> strings = dockerInfoService.allTag(workspaceId);
        return ApiResult.success("", strings);
    }
}
