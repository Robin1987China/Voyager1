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

import io.voyager1.util.EnumUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseAgentController;
import io.voyager1.common.commander.CommandOpResult;
import io.voyager1.common.commander.ProjectCommander;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.model.data.NodeProjectInfoModel;
import io.voyager1.socket.ConsoleCommandOp;
import io.voyager1.util.CommandUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目文件管理
 *
 * @since 2019/4/17
 */
@RestController
@RequestMapping(value = "/manage/")
@Slf4j
public class ProjectStatusController extends BaseAgentController {
    private final ProjectCommander projectCommander;

    public ProjectStatusController(ProjectCommander projectCommander) {
        this.projectCommander = projectCommander;
    }

    /**
     * 获取项目的进程id
     *
     * @param id 项目id
     * @return json
     */
    @RequestMapping(value = "getProjectStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> getProjectStatus(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "项目id 不正确") String id, String getCopy) {
        NodeProjectInfoModel nodeProjectInfoModel = tryGetProjectInfoModel();
        Assert.notNull(nodeProjectInfoModel, "项目id不存在");
        JSONObject jsonObject = new JSONObject();
        try {
            CommandUtil.openCache();
            try {
                CommandOpResult status = projectCommander.execCommand(ConsoleCommandOp.status, nodeProjectInfoModel);
                jsonObject.put("pId", status.getPid());
                jsonObject.put("pIds", status.getPids());
                jsonObject.put("statusMsg", status.getStatusMsg());
            } catch (Exception e) {
                log.error("获取项目pid 失败", e);
            }
        } finally {
            CommandUtil.closeCache();
        }
        return ApiResult.success("", jsonObject);
    }

    /**
     * 获取项目的运行端口
     *
     * @param ids ids
     * @return obj
     */
    @RequestMapping(value = "getProjectPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> getProjectPort(String ids) {
        Assert.hasText(ids, "没有要获取的信息");
        JSONArray jsonArray = JSONArray.parseArray(ids);
        JSONObject jsonObject = new JSONObject();
        try {
            CommandUtil.openCache();
            for (Object object : jsonArray) {
                String item = object.toString();
                JSONObject itemObj = new JSONObject();
                try {
                    NodeProjectInfoModel projectInfoServiceItem = projectInfoService.getItem(item);
                    itemObj.put("name", projectInfoServiceItem.getName());
                    CommandOpResult commandOpResult = projectCommander.execCommand(ConsoleCommandOp.status, projectInfoServiceItem);
                    Integer pid = commandOpResult.getPid();
                    //
                    itemObj.put("pid", pid);
                    itemObj.put("pids", commandOpResult.getPids());
                    itemObj.put("statusMsg", commandOpResult.getStatusMsg());
                    if ((commandOpResult.getPorts() != null && !commandOpResult.getPorts().isEmpty())) {
                        itemObj.put("port", commandOpResult.getPorts());
                    } else {
                        String port = projectCommander.getMainPort(pid);
                        itemObj.put("port", port);
                    }
                } catch (Exception e) {
                    log.error("获取端口错误", e);
                    itemObj.put("error", e.getMessage());
                }
                jsonObject.put(item, itemObj);
            }
        } finally {
            CommandUtil.closeCache();
        }
        return ApiResult.success("", jsonObject);
    }


    @RequestMapping(value = "operate", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<CommandOpResult> operate(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "项目id 不正确") String id,
                                                 @ValidatorItem(msg = "操作参数缺失") String opt) {
        NodeProjectInfoModel item = projectInfoService.getItem(id);
        Assert.notNull(item, "没有找到对应的项目");
        ConsoleCommandOp consoleCommandOp = EnumUtil.fromStringQuietly(ConsoleCommandOp.class, opt);
        Assert.notNull(consoleCommandOp, "请选择操作类型");
        Assert.state(consoleCommandOp.isCanOpt(), "不支持当前操作：" + opt);
        CommandOpResult result = projectCommander.execCommand(consoleCommandOp, item);
        String success = "操作成功";
        String errorMsg = "操作失败:" + result.msgStr();
        return new ApiResult<>(result.isSuccess() ? 200 : 201, result.isSuccess() ? success : errorMsg, result);
    }
}
