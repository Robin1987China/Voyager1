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

package io.voyager1.func.user.controller;

import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.func.user.model.UserLoginLogModel;
import io.voyager1.func.user.server.UserLoginLogServer;
import io.voyager1.model.PageResultDto;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @since 2023/3/9
 */
@RestController
@RequestMapping(value = "/user/login-log")
@Feature(cls = ClassFeature.USER_LOGIN_LOG)
@SystemPermission
public class UserLoginLogController extends BaseServerController {

    private final UserLoginLogServer userLoginLogServer;

    public UserLoginLogController(UserLoginLogServer userLoginLogServer) {
        this.userLoginLogServer = userLoginLogServer;
    }

    /**
     * 登录日志列表
     *
     * @return json
     */
    @RequestMapping(value = "list-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<UserLoginLogModel>> listData(HttpServletRequest request) {
        PageResultDto<UserLoginLogModel> pageResult = userLoginLogServer.listPage(request);
        return ApiResult.success("", pageResult);
    }
}
