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

package io.voyager1.service.pipeline;


import io.voyager1.core.api.ApiResult;
import io.voyager1.util.CollUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.Tuple;
import io.voyager1.util.StrUtil;
import io.voyager1.common.SpringContextHolder;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.build.BuildExecuteService;
import io.voyager1.build.BuildExtraModule;
import io.voyager1.build.ReleaseManage;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.data.PipelineConfigModel;
import io.voyager1.model.data.PipelineExecuteRecordModel;
import io.voyager1.model.data.VersionModel;
import io.voyager1.model.enums.BuildStatus;
import io.voyager1.model.enums.PipelineExecuteStatus;
import io.voyager1.model.log.BuildHistoryLog;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.dblog.BuildInfoService;
import io.voyager1.service.dblog.DbBuildHistoryLogService;
import io.voyager1.service.version.VersionService;
import io.voyager1.util.CommandUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pipeline 执行引擎（阶段编排：build/exec/publish/approval）
 *
 * @since 2026/8/7
 */
@Service
@Slf4j
public class PipelineExecutorService {

    private final PipelineConfigService pipelineConfigService;
    private final PipelineExecuteRecordService executeRecordService;
    private final BuildExecuteService buildExecuteService;
    private final BuildInfoService buildInfoService;
    private final DbBuildHistoryLogService dbBuildHistoryLogService;
    private final VersionService versionService;

    public PipelineExecutorService(PipelineConfigService pipelineConfigService,
                                   PipelineExecuteRecordService executeRecordService,
                                   BuildExecuteService buildExecuteService,
                                   BuildInfoService buildInfoService,
                                   DbBuildHistoryLogService dbBuildHistoryLogService,
                                   VersionService versionService) {
        this.pipelineConfigService = pipelineConfigService;
        this.executeRecordService = executeRecordService;
        this.buildExecuteService = buildExecuteService;
        this.buildInfoService = buildInfoService;
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
        this.versionService = versionService;
    }

    /**
     * 触发 Pipeline 执行（异步）
     *
     * @param pipelineId  配置 id
     * @param triggerType 触发类型
     * @param operator    触发人
     */
    public void trigger(String pipelineId, String triggerType, String operator) {
        PipelineConfigModel config = pipelineConfigService.getByKey(pipelineId);
        Assert.notNull(config, "Pipeline 配置不存在: " + pipelineId);
        Assert.state(config.getEnabled() != null && config.getEnabled(), "Pipeline 未启用: " + pipelineId);

        PipelineExecuteRecordModel record = PipelineExecuteRecordModel.builder()
            .pipelineId(pipelineId)
            .triggerType(triggerType)
            .status(PipelineExecuteStatus.Wait.getCode())
            .operator(operator)
            .startTime(System.currentTimeMillis())
            .build();
        record.setId(java.util.UUID.randomUUID().toString());
        executeRecordService.insert(record);
        executeRecordService.updateStages(record.getId(), config.getStages(), PipelineExecuteStatus.Wait);

        // 异步执行（透传触发人作为操作者上下文）
        ThreadUtilExecute(() -> {
            try {
                UserModel operatorUser = new UserModel();
                operatorUser.setName(operator);
                io.voyager1.common.BaseServerController.resetInfo(operatorUser);
                this.execute(record.getId(), config);
            } catch (Exception e) {
                log.error("Pipeline 执行异常: {} {}", record.getId(), e.getMessage(), e);
                executeRecordService.finish(record.getId(), PipelineExecuteStatus.Failed, null, "执行异常: " + e.getMessage());
            }
        });
    }

