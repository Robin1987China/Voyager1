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

package io.voyager1.controller.pipeline;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.common.BaseServerController;
import io.voyager1.core.api.ApiResult;
import io.voyager1.model.data.PipelineConfigModel;
import io.voyager1.model.data.PipelineExecuteRecordModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.pipeline.PipelineConfigService;
import io.voyager1.service.pipeline.PipelineExecuteRecordService;
import io.voyager1.service.pipeline.PipelineExecutorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RestController;

import io.voyager1.model.data.PipelineConfigModel;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Pipeline 配置与执行 API
 *
 * @since 2026/8/7
 */
@RestController
@RequestMapping(value = "/pipeline")
@Feature(cls = ClassFeature.BUILD)
public class PipelineController extends BaseServerController {

    private final PipelineConfigService pipelineConfigService;
    private final PipelineExecutorService pipelineExecutorService;
    private final PipelineExecuteRecordService executeRecordService;

    public PipelineController(PipelineConfigService pipelineConfigService,
                              PipelineExecutorService pipelineExecutorService,
                              PipelineExecuteRecordService executeRecordService) {
        this.pipelineConfigService = pipelineConfigService;
        this.pipelineExecutorService = pipelineExecutorService;
        this.executeRecordService = executeRecordService;
    }

    /**
     * 保存 Pipeline 配置
     */
    @PostMapping(value = "save-config", produces = "application/json")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> saveConfig(String id, String name, String buildId, String triggers, String stages, Boolean enabled, String remark) {
        String configId = pipelineConfigService.saveConfig(id, name, buildId, triggers, stages, enabled, remark);
        return ApiResult.success("保存成功", configId);
    }

    /**
     * 查询 Pipeline 配置（按应用）
     */
    @PostMapping(value = "list-config", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<PipelineConfigModel>> listConfig(String buildId) {
        List<PipelineConfigModel> list = pipelineConfigService.listByBuildId(buildId);
        return ApiResult.success("", list);
    }

    /**
     * 触发 Pipeline 执行
     */
    @PostMapping(value = "trigger", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> trigger(String pipelineId) {
        UserModel userModel = this.getUserModel();
        String operator = userModel == null ? "system" : userModel.getName();
        pipelineExecutorService.trigger(pipelineId, "manual", operator);
        return ApiResult.success("已触发执行");
    }

    /**
     * 审批（通过/拒绝）
     */
    @PostMapping(value = "approval", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> approval(String executeId, Boolean approve) {
        UserModel userModel = this.getUserModel();
        String operator = userModel == null ? "system" : userModel.getName();
        pipelineExecutorService.approval(executeId, ConvertUtil.toBool(approve, false), operator);
        return ApiResult.success(approve ? "已批准，继续执行" : "已拒绝");
    }

    /**
     * WebHook 触发（token 校验）
     */
    @PostMapping(value = "trigger-webhook", produces = "application/json")
    @io.voyager1.common.interceptor.NotLogin
    public ApiResult<String> triggerWebhook(String id, String token) {
        PipelineConfigModel model = pipelineConfigService.checkWebhook(id, token);
        Assert.notNull(model, "WebHook 触发失败：配置不存在或 token 错误");
        pipelineExecutorService.trigger(model.getId(), "webhook", "system");
        return ApiResult.success("已触发");
    }

    /**
     * 删除 Pipeline 配置（注销 cron）
     */
    @PostMapping(value = "delete-config", produces = "application/json")
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> deleteConfig(String id) {
        pipelineConfigService.deleteConfig(id);
        return ApiResult.success("删除成功");
    }

    /**
     * 查询执行记录（按 Pipeline）
     */
    @PostMapping(value = "list-execute", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<PipelineExecuteRecordModel>> listExecute(String pipelineId) {
        return ApiResult.success("", executeRecordService.listByPipelineId(pipelineId));
    }
}
