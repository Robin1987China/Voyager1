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

package io.voyager1.func.system.controller;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.Validator;
import io.voyager1.util.CollectorUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.func.system.model.ClusterInfoModel;
import io.voyager1.func.system.service.ClusterInfoService;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.WorkspaceModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.system.WorkspaceService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @since 2023/8/20
 */
@RestController
@RequestMapping(value = "/cluster/")
@Feature(cls = ClassFeature.CLUSTER_INFO)
@SystemPermission()
@Slf4j
public class ClusterInfoController {

    private final ClusterInfoService clusterInfoService;
    private final WorkspaceService workspaceService;

    public ClusterInfoController(ClusterInfoService clusterInfoService,
                                 WorkspaceService workspaceService) {
        this.clusterInfoService = clusterInfoService;
        this.workspaceService = workspaceService;
    }

    /**
     * 分页列表
     *
     * @return json
     */
    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<ClusterInfoModel>> list(HttpServletRequest request) {
        PageResultDto<ClusterInfoModel> listPage = clusterInfoService.listPage(request);
        return ApiResult.success("", listPage);
    }

    @GetMapping(value = "list-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<ClusterInfoModel>> listAll() {
        List<ClusterInfoModel> list = clusterInfoService.list();
        return ApiResult.success("", list);
    }

    /**
     * 查询所有可以管理的分组名
     *
     * @return json
     */
    @GetMapping(value = "list-link-groups")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> listLinkGroups() {
        //
        List<String> all = clusterInfoService.listLinkGroups();
        // 查询集群已经绑定的分组
        List<ClusterInfoModel> list = clusterInfoService.list();
        Map<String, List<JSONObject>> map = list.stream()
            .map(clusterInfoModel -> {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", clusterInfoModel.getName());
                jsonObject.put("id", clusterInfoModel.getId());
                jsonObject.put("linkGroup", clusterInfoModel.getLinkGroup());
                return jsonObject;
            })
            .flatMap((Function<JSONObject, Stream<JSONObject>>) jsonObject -> {
                String string = jsonObject.getString("linkGroup");
                List<String> list1 = io.voyager1.util.ConvertUtil.splitTrim(string, ",");
                return list1.stream()
                    .map(s -> {
                        JSONObject clone = jsonObject.clone();
                        clone.remove("linkGroup");
                        clone.put("group", s);
                        return clone;
                    });
            })
            .collect(CollectorUtil.groupingBy(o -> o.getString("group"), Collectors.toList()));
        //
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("linkGroups", all);
        jsonObject.put("groupMap", map);

        return ApiResult.success("", jsonObject);
    }

    /**
     * 修改集群
     *
     * @param id ID
     * @return json
     */
    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> edit(@ValidatorItem(msg = "数据 id 不能为空") String id,
                                     @ValidatorItem(msg = "请填写集群名称") String name,
                                     String url,
                                     @ValidatorItem(msg = "请选择关联分组") String linkGroup) {
        Opt.ofBlankAble(url).ifPresent(s -> Validator.validateUrl(s, "请填写正确的 url"));
        //
        List<String> list = io.voyager1.util.ConvertUtil.splitTrim(linkGroup, ",");
        Assert.notEmpty(list, "请选择关联的分组");
        //
        ClusterInfoModel infoModel = new ClusterInfoModel();
        infoModel.setId(id);
        infoModel.setName(name);
        infoModel.setLinkGroup(linkGroup);
        infoModel.setUrl(url);
        clusterInfoService.updateById(infoModel);
        return ApiResult.success("修改成功");
    }


    /**
     * 删除集群
     *
     * @param id ID
     * @return json
     */
    @GetMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    @SystemPermission(superUser = true)
    public ApiResult<String> delete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "数据 id 不能为空") String id) {
        //
        ClusterInfoModel infoModel = clusterInfoService.getByKey(id);
        Assert.notNull(infoModel, "对应的集群不存在");
        Assert.state(!clusterInfoService.online(infoModel), "不能删除在线的集群");
        // 如果还有工作空间绑定,不能删除集群
        WorkspaceModel workspaceModel = new WorkspaceModel();
        workspaceModel.setClusterInfoId(infoModel.getId());
        long count = workspaceService.count(workspaceModel);
        Assert.state(count == 0, "当前集群还被工作空间绑定，不能直接删除（需要提前解绑或者删除关联数据后才能删除）");
        //
        clusterInfoService.delByKey(id);
        return ApiResult.success("删除成功");
    }
}