    /**
     * 顺序执行阶段
     */
    private void execute(String executeId, PipelineConfigModel config) {
        executeRecordService.updateStatus(executeId, PipelineExecuteStatus.Running);
        List<PipelineConfigService.PipelineStage> stages = pipelineConfigService.parseStages(config);
        Assert.state((stages != null && !stages.isEmpty()), "Pipeline 没有阶段");
        // 当前构建产物上下文（build 阶段产出，供 publish 消费）
        AtomicReference<String> artifactRef = new AtomicReference<>();
        AtomicReference<BuildInfoModel> buildInfoRef = new AtomicReference<>();
        AtomicReference<Integer> buildNumberRef = new AtomicReference<>();

        for (PipelineConfigService.PipelineStage stage : stages) {
            executeRecordService.updateCurrentStage(executeId, stage.getId());
            String type = (stage.getType() == null ? "" : stage.getType().toUpperCase());
            PipelineConfigService.StageType stageType = PipelineConfigService.StageType.valueOf(type);
            switch (stageType) {
                case BUILD:
                    this.runBuild(config, stage, executeId, artifactRef, buildInfoRef, buildNumberRef);
                    break;
                case EXEC:
                    this.runExec(config, stage, executeId);
                    break;
                case PUBLISH:
                    try {
                        this.runPublish(config, stage, executeId, artifactRef, buildInfoRef, buildNumberRef);
                    } catch (Exception e) {
                        // 发布失败：自动打回关联的已提测版本（解锁 CI）
                        this.autoReturnOnPublishFail(config, e);
                        throw e;
                    }
                    break;
                case APPROVAL:
                    this.runApproval(executeId, stage);
                    return;
                default:
                    throw new IllegalStateException("不支持的阶段类型: " + type);
            }
        }
        executeRecordService.finish(executeId, PipelineExecuteStatus.Success, null, "全部阶段执行完成");
    }

    /**
     * build 阶段：复用现有构建引擎
     */
    private void runBuild(PipelineConfigModel config, PipelineConfigService.PipelineStage stage,
                          String executeId, AtomicReference<String> artifactRef,
                          AtomicReference<BuildInfoModel> buildInfoRef, AtomicReference<Integer> buildNumberRef) {
        String buildId = this.getParam(stage, "buildId", config.getBuildId());
        ApiResult<Integer> result = buildExecuteService.start(buildId, null, null, 1, "pipeline 构建", new Object[0]);
        Assert.state(result.success(), "构建启动失败: " + result.getMsg());
        Integer buildNumberId = result.getData();
        Assert.notNull(buildNumberId, "构建启动失败");
        buildNumberRef.set(buildNumberId);
        BuildInfoModel buildInfoModel = buildInfoService.getByKey(buildId);
        buildInfoRef.set(buildInfoModel);
        // 等待构建完成（轮询）
        BuildStatus finalStatus = this.waitBuildFinish(buildId, buildNumberId);
        Assert.state(finalStatus == BuildStatus.Success || finalStatus == BuildStatus.PubSuccess,
            "构建未成功: " + finalStatus);
        String version = this.resolveVersion(buildInfoModel, buildNumberId);
        VersionModel versionModel = versionService.createVersion(buildId, buildNumberId, version, null, "pipeline 自动构建");
        artifactRef.set(versionModel.getId());
        executeRecordService.appendStageLog(executeId, stage.getId(), "构建完成: " + version);
        // 持久化构建结果（审批后 publish 消费）
        JSONObject buildResult = new JSONObject();
        buildResult.put("buildId", buildId);
        buildResult.put("buildNumberId", buildNumberId);
        buildResult.put("versionId", versionModel.getId());
        executeRecordService.updateStageResult(executeId, stage.getId(), buildResult.toJSONString());
    }

    /**
     * exec 阶段：执行命令
     */
    private void runExec(PipelineConfigModel config, PipelineConfigService.PipelineStage stage, String executeId) {
        String command = this.getParam(stage, "command", null);
        Assert.hasText(command, "exec 阶段缺少 command");
        String result = CommandUtil.execSystemCommand(command);
        executeRecordService.appendStageLog(executeId, stage.getId(), "命令执行结果: " + (result == null ? null : (result.length() <= 500 ? result : result.substring(0, 500))));
    }

