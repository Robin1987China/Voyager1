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

import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.IdUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseAgentController;
import io.voyager1.common.Const;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.model.data.NodeScriptExecLogModel;
import io.voyager1.model.data.NodeScriptModel;
import io.voyager1.script.NodeScriptProcessBuilder;
import io.voyager1.service.script.NodeScriptExecLogServer;
import io.voyager1.service.script.NodeScriptServer;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.FileUtils;
import io.voyager1.util.StringUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 脚本管理
 *
 * @since 2019/4/24
 */
@RestController
@RequestMapping(value = "/script")
public class ScriptController extends BaseAgentController {

    private final NodeScriptServer nodeScriptServer;
    private final NodeScriptExecLogServer nodeScriptExecLogServer;

    public ScriptController(NodeScriptServer nodeScriptServer,
                            NodeScriptExecLogServer nodeScriptExecLogServer) {
        this.nodeScriptServer = nodeScriptServer;
        this.nodeScriptExecLogServer = nodeScriptExecLogServer;
    }

    @RequestMapping(value = "list.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<NodeScriptModel>> list() {
        return ApiResult.success("", nodeScriptServer.list());
    }

    @RequestMapping(value = "item.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<NodeScriptModel> item(String id) {
        return ApiResult.success("", nodeScriptServer.getItem(id));
    }

    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Object> save(NodeScriptModel nodeScriptModel, String type, String global, String nodeId) {
        Assert.notNull(nodeScriptModel, "没有数据");
        Assert.hasText(nodeScriptModel.getContext(), "内容为空");
        //
        String autoExecCron = nodeScriptModel.getAutoExecCron();
        autoExecCron = StringUtil.checkCron(autoExecCron, s -> s);
        //
        boolean globalBool = ConvertUtil.toBool(global, false);
        if (globalBool) {
            nodeScriptModel.setWorkspaceId(Const.WORKSPACE_GLOBAL);
        } else {
            nodeScriptModel.setWorkspaceId(getWorkspaceId());
        }
        //
        nodeScriptModel.setContext(nodeScriptModel.getContext());
        NodeScriptModel eModel = nodeScriptServer.getItem(nodeScriptModel.getId());
        boolean needCreate = false;
        if ("add".equalsIgnoreCase(type)) {
            Assert.isNull(eModel, "id已经存在啦");

            nodeScriptModel.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
            nodeScriptModel.setNodeId(nodeId);
            nodeScriptServer.addItem(nodeScriptModel);
            return ApiResult.success("添加成功");
        } else if ("sync".equalsIgnoreCase(type)) {
            // 同步脚本
            if (eModel == null) {
                eModel = new NodeScriptModel();
                eModel.setId(nodeScriptModel.getId());
                eModel.setNodeId(nodeId);
                needCreate = true;
            } else {
                if (!eModel.global() && nodeScriptModel.global()) {
                    // 修改绑定的节点id
                    eModel.setNodeId(nodeId);
                }
            }
            eModel.setScriptType("server-sync");
            eModel.setWorkspaceId(nodeScriptModel.getWorkspaceId());
        }
        Assert.notNull(eModel, "对应数据不存在");
        eModel.setName(nodeScriptModel.getName());
        eModel.setAutoExecCron(autoExecCron);
        eModel.setDescription(nodeScriptModel.getDescription());
        eModel.setContext(nodeScriptModel.getContext());
        eModel.setDefArgs(nodeScriptModel.getDefArgs());
        if (needCreate) {
            nodeScriptServer.addItem(eModel);
        } else {
            nodeScriptServer.updateItem(eModel);
        }
        return ApiResult.success("修改成功");
    }

