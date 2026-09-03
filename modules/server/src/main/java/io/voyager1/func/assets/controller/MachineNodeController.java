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
import org.springframework.data.domain.Sort;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.func.BaseGroupNameController;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.func.assets.server.MachineNodeServer;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.data.WorkspaceModel;
import io.voyager1.model.node.NodeScriptCacheModel;
import io.voyager1.model.node.ProjectInfoCacheModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.node.NodeService;
import io.voyager1.service.node.ProjectInfoCacheService;
import io.voyager1.service.node.script.NodeScriptServer;
import io.voyager1.service.system.WorkspaceService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 机器节点
 *
 * @since 2023/2/18
 */
@RestController
@RequestMapping(value = "/system/assets/machine")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE)
@SystemPermission
public class MachineNodeController extends BaseGroupNameController {

    private final WorkspaceService workspaceService;
    private final ProjectInfoCacheService projectInfoCacheService;
    private final NodeScriptServer nodeScriptServer;
    private final NodeService nodeService;

    public MachineNodeController(WorkspaceService workspaceService,
                                 MachineNodeServer machineNodeServer,
                                 ProjectInfoCacheService projectInfoCacheService,
                                 NodeScriptServer nodeScriptServer,
                                 NodeService nodeService) {
        super(machineNodeServer);
        this.workspaceService = workspaceService;
        this.projectInfoCacheService = projectInfoCacheService;
        this.nodeScriptServer = nodeScriptServer;
        this.nodeService = nodeService;
    }

    @PostMapping(value = "list-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<MachineNodeModel>> listJson(HttpServletRequest request) {
        PageResultDto<MachineNodeModel> pageResultDto = machineNodeServer.listPage(request);
        return ApiResult.success("", pageResultDto);
    }