    /**
     * publish 阶段：复用现有发布通道
     */
    private void runPublish(PipelineConfigModel config, PipelineConfigService.PipelineStage stage,
                            String executeId, AtomicReference<String> artifactRef,
                            AtomicReference<BuildInfoModel> buildInfoRef, AtomicReference<Integer> buildNumberRef) {
        String buildId = this.getParam(stage, "buildId", config.getBuildId());
        Integer buildNumberId = buildNumberRef.get();
        if (buildNumberId == null) {
            // 从配置参数读取
            String configNumber = this.getParam(stage, "buildNumberId", null);
            if ((configNumber != null && !configNumber.isEmpty())) {
                buildNumberId = Integer.parseInt(configNumber);
            }
        }
        if (buildNumberId == null) {
            // 从前面 build 阶段的持久化结果读取
            buildNumberId = this.resolveBuildNumber(executeId, config.getBuildId());
        }
        Assert.notNull(buildNumberId, "publish 阶段缺少构建记录编号（未找到构建结果）");
        BuildInfoModel buildInfoModel = buildInfoRef.get() != null ? buildInfoRef.get() : buildInfoService.getByKey(buildId);
        BuildHistoryLog historyLog = this.getHistoryLog(buildInfoModel, buildNumberId);
        Assert.notNull(historyLog, "构建记录不存在: " + buildId + " #" + buildNumberId);
        BuildExtraModule buildExtraModule = BuildExtraModule.build(historyLog);
        ReleaseManage manage = ReleaseManage.builder()
            .buildExtraModule(buildExtraModule)
            .logId(historyLog.getId())
            .userModel(this.currentOperator())
            .buildNumberId(buildNumberId)
            .fromBuildNumberId(buildNumberId)
            .logRecorder(io.voyager1.util.LogRecorder.builder().file(FileUtil.file("logs/pipeline", "publish-" + executeId + ".log")).build())
            .buildEnv(historyLog.toEnvironmentMapBuilder())
            .build();
        try {
            String msg = manage.start(null, buildInfoModel);
            Assert.isTrue((msg == null || msg.isEmpty()), "发布失败: " + msg);
        } catch (Exception e) {
            throw new IllegalStateException("发布异常: " + e.getMessage(), e);
        }
        String environment = this.getParam(stage, "environment", "dev");
        executeRecordService.appendStageLog(executeId, stage.getId(), "发布完成 environment=" + environment);
    }

    /**
     * approval 阶段：挂起等待审批
     */
    private void runApproval(String executeId, PipelineConfigService.PipelineStage stage) {
        executeRecordService.updateStatus(executeId, PipelineExecuteStatus.WaitApproval);
        executeRecordService.appendStageLog(executeId, stage.getId(), "等待审批: " + stage.getParams());
    }

    /**
     * 审批通过/拒绝后恢复执行
     *
     * @param executeId 执行记录 id
     * @param approve   是否通过
     * @param operator  操作者
     */
    public void approval(String executeId, boolean approve, String operator) {
        PipelineExecuteRecordModel record = executeRecordService.getByKey(executeId);
        Assert.notNull(record, "执行记录不存在: " + executeId);
        Assert.state(record.getStatus() == PipelineExecuteStatus.WaitApproval.getCode(),
            "当前状态不需要审批: " + executeId);
        if (!approve) {
            executeRecordService.updateStageStatus(executeId, record.getCurrentStage(), "denied");
            executeRecordService.finish(executeId, PipelineExecuteStatus.Cancel, null, "审批拒绝 by " + operator);
            return;
        }
        PipelineConfigModel config = pipelineConfigService.getByKey(record.getPipelineId());
        Assert.notNull(config, "Pipeline 配置不存在: " + record.getPipelineId());
        executeRecordService.updateStageStatus(executeId, record.getCurrentStage(), "approved");
        ThreadUtilExecute(() -> {
            try {
                // 跳过已通过的审批阶段，继续后续阶段
                this.continueAfterApproval(executeId, config, record);
            } catch (Exception e) {
                log.error("审批后继续执行异常: {}", executeId, e);
                executeRecordService.finish(executeId, PipelineExecuteStatus.Failed, null, "审批后执行异常: " + e.getMessage());
            }
        });
    }