    @RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Object> del(String id) {
        nodeScriptServer.deleteItem(id);
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
    public ApiResult<JSONObject> getNowLog(@ValidatorItem(msg = "日志ID缺失") String id,
                                              @ValidatorItem(msg = "执行ID缺失") String executeId,
                                              @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "行号不正确") int line) {
        NodeScriptModel item = nodeScriptServer.getItem(id);
        Assert.notNull(item, "没有对应数据");
        File logFile = item.logFile(executeId);
        Assert.state(FileUtil.isFile(logFile), "日志文件错误");

        JSONObject data = FileUtils.readLogFile(logFile, line);
        // 运行中
        data.put("run", NodeScriptProcessBuilder.isRun(executeId));
        return ApiResult.success("", data);
    }

    /**
     * 删除日志
     *
     * @param id        id
     * @param executeId 执行ID
     * @return json
     */
    @RequestMapping(value = "del_log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Object> delLog(@ValidatorItem(msg = "日志ID缺失") String id,
                                       @ValidatorItem(msg = "执行ID缺失") String executeId) {
        NodeScriptModel item = nodeScriptServer.getItem(id);
        if (item == null) {
            return ApiResult.success("对应的脚本模版已经不存在拉");
        }
        Assert.notNull(item, "没有对应数据");
        File logFile = item.logFile(executeId);
        boolean fastDel = CommandUtil.systemFastDel(logFile);
        Assert.state(!fastDel, "删除日志文件失败");
        return ApiResult.success("删除成功");
    }

    /**
     * 执行
     *
     * @param id     ID
     * @param args   执行参数
     * @param params 环境变量参数
     * @return json
     */
    @RequestMapping(value = "exec", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> exec(@ValidatorItem() String id, String args, String params) {
        NodeScriptModel item = nodeScriptServer.getItem(id);
        Assert.notNull(item, "对应脚本已经不存在啦");
        String nowUserName = getNowUserName();

        Map<String, String> paramMap = Opt.ofBlankAble(params)
            .map(JSONObject::parseObject)
            .map(jsonObject -> {
                Map<String, String> paramMap1 = new HashMap<>(10);
                for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                    String key = String.format("trigger_%s", entry.getKey());
                    key = StrUtil.toUnderlineCase(key);
                    paramMap1.put(key, StrUtil.toString(entry.getValue()));
                }
                return paramMap1;
            })
            .orElse(null);
        //

        String execute = nodeScriptServer.execute(item, 2, nowUserName, null, args, paramMap);
        return ApiResult.success("开始执行", execute);
    }

    /**
     * 同步定时执行日志
     *
     * @param pullCount 领取个数
     * @return json
     */
    @RequestMapping(value = "pull_exec_log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<NodeScriptExecLogModel>> pullExecLog(@ValidatorItem int pullCount) {
        Assert.state(pullCount > 0, "pull count error");
        List<NodeScriptExecLogModel> list = nodeScriptExecLogServer.list();
        list = list.subList(0, pullCount);
        if (list == null) {
            return ApiResult.success("", Collections.emptyList());
        }
        return ApiResult.success("", list);
    }

    /**
     * 删除定时执行日志
     *
     * @param jsonObject 拉起参数
     * @return json
     */
    @RequestMapping(value = "del_exec_log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Object> delExecLog(@RequestBody JSONObject jsonObject) {
        JSONArray ids = jsonObject.getJSONArray("ids");
        if (ids != null) {
            for (Object id : ids) {
                String idStr = (String) id;
                nodeScriptExecLogServer.deleteItem(idStr);
            }
        }
        return ApiResult.success("删除成功");
    }

    @RequestMapping(value = "change-workspace-id", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Object> changeWorkspaceId(@ValidatorItem() String id, String newWorkspaceId, String newNodeId) {
        Assert.hasText(newWorkspaceId, "请选择要修改的工作空间");
        Assert.hasText(newWorkspaceId, "请选择要修改的节");
        NodeScriptModel item = nodeScriptServer.getItem(id);
        Assert.notNull(item, "找不到对应的脚本信息");
        //
        NodeScriptModel update = new NodeScriptModel();
        update.setNodeId(newNodeId);
        update.setWorkspaceId(newWorkspaceId);
        nodeScriptServer.updateById(update, item.getId());
        return ApiResult.success("修改成功");
    }
}
