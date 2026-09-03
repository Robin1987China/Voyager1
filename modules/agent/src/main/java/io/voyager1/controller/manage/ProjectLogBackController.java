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

package io.voyager1.controller.manage;

import io.voyager1.util.FileUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseAgentController;
import io.voyager1.common.commander.ProjectCommander;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.data.NodeProjectInfoModel;
import io.voyager1.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;

/**
 * @since 2019/4/17
 */
@RestController
@RequestMapping(value = "manage/log")
@Slf4j
public class ProjectLogBackController extends BaseAgentController {

    private final ProjectCommander projectCommander;

    public ProjectLogBackController(ProjectCommander projectCommander) {
        this.projectCommander = projectCommander;
    }

    @RequestMapping(value = "logSize", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> logSize(String id) {
        NodeProjectInfoModel nodeProjectInfoModel = getProjectInfoModel();
        JSONObject jsonObject = new JSONObject();
        //
        //获取日志备份路径
        File logBack = projectInfoService.resolveLogBack(nodeProjectInfoModel);
        boolean logBackBool = logBack.exists() && logBack.isDirectory();
        jsonObject.put("logBack", logBackBool);
        String info = this.getLogSize(nodeProjectInfoModel);
        jsonObject.put("logSize", info);
        return ApiResult.success("", jsonObject);
    }

    /**
     * 查看项目控制台日志文件大小
     *
     * @param nodeProjectInfoModel 项目
     * @return 文件大小
     */
    private String getLogSize(NodeProjectInfoModel nodeProjectInfoModel) {
        if (nodeProjectInfoModel == null) {
            return null;
        }
        File file = projectInfoService.resolveAbsoluteLogFile(nodeProjectInfoModel);
        if (file.exists()) {
            long fileSize = file.length();
            if (fileSize <= 0) {
                return null;
            }
            return FileUtil.readableFileSize(fileSize);
        }
        return null;
    }

    @RequestMapping(value = "resetLog", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> resetLog() {
        NodeProjectInfoModel pim = getProjectInfoModel();
        try {
            String msg = projectCommander.backLog(pim);
            if (msg.contains("ok")) {
                return ApiResult.success("重置成功");
            }
            return new ApiResult<>(201, "重置失败：" + msg);
        } catch (Exception e) {
            log.error("重置日志失败", e);
            return new ApiResult<>(500, "重置日志失败");
        }
    }

    @RequestMapping(value = "logBack_delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> clear(String name) {
        Assert.hasText(name, "没有对应到文件");
        NodeProjectInfoModel pim = getProjectInfoModel();
        File logBack = projectInfoService.resolveLogBack(pim);
        if (logBack.exists() && logBack.isDirectory()) {
            logBack = FileUtil.file(logBack, name);
            if (logBack.exists()) {
                FileUtil.del(logBack);
                return ApiResult.success("删除成功");
            }
            return new ApiResult<>(500, "没有对应文件");
        } else {
            return new ApiResult<>(500, "没有对应文件夹");
        }
    }

    @RequestMapping(value = "logBack_download", method = RequestMethod.GET)
    public void download(String key, HttpServletResponse response) {
        Assert.hasText(key, "请选择对应到文件");
        try {
            NodeProjectInfoModel pim = getProjectInfoModel();
            File logBack = projectInfoService.resolveLogBack(pim);
            if (logBack.exists() && logBack.isDirectory()) {
                logBack = FileUtil.file(logBack, key);
                JakartaServletUtil.write(response, logBack);
            } else {
                JakartaServletUtil.write(response, ApiResult.getString(400, "没有对应文件:" + logBack.getPath()), MediaType.APPLICATION_JSON_VALUE);
            }
        } catch (Exception e) {
            log.error("下载文件异常", e);
            JakartaServletUtil.write(response, ApiResult.getString(400, "下载失败。请刷新页面后重试", e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
        }
    }

    @RequestMapping(value = "logBack", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> console() {
        // 查询项目路径
        NodeProjectInfoModel pim = getProjectInfoModel();

        JSONObject jsonObject = new JSONObject();
        NodeProjectInfoModel infoModel = projectInfoService.resolveModel(pim);
        File logBack = projectInfoService.resolveLogBack(pim, infoModel);
        if (logBack.exists() && logBack.isDirectory()) {
            File[] filesAll = logBack.listFiles();
            if (filesAll != null) {
                List<JSONObject> jsonArray = FileUtils.parseInfo(filesAll, true, null, pim.isDisableScanDir());
                jsonObject.put("array", jsonArray);
            }
        }
        jsonObject.put("id", pim.getId());
        jsonObject.put("logPath", projectInfoService.resolveAbsoluteLog(pim, infoModel));
        jsonObject.put("logBackPath", logBack.getAbsolutePath());
        return ApiResult.success("", jsonObject);
    }

    @RequestMapping(value = "export", method = RequestMethod.GET)
    @ResponseBody
    public void export(HttpServletResponse response) {
        NodeProjectInfoModel pim = getProjectInfoModel();

        File file = projectInfoService.resolveAbsoluteLogFile(pim);
        if (!file.exists()) {
            JakartaServletUtil.write(response, ApiResult.getString(400, "没有日志文件:" + file.getPath()), MediaType.APPLICATION_JSON_VALUE);
            return;
        }
        JakartaServletUtil.write(response, file);
    }
}