    private void continueAfterApproval(String executeId, PipelineConfigModel config, PipelineExecuteRecordModel record) {
        // 从当前阶段之后继续（简化：重新从等待审批的阶段后续执行）
        List<PipelineConfigService.PipelineStage> stages = pipelineConfigService.parseStages(config);
        String currentStageId = record.getCurrentStage();
        int startIndex = 0;
        for (int i = 0; i < stages.size(); i++) {
            if (java.util.Objects.equals(stages.get(i).getId(), currentStageId)) {
                startIndex = i + 1;
                break;
            }
        }
        executeRecordService.updateStatus(executeId, PipelineExecuteStatus.Running);
        for (int i = startIndex; i < stages.size(); i++) {
            PipelineConfigService.PipelineStage stage = stages.get(i);
            executeRecordService.updateCurrentStage(executeId, stage.getId());
            PipelineConfigService.StageType stageType = PipelineConfigService.StageType.valueOf((stage.getType() == null ? "" : stage.getType().toUpperCase()));
            switch (stageType) {
                case EXEC:
                    this.runExec(config, stage, executeId);
                    break;
                case PUBLISH:
                    // 审批后的发布阶段：从配置读取构建编号（简化：跳过产物上下文）
                    this.runPublishFromConfig(config, stage, executeId);
                    break;
                case APPROVAL:
                    this.runApproval(executeId, stage);
                    return;
                default:
                    throw new IllegalStateException("审批后不支持阶段: " + stageType);
            }
        }
        executeRecordService.finish(executeId, PipelineExecuteStatus.Success, null, "全部阶段执行完成");
    }

    private void runPublishFromConfig(PipelineConfigModel config, PipelineConfigService.PipelineStage stage, String executeId) {
        String buildId = this.getParam(stage, "buildId", config.getBuildId());
        String configNumber = this.getParam(stage, "buildNumberId", null);
        Integer buildNumberId = (configNumber != null && !configNumber.isEmpty()) ? Integer.parseInt(configNumber) : null;
        if (buildNumberId == null) {
            buildNumberId = this.resolveBuildNumber(executeId, config.getBuildId());
        }
        Assert.notNull(buildNumberId, "publish 阶段缺少构建记录编号（未找到构建结果）");
        BuildInfoModel buildInfoModel = buildInfoService.getByKey(buildId);
        BuildHistoryLog historyLog = this.getHistoryLog(buildInfoModel, buildNumberId);
        Assert.notNull(historyLog, "构建记录不存在: " + buildId + " #" + buildNumberId);
        BuildExtraModule buildExtraModule = BuildExtraModule.build(historyLog);
        ReleaseManage manage = ReleaseManage.builder()
            .buildExtraModule(buildExtraModule)
            .logId(historyLog.getId())
            .userModel(this.currentOperator())
            .buildNumberId(buildNumberId)
            .fromBuildNumberId(buildNumberId)
            .logRecorder(io.voyager1.util.LogRecorder.builder().file(FileUtil.file("logs/pipeline", "publish-" + executeId + ".log")).build())
            .buildEnv(historyLog.toEnvironmentMapBuilder())
            .build();
        try {
            String msg = manage.start(null, buildInfoModel);
            Assert.isTrue((msg == null || msg.isEmpty()), "发布失败: " + msg);
        } catch (Exception e) {
            throw new IllegalStateException("发布异常: " + e.getMessage(), e);
        }
        String environment = this.getParam(stage, "environment", "dev");
        executeRecordService.appendStageLog(executeId, stage.getId(), "发布完成 environment=" + environment);
    }

    /**
     * 发布失败时自动打回已提测版本
     */
    private void autoReturnOnPublishFail(PipelineConfigModel config, Exception e) {
        try {
            String buildId = config.getBuildId();
            if ((buildId == null || buildId.isEmpty())) {
                return;
            }
            java.util.List<io.voyager1.model.data.VersionModel> versions = versionService.listByBuildId(buildId);
            for (io.voyager1.model.data.VersionModel v : versions) {
                if (v.getStatus() == io.voyager1.model.enums.VersionStatus.Submitted.getCode()) {
                    versionService.returnVersion(v.getId(), "发布失败自动打回: " + io.voyager1.util.StrUtil.maxLength(e.getMessage(), 200));
                    log.warn("发布失败自动打回版本: {} {}", v.getId(), v.getVersion());
                    break;
                }
            }
        } catch (Exception ex) {
            log.warn("自动打回失败: {}", ex.getMessage());
        }
    }

