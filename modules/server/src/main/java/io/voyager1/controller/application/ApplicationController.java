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

package io.voyager1.controller.application;

import io.voyager1.common.BaseServerController;
import io.voyager1.core.api.ApiResult;
import io.voyager1.model.data.ApplicationDetailModel;
import io.voyager1.model.data.ApplicationModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.application.ApplicationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 应用（逻辑服务）API
 *
 * @since 2026/9/7
 */
@RestController
@RequestMapping(value = "/application")
@Feature(cls = ClassFeature.BUILD)
public class ApplicationController extends BaseServerController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * 应用列表
     */
    @PostMapping(value = "list", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<ApplicationModel>> list() {
        return ApiResult.success("", applicationService.list());
    }

    /**
     * 保存应用
     */
    @PostMapping(value = "save", produces = "application/json")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> save(String id, String name, String repositoryId, String buildId, String remark) {
        return ApiResult.success("保存成功",
            applicationService.save(id, name, repositoryId, buildId, remark));
    }

    /**
     * 应用详情（环境泳道 + 构建历史 + 部署记录）
     */
    @PostMapping(value = "detail", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<ApplicationDetailModel> detail(String id) {
        return ApiResult.success("", applicationService.detail(id));
    }

    /**
     * 删除应用
     */
    @PostMapping(value = "del", produces = "application/json")
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> del(String id) {
        applicationService.delByKey(id);
        return ApiResult.success("删除成功");
    }
}
