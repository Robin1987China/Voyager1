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

import io.voyager1.util.Tuple;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseAgentController;
import io.voyager1.common.commander.ProjectCommander;
import io.voyager1.model.RunMode;
import io.voyager1.model.data.DslYmlDto;
import io.voyager1.model.data.NodeProjectInfoModel;
import io.voyager1.service.script.DslScriptServer;
import io.voyager1.socket.ConsoleCommandOp;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理的信息获取接口
 *
 * @since 2019/4/16
 */
@RestController
@RequestMapping(value = "/manage/")
@Slf4j
public class ProjectListController extends BaseAgentController {

    private final ProjectCommander projectCommander;
    private final DslScriptServer dslScriptServer;

    public ProjectListController(ProjectCommander projectCommander,
                                 DslScriptServer dslScriptServer) {
        this.projectCommander = projectCommander;
        this.dslScriptServer = dslScriptServer;
    }

    /**
     * 获取项目的信息
     *
     * @param id id
     * @return item
     * @see NodeProjectInfoModel
     */
    @RequestMapping(value = "getProjectItem", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<NodeProjectInfoModel> getProjectItem(String id) {
        NodeProjectInfoModel nodeProjectInfoModel = projectInfoService.getItem(id);
        if (nodeProjectInfoModel != null) {
            RunMode runMode = nodeProjectInfoModel.getRunMode();
            if (runMode != RunMode.Dsl && runMode != RunMode.File) {
                // 返回实际执行的命令
                String command = projectCommander.buildRunCommand(nodeProjectInfoModel);
                nodeProjectInfoModel.setRunCommand(command);
            }
            if (runMode == RunMode.Dsl) {
                DslYmlDto dslYmlDto = nodeProjectInfoModel.mustDslConfig();
                boolean reload = dslYmlDto.hasRunProcess(ConsoleCommandOp.reload.name());
                nodeProjectInfoModel.setCanReload(reload);
                // 查询 dsl 流程信息
                List<JSONObject> list = Arrays.stream(ConsoleCommandOp.values())
                    .filter(ConsoleCommandOp::isCanOpt)
                    .map(consoleCommandOp -> {
                        Tuple tuple = dslScriptServer.resolveProcessScript(nodeProjectInfoModel, dslYmlDto, consoleCommandOp);
                        JSONObject jsonObject = tuple.get(0);
                        jsonObject.put("process", consoleCommandOp);
                        return jsonObject;
                    })
                    .collect(Collectors.toList());
                nodeProjectInfoModel.setDslProcessInfo(list);
            }
        }
        return ApiResult.success("", nodeProjectInfoModel);
    }

    /**
     * 程序项目信息
     *
     * @return json
     */
    @RequestMapping(value = "getProjectInfo", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<NodeProjectInfoModel>> getProjectInfo() {
        // 查询数据
        List<NodeProjectInfoModel> nodeProjectInfoModels = projectInfoService.list();
        return ApiResult.success("", nodeProjectInfoModels);
    }
}
