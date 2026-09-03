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

package io.voyager1.controller.monitor;

import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.log.MonitorNotifyLog;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.dblog.DbMonitorNotifyLogService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 监控列表
 *
 * @since 2019/7/16
 */
@RestController
@RequestMapping(value = "/monitor")
@Feature(cls = ClassFeature.MONITOR_LOG)
public class MonitorLogController extends BaseServerController {

    private final DbMonitorNotifyLogService dbMonitorNotifyLogService;

    public MonitorLogController(DbMonitorNotifyLogService dbMonitorNotifyLogService) {
        this.dbMonitorNotifyLogService = dbMonitorNotifyLogService;
    }

    /**
     * 展示用户列表
     *
     * @return json
     */
    @RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<MonitorNotifyLog>> listData(HttpServletRequest request) {
        PageResultDto<MonitorNotifyLog> pageResult = dbMonitorNotifyLogService.listPage(request);
        return ApiResult.success("获取成功", pageResult);
    }
}
