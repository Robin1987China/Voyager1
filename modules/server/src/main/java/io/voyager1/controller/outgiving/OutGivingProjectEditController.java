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
import io.voyager1.util.Tuple;
import io.voyager1.util.Validator;
import io.voyager1.util.EnumUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Const;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.model.AfterOpt;
import io.voyager1.model.BaseEnum;
import io.voyager1.model.RunMode;
import io.voyager1.model.data.AgentWhitelist;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.data.ServerWhitelist;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 节点分发编辑项目
 *
 * @since 2019/4/22
 */
@RestController
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING)
@Slf4j
public class OutGivingProjectEditController extends BaseServerController {

    private final OutGivingWhitelistService outGivingWhitelistService;
    private final OutGivingServer outGivingServer;
    private final ProjectInfoCacheService projectInfoCacheService;
    private final BuildInfoService buildService;
    private final DbOutGivingLogService dbOutGivingLogService;

    public OutGivingProjectEditController(OutGivingWhitelistService outGivingWhitelistService,
                                          OutGivingServer outGivingServer,
                                          ProjectInfoCacheService projectInfoCacheService,
                                          BuildInfoService buildService,
                                          DbOutGivingLogService dbOutGivingLogService) {
        this.outGivingWhitelistService = outGivingWhitelistService;
        this.outGivingServer = outGivingServer;
        this.projectInfoCacheService = projectInfoCacheService;
        this.buildService = buildService;
        this.dbOutGivingLogService = dbOutGivingLogService;
    }

    /**
     * 保存节点分发项目
     *
     * @param id   id
     * @param type 类型
     * @return json
     */
    @RequestMapping(value = "save_project", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> save(@ValidatorItem String id, String type, HttpServletRequest request) {
        if ("add".equalsIgnoreCase(type)) {
            //boolean general = StringUtil.isGeneral(id, 2, 20);
            //Assert.state(general, "分发id 不能为空并且长度在2-20（英文字母 、数字和下划线）");
            String checkId = id.replace("-", "_");
            Validator.validateGeneral(checkId, 2, Const.ID_MAX_LEN, "分发id 不能为空并且长度在2-20（英文字母 、数字和下划线）");
            return addOutGiving(id, request);
        } else {
            return updateGiving(id, request);
        }
    }

    /**
     * 删除分发项目
     *
     * @param id 项目id
     * @return json
     */
    @RequestMapping(value = "delete_project", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> delete(String id, String thorough, HttpServletRequest request) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingModel, "没有对应的分发项目");

        // 判断构建
        boolean releaseMethod = buildService.checkReleaseMethod(id, request, BuildReleaseMethod.Outgiving);
        Assert.state(!releaseMethod, "当前分发存在构建项，不能直接删除（需要提前解绑或者删除关联数据后才能删除）");
        //
        Assert.state(outGivingModel.outGivingProject(), "该项目不是节点分发项目,不能在此次删除");

        List<OutGivingNodeProject> deleteNodeProject = outGivingModel.outGivingNodeProjectList();
        if (deleteNodeProject != null) {
            // 删除实际的项目
            for (OutGivingNodeProject outGivingNodeProject1 : deleteNodeProject) {
                NodeModel nodeModel = nodeService.getByKey(outGivingNodeProject1.getNodeId());
                ApiResult<String> jsonMessage = this.deleteNodeProject(nodeModel, outGivingNodeProject1.getProjectId(), thorough);
                if (!jsonMessage.success()) {
                    return new ApiResult<>(406, nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
                }
            }
        }

        int byKey = outGivingServer.delByKey(id, request);
        // 删除日志
        if (byKey > 0) {
            dbOutGivingLogService.delByOutGivingId(id);
        }
        return ApiResult.success("删除成功");
    }

    private ApiResult<String> addOutGiving(String id, HttpServletRequest request) {
        // 全局判断 id
        OutGivingModel outGivingModel = outGivingServer.getByKey(id);
        Assert.isNull(outGivingModel, "分发id已经存在啦");

        outGivingModel = new OutGivingModel();
        outGivingModel.setOutGivingProject(true);
        outGivingModel.setId(id);
        //
        List<Tuple> tuples = doData(outGivingModel, false, request);

        outGivingServer.insert(outGivingModel);
        ApiResult<String> error = saveNodeData(outGivingModel, tuples, false);
        return Optional.ofNullable(error).orElseGet(() -> ApiResult.success("添加成功"));
    }


    private ApiResult<String> updateGiving(String id, HttpServletRequest request) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingModel, "没有找到对应的分发id");
        List<Tuple> tuples = doData(outGivingModel, true, request);

