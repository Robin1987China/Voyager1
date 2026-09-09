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

package io.voyager1.controller.environment;

import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.model.data.DeploymentRecordModel;
import io.voyager1.model.data.EnvironmentModel;
import io.voyager1.model.data.EnvironmentTargetModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.environment.DeploymentService;
import io.voyager1.service.environment.EnvironmentService;
import io.voyager1.service.version.VersionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 环境与部署 API
 *
 * @since 2026/8/8
 */
@RestController
@RequestMapping(value = "/environment")
@Feature(cls = ClassFeature.BUILD)
public class EnvironmentController extends BaseServerController {

    private final EnvironmentService environmentService;
    private final DeploymentService deploymentService;
    private final VersionService versionService;

    public EnvironmentController(EnvironmentService environmentService,
                                 DeploymentService deploymentService,
                                 VersionService versionService) {
        this.environmentService = environmentService;
        this.deploymentService = deploymentService;
        this.versionService = versionService;
    }

    /**
     * 环境列表
     */
    @PostMapping(value = "list", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<EnvironmentModel>> list() {
        return ApiResult.success("", environmentService.listEnabled());
    }

    /**
     * 保存环境
     */
    @PostMapping(value = "save", produces = "application/json")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> save(String id, String name, Integer sortValue, Boolean enabled,
                                  String type, String strategy, Boolean approvalRequired) {
        return ApiResult.success("保存成功",
            environmentService.saveEnvironment(id, name, sortValue, enabled, type, strategy, approvalRequired));
    }

    /**
     * 给环境绑定部署目标（节点/集群/SSH）
     * <p>
     * 工作区以服务端请求上下文为准（防跨工作区越权绑定），不接受客户端传值。
     */
    @PostMapping(value = "bind-target", produces = "application/json")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> bindTarget(String environmentId, String targetType, String targetId, String projectId,
                                        jakarta.servlet.http.HttpServletRequest request) {
        String workspaceId = io.voyager1.core.jpa.WorkspaceContext.getWorkspaceId(request);
        return ApiResult.success("绑定成功",
            environmentService.bindTarget(environmentId, targetType, targetId, projectId, workspaceId));
    }

    /**
     * 解绑环境目标
     */
    @PostMapping(value = "unbind-target", produces = "application/json")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> unbindTarget(String id) {
        environmentService.unbindTarget(id);
        return ApiResult.success("解绑成功");
    }

    /**
     * 查询环境的目标列表
     */
    @PostMapping(value = "targets", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<EnvironmentTargetModel>> targets(String environmentId) {
        return ApiResult.success("", environmentService.listTargets(environmentId));
    }

    /**
     * 部署版本到环境（人工 CD）。
     * 发布异步执行：同步返回部署记录 id，最终结果见部署记录状态。
     */
    @PostMapping(value = "deploy", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> deploy(String versionId, String environment,
                                    jakarta.servlet.http.HttpServletRequest request) {
        String operator = currentOperator();
        String workspaceId = io.voyager1.core.jpa.WorkspaceContext.getWorkspaceId(request);
        // 环境化 CD：把版本真实发布到环境绑定的目标节点（复用 ReleaseManage）
        String recordId = deploymentService.deployPublish(versionId, environment, operator, workspaceId, false);
        return ApiResult.success("部署已提交，发布过程请查看部署记录", recordId);
    }

    /**
     * 版本晋升到下一环境（同一版本跨环境部署，不重新构建）。
     * 与直接部署的区别：要求上一环境已成功部署过该版本。
     */
    @PostMapping(value = "promote", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> promote(String versionId, String environment,
                                     jakarta.servlet.http.HttpServletRequest request) {
        String operator = currentOperator();
        String workspaceId = io.voyager1.core.jpa.WorkspaceContext.getWorkspaceId(request);
        String recordId = deploymentService.deployPublish(versionId, environment, operator, workspaceId, true);
        return ApiResult.success("晋升已提交，发布过程请查看部署记录", recordId);
    }

    /**
     * 审批待部署记录（需审批环境的部署闸门）
     */
    @PostMapping(value = "approve-deploy", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> approveDeploy(String recordId, Boolean approve, String remark) {
        org.springframework.util.Assert.notNull(approve, "审批结果不能为空");
        deploymentService.approve(recordId, approve, currentOperator(), remark);
        return ApiResult.success(approve ? "已批准，开始部署" : "已拒绝");
    }

    private String currentOperator() {
        io.voyager1.model.user.UserModel um = BaseServerController.getUserByThreadLocal();
        if (um == null || um.getName() == null || um.getName().isEmpty()) {
            return "system";
        }
        return um.getName();
    }

    /**
     * 版本部署记录
     */
    @PostMapping(value = "deploy-records", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<DeploymentRecordModel>> deployRecords(String versionId, String environment) {
        if ((versionId != null && !versionId.isEmpty())) {
            return ApiResult.success("", deploymentService.listByVersionId(versionId));
        }
        return ApiResult.success("", deploymentService.listByEnvironment(environment));
    }
}
