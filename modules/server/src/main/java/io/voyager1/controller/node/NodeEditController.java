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

package io.voyager1.controller.node;

import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.node.NodeScriptCacheModel;
import io.voyager1.model.node.ProjectInfoCacheModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.dblog.BuildInfoService;
import io.voyager1.service.monitor.MonitorService;
import io.voyager1.service.node.ProjectInfoCacheService;
import io.voyager1.service.node.script.NodeScriptExecuteLogServer;
import io.voyager1.service.node.script.NodeScriptServer;
import io.voyager1.service.outgiving.LogReadServer;
import io.voyager1.service.outgiving.OutGivingServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 节点管理
 */
@RestController
@RequestMapping(value = "/node")
@Feature(cls = ClassFeature.NODE)
public class NodeEditController extends BaseServerController {

    private final OutGivingServer outGivingServer;
    private final MonitorService monitorService;
    private final BuildInfoService buildService;
    private final LogReadServer logReadServer;
    private final ProjectInfoCacheService projectInfoCacheService;
    private final NodeScriptServer nodeScriptServer;
    private final NodeScriptExecuteLogServer nodeScriptExecuteLogServer;

    public NodeEditController(OutGivingServer outGivingServer,
                              MonitorService monitorService,
                              BuildInfoService buildService,
                              LogReadServer logReadServer,
                              ProjectInfoCacheService projectInfoCacheService,
                              NodeScriptServer nodeScriptServer,
                              NodeScriptExecuteLogServer nodeScriptExecuteLogServer) {
        this.outGivingServer = outGivingServer;
        this.monitorService = monitorService;
        this.buildService = buildService;
        this.logReadServer = logReadServer;
        this.projectInfoCacheService = projectInfoCacheService;
        this.nodeScriptServer = nodeScriptServer;
        this.nodeScriptExecuteLogServer = nodeScriptExecuteLogServer;
    }


    @PostMapping(value = "list_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<NodeModel>> listJson(HttpServletRequest request) {
        PageResultDto<NodeModel> nodeModelPageResultDto = nodeService.listPage(request);
        nodeModelPageResultDto.each(nodeModel -> nodeModel.setMachineNodeData(machineNodeServer.getByKey(nodeModel.getMachineId())));
        return ApiResult.success("", nodeModelPageResultDto);
    }

    @GetMapping(value = "list_data_all.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<NodeModel>> listDataAll(HttpServletRequest request) {
        List<NodeModel> list = nodeService.listByWorkspace(request);
        return ApiResult.success("", list);
    }

    /**
     * 查询所有的分组
     *
     * @return list
     */
    @GetMapping(value = "list_group_all.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<String>> listGroupAll(HttpServletRequest request) {
        List<String> listGroup = nodeService.listGroup(request);
        return ApiResult.success("", listGroup);
    }

    @PostMapping(value = "save.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> save(HttpServletRequest request) {
        nodeService.update(request);
        return ApiResult.success("操作成功");
    }


    /**
     * 删除节点
     *
     * @param id 节点id
     * @return json
     */
    @PostMapping(value = "del.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> del(@ValidatorItem String id, HttpServletRequest request) {
        this.checkDataBind(id, request, "删除");
        //
        {
            ProjectInfoCacheModel projectInfoCacheModel = new ProjectInfoCacheModel();
            projectInfoCacheModel.setNodeId(id);
            projectInfoCacheModel.setWorkspaceId(projectInfoCacheService.getCheckUserWorkspace(request));
            boolean exists = projectInfoCacheService.exists(projectInfoCacheModel);
            Assert.state(!exists, "该节点下还存在项目，不能直接删除（需要提前解绑或者删除关联数据后才能删除）");
        }
        //
        {
            NodeScriptCacheModel nodeScriptCacheModel = new NodeScriptCacheModel();
            nodeScriptCacheModel.setNodeId(id);
            nodeScriptCacheModel.setWorkspaceId(nodeScriptServer.getCheckUserWorkspace(request));
            boolean exists = nodeScriptServer.exists(nodeScriptCacheModel);
            Assert.state(!exists, "该节点下还存在脚本模版，不能直接删除（需要提前解绑或者删除关联数据后才能删除）");
        }
        //
        this.delNodeData(id, request);
        return ApiResult.success("操作成功");
    }

    private void checkDataBind(String id, HttpServletRequest request, String msg) {
        //  判断分发
        boolean checkNode = outGivingServer.checkNode(id, request);
        Assert.state(!checkNode, "该节点存在分发项目，不能" + msg);
        boolean checkLogRead = logReadServer.checkNode(id, request);
        Assert.state(!checkLogRead, "该节点存在日志搜索（阅读）项目，不能" + msg);
        // 监控
        boolean checkNode1 = monitorService.checkNode(id);
        Assert.state(!checkNode1, "该节点存在监控项，不能" + msg);
        boolean checkNode2 = buildService.checkNode(id, request);
        Assert.state(!checkNode2, "该节点存在构建项，不能" + msg);
    }

    private void delNodeData(String id, HttpServletRequest request) {
        //
        int i = nodeService.delByKey(id, request);
        if (i > 0) {
            //
            nodeScriptExecuteLogServer.delCache(id, request);
        }
    }

    /**
     * 解绑
     *
     * @param id 分发id
     * @return json
     */
    @GetMapping(value = "unbind.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> unbind(String id, HttpServletRequest request) {
        this.checkDataBind(id, request, "解绑");
        //
        projectInfoCacheService.delCache(id, request);
        nodeScriptServer.delCache(id, request);
        this.delNodeData(id, request);
        return ApiResult.success("操作成功");
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
        String nowWorkspaceId = nodeService.getCheckUserWorkspace(request);
        //
        nodeService.checkUserWorkspace(toWorkspaceId);
        nodeService.syncToWorkspace(ids, nowWorkspaceId, toWorkspaceId);
        return ApiResult.success("操作成功");
    }

    /**
     * 排序
     *
     * @param id        节点ID
     * @param method    方法
     * @param compareId 比较的ID
     * @return msg
     */
    @GetMapping(value = "sort-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> sortItem(@ValidatorItem String id, @ValidatorItem String method, String compareId, HttpServletRequest request) {
        if ((method != null && method.equalsIgnoreCase("top"))) {
            nodeService.sortToTop(id, request);
        } else if ((method != null && method.equalsIgnoreCase("up"))) {
            nodeService.sortMoveUp(id, compareId, request);
        } else if ((method != null && method.equalsIgnoreCase("down"))) {
            nodeService.sortMoveDown(id, compareId, request);
        } else {
            return new ApiResult<>(400, "不支持的方式" + method);
        }
        return ApiResult.success("操作成功");
    }
}