        outGivingServer.updateById(outGivingModel);
        ApiResult<String> error = saveNodeData(outGivingModel, tuples, true);
        return Optional.ofNullable(error).orElseGet(() -> ApiResult.success("修改成功"));
    }

    /**
     * 保存节点项目数据
     *
     * @param outGivingModel 节点分发项目
     * @param edit           是否为编辑模式
     * @return 错误信息
     */
    private ApiResult<String> saveNodeData(OutGivingModel outGivingModel, List<Tuple> tuples, boolean edit) {

//		if () {
//			if (!edit) {
//				outGivingServer.delByKey(outGivingModel.getId());
//			}
//			return ApiResult.getString(405, "数据异常,请重新操作");
//		}
        List<Tuple> success = new ArrayList<>();
        boolean fail = false;
        try {
            for (Tuple tuple : tuples) {
                NodeModel nodeModel = tuple.get(0);
                JSONObject data = tuple.get(1);
                //
                ApiResult<String> jsonMessage = this.sendData(nodeModel, data, true);
                if (!jsonMessage.success()) {
                    if (!edit) {
                        fail = true;
                        outGivingServer.delByKey(outGivingModel.getId());
                    }
                    return new ApiResult<>(406, nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
                }
                success.add(tuple);
                // 同步项目信息
                projectInfoCacheService.syncNode(nodeModel, outGivingModel.getId());
            }
        } catch (Exception e) {
            log.error("保存分发项目失败", e);
            if (!edit) {
                fail = true;
                outGivingServer.delByKey(outGivingModel.getId());
            }
            return new ApiResult<>(500, "保存节点数据失败:" + e.getMessage());
        } finally {
            if (fail) {
                try {
                    for (Tuple entry : success) {
                        deleteNodeProject(entry.get(0), outGivingModel.getId(), null);
                    }
                } catch (Exception e) {
                    log.error("还原项目失败", e);
                }
            }
        }
        return null;
    }

    /**
     * 删除项目
     *
     * @param nodeModel 节点
     * @param project   判断id
     * @param thorough  是否彻底删除
     * @return json
     */
    private ApiResult<String> deleteNodeProject(NodeModel nodeModel, String project, String thorough) {
        JSONObject data = new JSONObject();
        data.put("id", project);
        data.put("thorough", thorough);
        ApiResult<String> request = NodeForward.request(nodeModel, NodeUrl.Manage_DeleteProject, data);
        if (request.success()) {
            // 同步项目信息
            projectInfoCacheService.syncNode(nodeModel, project);
        }
        return request;
//        // 发起预检查数据
//        String url = nodeModel.getRealUrl(NodeUrl.Manage_DeleteProject);
//        HttpRequest request = HttpUtil.createPost(url);
//        // 授权信息
//        NodeForward.addUser(request, nodeModel, userModel);

//        request.form(data);
//        //
//        String body = request.execute()
//                .body();
//        return NodeForward.toJsonMessage(body);
    }

    /**
     * 创建项目管理的默认数据
     *
     * @param outGivingModel 分发实体
     * @param edit           是否为编辑模式
     * @return String为有异常
     */
    private JSONObject getDefData(OutGivingModel outGivingModel, boolean edit, HttpServletRequest request) {
        JSONObject defData = new JSONObject();
        defData.put("id", outGivingModel.getId());
        defData.put("name", outGivingModel.getName());
        defData.put("group", outGivingModel.getGroup());
        //
        defData.put("logCharset", getParameter("logCharset"));
        // 运行模式
        String runMode = getParameter("runMode");
        RunMode runMode1 = EnumUtil.fromString(RunMode.class, runMode, RunMode.ClassPath);
        defData.put("runMode", runMode1.name());
        if (runMode1 == RunMode.ClassPath || runMode1 == RunMode.JavaExtDirsCp) {
            String mainClass = getParameter("mainClass");
            defData.put("mainClass", mainClass);
        }
        if (runMode1 == RunMode.JavaExtDirsCp) {
            defData.put("javaExtDirsCp", getParameter("javaExtDirsCp"));
        }
        if (runMode1 == RunMode.Dsl) {
            defData.put("dslContent", getParameter("dslContent"));
        }
        String whitelistDirectory = getParameter("whitelistDirectory");
        ServerWhitelist configDeNewInstance = outGivingWhitelistService.getServerWhitelistData(request);
        List<String> whitelistServerOutGiving = configDeNewInstance.getOutGiving();
        Assert.state(AgentWhitelist.checkPath(whitelistServerOutGiving, whitelistDirectory), "请选择正确的项目路径,或者还没有配置授权");

        defData.put("whitelistDirectory", whitelistDirectory);
        String logPath = getParameter("logPath");
        if ((logPath != null && !logPath.isEmpty())) {
            Assert.state(AgentWhitelist.checkPath(whitelistServerOutGiving, logPath), "请选择正确的日志路径,或者还没有配置授权");
            defData.put("logPath", logPath);
        }
        String lib = getParameter("lib");
        defData.put("lib", lib);
        if (edit) {
            // 编辑模式
            defData.put("edit", "on");
        }
        defData.put("previewData", true);
        return defData;
    }

    /**
     * 处理页面数据
     *
     * @param outGivingModel 分发实体
     * @param edit           是否为编辑模式
     */
    private List<Tuple> doData(OutGivingModel outGivingModel, boolean edit, HttpServletRequest request) {
        outGivingModel.setName(getParameter("name"));
        outGivingModel.setGroup(getParameter("group"));
        Assert.hasText(outGivingModel.getName(), "分发名称不能为空");
        //
        int intervalTime = getParameterInt("intervalTime", 10);
        outGivingModel.setIntervalTime(intervalTime);
        outGivingModel.setClearOld(ConvertUtil.toBool(getParameter("clearOld"), false));
        //
        String nodeIdsStr = getParameter("nodeIds");
        List<String> nodeIds = io.voyager1.util.ConvertUtil.splitTrim(nodeIdsStr, ",");
        //List<NodeModel> nodeModelList = nodeService.listByWorkspace(request);
        Assert.notEmpty(nodeIds, "没有任何节点信息");

        //
        String afterOpt = getParameter("afterOpt");
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, ConvertUtil.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择分发后的操作");
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        JSONObject defData = getDefData(outGivingModel, edit, request);

        //
        List<OutGivingModel> outGivingModels = outGivingServer.list();
        List<OutGivingNodeProject> outGivingNodeProjects = new ArrayList<>();
        OutGivingNodeProject outGivingNodeProject;

        List<Tuple> tuples = new ArrayList<>();

        for (String nodeId : nodeIds) {
            NodeModel nodeModel = nodeService.getByKey(nodeId);
            Assert.notNull(nodeModel, "对应的节点不存在");
            //String add = getParameter("add_" + nodeModel.getId());
//			if (!nodeModel.getId().equals(add)) {
//				iterator.remove();
//				continue;
//			}
            // 判断项目是否已经被使用过啦
            if (outGivingModels != null) {
                for (OutGivingModel outGivingModel1 : outGivingModels) {
                    if (outGivingModel1.getId().equalsIgnoreCase(outGivingModel.getId())) {
                        continue;
                    }
                    Assert.state(!outGivingModel1.checkContains(nodeModel.getId(), outGivingModel.getId()), "已经存在相同的分发项目:" + outGivingModel.getId());

                }
            }
            outGivingNodeProject = outGivingModel.getNodeProject(nodeModel.getId(), outGivingModel.getId());
            if (outGivingNodeProject == null) {
                outGivingNodeProject = new OutGivingNodeProject();
            }
            outGivingNodeProject.setNodeId(nodeModel.getId());
            // 分发id为项目id
            outGivingNodeProject.setProjectId(outGivingModel.getId());
            outGivingNodeProjects.add(outGivingNodeProject);
            // 检查数据
            JSONObject allData = defData.clone();
            String token = getParameter(String.format("%s_token", nodeModel.getId()));
            allData.put("token", token);
            String jvm = getParameter(String.format("%s_jvm", nodeModel.getId()));
            allData.put("jvm", jvm);
            String args = getParameter(String.format("%s_args", nodeModel.getId()));
            allData.put("args", args);
            String autoStart = getParameter(String.format("%s_autoStart", nodeModel.getId()));
            String disableScanDir = getParameter(String.format("%s_disableScanDir", nodeModel.getId()));
            allData.put("autoStart", ConvertUtil.toBool(autoStart, false));
            allData.put("disableScanDir", ConvertUtil.toBool(disableScanDir, false));
            allData.put("dslEnv", getParameter(String.format("%s_dslEnv", nodeModel.getId())));
            allData.put("nodeId", nodeModel.getId());
            ApiResult<String> jsonMessage = this.sendData(nodeModel, allData, false);
            Assert.state(jsonMessage.success(), nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
            tuples.add(new Tuple(nodeModel, allData));
        }
        // 删除已经删除的项目
        deleteProject(outGivingModel, outGivingNodeProjects);

        outGivingModel.outGivingNodeProjectList(outGivingNodeProjects);
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
        return tuples;
    }

    /**
     * 删除已经删除过的项目
     *
     * @param outGivingModel        分发项目
     * @param outGivingNodeProjects 新的节点项目
     */
    private void deleteProject(OutGivingModel outGivingModel, List<OutGivingNodeProject> outGivingNodeProjects) {
        Assert.state((outGivingNodeProjects == null ? 0 : outGivingNodeProjects.size()) >= 1, "至少选择一个节点");
        // 删除
        List<OutGivingNodeProject> deleteNodeProject = outGivingModel.getDelete(outGivingNodeProjects);
        if (deleteNodeProject != null) {
            ApiResult<String> jsonMessage;
            // 删除实际的项目
            for (OutGivingNodeProject outGivingNodeProject1 : deleteNodeProject) {
                NodeModel nodeModel = nodeService.getByKey(outGivingNodeProject1.getNodeId());
                //outGivingNodeProject1.getNodeData(true);
                // 调用彻底删除
                jsonMessage = this.deleteNodeProject(nodeModel, outGivingNodeProject1.getProjectId(), "thorough");
                Assert.state(jsonMessage.success(), nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
            }
        }
    }

    private ApiResult<String> sendData(NodeModel nodeModel, JSONObject data, boolean save) {
        if (save) {
            data.remove("previewData");
        }
        data.put("outGivingProject", true);
        // 发起预检查数据
        return NodeForward.request(nodeModel, NodeUrl.Manage_SaveProject, data);
    }
}
