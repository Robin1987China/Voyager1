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

package io.voyager1.service.script;

import io.voyager1.util.FileUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.Tuple;
import io.voyager1.util.MapUtil;
import io.voyager1.util.UrlQuery;
import io.voyager1.util.CharsetUtil;
import io.voyager1.util.IdUtil;
import io.voyager1.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.Voyager1Application;
import io.voyager1.common.Const;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.i18n.I18nThreadUtil;
import io.voyager1.exception.IllegalArgument2Exception;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.model.data.DslYmlDto;
import io.voyager1.model.data.NodeProjectInfoModel;
import io.voyager1.model.data.NodeScriptModel;
import io.voyager1.model.data.ScriptLibraryModel;
import io.voyager1.script.DslScriptBuilder;
import io.voyager1.service.manage.ProjectInfoService;
import io.voyager1.service.system.AgentWorkspaceEnvVarService;
import io.voyager1.socket.ConsoleCommandOp;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Future;
import java.nio.charset.StandardCharsets;

/**
 * @since 23/12/30 030
 */
@Service
public class DslScriptServer {

    private final AgentWorkspaceEnvVarService agentWorkspaceEnvVarService;
    private final NodeScriptServer nodeScriptServer;
    private final Voyager1Application voyager1Application;
    private final ProjectInfoService projectInfoService;
    private final ScriptLibraryService scriptLibraryService;

    public DslScriptServer(AgentWorkspaceEnvVarService agentWorkspaceEnvVarService,
                           NodeScriptServer nodeScriptServer,
                           Voyager1Application voyager1Application,
                           ProjectInfoService projectInfoService,
                           ScriptLibraryService scriptLibraryService) {
        this.agentWorkspaceEnvVarService = agentWorkspaceEnvVarService;
        this.nodeScriptServer = nodeScriptServer;
        this.voyager1Application = voyager1Application;
        this.projectInfoService = projectInfoService;
        this.scriptLibraryService = scriptLibraryService;
    }

    /**
     * 异步执行
     *
     * @param dslYmlDto        dsl 配置
     * @param consoleCommandOp 操作
     */
    public void run(DslYmlDto dslYmlDto, ConsoleCommandOp consoleCommandOp, NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel, boolean sync) throws Exception {
        String log = projectInfoService.resolveAbsoluteLog(nodeProjectInfoModel, originalModel);
        DslScriptBuilder builder = this.create(dslYmlDto, consoleCommandOp, nodeProjectInfoModel, originalModel, log);
        Future<?> execute = I18nThreadUtil.execAsync(builder);
        if (sync) {
            execute.get();
        }
    }

    /**
     * 同步执行
     *
     * @param dslYmlDto        dsl 配置
     * @param consoleCommandOp 操作
     */
    public Tuple syncRun(DslYmlDto dslYmlDto, ConsoleCommandOp consoleCommandOp, NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel) {
        try (DslScriptBuilder builder = this.create(dslYmlDto, consoleCommandOp, nodeProjectInfoModel, originalModel, null)) {
            return builder.syncExecute();
        }
    }

    /**
     * 解析流程脚本信息
     *
     * @param nodeProjectInfoModel 项目信息
     * @param dslYml               dsl 配置信息
     * @param op                   流程
     * @return data
     */
    public Tuple resolveProcessScript(NodeProjectInfoModel nodeProjectInfoModel, DslYmlDto dslYml, ConsoleCommandOp op) {
        DslYmlDto.BaseProcess baseProcess = dslYml.tryDslProcess(op.name());
        return this.resolveProcessScript(nodeProjectInfoModel, baseProcess);
    }

