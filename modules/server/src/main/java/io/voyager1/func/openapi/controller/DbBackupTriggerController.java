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

package io.voyager1.func.openapi.controller;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.core.api.ApiResult;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.ServerOpenApi;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.interceptor.NotLogin;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.BackupInfoModel;
import io.voyager1.model.enums.BackupTypeEnum;
import io.voyager1.service.dblog.BackupInfoService;
import io.voyager1.service.system.SystemParametersServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.Future;

/**
 * @since 2024/9/29
 */
@RestController
@NotLogin
@Slf4j
public class DbBackupTriggerController {

    private final BackupInfoService backupInfoService;
    private final SystemParametersServer systemParametersServer;

    public DbBackupTriggerController(BackupInfoService backupInfoService,
                                     SystemParametersServer systemParametersServer) {
        this.backupInfoService = backupInfoService;
        this.systemParametersServer = systemParametersServer;
    }

    /**
     * 备份数据触发器
     *
     * @param token        token
     * @param reserveCount 保留份数
     * @return json
     */
    @RequestMapping(value = ServerOpenApi.BACKUP_TRIGGER_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> trigger(@PathVariable String token,
                                        HttpServletRequest request,
                                        String reserveCount) {
        String configToken = systemParametersServer.getConfig("backup-db-token", String.class);
        Assert.state(java.util.Objects.equals(configToken, token), "触发token错误,或者已经失效");
        //
        Future<BackupInfoModel> future = backupInfoService.triggerBackup();
        Assert.notNull(future, "当前数据库不支持自动备份");
        //
        int reserveCountInt = ConvertUtil.toInt(reserveCount, 0);
        if (reserveCountInt > 0) {
            while (true) {
                Entity entity = Entity.create();
                entity.set("backupType", BackupTypeEnum.TRIGGER.getCode());
                Pageable page = PageRequest.of(1, reserveCountInt, Sort.by(Sort.Order.desc("createTimeMillis")));

                try {
                    PageResultDto<BackupInfoModel> pageResultDto = backupInfoService.listPage(entity, page);
                    if (pageResultDto.isEmpty()) {
                        break;
                    }
                    pageResultDto.each(backupInfoModel -> backupInfoService.delByKey(backupInfoModel.getId()));
                } catch (Exception e) {
                    if (StrUtil.equals(e.getMessage(), "筛选的分页有问题,当前页码查询不到任何数据")) {
                        // 没有任何数据
                        break;
                    } else {
                        throw Lombok.sneakyThrow(e);
                    }
                }
            }
        }
        return ApiResult.success("触发成功");
    }
}
