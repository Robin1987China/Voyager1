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

package io.voyager1.build;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.BaseEnum;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.data.RepositoryModel;
import io.voyager1.model.enums.BuildStatus;
import io.voyager1.model.log.BuildHistoryLog;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.dblog.BuildInfoService;
import io.voyager1.service.dblog.DbBuildHistoryLogService;
import io.voyager1.service.dblog.RepositoryService;
import io.voyager1.service.system.WorkspaceEnvVarService;
import io.voyager1.util.LogRecorder;
import io.voyager1.util.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Objects;

/**
 * @since 2022/1/26
 */
@Service
@Slf4j
public class BuildExecuteService {


    private final BuildInfoService buildService;
    private final DbBuildHistoryLogService dbBuildHistoryLogService;
    private final RepositoryService repositoryService;
    private final WorkspaceEnvVarService workspaceEnvVarService;
    private final BuildExecutorPoolService buildExecutorPoolService;
    private final io.voyager1.service.version.VersionService versionService;

    public BuildExecuteService(BuildInfoService buildService,
                               DbBuildHistoryLogService dbBuildHistoryLogService,
                               RepositoryService repositoryService,
                               WorkspaceEnvVarService workspaceEnvVarService,
                               BuildExecutorPoolService buildExecutorPoolService,
                               io.voyager1.service.version.VersionService versionService) {
        this.buildService = buildService;
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
        this.repositoryService = repositoryService;
        this.workspaceEnvVarService = workspaceEnvVarService;
        this.buildExecutorPoolService = buildExecutorPoolService;
        this.versionService = versionService;
    }


    /**
     * check status
     *
     * @param buildInfoModel 构建信息
     * @return 错误消息
     */
    public String checkStatus(BuildInfoModel buildInfoModel) {
        if (buildInfoModel == null) {
            return "不存在对应的构建信息";
        }
        Integer status = buildInfoModel.getStatus();
        if (status == null) {
            return null;
        }
        BuildStatus nowStatus = BaseEnum.getEnum(BuildStatus.class, status);
        Objects.requireNonNull(nowStatus);
        if (nowStatus.isProgress()) {
            return buildInfoModel.getName() + " 当前还在：" + nowStatus.getDesc();
        }
        return null;
    }

    /**
     * start build
     *
     * @param buildInfoId      构建Id
     * @param userModel        用户信息
     * @param delay            延迟的时间
     * @param triggerBuildType 触发构建类型
     * @param buildRemark      构建备注
     * @param parametersEnv    外部环境变量
     * @return json
     */
    public ApiResult<Integer> start(String buildInfoId, UserModel userModel, Integer delay, int triggerBuildType, String buildRemark, Object... parametersEnv) {
        Object[] env = parametersEnv == null ? new Object[0] : parametersEnv;
        return this.start(buildInfoId, userModel, delay, triggerBuildType, buildRemark, null, env);
    }

