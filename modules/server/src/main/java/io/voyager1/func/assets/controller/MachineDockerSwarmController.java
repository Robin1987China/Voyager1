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

package io.voyager1.func.assets.controller;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.ArrayUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.plugin.IPlugin;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.func.assets.model.MachineDockerModel;
import io.voyager1.func.assets.server.MachineDockerServer;
import io.voyager1.model.docker.DockerSwarmInfoMode;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.docker.DockerInfoService;
import io.voyager1.service.docker.DockerSwarmInfoService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @since 2023/3/4
 */
@RestController
@RequestMapping(value = "/system/assets/docker")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE_DOCKER)
@SystemPermission
@Slf4j
public class MachineDockerSwarmController extends BaseServerController {

    private final MachineDockerServer machineDockerServer;
    private final DockerSwarmInfoService dockerSwarmInfoService;

    public MachineDockerSwarmController(MachineDockerServer machineDockerServer,
                                        DockerSwarmInfoService dockerSwarmInfoService) {
        this.machineDockerServer = machineDockerServer;
        this.dockerSwarmInfoService = dockerSwarmInfoService;
    }

    @PostMapping(value = "init", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Object> init(@ValidatorItem String id) throws Exception {
        MachineDockerModel dockerInfoModel1 = machineDockerServer.getByKey(id, false);
        Assert.notNull(dockerInfoModel1, "对应的 docker 不存在");
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = machineDockerServer.toParameter(dockerInfoModel1);
        JSONObject data = (JSONObject) plugin.execute("tryInitializeSwarm", parameter);

        //
        IPlugin pluginCheck = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        JSONObject info = pluginCheck.execute("info", parameter, JSONObject.class);
        machineDockerServer.updateSwarmInfo(id, data, info);
        //
        return ApiResult.success("集群创建成功");
    }

    /**
     * 加入集群
     *
     * @param id         docker ID
     * @param managerId  管理节点
     * @param remoteAddr 集群ID
     * @param role       加入角色
     * @return json
     * @throws Exception 异常
     */
    @PostMapping(value = "join", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Object> join(@ValidatorItem String managerId,
                                     @ValidatorItem String id,
                                     @ValidatorItem String remoteAddr,
                                     @ValidatorItem String role) throws Exception {
        MachineDockerModel managerSwarmDocker = machineDockerServer.getByKey(managerId, false);
        Assert.notNull(managerSwarmDocker, "对应的 docker 不存在");
        MachineDockerModel joinSwarmDocker = machineDockerServer.getByKey(id, false);
        Assert.notNull(joinSwarmDocker, "对应的 docker 不存在:-1");
        JSONObject managerSwarmInfo;
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        IPlugin checkPlugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        {
            // 获取集群信息
            managerSwarmInfo = (JSONObject) plugin.execute("inSpectSwarm", machineDockerServer.toParameter(managerSwarmDocker));
            Assert.notNull(managerSwarmInfo, "集群信息不完整,不能加入该集群");
        }
        {
            // 检查节点存在的信息
            JSONObject info = checkPlugin.execute("info", machineDockerServer.toParameter(joinSwarmDocker), JSONObject.class);
            Assert.notNull(info, "获取 docker 集群信息失败");
            JSONObject swarm = info.getJSONObject("swarm");
            Assert.notNull(swarm, "获取 docker 集群信息失败:-1");
            JSONArray remoteManagers = swarm.getJSONArray("remoteManagers");
            if ((remoteManagers != null && !remoteManagers.isEmpty())) {
                Optional<Object> optional = remoteManagers.stream().filter(o -> {
                    JSONObject jsonObject = (JSONObject) o;
                    String addr = jsonObject.getString("addr");
                    List<String> strings = io.voyager1.util.ConvertUtil.splitTrim(addr, ":");
                    return java.util.Objects.equals((strings == null || strings.isEmpty() ? null : strings.get(0)), remoteAddr);
                }).findFirst();
                Assert.state(optional.isPresent(), "当前 docker 已经加入到其他集群啦");
                // 绑定数据
                machineDockerServer.updateSwarmInfo(joinSwarmDocker.getId(), managerSwarmInfo, info);
                return ApiResult.success("集群绑定成功");
            }
        }
        String roleToken;
        {
            // 准备加入集群
            JSONObject joinTokens = managerSwarmInfo.getJSONObject("joinTokens");
            Assert.notNull(joinTokens, "集群信息不完整,不能加入该集群:-1");
            roleToken = joinTokens.getString(role);
            Assert.hasText(roleToken, String.format("不能以 %s 角色加入集群", role));
        }
        Map<String, Object> parameter = machineDockerServer.toParameter(joinSwarmDocker);
        parameter.put("token", roleToken);
        parameter.put("remoteAddrs", remoteAddr);
        plugin.execute("joinSwarm", parameter);
        // 绑定数据
        JSONObject info = checkPlugin.execute("info", machineDockerServer.toParameter(joinSwarmDocker), JSONObject.class);
        machineDockerServer.updateSwarmInfo(joinSwarmDocker.getId(), managerSwarmInfo, info);
        return ApiResult.success("集群创建成功");
    }

    /**
     * 强制退出集群
     *
     * @param id 集群ID
     * @return json
     */
    @GetMapping(value = "leave-force", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> leaveForce(@ValidatorItem String id) throws Exception {
        //
        MachineDockerModel dockerInfoModel = machineDockerServer.getByKey(id, false);
        Assert.notNull(dockerInfoModel, "不存在对应的 docker");
        Assert.hasText(dockerInfoModel.getSwarmId(), "当前 docker 没有集群信息");
        {
            MachineDockerModel where = new MachineDockerModel();
            where.setSwarmId(dockerInfoModel.getSwarmId());
            long allCount = machineDockerServer.count(where);

            where.setSwarmControlAvailable(true);
            long controlCount = machineDockerServer.count(where);
            if (controlCount == 1 && allCount > 1 && dockerInfoModel.isControlAvailable()) {
                // 如果只有一个管理节点，并且还有多个节点, 并且当前操作的是管理节点
                throw new IllegalArgumentException("还存在多个工作节点,不能退出最后一个管理节点");
            }
        }
        //
        DockerSwarmInfoMode dockerSwarmInfoMode = new DockerSwarmInfoMode();
        dockerSwarmInfoMode.setSwarmId(dockerInfoModel.getSwarmId());
        long count = dockerSwarmInfoService.count(dockerSwarmInfoMode);
        Assert.state(count <= 0, String.format("当前 docker 集群还关联 %s 个工作空间集群, 不能退出集群", count));
        //
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = machineDockerServer.toParameter(dockerInfoModel);
        parameter.put("force", true);
        plugin.execute("leaveSwarm", parameter, JSONObject.class);
        //
        MachineDockerModel update = new MachineDockerModel();
        update.setId(dockerInfoModel.getId());
        update.restSwarm();
        machineDockerServer.updateById(update);
        return new ApiResult<>(200, "强制解绑成功");
    }

    /**
     * 退出集群
     *
     * @param id 集群ID
     * @return json
     */
    @GetMapping(value = "leave-node", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> leave(@ValidatorItem String id, @ValidatorItem String nodeId) throws Exception {
        MachineDockerModel dockerModel = machineDockerServer.getByKey(id, false);
        Assert.notNull(dockerModel, "没有对应的 docker");
        // 找到对应 docker 集群 node id 关联的数据
        MachineDockerModel find = new MachineDockerModel();
        find.setSwarmId(dockerModel.getSwarmId());
        find.setSwarmNodeId(nodeId);
        List<MachineDockerModel> machineDockerModels1 = machineDockerServer.listByBean(find, false);
        Assert.notEmpty(machineDockerModels1, "当前集群对应的节点，不在本系统中无法退出集群");
        MachineDockerModel first = (machineDockerModels1 == null || machineDockerModels1.isEmpty() ? null : machineDockerModels1.get(0));
        // 找到管理节点
        MachineDockerModel machineDockerModel = new MachineDockerModel();
        machineDockerModel.setSwarmId(dockerModel.getSwarmId());
        machineDockerModel.setSwarmControlAvailable(true);
        List<MachineDockerModel> machineDockerModels = machineDockerServer.listByBean(machineDockerModel, false);
        Assert.notEmpty(machineDockerModels, "当前集群未找到管理节点");
        Optional<MachineDockerModel> dockerModelOptional = machineDockerModels.stream()
            .filter(machineDockerModel1 -> machineDockerModel1.getStatus() != null && machineDockerModel1.getStatus() == 1)
            .findFirst();
        Assert.state(dockerModelOptional.isPresent(), "当前集群未找到在线的管理节点");
        //
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        //
        {
            //节点强制退出
            Map<String, Object> parameter = machineDockerServer.toParameter(first);
            parameter.put("force", true);
            plugin.execute("leaveSwarm", parameter, JSONObject.class);
        }
        {
            // 集群删除节点
            Map<String, Object> map = machineDockerServer.toParameter(dockerModelOptional.get());
            map.put("nodeId", nodeId);
            plugin.execute("removeSwarmNode", map);
        }
        //
        MachineDockerModel update = new MachineDockerModel();
        update.setId(first.getId());
        update.restSwarm();
        machineDockerServer.updateById(update);
        return new ApiResult<>(200, "剔除成功");
    }
}
