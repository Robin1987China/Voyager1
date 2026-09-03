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
import io.voyager1.util.IoUtil;
import io.voyager1.util.ResourceUtil;
import io.voyager1.util.Tree;
import io.voyager1.util.TreeNode;
import io.voyager1.util.TreeUtil;
import io.voyager1.util.CharsetUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Const;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.core.db.TableName;
import io.voyager1.func.system.model.ClusterInfoModel;
import io.voyager1.func.system.service.ClusterInfoService;
import io.voyager1.model.BaseWorkspaceModel;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.WorkspaceModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.system.SystemParametersServer;
import io.voyager1.service.system.WorkspaceService;
import io.voyager1.service.user.UserBindWorkspaceService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.nio.charset.StandardCharsets;

/**
 * @since 2021/12/3
 */
@RestController
@Feature(cls = ClassFeature.SYSTEM_WORKSPACE)
@RequestMapping(value = "/system/workspace/")
@SystemPermission
public class WorkspaceController extends BaseServerController {

    private final WorkspaceService workspaceService;
    private final UserBindWorkspaceService userBindWorkspaceService;
    private final SystemParametersServer systemParametersServer;
    private final ClusterInfoService clusterInfoService;

    public WorkspaceController(WorkspaceService workspaceService,
                               UserBindWorkspaceService userBindWorkspaceService,
                               SystemParametersServer systemParametersServer,
                               ClusterInfoService clusterInfoService) {
        this.workspaceService = workspaceService;
        this.userBindWorkspaceService = userBindWorkspaceService;
        this.systemParametersServer = systemParametersServer;
        this.clusterInfoService = clusterInfoService;
    }

    /**
     * 编辑工作空间
     *
     * @param name        工作空间名称
     * @param description 描述
     * @return json
     */
    @PostMapping(value = "/edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> create(String id,
                                       @ValidatorItem String name,
                                       @ValidatorItem String description,
                                       String group,
                                       @ValidatorItem(msg = "请选择集群") String clusterInfoId) {
        //
        ClusterInfoModel clusterInfoModel = clusterInfoService.getByKey(clusterInfoId);
        Assert.notNull(clusterInfoModel, "对应的集群不存在");
        this.checkInfo(id, name);
        //
        WorkspaceModel workspaceModel = new WorkspaceModel();
        workspaceModel.setName(name);
        workspaceModel.setDescription(description);
        workspaceModel.setGroup(group);
        workspaceModel.setClusterInfoId(clusterInfoModel.getId());
        if ((id == null || id.isEmpty())) {
            // 创建
            workspaceService.insert(workspaceModel);
        } else {
            workspaceModel.setId(id);
            workspaceService.updateById(workspaceModel);
        }
        return ApiResult.success("操作成功");
    }

    private void checkInfo(String id, String name) {
        Entity entity = Entity.create();
        entity.set("name", name);
        if ((id != null && !id.isEmpty())) {
            entity.set("id", String.format(" <> %s", id));
        }
        boolean exists = workspaceService.exists(entity);
        Assert.state(!exists, "对应的工作空间名称已经存在啦");
    }

    /**
     * 工作空间分页列表
     *
     * @return json
     */
    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<WorkspaceModel>> list(HttpServletRequest request) {
        PageResultDto<WorkspaceModel> listPage = workspaceService.listPage(request);
        return ApiResult.success("", listPage);
    }

    /**
     * 查询所有的分组
     *
     * @return list
     */
    @GetMapping(value = "list-group-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<String>> listGroupAll() {
        List<String> listGroup = workspaceService.listGroup();
        return ApiResult.success("", listGroup);
    }

    /**
     * 查询工作空间列表
     *
     * @return json
     */
    @GetMapping(value = "/list_all")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<WorkspaceModel>> listAll() {
        List<WorkspaceModel> list = workspaceService.list();
        return ApiResult.success("", list);
    }

