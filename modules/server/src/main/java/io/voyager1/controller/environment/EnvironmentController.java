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
    public ApiResult<String> save(String id, String name, Integer sortValue, Boolean enabled) {
        return ApiResult.success("保存成功", environmentService.saveEnvironment(id, name, sortValue, enabled));
    }

    /**
     * 部署版本到环境（人工 CD）
     */
    @PostMapping(value = "deploy", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> deploy(String versionId, String environment) {
        String operator = "system";
        io.voyager1.model.user.UserModel um = BaseServerController.getUserByThreadLocal();
        if (um != null) { operator = (um.getName() == null || um.getName().isEmpty() ? "system" : um.getName()); }
        // Phase 3 简化：创建部署记录（发布执行走 Pipeline publish 或由前端联动）
        String recordId = deploymentService.createRecord(versionId, environment, "manual", operator, 0, "");
        return ApiResult.success("部署完成", recordId);
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
