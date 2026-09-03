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

import io.voyager1.util.StrUtil;
import io.voyager1.common.BaseServerController;
import io.voyager1.core.api.ApiResult;
import io.voyager1.model.data.VersionModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.version.VersionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 版本生命周期 API（提测/打回/发布）
 *
 * @since 2026/8/7
 */
@RestController
@RequestMapping(value = "/version")
@Feature(cls = ClassFeature.BUILD)
public class VersionController extends BaseServerController {

    private final VersionService versionService;

    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    /**
     * 创建版本（绑定构建产物）
     */
    @PostMapping(value = "create", produces = "application/json")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<VersionModel> create(String buildId, Integer buildNumberId, String version, String artifactRef, String remark) {
        VersionModel model = versionService.createVersion(buildId, buildNumberId, version, artifactRef, remark);
        return ApiResult.success("创建成功", model);
    }

    /**
     * 提测（冻结 CI）
     */
    @PostMapping(value = "submit", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> submit(String id, String remark) {
        versionService.submit(id, remark);
        return ApiResult.success("提测成功，CI 已冻结");
    }

    /**
     * 打回（解锁 CI）
     */
    @PostMapping(value = "return", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> returnVersion(String id, String remark) {
        versionService.returnVersion(id, remark);
        return ApiResult.success("打回成功，CI 已恢复");
    }

    /**
     * 发布（晋升）
     */
    @PostMapping(value = "release", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> release(String id, String remark) {
        versionService.release(id, remark);
        return ApiResult.success("发布成功");
    }

    /**
     * 版本列表（按应用）
     */
    @PostMapping(value = "list", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<VersionModel>> list(String buildId) {
        return ApiResult.success("", versionService.listByBuildId(buildId));
    }
}