    @GetMapping(value = "search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<MachineNodeModel>> search(String name, String appendIds, int limit) {
        Entity entity = new Entity();
        if ((name != null && !name.isEmpty())) {
            entity.set("name", String.format(" like '%%s%'", name));
        }
        limit = Math.max(limit, 1);
        List<String> appendIdList = io.voyager1.util.ConvertUtil.splitTrim(appendIds, ",");
        List<MachineNodeModel> machineNodeModels = machineNodeServer.queryList(entity, limit, machineNodeServer.defaultSort());
        appendIdList = appendIdList.stream()
            .filter(s -> machineNodeModels.stream()
                .noneMatch(machineNodeModel -> java.util.Objects.equals(s, machineNodeModel.getId())))
            .collect(Collectors.toList());
        for (String s : appendIdList) {
            MachineNodeModel nodeModel = machineNodeServer.getByKey(s);
            if (nodeModel == null) {
                continue;
            }
            machineNodeModels.add(nodeModel);
        }
        return ApiResult.success("", machineNodeModels);
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> save(HttpServletRequest request) {
        machineNodeServer.update(request);
        return ApiResult.success("操作成功");
    }

    @PostMapping(value = "delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> delete(@ValidatorItem String id) {
        long count = nodeService.countByMachine(id);
        Assert.state(count <= 0, String.format("当前机器还关联%s个节点，不能直接删除（需要提前解绑或者删除关联数据后才能删除）", count));
        machineNodeServer.delByKey(id);
        return ApiResult.success("操作成功");
    }

    /**
     * 将机器分配到指定工作空间
     *
     * @param ids         机器id
     * @param workspaceId 工作空间id
     * @return json
     */
    @PostMapping(value = "distribute", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> distribute(@ValidatorItem String ids, @ValidatorItem String workspaceId) {
        List<String> list = io.voyager1.util.ConvertUtil.splitTrim(ids, ",");
        for (String id : list) {
            MachineNodeModel machineNodeModel = machineNodeServer.getByKey(id);
            Assert.notNull(machineNodeModel, "没有对应的机器");
            WorkspaceModel workspaceModel = new WorkspaceModel(workspaceId);
            boolean exists = workspaceService.exists(workspaceModel);
            Assert.state(exists, "不存在对应的工作空间");
            //
            if (!nodeService.existsNode2(workspaceId, id)) {
                //
                machineNodeServer.insertNode(machineNodeModel, workspaceId);
            }
        }

        return ApiResult.success("操作成功");
    }

    @GetMapping(value = "list-node", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<NodeModel>> listData(@ValidatorItem String id) {
        MachineNodeModel machineNodeModel = machineNodeServer.getByKey(id);
        Assert.notNull(machineNodeModel, "没有对应的机器");
        NodeModel nodeModel = new NodeModel();
        nodeModel.setMachineId(id);
        List<NodeModel> modelList = nodeService.listByBean(nodeModel);
        modelList = Optional.ofNullable(modelList).orElseGet(ArrayList::new);
        for (NodeModel model : modelList) {
            model.setWorkspace(workspaceService.getByKey(model.getWorkspaceId()));
        }
        return ApiResult.success("", modelList);
    }

    /**
     * 查询模板节点
     *
     * @return list
     */
    @GetMapping(value = "list-template-node", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<MachineNodeModel>> listTemplate() {
        MachineNodeModel machineNodeModel = new MachineNodeModel();
        machineNodeModel.setTemplateNode(true);
        List<MachineNodeModel> modelList = machineNodeServer.listByBean(machineNodeModel);
        return ApiResult.success("", modelList);
    }


    /**
     * 保存授权配置
     *
     * @return json
     */
    @RequestMapping(value = "save-whitelist", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SYSTEM_NODE_WHITELIST, method = MethodFeature.EDIT)
    public ApiResult<Object> saveWhitelist(@ValidatorItem(msg = "请选择分发的机器") String ids,
                                              HttpServletRequest request) {
        //
        List<String> idList = io.voyager1.util.ConvertUtil.splitTrim(ids, ",");
        for (String s : idList) {
            MachineNodeModel machineNodeModel = machineNodeServer.getByKey(s);
            Assert.notNull(machineNodeModel, "没有对应的机器");
            ApiResult<String> jsonMessage = NodeForward.request(machineNodeModel, request, NodeUrl.WhitelistDirectory_Submit);
            Assert.state(jsonMessage.success(), String.format("分发 %s 节点授权失败 %s", machineNodeModel.getName(), jsonMessage.getMsg()));
        }
        return ApiResult.success("保存成功");
    }


    @PostMapping(value = "save-node-config", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SYSTEM_CONFIG, method = MethodFeature.EDIT)
    @SystemPermission(superUser = true)
    public ApiResult<Object> saveNodeConfig(@ValidatorItem(msg = "请选择分发的机器") String ids,
                                               String content,
                                               String restart) {
        List<String> idList = io.voyager1.util.ConvertUtil.splitTrim(ids, ",");
        for (String s : idList) {
            MachineNodeModel machineNodeModel = machineNodeServer.getByKey(s);
            Assert.notNull(machineNodeModel, "没有对应的机器");
            JSONObject reqData = new JSONObject();
            reqData.put("content", content);
            reqData.put("restart", restart);
            ApiResult<String> jsonMessage = NodeForward.request(machineNodeModel, NodeUrl.SystemSaveConfig, reqData);
            Assert.state(jsonMessage.success(), String.format("分发 %s 节点配置失败 %s", machineNodeModel.getName(), jsonMessage.getMsg()));
        }
        return ApiResult.success("修改成功");
    }

    /**
     * 查询集群孤独的数据
     *
     * @param id 集群ID
     * @return json
     */
    @GetMapping(value = "lonely-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<Object> lonelyData(@ValidatorItem String id) {
        MachineNodeModel machineNodeModel = machineNodeServer.getByKey(id);
        Assert.notNull(machineNodeModel, "没有对应的机器");
        List<ProjectInfoCacheModel> models = projectInfoCacheService.lonelyDataArray(machineNodeModel);
        List<NodeScriptCacheModel> scriptCacheModels = nodeScriptServer.lonelyDataArray(machineNodeModel);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("projects", models);
        jsonObject.put("scripts", scriptCacheModels);
        return ApiResult.success("", jsonObject);
    }

    @PostMapping(value = "correct-lonely-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<Object> correctLonelyData(@ValidatorItem String id,
                                                  @ValidatorItem String type,
                                                  @ValidatorItem String dataId,
                                                  @ValidatorItem String toNodeId) {
        MachineNodeModel machineNodeModel = machineNodeServer.getByKey(id);
        Assert.notNull(machineNodeModel, "没有对应的机器");
        {
            NodeModel nodeModel = nodeService.getByKey(toNodeId);
            Assert.notNull(nodeModel, "没有对应的节点");
            Assert.hasText(nodeModel.getWorkspaceId(), "节点没有工作空间");
            Assert.state(java.util.Objects.equals(nodeModel.getMachineId(), machineNodeModel.getId()), "资产集群和节点不匹配");
            NodeUrl nodeUrl;
            if ((type != null && type.equalsIgnoreCase("script"))) {
                nodeUrl = NodeUrl.Script_ChangeWorkspaceId;
            } else if ((type != null && type.equalsIgnoreCase("project"))) {
                nodeUrl = NodeUrl.Manage_ChangeWorkspaceId;
            } else {
                throw new IllegalArgumentException("不支持的类型：" + type);
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("newWorkspaceId", nodeModel.getWorkspaceId());
            jsonObject.put("newNodeId", toNodeId);
            jsonObject.put("id", dataId);
            ApiResult<String> jsonMessage = NodeForward.request(machineNodeModel, nodeUrl, jsonObject);
            if (!jsonMessage.success()) {
                return new ApiResult<>(406, "修正数据失败：" + jsonMessage.getMsg());
            }
        }
        // 重新同步节点数据
        {
            NodeModel nodeModel = new NodeModel();
            nodeModel.setMachineId(id);
            List<NodeModel> modelList = nodeService.listByBean(nodeModel);
            for (NodeModel model : modelList) {
                if ((type != null && type.equalsIgnoreCase("script"))) {
                    nodeScriptServer.syncExecuteNode(model);
                } else if ((type != null && type.equalsIgnoreCase("project"))) {
                    projectInfoCacheService.syncExecuteNode(model);
                }
            }
        }
        return ApiResult.success("修正成功");
    }

    @GetMapping(value = "monitor-config", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> monitorConfig(HttpServletRequest request, String id) {
        ApiResult<JSONObject> message = this.tryRequestMachine(id, request, NodeUrl.Info);
        Assert.notNull(message, "没有对应的资产机器");
        Assert.state(message.success(), message.getMsg());
        JSONObject data = message.getData();
        JSONObject monitor = Optional.ofNullable(data).map(jsonObject -> jsonObject.getJSONObject("monitor")).orElse(null);
        return ApiResult.success("", monitor);
    }
}
