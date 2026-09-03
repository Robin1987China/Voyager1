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

package io.voyager1.controller.outgiving;

import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.RegexPool;
import io.voyager1.util.Validator;
import io.voyager1.util.StrUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Const;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.model.AfterOpt;
import io.voyager1.model.BaseEnum;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.enums.BuildReleaseMethod;
import io.voyager1.model.outgiving.OutGivingModel;
import io.voyager1.model.outgiving.OutGivingNodeProject;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.dblog.BuildInfoService;
import io.voyager1.service.node.ProjectInfoCacheService;
import io.voyager1.service.outgiving.DbOutGivingLogService;
import io.voyager1.service.outgiving.OutGivingServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分发控制
 *
 * @since 2019/4/20
 */
@RestController
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING)
public class OutGivingController extends BaseServerController {

    private final OutGivingServer outGivingServer;
    private final BuildInfoService buildService;
    private final DbOutGivingLogService dbOutGivingLogService;
    private final ProjectInfoCacheService projectInfoCacheService;

    public OutGivingController(OutGivingServer outGivingServer,
                               BuildInfoService buildService,
                               DbOutGivingLogService dbOutGivingLogService,
                               ProjectInfoCacheService projectInfoCacheService) {
        this.outGivingServer = outGivingServer;
        this.buildService = buildService;
        this.dbOutGivingLogService = dbOutGivingLogService;
        this.projectInfoCacheService = projectInfoCacheService;
    }

    /**
     * load dispatch list
     * 加载分发列表
     *
     * @return json
     */
    @PostMapping(value = "dispatch-list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<OutGivingModel>> dispatchList(HttpServletRequest request) {
        PageResultDto<OutGivingModel> pageResultDto = outGivingServer.listPage(request);
        return ApiResult.success("", pageResultDto);
    }

    /**
     * load dispatch list
     * 加载分发列表
     *
     * @return json
     */
    @GetMapping(value = "dispatch-list-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<OutGivingModel>> dispatchListAll(HttpServletRequest request) {
        List<OutGivingModel> outGivingModels = outGivingServer.listByWorkspace(request);
        return ApiResult.success("", outGivingModels);
    }


