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

package io.voyager1.controller.system;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.Validator;
import io.voyager1.util.BooleanUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerConst;
import io.voyager1.common.ServerOpenApi;
import io.voyager1.common.UrlRedirectUtil;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.data.WorkspaceEnvVarModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.system.WorkspaceEnvVarService;
import io.voyager1.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 2021/12/10
 */

@RestController
@Feature(cls = ClassFeature.SYSTEM_WORKSPACE_ENV)
@RequestMapping(value = "/system/workspace_env/")
public class WorkspaceEnvVarController extends BaseServerController {

    private final WorkspaceEnvVarService workspaceEnvVarService;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public WorkspaceEnvVarController(WorkspaceEnvVarService workspaceEnvVarService,
                                     TriggerTokenLogServer triggerTokenLogServer) {
        this.workspaceEnvVarService = workspaceEnvVarService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * 分页列表
     *
     * @return json
     */
    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<WorkspaceEnvVarModel>> list(HttpServletRequest request) {
        PageResultDto<WorkspaceEnvVarModel> listPage = workspaceEnvVarService.listPage(request);
        listPage.each(workspaceEnvVarModel -> {
            Integer privacy = workspaceEnvVarModel.getPrivacy();
            if (privacy != null && privacy == 1) {
                workspaceEnvVarModel.setValue("");
            }
        });
        return ApiResult.success("", listPage);
    }

    /**
     * 全部环境变量
     *
     * @return json
     */
    @PostMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<WorkspaceEnvVarModel>> allList(HttpServletRequest request) {
        List<WorkspaceEnvVarModel> list = workspaceEnvVarService.listByWorkspace(request);
        list.forEach(workspaceEnvVarModel -> {
            Integer privacy = workspaceEnvVarModel.getPrivacy();
            if (privacy != null && privacy == 1) {
                workspaceEnvVarModel.setValue("");
            }
        });
        return ApiResult.success("", list);
    }

    /**
     * 编辑变量
     *
     * @param workspaceId 空间id
     * @param name        变量名称
     * @param value       值
     * @param description 描述
     * @return json
     */
    @PostMapping(value = "/edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Object> edit(String id,
                                     @ValidatorItem String workspaceId,
                                     @ValidatorItem String name,
                                     String value,
                                     @ValidatorItem String description,
                                     String privacy,
                                     String nodeIds) {
        if (!getUser().checkSystemUser()) {
            Assert.state(!java.util.Objects.equals(workspaceId, ServerConst.WORKSPACE_GLOBAL), "全局工作空间变量请到系统管理修改");
        }

        workspaceEnvVarService.checkUserWorkspace(workspaceId);

        this.checkInfo(id, name, workspaceId);
        boolean privacyBool = Boolean.parseBoolean(privacy);
        //
        WorkspaceEnvVarModel workspaceModel = new WorkspaceEnvVarModel();
        workspaceModel.setName(name);
        if (privacyBool) {
            if ((value != null && !value.isEmpty())) {
                workspaceModel.setValue(value);
            } else {
                // 隐私字段 创建必填
                Assert.state((id != null && !id.isEmpty()), "请填写参数值");
            }
        } else {
            // 非隐私必填
            Assert.hasText(value, "请填写参数值");
            workspaceModel.setValue(value);
        }
        workspaceModel.setWorkspaceId(workspaceId);
        workspaceModel.setNodeIds(nodeIds);
        workspaceModel.setDescription(description);
        //
        String oldNodeIds = null;
        if ((id == null || id.isEmpty())) {
            // 创建
            workspaceModel.setPrivacy(privacyBool ? 1 : 0);
            workspaceEnvVarService.insert(workspaceModel);
        } else {
            WorkspaceEnvVarModel byKey = workspaceEnvVarService.getByKey(id);
            Assert.notNull(byKey, "没有对应的数据");
            Assert.state(java.util.Objects.equals(workspaceId, byKey.getWorkspaceId()), "工作空间错误,或者没有权限编辑此数据");
            oldNodeIds = byKey.getNodeIds();
            workspaceModel.setId(id);
            // 不能修改
            workspaceModel.setPrivacy(null);
            workspaceEnvVarService.updateById(workspaceModel);
        }
        this.syncNodeEnvVar(workspaceModel, oldNodeIds);
        return ApiResult.success("操作成功");
    }

    private void syncDelNodeEnvVar(String name, Collection<String> delNode, String workspaceId) {
        for (String s : delNode) {
            NodeModel byKey = nodeService.getByKey(s);
            Assert.state(java.util.Objects.equals(workspaceId, byKey.getWorkspaceId()), "选择节点错误");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", name);
            ApiResult<String> jsonMessage = NodeForward.request(byKey, NodeUrl.Workspace_EnvVar_Delete, jsonObject);
            Assert.state(jsonMessage.success(), String.format("处理 %s 节点删除脚本失败 %s", byKey.getName(), jsonMessage.getMsg()));
        }
    }

    private void syncNodeEnvVar(WorkspaceEnvVarModel workspaceEnvVarModel, String oldNode) {
        String workspaceId = workspaceEnvVarModel.getWorkspaceId();
        List<String> newNodeIds = StrUtil.splitTrim(workspaceEnvVarModel.getNodeIds(), ",");
        if (newNodeIds == null) {
            newNodeIds = new java.util.ArrayList<>();
        }
        List<String> oldNodeIds = io.voyager1.util.ConvertUtil.splitTrim(oldNode, ",");
        Collection<String> delNode = CollUtil.subtract(oldNodeIds, newNodeIds);
        // 删除
        this.syncDelNodeEnvVar(workspaceEnvVarModel.getName(), delNode, workspaceId);
        // 更新
        for (String newNodeId : newNodeIds) {
            NodeModel byKey = nodeService.getByKey(newNodeId);
            Assert.state(java.util.Objects.equals(workspaceId, byKey.getWorkspaceId()), "选择节点错误");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("description", workspaceEnvVarModel.getDescription());
            jsonObject.put("name", workspaceEnvVarModel.getName());
            jsonObject.put("privacy", workspaceEnvVarModel.getPrivacy());
            if ((workspaceEnvVarModel.getValue() != null && !workspaceEnvVarModel.getValue().isEmpty())) {
                jsonObject.put("value", workspaceEnvVarModel.getValue());
            } else {
                // 查询
                WorkspaceEnvVarModel byKeyExits = workspaceEnvVarService.getByKey(workspaceEnvVarModel.getId());
                jsonObject.put("value", byKeyExits.getValue());
            }
            ApiResult<String> jsonMessage = NodeForward.request(byKey, NodeUrl.Workspace_EnvVar_Update, jsonObject);
            Assert.state(jsonMessage.getCode() == 200, String.format("处理 %s 节点同步脚本失败 %s", byKey.getName(), jsonMessage.getMsg()));
        }
    }

    private void checkInfo(String id, String name, String workspaceId) {
        Validator.validateGeneral(name, 1, 50, "变量名称 1-50 英文字母 、数字和下划线");
        //
        Entity entity = Entity.create();
        entity.set("name", name);
        if (!java.util.Objects.equals(workspaceId, ServerConst.WORKSPACE_GLOBAL)) {
            entity.set("workspaceId", new java.util.ArrayList<>(java.util.Arrays.asList(workspaceId, ServerConst.WORKSPACE_GLOBAL)));
        }
        if ((id != null && !id.isEmpty())) {
            entity.set("id", String.format(" <> %s", id));
        }
        boolean exists = workspaceEnvVarService.exists(entity);
        Assert.state(!exists, "对应的变量名称已经存在啦");
    }


    /**
     * 删除变量
     *
     * @param id 变量 ID
     * @return json
     */
    @GetMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> delete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "数据 id 不能为空") String id,
                                       @ValidatorItem String workspaceId) {
        if (!getUser().checkSystemUser()) {
            Assert.state(!java.util.Objects.equals(workspaceId, ServerConst.WORKSPACE_GLOBAL), "全局工作空间变量请到系统管理修改");
        }
        workspaceEnvVarService.checkUserWorkspace(workspaceId);
        WorkspaceEnvVarModel byKey = workspaceEnvVarService.getByKey(id);
        Assert.notNull(byKey, "没有对应的数据");
        Assert.state(java.util.Objects.equals(workspaceId, byKey.getWorkspaceId()), "选择工作空间错误");
        String oldNodeIds = byKey.getNodeIds();
        List<String> delNode = io.voyager1.util.ConvertUtil.splitTrim(oldNodeIds, ",");
        this.syncDelNodeEnvVar(byKey.getName(), delNode, workspaceId);
        // 删除信息
        workspaceEnvVarService.delByKey(id);
        return ApiResult.success("删除成功");
    }

    /**
     * get a trigger url
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "trigger-url", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Map<String, String>> getTriggerUrl(@ValidatorItem String id, @ValidatorItem String workspaceId, String rest, HttpServletRequest request) {
        workspaceEnvVarService.checkUserWorkspace(workspaceId);
        WorkspaceEnvVarModel item = workspaceEnvVarService.getByKey(id);
        Assert.notNull(item, "没有对应的环境变量");
        Assert.state(java.util.Objects.equals(workspaceId, item.getWorkspaceId()), "选择工作空间错误");
        //
        Assert.state((item.getPrivacy() != null ? item.getPrivacy() : -1) == 0, "隐私变量不能生成触发器");
        UserModel user = getUser();
        WorkspaceEnvVarModel updateInfo;
        if ((item.getTriggerToken() == null || item.getTriggerToken().isEmpty()) || (rest != null && !rest.isEmpty())) {
            updateInfo = new WorkspaceEnvVarModel();
            updateInfo.setId(id);
            updateInfo.setTriggerToken(triggerTokenLogServer.restToken(item.getTriggerToken(), workspaceEnvVarService.typeName(),
                item.getId(), user.getId()));
            workspaceEnvVarService.updateById(updateInfo);
        } else {
            updateInfo = item;
        }
        Map<String, String> map = this.getBuildToken(updateInfo, request);
        String string = "重置成功";
        return ApiResult.success((rest == null || rest.isEmpty()) ? "ok" : string, map);
    }

    private Map<String, String> getBuildToken(WorkspaceEnvVarModel item, HttpServletRequest request) {
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
        String url = ServerOpenApi.SERVER_ENV_VAR_TRIGGER_URL.
            replace("{id}", item.getId()).
            replace("{token}", item.getTriggerToken());
        String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
        Map<String, String> map = new HashMap<>(10);
        map.put("triggerUrl", FileUtil.normalize(triggerBuildUrl));

        map.put("id", item.getId());
        map.put("token", item.getTriggerToken());
        return map;
    }
}
