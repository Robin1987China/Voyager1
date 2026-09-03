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

package io.voyager1.func.assets.controller;

import io.voyager1.core.api.ApiResult;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.func.assets.model.ScriptLibraryModel;
import io.voyager1.func.assets.server.ScriptLibraryServer;
import io.voyager1.model.PageResultDto;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @since 2024/6/16
 */
@RestController
@RequestMapping(value = "/system/assets/script-library")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_GLOBAL_SCRIPT)
@Slf4j
public class ScriptLibraryNoPermissionController {

    private final ScriptLibraryServer scriptLibraryServer;

    public ScriptLibraryNoPermissionController(ScriptLibraryServer scriptLibraryServer) {
        this.scriptLibraryServer = scriptLibraryServer;
    }

    @PostMapping(value = "list-data-no-permission", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<ScriptLibraryModel>> listJson(HttpServletRequest request) {
        PageResultDto<ScriptLibraryModel> pageResultDto = scriptLibraryServer.listPage(request);
        return ApiResult.success("", pageResultDto);
    }
}
