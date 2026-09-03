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

import io.voyager1.util.JakartaServletUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.func.user.dto.UserNotificationDto;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.system.SystemParametersServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @since 2024/4/20
 */
@RestController
@RequestMapping(value = "/user/notification")
@Feature(cls = ClassFeature.USER)
@SystemPermission
public class UserNotificationController {

    public static final String KEY = "SYSTEM-USER-NOTIFICATION";

    private final SystemParametersServer systemParametersServer;

    public UserNotificationController(SystemParametersServer systemParametersServer) {
        this.systemParametersServer = systemParametersServer;
    }

    /**
     * 获取通知
     *
     * @return json
     */
    @GetMapping(value = "get", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<UserNotificationDto> getNotification() {
        UserNotificationDto notificationDto = systemParametersServer.getConfigDefNewInstance(KEY, UserNotificationDto.class);
        return ApiResult.success("", notificationDto);
    }


    /**
     * 保存通知
     *
     * @return json
     */
    @PostMapping(value = "save", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<UserNotificationDto> saveNotification(HttpServletRequest request) {
        UserNotificationDto userNotification = JakartaServletUtil.toBean(request, UserNotificationDto.class, true);
        Assert.notNull(userNotification, "请配置用户通知");
        userNotification.verify();
        systemParametersServer.upsert(KEY, userNotification, "");
        return ApiResult.success("保存成功");
    }
}