    /**
     * start build
     *
     * @param buildInfoId         构建Id
     * @param userModel           用户信息
     * @param delay               延迟的时间
     * @param triggerBuildType    触发构建类型
     * @param buildRemark         构建备注
     * @param checkRepositoryDiff 差异构建
     * @param parametersEnv       外部环境变量
     * @return json
     */
    public ApiResult<Integer> start(String buildInfoId, UserModel userModel, Integer delay,
                                       int triggerBuildType, String buildRemark, String checkRepositoryDiff,
                                       Object... parametersEnv) {
        synchronized (buildInfoId.intern()) {
            BuildInfoModel buildInfoModel = buildService.getByKey(buildInfoId);
            String e = this.checkStatus(buildInfoModel);
            Assert.isNull(e, () -> e);
            // CI 冻结：自动触发（WebHook/cron）且应用存在已提测版本时挂起
            if ((triggerBuildType == 1 || triggerBuildType == 2)
                && versionService.hasSubmittedVersion(buildInfoId)) {
                return new ApiResult<>(405, "该应用存在已提测版本，CI 已冻结，请打回或发布后再构建");
            }
            //
            boolean containsKey = BuildExecuteManage.BUILD_MANAGE_MAP.containsKey(buildInfoModel.getId());
            Assert.state(!containsKey, "当前构建还在进行中");
            //
            BuildExtraModule buildExtraModule = buildInfoModel.extraData();
            Assert.notNull(buildExtraModule, "构建信息缺失");
            // load repository
            RepositoryModel repositoryModel = repositoryService.getByKey(buildInfoModel.getRepositoryId(), false);
            Assert.notNull(repositoryModel, "仓库信息不存在");
            EnvironmentMapBuilder environmentMapBuilder = workspaceEnvVarService.getEnv(buildInfoModel.getWorkspaceId());
            // 解析外部变量
            environmentMapBuilder.putObjectArray(parametersEnv).putStr(StringUtil.parseEnvStr(buildInfoModel.getBuildEnvParameter()));
            // set buildId field
            buildInfoModel.setBuildId(this.nextBuildId(buildInfoModel));
            //
            TaskData.TaskDataBuilder taskBuilder = TaskData.builder()
                .buildInfoModel(buildInfoModel)
                .repositoryModel(repositoryModel)
                .userModel(userModel)
                .buildRemark(buildRemark)
                .delay(delay)
                .environmentMapBuilder(environmentMapBuilder)
                .triggerBuildType(triggerBuildType);
            //
            Opt.ofBlankAble(checkRepositoryDiff).map(ConvertUtil::toBool).ifPresent(taskBuilder::checkRepositoryDiff);
            this.runTask(taskBuilder.build(), buildExtraModule);
            String startMsg = "开始构建中";
            String delayMsg = String.format("延迟 %s 秒后开始构建", delay);
            String msg = (delay == null || delay <= 0) ? startMsg : delayMsg;
            return ApiResult.success(msg, buildInfoModel.getBuildId());
        }
    }

    /**
     * 回滚
     *
     * @param oldLog    构建历史
     * @param item      构建项
     * @param userModel 用户信息
     */
    public int rollback(BuildHistoryLog oldLog, BuildInfoModel item, UserModel userModel) {
        synchronized (item.getId().intern()) {
            String e = this.checkStatus(item);
            Assert.isNull(e, () -> e);
            Integer fromBuildNumberId = (oldLog.getFromBuildNumberId() != null ? oldLog.getFromBuildNumberId() : oldLog.getBuildNumberId());
            int buildId = this.nextBuildId(item);
            item.setBuildId(buildId);
            // 创建新的构建记录
            BuildHistoryLog buildHistoryLog = oldLog.toJson().to(BuildHistoryLog.class);
            buildHistoryLog.setId(null);
            buildHistoryLog.setCreateUser(null);
            buildHistoryLog.setCreateTimeMillis(null);
            buildHistoryLog.setModifyUser(null);
            buildHistoryLog.setModifyTimeMillis(null);
            buildHistoryLog.setResultFileSize(null);
            BuildStatus pubIng = BuildStatus.PubIng;
            buildHistoryLog.setStatus(pubIng.getCode());
            buildHistoryLog.setTriggerBuildType(3);
            buildHistoryLog.setBuildNumberId(buildId);
            buildHistoryLog.setFromBuildNumberId(fromBuildNumberId);
            buildHistoryLog.setStartTime(System.currentTimeMillis());
            buildHistoryLog.setEndTime(null);
            dbBuildHistoryLogService.insert(buildHistoryLog);
            //
            buildService.updateStatus(buildHistoryLog.getBuildDataId(), pubIng, "开始回滚执行");

            BuildExtraModule buildExtraModule = BuildExtraModule.build(buildHistoryLog);
            //
            EnvironmentMapBuilder environmentMapBuilder = buildHistoryLog.toEnvironmentMapBuilder();
            //
            File logFile = BuildUtil.getLogFile(item.getId(), buildId);
            LogRecorder logRecorder = LogRecorder.builder().file(logFile).build();
            ReleaseManage manage = ReleaseManage.builder()
                .buildExtraModule(buildExtraModule)
                .logId(buildHistoryLog.getId())
                .userModel(userModel)
                .buildNumberId(buildHistoryLog.getBuildNumberId())
                .fromBuildNumberId(fromBuildNumberId)
                .logRecorder(logRecorder)
                .buildEnv(environmentMapBuilder)
                .build();
            //
            logRecorder.system("开始准备回滚：{} -> {}", fromBuildNumberId, buildId);
            //
            buildExecutorPoolService.execute(() -> manage.rollback(item));
            return buildId;
        }
    }

