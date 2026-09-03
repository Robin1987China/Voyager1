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

import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerConst;
import io.voyager1.common.ServerOpenApi;
import io.voyager1.common.UrlRedirectUtil;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.node.ProjectInfoCacheModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.node.ProjectInfoCacheService;
import io.voyager1.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点管理
 *
 * @since 2019/4/16
 */
@RestController
@RequestMapping(value = "/node")
@Feature(cls = ClassFeature.NODE)
@Slf4j
public class NodeProjectInfoController extends BaseServerController {

    private final ProjectInfoCacheService projectInfoCacheService;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public NodeProjectInfoController(ProjectInfoCacheService projectInfoCacheService,
                                     TriggerTokenLogServer triggerTokenLogServer) {
        this.projectInfoCacheService = projectInfoCacheService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }


    /**
     * load node project list
     * 加载节点项目列表
     *
     * @return json
     */
    @PostMapping(value = "project_list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<PageResultDto<ProjectInfoCacheModel>> projectList(HttpServletRequest request) {
        PageResultDto<ProjectInfoCacheModel> resultDto = projectInfoCacheService.listPage(request);
        return ApiResult.success("", resultDto);
    }

    /**
     * load node project list
     * 加载节点项目列表
     *
     * @return json
     */
    @GetMapping(value = "project_list_all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<ProjectInfoCacheModel>> projectListAll(HttpServletRequest request) {
        List<ProjectInfoCacheModel> projectInfoCacheModels = projectInfoCacheService.listByWorkspace(request);
        return ApiResult.success("", projectInfoCacheModels);
    }

    /**
     * 查询所有的分组
     *
     * @return list
     */
    @GetMapping(value = "list-project-group-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<String>> listGroupAll(HttpServletRequest request) {
        List<String> listGroup = projectInfoCacheService.listGroup(request);
        return ApiResult.success("", listGroup);
    }

    /**
     * 同步节点项目
     *
     * @return json
     */
    @GetMapping(value = "sync_project", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT, method = MethodFeature.DEL)
    public ApiResult<Object> syncProject(String nodeId, HttpServletRequest request) {
        NodeModel nodeModel = nodeService.getByKey(nodeId);
        Assert.notNull(nodeModel, "对应的节点不存在");
        int count = projectInfoCacheService.delCache(nodeId, request);
        String msg = projectInfoCacheService.syncExecuteNode(nodeModel);
        return ApiResult.success("主动清除：" + count + " " + msg);
    }

    /**
     * 排序
     *
     * @param id        节点ID
     * @param method    方法
     * @param compareId 比较的ID
     * @return msg
     */
    @GetMapping(value = "project-sort-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> sortItem(@ValidatorItem String id, @ValidatorItem String method, String compareId, HttpServletRequest request) {
        if ((method != null && method.equalsIgnoreCase("top"))) {
            projectInfoCacheService.sortToTop(id, request);
        } else if ((method != null && method.equalsIgnoreCase("up"))) {
            projectInfoCacheService.sortMoveUp(id, compareId, request);
        } else if ((method != null && method.equalsIgnoreCase("down"))) {
            projectInfoCacheService.sortMoveDown(id, compareId, request);
        } else {
            return new ApiResult<>(400, "不支持的方式" + method);
        }
        return new ApiResult<>(200, "操作成功");
    }

    /**
     * get a trigger url
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "project-trigger-url", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Map<String, String>> getTriggerUrl(String id, String rest, HttpServletRequest request) {
        ProjectInfoCacheModel item = projectInfoCacheService.getByKey(id, request);
        UserModel user = getUser();
        ProjectInfoCacheModel updateItem;
        if ((item.getTriggerToken() == null || item.getTriggerToken().isEmpty()) || (rest != null && !rest.isEmpty())) {
            updateItem = new ProjectInfoCacheModel();
            updateItem.setId(id);
            updateItem.setTriggerToken(triggerTokenLogServer.restToken(item.getTriggerToken(), projectInfoCacheService.typeName(),
                item.getId(), user.getId()));
            projectInfoCacheService.updateById(updateItem);
        } else {
            updateItem = item;
        }
        Map<String, String> map = this.getBuildToken(updateItem, request);
        String string = "重置成功";
        return ApiResult.success((rest == null || rest.isEmpty()) ? "ok" : string, map);
    }

    private Map<String, String> getBuildToken(ProjectInfoCacheModel item, HttpServletRequest request) {
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
        String url = ServerOpenApi.SERVER_PROJECT_TRIGGER_URL.
            replace("{id}", item.getId()).
            replace("{token}", item.getTriggerToken());
        String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
        Map<String, String> map = new HashMap<>(10);
        map.put("triggerUrl", FileUtil.normalize(triggerBuildUrl));
        String batchTriggerBuildUrl = String.format("/%s/%s", contextPath, ServerOpenApi.SERVER_PROJECT_TRIGGER_BATCH);
        map.put("batchTriggerUrl", FileUtil.normalize(batchTriggerBuildUrl));

        map.put("id", item.getId());
        map.put("token", item.getTriggerToken());
        return map;
    }
}