    @RequestMapping(value = "save", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> save(String type, @ValidatorItem String id, HttpServletRequest request) throws IOException {
        if ("add".equalsIgnoreCase(type)) {
            //
            String checkId = id.replace("-", "_");
            Validator.validateGeneral(checkId, 2, Const.ID_MAX_LEN, "分发id 不能为空并且长度在2-20（英文字母 、数字和下划线）");
            //boolean general = StringUtil.isGeneral(id, 2, 20);
            //Assert.state(general, );
            return addOutGiving(id, request);
        } else {
            return updateGiving(id, request);
        }
    }

    private ApiResult<String> addOutGiving(String id, HttpServletRequest request) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id);
        Assert.isNull(outGivingModel, "分发id已经存在啦,分发id需要全局唯一");
        //
        outGivingModel = new OutGivingModel();
        outGivingModel.setId(id);
        this.doData(outGivingModel, request);
        //
        outGivingServer.insert(outGivingModel);
        return ApiResult.success("添加成功");
    }

    private ApiResult<String> updateGiving(String id, HttpServletRequest request) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingModel, "没有找到对应的分发id");
        doData(outGivingModel, request);

        outGivingServer.updateById(outGivingModel);
        return ApiResult.success("修改成功");
    }

    private void doData(OutGivingModel outGivingModel, HttpServletRequest request) {
        outGivingModel.setName(getParameter("name"));
        outGivingModel.setGroup(getParameter("group"));
        Assert.hasText(outGivingModel.getName(), "分发名称不能为空");
        List<OutGivingModel> outGivingModels = outGivingServer.list();
        //
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        List<OutGivingNodeProject> outGivingNodeProjects = paramMap.entrySet()
            .stream()
            .filter(stringStringEntry -> StrUtil.startWith(stringStringEntry.getKey(), "node_"))
            .map(stringStringEntry -> {
                int lastIndexOf = StrUtil.lastIndexOfIgnoreCase(stringStringEntry.getKey(), "_");
                int indexOf = StrUtil.indexOfIgnoreCase(stringStringEntry.getKey(), "_") + 1;
                String nodeId = StrUtil.sub(stringStringEntry.getKey(), indexOf, lastIndexOf);
                //
                String nodeIdProject = stringStringEntry.getValue();
                NodeModel nodeModel = nodeService.getByKey(nodeId);
                Assert.notNull(nodeModel, "不存在对应的节点");
                //
                boolean exists = projectInfoCacheService.exists(nodeModel.getWorkspaceId(), nodeModel.getId(), nodeIdProject);
                Assert.state(exists, "没有找到对应的项目id:" + nodeIdProject);
                //
                OutGivingNodeProject outGivingNodeProject = outGivingModel.getNodeProject(nodeModel.getId(), nodeIdProject);
                if (outGivingNodeProject == null) {
                    outGivingNodeProject = new OutGivingNodeProject();
                }
                outGivingNodeProject.setNodeId(nodeModel.getId());
                outGivingNodeProject.setProjectId(nodeIdProject);
                return outGivingNodeProject;
            })
            .peek(outGivingNodeProject -> {
                // 判断项目是否已经被使用过啦
                if (outGivingModels != null) {
                    for (OutGivingModel outGivingModel1 : outGivingModels) {
                        if (outGivingModel1.getId().equalsIgnoreCase(outGivingModel.getId())) {
                            continue;
                        }
                        boolean checkContains = outGivingModel1.checkContains(outGivingNodeProject.getNodeId(), outGivingNodeProject.getProjectId());
                        Assert.state(!checkContains, "已经存在相同的分发项目:" + outGivingNodeProject.getProjectId());
                    }
                }
            }).collect(Collectors.toList());

        Assert.state((outGivingNodeProjects == null ? 0 : outGivingNodeProjects.size()) >= 1, "至少选择1个节点项目");

        outGivingModel.outGivingNodeProjectList(outGivingNodeProjects);
        //
        String afterOpt = getParameter("afterOpt");
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, ConvertUtil.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择分发后的操作");
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        //
        int intervalTime = getParameterInt("intervalTime", 10);
        outGivingModel.setIntervalTime(intervalTime);
        //
        outGivingModel.setClearOld(ConvertUtil.toBool(getParameter("clearOld"), false));
        //
        String secondaryDirectory = getParameter("secondaryDirectory");
        outGivingModel.setSecondaryDirectory(secondaryDirectory);
        outGivingModel.setUploadCloseFirst(ConvertUtil.toBool(getParameter("uploadCloseFirst"), false));
        //
        String webhook = getParameter("webhook");
        webhook = Opt.ofBlankAble(webhook)
            .map(s -> {
                Validator.validateMatchRegex(RegexPool.URL_HTTP, s, "WebHooks 地址不合法");
                return s;
            })
            .orElse("");
        outGivingModel.setWebhook(webhook);
    }

    /**
     * 删除分发信息
     *
     * @param id 分发id
     * @return json
     */
    @RequestMapping(value = "release_del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> releaseDel(String id, HttpServletRequest request) {
        // 判断构建
        boolean releaseMethod = buildService.checkReleaseMethod(id, request, BuildReleaseMethod.Outgiving);
        Assert.state(!releaseMethod, "当前分发存在构建项，不能直接删除（需要提前解绑或者删除关联数据后才能删除）");

        OutGivingModel outGivingServerItem = outGivingServer.getByKey(id, request);

        // 解除项目分发独立分发属性
        List<OutGivingNodeProject> outGivingNodeProjectList = outGivingServerItem.outGivingNodeProjectList();
        if (outGivingNodeProjectList != null) {
            outGivingNodeProjectList.forEach(outGivingNodeProject -> {
                NodeModel item = nodeService.getByKey(outGivingNodeProject.getNodeId());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", outGivingNodeProject.getProjectId());
                ApiResult<String> message = NodeForward.request(item, NodeUrl.Manage_ReleaseOutGiving, jsonObject);
                Assert.state(message.success(), "释放节点项目失败：" + message.getMsg());
            });
        }

        int byKey = outGivingServer.delByKey(id, request);
        if (byKey > 0) {
            // 删除日志
            dbOutGivingLogService.delByOutGivingId(id);
        }
        return ApiResult.success("操作成功");
    }

    /**
     * 解绑
     *
     * @param id 分发id
     * @return json
     */
    @GetMapping(value = "unbind.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> unbind(String id, HttpServletRequest request) {
        OutGivingModel outGivingServerItem = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingServerItem, "对应的分发不存在");
        // 判断构建
        boolean releaseMethod = buildService.checkReleaseMethod(id, request, BuildReleaseMethod.Outgiving);
        Assert.state(!releaseMethod, "当前分发存在构建项，不能解绑");

        int byKey = outGivingServer.delByKey(id, request);
        if (byKey > 0) {
            // 删除日志
            dbOutGivingLogService.delByOutGivingId(id);
        }
        return ApiResult.success("操作成功");
    }
}