    /**
     * 从执行记录中解析 build 阶段产出的构建编号
     */
    private Integer resolveBuildNumber(String executeId, String buildId) {
        PipelineExecuteRecordModel record = executeRecordService.getByKey(executeId);
        if (record == null || (record.getStages() == null || record.getStages().isEmpty())) {
            return null;
        }
        JSONArray snapshot = JSON.parseArray(record.getStages());
        for (int i = 0; i < snapshot.size(); i++) {
            JSONObject obj = snapshot.getJSONObject(i);
            if ("build".equalsIgnoreCase(obj.getString("type")) && obj.getJSONObject("result") != null) {
                return obj.getJSONObject("result").getInteger("buildNumberId");
            }
        }
        return null;
    }

    /**
     * 等待构建完成（轮询构建状态）
     */
    private BuildStatus waitBuildFinish(String buildId, int buildNumberId) {
        for (int i = 0; i < 300; i++) {
            BuildHistoryLog historyLog = this.getHistoryLog(buildInfoService.getByKey(buildId), buildNumberId);
            if (historyLog != null) {
                BuildStatus status = io.voyager1.model.BaseEnum.getEnum(BuildStatus.class, historyLog.getStatus());
                if (status != null && !status.isProgress()) {
                    return status;
                }
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("构建等待被打断", e);
            }
        }
        throw new IllegalStateException("构建等待超时");
    }

    private String getParam(PipelineConfigService.PipelineStage stage, String key, String defaultVal) {
        if (stage.getParams() == null) {
            return defaultVal;
        }
        Object val = stage.getParams().get(key);
        return val == null ? defaultVal : String.valueOf(val);
    }


    /**
     * 按构建编号查询历史日志
     */
    private BuildHistoryLog getHistoryLog(BuildInfoModel buildInfo, int buildNumberId) {
        io.voyager1.core.db.Entity where = io.voyager1.core.db.Entity.create("CI_BUILD_LOG");
        where.set("buildDataId", buildInfo.getId());
        where.set("buildNumberId", buildNumberId);
        List<BuildHistoryLog> list = dbBuildHistoryLogService.queryList(where, 1);
        return (list == null || list.isEmpty() ? null : list.get(0));
    }

    private void ThreadUtilExecute(Runnable runnable) {
        io.voyager1.util.ThreadUtil.execute(runnable);
    }

    /**
     * 当前操作者（异步线程上下文中透传的触发人，无则空用户）
     */
    private UserModel currentOperator() {
        UserModel userModel = io.voyager1.common.BaseServerController.getUserByThreadLocal();
        return userModel == null ? new UserModel() : userModel;
    }

    /**
     * 生成语义化版本号：优先使用构建配置的标签（branchTagName），否则合成版本号
     */
    private String resolveVersion(BuildInfoModel buildInfoModel, int buildNumberId) {
        String tagName = buildInfoModel.getBranchTagName();
        if ((tagName != null && !tagName.isEmpty())) {
            return tagName.startsWith("v") ? tagName : "v" + tagName;
        }
        // 语义化：用 commit hash 前缀（替代无意义的随机数）
        String commit = buildInfoModel.getRepositoryLastCommitId();
        if ((commit != null && !commit.isEmpty())) {
            String shortCommit = commit.length() > 8 ? commit.substring(0, 8) : commit;
            return String.format("v%s.%s.%s", buildInfoModel.getBuildId(), buildNumberId, shortCommit);
        }
        return String.format("v%s.%s.%s", buildInfoModel.getBuildId(), buildNumberId, System.currentTimeMillis() % 1000);
    }

}