    private int nextBuildId(BuildInfoModel buildInfoModel) {
        // set buildId field
        int buildId = (buildInfoModel.getBuildId() != null ? buildInfoModel.getBuildId() : 0);
        BuildInfoModel update = new BuildInfoModel();
        update.setBuildId(buildId + 1);
        update.setId(buildInfoModel.getId());
        buildService.updateById(update);
        return update.getBuildId();
    }

    /**
     * 创建构建
     *
     * @param taskData         任务
     * @param buildExtraModule 构建更多配置信息
     */
    private void runTask(TaskData taskData, BuildExtraModule buildExtraModule) {
        String logId = this.insertLog(buildExtraModule, taskData);
        //
        BuildExecuteManage.BuildExecuteManageBuilder builder = BuildExecuteManage.builder()
            .taskData(taskData)
            .logId(logId)
            .buildExtraModule(buildExtraModule);
        builder.build().submitTask();
    }


    /**
     * 插入记录
     */
    private String insertLog(BuildExtraModule buildExtraModule, TaskData taskData) {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        buildExtraModule.updateValue(buildInfoModel);
        BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
        // 更新其他配置字段
        //buildHistoryLog.fillLogValue(buildExtraModule);
        buildHistoryLog.setTriggerBuildType(taskData.triggerBuildType);
        //
        buildHistoryLog.setBuildNumberId(buildInfoModel.getBuildId());
        buildHistoryLog.setBuildName(buildInfoModel.getName());
        buildHistoryLog.setBuildDataId(buildInfoModel.getId());
        buildHistoryLog.setWorkspaceId(buildInfoModel.getWorkspaceId());
        buildHistoryLog.setResultDirFile(buildInfoModel.getResultDirFile());
        buildHistoryLog.setReleaseMethod(buildExtraModule.getReleaseMethod());
        //
        BuildStatus waitExec = BuildStatus.WaitExec;
        buildHistoryLog.setStatus(waitExec.getCode());
        buildHistoryLog.setStartTime(System.currentTimeMillis());
        buildHistoryLog.setBuildRemark(taskData.buildRemark);
        // 缓存数据 - 保证数据一直
        buildHistoryLog.setExtraData(buildExtraModule.toJson().toString());
        dbBuildHistoryLogService.insert(buildHistoryLog);
        //
        buildService.updateStatus(buildHistoryLog.getBuildDataId(), waitExec, "开始排队等待执行");
        return buildHistoryLog.getId();
    }

    /**
     * 更新状态
     *
     * @param buildId     构建ID
     * @param logId       记录ID
     * @param buildStatus to status
     */
    public void updateStatus(String buildId, String logId, int buildNumberId, BuildStatus buildStatus, String msg) {
        BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
        buildHistoryLog.setId(logId);
        buildHistoryLog.setStatusMsg(msg);
        buildHistoryLog.setStatus(buildStatus.getCode());
        if (!buildStatus.isProgress()) {
            // 结束
            buildHistoryLog.setEndTime(System.currentTimeMillis());
        }
        dbBuildHistoryLogService.updateById(buildHistoryLog);
        buildService.updateStatus(buildId, buildNumberId, buildStatus, msg);
    }


}