    /**
     * 删除工作空间前检查
     *
     * @param id 工作空间 ID
     * @return json
     */
    @GetMapping(value = "pre-check-delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    @SystemPermission(superUser = true)
    public ApiResult<Tree<String>> preCheckDelete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "数据 id 不能为空") String id) {
        //
        Assert.state(!java.util.Objects.equals(id, Const.WORKSPACE_DEFAULT_ID), "不能删除默认工作空间");
        // 判断是否存在关联数据
        Set<Class<?>> classes = BaseWorkspaceModel.allTableClass();

        List<TreeNode<String>> nodes = new ArrayList<>(classes.size());
        for (Class<?> aClass : classes) {
            TableName tableName = aClass.getAnnotation(TableName.class);
            Class<?> parents = tableName.parents();
            //
            String parent = Optional.of(parents)
                .map(aClass1 -> aClass1 != Void.class ? aClass1 : null)
                .map(aClass1 -> {
                    TableName tableName1 = aClass1.getAnnotation(TableName.class);
                    return tableName1.value();
                })
                .orElse("");
            //
            String sql = "select  count(1) as cnt from " + tableName.value() + " where workspaceId=?";
            Number number = workspaceService.queryNumber(sql, id);

            TreeNode<String> treeNode = new TreeNode<>(tableName.value(), parent, I18nMessageUtil.get(tableName.nameKey()), 0);
            //
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("workspaceBind", tableName.workspaceBind());
            jsonObject.put("count", (number != null ? number.intValue() : 0));
            treeNode.setExtra(jsonObject);
            nodes.add(treeNode);
        }
        Tree<String> stringTree = TreeUtil.buildSingle(nodes, "");
        stringTree.setName("");
        return new ApiResult<>(200, "", stringTree);
    }

    /**
     * 删除工作空间
     *
     * @param id 工作空间 ID
     * @return json
     */
    @GetMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    @SystemPermission(superUser = true)
    public ApiResult<String> delete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "数据 id 不能为空") String id) {
        //
        Assert.state(!java.util.Objects.equals(id, Const.WORKSPACE_DEFAULT_ID), "不能删除默认工作空间");
        // 判断是否存在关联数据
        Set<Class<?>> classes = BaseWorkspaceModel.allTableClass();

        List<Class<?>> autoDeleteClass = new ArrayList<>();
        for (Class<?> aClass : classes) {
            TableName tableName = aClass.getAnnotation(TableName.class);
            int workspaceBind = tableName.workspaceBind();
            if (workspaceBind == 2) {
                // 先忽略不执行自动删除
                autoDeleteClass.add(aClass);
            } else if (workspaceBind == 3) {
                // 父级不存在自动删除
                Class<?> parents = tableName.parents();
                Assert.state(parents != Void.class, "表信息配置错误");
                TableName tableName1 = parents.getAnnotation(TableName.class);
                Assert.notNull(tableName1, "父级表信息配置错误," + aClass);
                //
                String sql = "select  count(1) as cnt from " + tableName1.value() + " where workspaceId=?";
                Number cntNum = workspaceService.queryNumber(sql, id);
                int cnt = cntNum == null ? 0 : cntNum.intValue();
                Assert.state(cnt <= 0, String.format("当前工作空间下还存在关联：%s 和 %s 数据", I18nMessageUtil.get(tableName.nameKey()), I18nMessageUtil.get(tableName1.nameKey())));
                // 等待自动删除
                autoDeleteClass.add(aClass);
            } else {
                // 其他严格检查的情况
                String sql = "select  count(1) as cnt from " + tableName.value() + " where workspaceId=?";
                Number cntNum = workspaceService.queryNumber(sql, id);
                int cnt = cntNum == null ? 0 : cntNum.intValue();
                Assert.state(cnt <= 0, "当前工作空间下还存在关联数据：" + I18nMessageUtil.get(tableName.nameKey()));
            }
        }
        // 判断用户绑定关系
        boolean workspace = userBindWorkspaceService.existsWorkspace(id);
        Assert.state(!workspace, "当前工作空间下还绑定着用户（权限组）信息");
        // 最后执行自动删除
        StringBuilder autoDelete = new StringBuilder("");
        for (Class<?> aClass : autoDeleteClass) {
            TableName tableName = aClass.getAnnotation(TableName.class);
            // 自动删除
            String sql = "delete from " + tableName.value() + " where workspaceId=?";
            int execute = workspaceService.execute(sql, id);
            if (execute > 0) {
                autoDelete.append(String.format(" 自动删除 %s 表中数据 %s 条数据", tableName.value(), execute));
            }
        }
        // 删除缓存
        String menusConfigKey = String.format("menus_config_%s", id);
        systemParametersServer.delByKey(menusConfigKey);
        String whitelistConfigKey = String.format("node_whitelist_%s", id);
        systemParametersServer.delByKey(whitelistConfigKey);
        systemParametersServer.delByKey(String.format("node_config_%s", id));
        // 删除信息
        workspaceService.delByKey(id);
        return new ApiResult<>(200, "删除成功:" + autoDelete);
    }

    /**
     * 加载菜单配置
     *
     * @return json
     */
    @RequestMapping(value = "get_menus_config", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> getMenusConfig(String workspaceId, HttpServletRequest request) {
        WorkspaceModel workspaceModel = workspaceService.getByKey(workspaceId);
        Assert.notNull(workspaceModel, "不存在对应的工作空间");
        JSONObject config = systemParametersServer.getConfigDefNewInstance(String.format("menus_config_%s", workspaceId), JSONObject.class);
        //"classpath:/menus/index.json"
        //"classpath:/menus/node-index.json"
        String language = I18nMessageUtil.tryGetNormalLanguage();
        config.put("serverMenus", this.readMenusJson("classpath:/menus/" + language + "/index.json"));
        return ApiResult.success("", config);
    }

    @PostMapping(value = "save_menus_config.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Object> saveMenusConfig(String serverMenuKeys, String nodeMenuKeys, String workspaceId) {
        WorkspaceModel workspaceModel = workspaceService.getByKey(workspaceId);
        Assert.notNull(workspaceModel, "不存在对应的工作空间");
        //
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nodeMenuKeys", io.voyager1.util.ConvertUtil.splitTrim(nodeMenuKeys, ","));
        jsonObject.put("serverMenuKeys", io.voyager1.util.ConvertUtil.splitTrim(serverMenuKeys, ","));
        String format = String.format("menus_config_%s", workspaceId);
        systemParametersServer.upsert(format, jsonObject, format);
        //
        return ApiResult.success("修改成功");
    }

    private JSONArray readMenusJson(String path) {
        // 菜单
        InputStream inputStream = ResourceUtil.getStream(path);
        String json = IoUtil.read(inputStream, StandardCharsets.UTF_8);
        return JSONArray.parseArray(json);
    }
}