    /**
     * 解析流程脚本信息
     *
     * @param nodeProjectInfoModel 项目信息
     * @param scriptProcess        流程
     * @return data
     */
    private Tuple resolveProcessScript(NodeProjectInfoModel nodeProjectInfoModel, DslYmlDto.BaseProcess scriptProcess) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", false);
        if (scriptProcess == null) {
            String value = "流程不存在";
            jsonObject.put("msg", value);
            return new Tuple(jsonObject, null);
        }
        String scriptId = scriptProcess.getScriptId();
        if ((scriptId == null || scriptId.isEmpty())) {
            String value = "请填写脚本模板id";
            jsonObject.put("msg", value);
            return new Tuple(jsonObject, null);
        }
        if ((scriptId != null && scriptId.toLowerCase().startsWith("G@".toLowerCase()))) {
            // 判断是否引用脚本库
            scriptId = scriptId.substring(2);
            ScriptLibraryModel libraryModel = scriptLibraryService.get(scriptId);
            if (libraryModel != null) {
                jsonObject.put("status", true);
                jsonObject.put("type", "library");
                jsonObject.put("scriptId", scriptId);
                return new Tuple(jsonObject, libraryModel);
            } else {
                String string = "对应的脚本库不存在：" + scriptId;
                jsonObject.put("msg", string);
                return new Tuple(jsonObject, null);
            }
        }
        //
        NodeScriptModel item = nodeScriptServer.getItem(scriptId);
        if (item != null) {
            // 脚本存在
            jsonObject.put("status", true);
            jsonObject.put("type", "script");
            jsonObject.put("scriptId", scriptId);
            return new Tuple(jsonObject, item);
        }
        File lib = projectInfoService.resolveLibFile(nodeProjectInfoModel);
        File scriptFile = FileUtil.file(lib, scriptId);
        if (FileUtil.isFile(scriptFile)) {
            // 文件存在
            jsonObject.put("status", true);
            jsonObject.put("type", "file");
            jsonObject.put("scriptId", scriptId);
            return new Tuple(jsonObject, scriptFile);
        }
        String value = "脚本模版不存在:" + scriptId;
        jsonObject.put("msg", value);
        return new Tuple(jsonObject, null);
    }

    /**
     * 构建 DSL 执行器
     *
     * @param dslYmlDto            脚本流程
     * @param nodeProjectInfoModel 项目
     * @param log                  日志路径
     * @param consoleCommandOp     具体操作
     */
    private DslScriptBuilder create(DslYmlDto dslYmlDto, ConsoleCommandOp consoleCommandOp, NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel, String log) {
        DslYmlDto.BaseProcess scriptProcess = dslYmlDto.getDslProcess(consoleCommandOp.name());
        Tuple tuple = this.resolveProcessScript(originalModel, scriptProcess);
        JSONObject jsonObject = tuple.get(0);
        // 判断状态
        boolean status = jsonObject.getBooleanValue("status");
        if (!status) {
            String msg = jsonObject.getString("msg");
            throw new IllegalArgument2Exception(msg);
        }
        String type = jsonObject.getString("type");
        EnvironmentMapBuilder environment = this.environment(nodeProjectInfoModel, scriptProcess);
        environment.put("PROJECT_LOG_FILE", log);
        DslYmlDto.Run run = dslYmlDto.getRun();
        String execPath = run.getExecPath();
        environment.put("VOYAGER1_EXEC_PATH", execPath);
        File scriptFile;
        boolean autoDelete = false;
        if (java.util.Objects.equals(type, "file")) {
            // 项目文件
            scriptFile = tuple.get(1);
        } else if ("library".equals(type)) {
            // 脚本库
            ScriptLibraryModel libraryModel = tuple.get(1);
            scriptFile = this.initScriptFile(libraryModel);
            // 系统生成的脚本需要自动删除
            autoDelete = true;
        } else {
            // 节点脚本
            NodeScriptModel item = tuple.get(1);
            scriptFile = this.initScriptFile(item);
            // 系统生成的脚本需要自动删除
            autoDelete = true;
        }
        Charset charset = projectInfoService.resolveLogCharset(nodeProjectInfoModel, originalModel);
        DslScriptBuilder builder = new DslScriptBuilder(consoleCommandOp.name(), environment, scriptProcess.getScriptArgs(), log, charset);
        builder.setScriptFile(scriptFile);
        builder.setAutoDelete(autoDelete);
        return builder;
    }

    /**
     * 创建脚本文件
     *
     * @param scriptModel 脚本对象
     * @return file
     */
    private File initScriptFile(NodeScriptModel scriptModel) {
        return nodeScriptServer.toExecuteFile(scriptModel);
    }

    /**
     * 创建脚本文件
     *
     * @param scriptModel 脚本对象
     * @return file
     */
    private File initScriptFile(ScriptLibraryModel scriptModel) {
        String dataPath = voyager1Application.getDataPath();
        File scriptFile = FileUtil.file(dataPath, Const.SCRIPT_RUN_CACHE_DIRECTORY, String.format("%s.%s", java.util.UUID.randomUUID().toString().replace("-", ""), CommandUtil.SUFFIX));
        // 替换内容
        String context = scriptModel.getScript();
        FileUtils.writeScript(context, scriptFile, ExtConfigBean.getConsoleLogCharset());
        return scriptFile;
    }

    private EnvironmentMapBuilder environment(NodeProjectInfoModel nodeProjectInfoModel, DslYmlDto.BaseProcess scriptProcess) {
        //
        EnvironmentMapBuilder environmentMapBuilder = agentWorkspaceEnvVarService.getEnv(nodeProjectInfoModel.getWorkspaceId());
        // 项目配置的环境变量
        String dslEnv = nodeProjectInfoModel.getDslEnv();
        Opt.ofBlankAble(dslEnv)
            .map(s -> UrlQuery.of(s, StandardCharsets.UTF_8))
            .map(UrlQuery::getQueryMap)
            .map(map -> {
                Map<String, String> map1 = new java.util.HashMap<>();
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    map1.put(StrUtil.toString(entry.getKey()), StrUtil.toString(entry.getValue()));
                }
                return map1;
            })
            .ifPresent(environmentMapBuilder::putStr);
        String lib = projectInfoService.resolveLibPath(nodeProjectInfoModel);
        //
        environmentMapBuilder
            .putStr(scriptProcess.getScriptEnv())
            .put("PROJECT_ID", nodeProjectInfoModel.getId())
            .put("PROJECT_NAME", nodeProjectInfoModel.getName())
            .put("PROJECT_PATH", lib);
        return environmentMapBuilder;
    }
}
