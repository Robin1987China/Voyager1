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

package io.voyager1.controller.user;

import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.log.UserOperateLogV1;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.dblog.DbUserOperateLogService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户操作日志
 *
 * @since 2019/4/19
 */
@RestController
@RequestMapping(value = "/user/log")
@Feature(cls = ClassFeature.USER_LOG)
@SystemPermission
public class UserOptLogController extends BaseServerController {

    private final DbUserOperateLogService dbUserOperateLogService;

    public UserOptLogController(DbUserOperateLogService dbUserOperateLogService) {
        this.dbUserOperateLogService = dbUserOperateLogService;
    }

    /**
     * 展示用户列表
     *
     * @return json
     */
    @RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<UserOperateLogV1>> listData(HttpServletRequest request) {
        PageResultDto<UserOperateLogV1> pageResult = dbUserOperateLogService.listPage(request);
        return ApiResult.success("", pageResult);
    }
}
