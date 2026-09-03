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

package io.voyager1.controller.system;

import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.socket.ServiceFileTailWatcher;
import io.voyager1.system.LogbackConfig;
import io.voyager1.util.DirTreeUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 系统日志管理
 *
 * @since 2019/7/20
 */
@RestController
@RequestMapping(value = "system")
@Feature(cls = ClassFeature.SYSTEM_LOG)
@SystemPermission
public class LogManageController extends BaseServerController {


    @RequestMapping(value = "log_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<JSONObject>> logData(String machineId, HttpServletRequest request) {
        ApiResult<List<JSONObject>> message = this.tryRequestMachine(machineId, request, NodeUrl.SystemLog);
        return Optional.ofNullable(message)
            .orElseGet(() -> {
                List<JSONObject> data = DirTreeUtil.getTreeData(LogbackConfig.getPath());
                return ApiResult.success("", data);
            });
    }

    /**
     * 删除 需要验证是否最后修改时间
     *
     * @param path 路径
     * @return json
     */
    @RequestMapping(value = "log_del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> logData(String machineId,
                                        @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "参数错误path错误") String path,
                                        HttpServletRequest request) {
        ApiResult<String> jsonMessage = this.tryRequestMachine(machineId, request, NodeUrl.DelSystemLog);
        return Optional.ofNullable(jsonMessage).orElseGet(() -> {
            File file = FileUtil.file(LogbackConfig.getPath(), path);
            // 判断修改时间
            long modified = file.lastModified();
            Assert.state(System.currentTimeMillis() - modified > TimeUnit.DAYS.toMillis(1), "不能删除近一天相关的日志(文件修改时间)");
            // 离线上一个日志
            ServiceFileTailWatcher.offlineFile(file);
            if (FileUtil.del(file)) {
                FileUtil.cleanEmpty(file.getParentFile());
                return new ApiResult<>(200, "删除成功");
            }
            return new ApiResult<>(500, "删除失败");
        });
    }


    @RequestMapping(value = "log_download", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void logDownload(String machineId,
                            @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "参数错误path错误") String path,
                            HttpServletResponse response,
                            HttpServletRequest request) {
        if ((machineId != null && !machineId.isEmpty())) {
            MachineNodeModel model = machineNodeServer.getByKey(machineId);
            Assert.notNull(model, "没有找到对应的机器");
            NodeForward.requestDownload(model, request, response, NodeUrl.DownloadSystemLog);
            return;
        }
        File file = FileUtil.file(LogbackConfig.getPath(), path);
        if (file.isFile()) {
            FileUtil.cleanEmpty(file.getParentFile());
            JakartaServletUtil.write(response, file);
        }
    }
}
