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

package io.voyager1.controller.build;

import io.voyager1.util.FileUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.build.*;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Const;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorConfig;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.model.BaseEnum;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.enums.BuildStatus;
import io.voyager1.model.log.BuildHistoryLog;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.dblog.BuildInfoService;
import io.voyager1.service.dblog.DbBuildHistoryLogService;
import io.voyager1.service.system.WorkspaceEnvVarService;
import io.voyager1.util.FileUtils;
import io.voyager1.util.StringUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * new build info manage controller
 * ` *
 *
 * @since 2021-08-23
 */
@RestController
@Feature(cls = ClassFeature.BUILD)
public class BuildInfoManageController extends BaseServerController {

    private final BuildInfoService buildInfoService;
    private final DbBuildHistoryLogService dbBuildHistoryLogService;
    private final BuildExecuteService buildExecuteService;
    private final WorkspaceEnvVarService workspaceEnvVarService;

    public BuildInfoManageController(BuildInfoService buildInfoService,
                                     DbBuildHistoryLogService dbBuildHistoryLogService,
                                     BuildExecuteService buildExecuteService,
                                     WorkspaceEnvVarService workspaceEnvVarService) {
        this.buildInfoService = buildInfoService;
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
        this.buildExecuteService = buildExecuteService;
        this.workspaceEnvVarService = workspaceEnvVarService;
    }

    /**
     * 开始构建
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "/build/manage/start", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<Integer> start(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String id,
                                       String buildRemark,
                                       String resultDirFile,
                                       String branchName,
                                       String branchTagName,
                                       String checkRepositoryDiff,
                                       String projectSecondaryDirectory,
                                       String buildEnvParameter,
                                       String dispatchSelectProject,
                                       HttpServletRequest request) {
        BuildInfoModel item = buildInfoService.getByKey(id, request);
        Assert.notNull(item, "没有对应数据");
        // 更新数据
        BuildInfoModel update = new BuildInfoModel();
        Opt.ofBlankAble(resultDirFile).ifPresent(s -> {
            ResultDirFileAction parse = ResultDirFileAction.parse(s);
            parse.check();
            update.setResultDirFile(s);
        });
        if (branchName != null && !branchName.isEmpty()) update.setBranchName(branchName);
        if (branchTagName != null && !branchTagName.isEmpty()) update.setBranchTagName(branchTagName);
        Opt.ofBlankAble(projectSecondaryDirectory).ifPresent(s -> {
            FileUtils.checkSlip(s, e -> new IllegalArgumentException("二级目录不能越级：" + e.getMessage()));
            //
            String extraData = item.getExtraData();
            JSONObject jsonObject = JSONObject.parseObject(extraData);
            jsonObject.put("projectSecondaryDirectory", s);
            update.setExtraData(jsonObject.toString());
        });
        // 会存在清空的情况
        update.setBuildEnvParameter(Optional.ofNullable(buildEnvParameter).orElse(""));
        update.setId(id);
        buildInfoService.updateById(update);
        // userModel
        UserModel userModel = getUser();
        Object[] parametersEnv = (dispatchSelectProject != null && !dispatchSelectProject.isEmpty()) ? new Object[]{"dispatchSelectProject", dispatchSelectProject} : new Object[]{};
        // 执行构建
        return buildExecuteService.start(item.getId(), userModel, null, 0, buildRemark, checkRepositoryDiff, parametersEnv);
    }

    /**
     * 取消构建
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "/build/manage/cancel", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> cancel(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据")) String id, HttpServletRequest request) {
        BuildInfoModel item = buildInfoService.getByKey(id, request);
        Objects.requireNonNull(item, "没有对应数据");
        String checkStatus = buildExecuteService.checkStatus(item);
        BuildStatus nowStatus = BaseEnum.getEnum(BuildStatus.class, item.getStatus());
        Objects.requireNonNull(nowStatus);
        if (checkStatus == null) {
            return ApiResult.success("当前状态不在进行中," + nowStatus.getDesc());
        }
        boolean status = BuildExecuteManage.cancelTaskById(item.getId());
        if (!status) {
            // 缓存中可能不存在数据,还是需要执行取消
            buildInfoService.updateStatus(id, BuildStatus.Cancel, "手动取消");
        }
        return ApiResult.success("取消成功");
    }

    /**
     * 获取可用环境变量
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "/build/manage/environment", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> environment(String id, Integer buildMode, HttpServletRequest request) {
        BuildInfoModel item = buildInfoService.getByKey(id, request);
        EnvironmentMapBuilder environmentMapBuilder;
        // 解析变量
        if (item != null) {
            environmentMapBuilder = workspaceEnvVarService.getEnv(item.getWorkspaceId());
            environmentMapBuilder.putStr(StringUtil.parseEnvStr(item.getBuildEnvParameter()));
            //
            buildMode = item.getBuildMode();
            if (buildMode != null && buildMode == 1) {
                // 容器中的环境变量
                String script = item.getScript();
                DockerYmlDsl dockerYmlDsl = DockerYmlDsl.build(script);
                Map<String, String> dockerEnv = (dockerYmlDsl.getEnv() != null ? dockerYmlDsl.getEnv() : new HashMap<>(0));
                environmentMapBuilder.putStr(dockerEnv);
            }
            //
            BuildExecuteManage.appendBuildDefaultEnv(environmentMapBuilder, item);
        } else {
            String workspace = buildInfoService.getCheckUserWorkspace(request);
            environmentMapBuilder = workspaceEnvVarService.getEnv(workspace);
        }
        // 获取系统变量
        if (buildMode != null && buildMode == 0) {
            ProcessBuilder processBuilder = new ProcessBuilder();
            Map<String, String> environment = processBuilder.environment();
            environment.forEach(environmentMapBuilder::putSystem);
        }
        Map<String, EnvironmentMapBuilder.Item> data = environmentMapBuilder.clonePrivacyData();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("data", data);
        jsonObject.put("privacyVariableKeywords", Const.PRIVACY_VARIABLE_KEYWORDS);
        return ApiResult.success("", jsonObject);
    }

    /**
     * 重新发布
     *
     * @param logId logId
     * @return json
     */
    @RequestMapping(value = "/build/manage/reRelease", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<Integer> reRelease(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String logId,
                                           HttpServletRequest request) {
        String workspaceId = dbBuildHistoryLogService.getCheckUserWorkspace(request);
        BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(logId, false, entity -> entity.set("workspaceId", workspaceId));
        Objects.requireNonNull(buildHistoryLog, "没有对应构建记录.");
        BuildInfoModel item = buildInfoService.getByKey(buildHistoryLog.getBuildDataId(), request);
        Objects.requireNonNull(item, "没有对应数据");
        int buildId = buildExecuteService.rollback(buildHistoryLog, item, getUser());
        return ApiResult.success("重新发布中", buildId);
    }

    /**
     * 获取构建的日志
     *
     * @param id      id
     * @param buildId 构建编号
     * @param line    需要获取的行号
     * @return json
     */
    @RequestMapping(value = "/build/manage/get-now-log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> getNowLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String id,
                                              @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "没有buildId") int buildId,
                                              @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "行号错误") int line,
                                              HttpServletRequest request) {
        BuildInfoModel item = buildInfoService.getByKey(id, request);
        Assert.notNull(item, "没有对应数据");
        Assert.state(buildId <= item.getBuildId(), "还没有对应的构建记录");

        BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
        buildHistoryLog.setBuildDataId(id);
        buildHistoryLog.setBuildNumberId(buildId);
        BuildHistoryLog queryByBean = dbBuildHistoryLogService.queryByBean(buildHistoryLog);
        Assert.notNull(queryByBean, "没有对应的构建历史");

        File file = BuildUtil.getLogFile(item.getId(), buildId);
        Assert.state(FileUtil.isFile(file), "日志文件不存在或者错误");

        if (!file.exists()) {
            if (buildId == item.getBuildId()) {
                return new ApiResult<>(201, "还没有日志文件");
            }
            return new ApiResult<>(300, "日志文件不存在");
        }
        JSONObject data = FileUtils.readLogFile(file, line);
        // 运行中
        Integer status = queryByBean.getStatus();
        data.put("run", buildExecuteService.checkStatus(item) != null);
        data.put("logId", queryByBean.getId());
        data.put("status", status);
        data.put("statusMsg", queryByBean.getStatusMsg());
        data.put("environment", queryByBean.toEnvironmentMapBuilder().clonePrivacyData());
        // 构建中
        //data.put("buildRun", status == BuildStatus.Ing.getCode());
        return ApiResult.success("", data);
    }
}
