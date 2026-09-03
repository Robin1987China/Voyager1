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

package io.voyager1.controller.ssh;

import io.voyager1.util.FileUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.CommandExecLogModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.node.ssh.CommandExecLogService;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;

/**
 * 命令执行日志
 *
 * @since 2021/12/23
 */
@RestController
@RequestMapping(value = "/node/ssh_command_log")
@Feature(cls = ClassFeature.SSH_COMMAND_LOG)
public class CommandLogController extends BaseServerController {

    private final CommandExecLogService commandExecLogService;

    public CommandLogController(CommandExecLogService commandExecLogService) {
        this.commandExecLogService = commandExecLogService;
    }

    /**
     * 分页获取命令信息
     *
     * @return result
     */
    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<CommandExecLogModel>> page(HttpServletRequest request) {
        PageResultDto<CommandExecLogModel> page = commandExecLogService.listPage(request);
        return ApiResult.success("", page);
    }

    /**
     * 删除日志记录
     *
     * @param id id
     * @return result
     * @api {POST} node/ssh_command_log/del 删除日志记录
     * @apiGroup node/ssh_command_log
     * @apiUse defResultJson
     * @apiParam {String} id 记录 id
     */
    @RequestMapping(value = "del", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> del(String id, HttpServletRequest request) {
        CommandExecLogModel execLogModel = commandExecLogService.getByKey(id, request);
        Assert.notNull(execLogModel, "没有对应的记录");
        File logFile = execLogModel.logFile();
        boolean fastDel = CommandUtil.systemFastDel(logFile);
        Assert.state(!fastDel, "清理日志文件失败");
        //
        commandExecLogService.delByKey(id);
        return ApiResult.success("操作成功");
    }

    /**
     * 命令执行记录
     *
     * @param commandId 命令ID
     * @param batchId   批次ID
     * @return result
     * @api {GET}  node/ssh_command_log/batch_list 命令执行记录
     * @apiGroup node/ssh_command_log
     * @apiUse defResultJson
     * @apiParam {String} commandId 命令ID
     * @apiParam {String} batchId 批次ID
     * @apiSuccess {Object} commandExecLogModels 命令执行记录
     * @apiSuccess {String} commandExecLogModels.commandId 命令ID
     * @apiSuccess {String} commandExecLogModels.batchId 批次ID
     * @apiSuccess {String} commandExecLogModels.sshId ssh Id
     * @apiSuccess {Number} commandExecLogModels.status Status
     * @apiSuccess {String} commandExecLogModels.commandName 命令名称
     * @apiSuccess {String} commandExecLogModels.sshName ssh 名称
     * @apiSuccess {String} commandExecLogModels.params 参数
     * @apiSuccess {Number} commandExecLogModels.triggerExecType 触发类型 {0，手动，1 自动触发}
     * @apiSuccess {Boolean} commandExecLogModels.hasLog 日志文件是否存在
     */
    @GetMapping(value = "batch_list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<CommandExecLogModel>> batchList(@ValidatorItem String commandId, @ValidatorItem String batchId, HttpServletRequest request) {
        CommandExecLogModel commandExecLogModel = new CommandExecLogModel();
        String workspace = commandExecLogService.getCheckUserWorkspace(request);
        commandExecLogModel.setWorkspaceId(workspace);
        commandExecLogModel.setCommandId(commandId);
        commandExecLogModel.setBatchId(batchId);
        List<CommandExecLogModel> commandExecLogModels = commandExecLogService.listByBean(commandExecLogModel);

        return ApiResult.success("", commandExecLogModels);
    }

    /**
     * 获取日志
     *
     * @param id   id
     * @param line 需要获取的行号
     * @return json
     * @api {POST} node/ssh_command_log/log 获取日志
     * @apiGroup node/ssh_command_log
     * @apiUse defResultJson
     * @apiParam {String} id 日志 id
     * @apiParam {Number} line 需要获取的行号
     * @apiSuccess {Boolean} run 运行状态
     */
    @RequestMapping(value = "log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> log(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String id,
                                        @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "行号错误") int line, HttpServletRequest request) {
        CommandExecLogModel item = commandExecLogService.getByKey(id, request);
        Assert.notNull(item, "没有对应数据");

        File file = item.logFile();
        if (!FileUtil.exist(file)) {
            return ApiResult.success("还没有日志信息");
        }
        Assert.state(FileUtil.isFile(file), "日志文件错误");

        JSONObject data = FileUtils.readLogFile(file, line);
        // 运行中
        Integer status = item.getStatus();
        data.put("run", status != null && status == CommandExecLogModel.Status.ING.getCode());

        return ApiResult.success("", data);
    }

    /**
     * 下载日志
     *
     * @param logId 日志 id
     * @api {GET} node/ssh_command_log/download_log 下载日志
     * @apiGroup node/ssh_command_log
     * @apiUse defResultJson
     * @apiParam {String} logId 日志 id
     * @apiSuccess {File} file 日志文件
     */
    @RequestMapping(value = "download_log", method = RequestMethod.GET)
    @ResponseBody
    @Feature(method = MethodFeature.DOWNLOAD)
    public void downloadLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String logId, HttpServletRequest request, HttpServletResponse response) {
        CommandExecLogModel item = commandExecLogService.getByKey(logId, request);
        Assert.notNull(item, "没有对应数据");
        File logFile = item.logFile();
        if (!FileUtil.exist(logFile)) {
            return;
        }
        if (logFile.isFile()) {
            JakartaServletUtil.write(response, logFile);
        }
    }
}
