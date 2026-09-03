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

package io.voyager1.controller.script;

import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.script.ScriptExecuteLogModel;
import io.voyager1.model.script.ScriptModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.script.ScriptExecuteLogServer;
import io.voyager1.service.script.ScriptServer;
import io.voyager1.socket.ServerScriptProcessBuilder;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;

/**
 * @since 2022/1/20
 */

@RestController
@RequestMapping(value = "/script_log")
@Feature(cls = ClassFeature.SCRIPT_LOG)
public class ScriptLogController extends BaseServerController {

    private final ScriptExecuteLogServer scriptExecuteLogServer;
    private final ScriptServer scriptServer;

    public ScriptLogController(ScriptExecuteLogServer scriptExecuteLogServer,
                               ScriptServer scriptServer) {
        this.scriptExecuteLogServer = scriptExecuteLogServer;
        this.scriptServer = scriptServer;
    }

    /**
     * get script log list
     *
     * @return json
     */
    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<ScriptExecuteLogModel>> scriptList(HttpServletRequest request) {
        PageResultDto<ScriptExecuteLogModel> pageResultDto = scriptExecuteLogServer.listPage(request);
        return ApiResult.success("", pageResultDto);
    }

    /**
     * 删除日志
     *
     * @param id        id
     * @param executeId 执行ID
     * @return json
     */
    @RequestMapping(value = "del_log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> delLog(@ValidatorItem() String id,
                                       @ValidatorItem() String executeId,
                                       HttpServletRequest request) {
        ScriptModel item = null;
        try {
            item = scriptServer.getByKeyAndGlobal(id, request, "ignore");
        } catch (IllegalArgumentException | IllegalStateException e) {
            if (!java.util.Objects.equals("ignore", e.getMessage())) {
                throw e;
            }
        }
        File logFile = item == null ? ScriptModel.logFile(id, executeId) : item.logFile(executeId);
        boolean fastDel = CommandUtil.systemFastDel(logFile);
        Assert.state(!fastDel, "删除日志文件失败");
        scriptExecuteLogServer.delByKey(executeId);
        return ApiResult.success("删除成功");
    }

    /**
     * 获取的日志
     *
     * @param id        id
     * @param executeId 执行ID
     * @param line      需要获取的行号
     * @return json
     */
    @RequestMapping(value = "log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> getNowLog(@ValidatorItem() String id,
                                              @ValidatorItem() String executeId,
                                              @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "行号错误") int line,
                                              HttpServletRequest request) {
        ScriptModel item = scriptServer.getByKey(id, request);
        Assert.notNull(item, "没有对应数据");
        File logFile = item.logFile(executeId);
        Assert.state(FileUtil.isFile(logFile), "日志文件错误");
        JSONObject data = FileUtils.readLogFile(logFile, line);
        // 运行中
        data.put("run", ServerScriptProcessBuilder.isRun(executeId));
        return ApiResult.success("", data);
    }
}
