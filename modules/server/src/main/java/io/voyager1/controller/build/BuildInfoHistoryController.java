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

package io.voyager1.controller.build;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.build.BuildUtil;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorConfig;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.log.BuildHistoryLog;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.dblog.BuildInfoService;
import io.voyager1.service.dblog.DbBuildHistoryLogService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * new version for build info history controller
 *
 * @since 2021-08-26
 */
@RestController
@Feature(cls = ClassFeature.BUILD_LOG)
public class BuildInfoHistoryController extends BaseServerController {

    private final BuildInfoService buildInfoService;
    private final DbBuildHistoryLogService dbBuildHistoryLogService;

    public BuildInfoHistoryController(BuildInfoService buildInfoService,
                                      DbBuildHistoryLogService dbBuildHistoryLogService) {
        this.buildInfoService = buildInfoService;
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
    }

    /**
     * 下载构建物
     *
     * @param logId 日志id
     */
    @RequestMapping(value = "/build/history/download_file", method = RequestMethod.GET)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void downloadFile(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String logId, HttpServletRequest request, HttpServletResponse response) {
        BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(logId, request, false);
        this.downloadFile(buildHistoryLog, response);
    }

    /**
     * 下载构建物
     *
     * @param buildId       构建ID
     * @param buildNumberId 构建序号ID
     */
    @RequestMapping(value = "/build/history/download_file_by_build", method = RequestMethod.GET)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void downloadFile(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String buildId,
                             @ValidatorItem(value = ValidatorRule.NUMBERS,msg = "构建序号ID缺失") int buildNumberId,
                             HttpServletResponse response,
                             HttpServletRequest request) {
        String workspaceId = dbBuildHistoryLogService.getCheckUserWorkspace(request);
        //
        BuildHistoryLog historyLog = new BuildHistoryLog();
        historyLog.setWorkspaceId(workspaceId);
        historyLog.setBuildDataId(buildId);
        historyLog.setBuildNumberId(buildNumberId);
        List<BuildHistoryLog> buildHistoryLogs = dbBuildHistoryLogService.listByBean(historyLog, false);
        BuildHistoryLog first = (buildHistoryLogs == null || buildHistoryLogs.isEmpty() ? null : buildHistoryLogs.get(0));
        this.downloadFile(first, response);
    }

    private void downloadFile(BuildHistoryLog buildHistoryLog, HttpServletResponse response) {
        if (buildHistoryLog == null) {
            JakartaServletUtil.write(response, ApiResult.getString(404, "构建记录不存在"), MediaType.APPLICATION_JSON_VALUE);
            return;
        }
        EnvironmentMapBuilder environmentMapBuilder = buildHistoryLog.toEnvironmentMapBuilder();
        boolean tarGz = environmentMapBuilder.getBool(BuildUtil.USE_TAR_GZ, false);
        File resultDirFile = BuildUtil.getHistoryPackageFile(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId(), buildHistoryLog.getResultDirFile());
        File dirPackage = BuildUtil.loadDirPackage(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId(), resultDirFile, tarGz, (aBoolean, file) -> file);
        JakartaServletUtil.write(response, dirPackage);
    }


    @RequestMapping(value = "/build/history/download_log", method = RequestMethod.GET)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void downloadLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String logId, HttpServletRequest request, HttpServletResponse response) {
        BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(logId, request);
        Objects.requireNonNull(buildHistoryLog);
        BuildInfoModel item = buildInfoService.getByKey(buildHistoryLog.getBuildDataId());
        Objects.requireNonNull(item);
        File logFile = BuildUtil.getLogFile(item.getId(), buildHistoryLog.getBuildNumberId());
        if (!FileUtil.exist(logFile)) {
            return;
        }
        if (logFile.isFile()) {
            JakartaServletUtil.write(response, logFile);
        }
    }

    @RequestMapping(value = "/build/history/history_list.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<BuildHistoryLog>> historyList(HttpServletRequest request) {
        PageResultDto<BuildHistoryLog> pageResultTemp = dbBuildHistoryLogService.listPage(request);
        pageResultTemp.each(buildHistoryLog -> {
            File file = BuildUtil.getHistoryPackageFile(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId(), buildHistoryLog.getResultDirFile());
            buildHistoryLog.setHasFile(FileUtil.isNotEmpty(file));
            //
            File logFile = BuildUtil.getLogFile(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId());
            buildHistoryLog.setHasLog(FileUtil.exist(logFile));
        });
        return ApiResult.success("获取成功", pageResultTemp);
    }

    /**
     * 删除构建历史，支持批量删除，用逗号分隔
     *
     * @param logId id
     * @return json
     */
    @RequestMapping(value = "/build/history/delete_log.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> delete(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据")) String logId, HttpServletRequest request) {
        List<String> strings = io.voyager1.util.ConvertUtil.splitTrim(logId, ",");
        for (String itemId : strings) {
            BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(itemId, request);
            ApiResult<String> jsonMessage = dbBuildHistoryLogService.deleteLogAndFile(buildHistoryLog);
            if (!jsonMessage.success()) {
                return jsonMessage;
            }
        }
        return new ApiResult<>(200, "删除成功");
    }
}
