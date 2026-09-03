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

package io.voyager1.controller;

import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.SystemUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseAgentController;
import io.voyager1.common.Voyager1Manifest;
import io.voyager1.common.RemoteVersion;
import io.voyager1.common.commander.ProjectCommander;
import io.voyager1.common.commander.SystemCommander;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.interceptor.NotAuthorize;
import io.voyager1.configuration.AgentConfig;
import io.voyager1.configuration.MonitorConfig;
import io.voyager1.model.data.NodeProjectInfoModel;
import io.voyager1.model.data.NodeScriptModel;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.manage.ProjectInfoService;
import io.voyager1.service.script.NodeScriptServer;
import io.voyager1.util.JvmUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 首页
 *
 * @since 2019/4/17
 */
@RestController
@Slf4j
public class IndexController extends BaseAgentController {

    private final ProjectInfoService projectInfoService;
    private final NodeScriptServer nodeScriptServer;
    private final SystemCommander systemCommander;
    private final ProjectCommander projectCommander;
    private final AgentConfig agentConfig;

    public IndexController(ProjectInfoService projectInfoService,
                           NodeScriptServer nodeScriptServer,
                           SystemCommander systemCommander,
                           ProjectCommander projectCommander,
                           AgentConfig agentConfig) {
        this.projectInfoService = projectInfoService;
        this.nodeScriptServer = nodeScriptServer;
        this.systemCommander = systemCommander;
        this.projectCommander = projectCommander;
        this.agentConfig = agentConfig;
    }

    @RequestMapping(value = {"index", "", "index.html", "/"}, produces = MediaType.TEXT_PLAIN_VALUE)
    @NotAuthorize
    public String index() {
        return "Voyager1-Agent,Can't access directly,Please configure it to VOYAGER1 server";
    }

    @RequestMapping(value = "info", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> info() {

        Voyager1Manifest instance = Voyager1Manifest.getInstance();
        io.voyager1.RemoteVersion remoteVersion = RemoteVersion.cacheInfo();
        //
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("manifest", instance);
        jsonObject.put("remoteVersion", remoteVersion);
        jsonObject.put("pluginSize", PluginFactory.size());
        jsonObject.put("joinBetaRelease", RemoteVersion.betaRelease());
        jsonObject.put("monitor", agentConfig.getMonitor());
        return ApiResult.success("", jsonObject);
    }

    /**
     * 获取节点统计信息
     *
     * @return json
     */
    @PostMapping(value = "get-stat-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> getDirectTop() {
        JSONObject jsonObject = new JSONObject();
        try {
            Optional<MonitorConfig> monitorConfig = Optional.ofNullable(agentConfig).map(AgentConfig::getMonitor);

            JSONObject topInfo = io.voyager1.util.OshiUtils.getSimpleInfo(monitorConfig.orElse(null));
            jsonObject.put("simpleStatus", topInfo);
            // 系统固定休眠时间
            jsonObject.put("systemSleep", io.voyager1.util.OshiUtils.NET_STAT_SLEEP + io.voyager1.util.OshiUtils.CPU_STAT_SLEEP);

            JSONObject systemInfo = io.voyager1.util.OshiUtils.getSystemInfo();
            jsonObject.put("systemInfo", systemInfo);
            //jsonObject.put("oshiError", "测试异常");
        } catch (Throwable e) {
            log.error("oshi 系统监控异常", e);
            jsonObject.put("oshiError", e.getMessage());
        }

        JSONObject voyager1Info = this.getVoyager1Info();
        jsonObject.put("voyager1Info", voyager1Info);
        return ApiResult.success("", jsonObject);
    }

    private JSONObject getVoyager1Info() {
        List<NodeProjectInfoModel> nodeProjectInfoModels = projectInfoService.list();
        List<NodeScriptModel> list = nodeScriptServer.list();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("javaVirtualCount", JvmUtil.getJavaVirtualCount());
        Voyager1Manifest instance = Voyager1Manifest.getInstance();
        jsonObject.put("voyager1Manifest", instance);
        jsonObject.put("javaVersion", SystemUtil.getJavaRuntimeInfo().getVersion());
        //  获取JVM中内存总大小
        jsonObject.put("totalMemory", SystemUtil.getTotalMemory());
        //
        jsonObject.put("freeMemory", SystemUtil.getFreeMemory());
        Map<String, JSONObject> workspaceMap = new HashMap<>(4);
        //
        {
            for (NodeProjectInfoModel model : nodeProjectInfoModels) {
                JSONObject jsonObject1 = workspaceMap.computeIfAbsent(model.getWorkspaceId(), s -> {
                    JSONObject jsonObject11 = new JSONObject();
                    jsonObject11.put("projectCount", 0);
                    jsonObject11.put("scriptCount", 0);
                    return jsonObject11;
                });
                jsonObject1.merge("projectCount", 1, (v1, v2) -> Integer.sum((Integer) v1, (Integer) v2));
            }
            jsonObject.put("projectCount", (nodeProjectInfoModels == null ? 0 : nodeProjectInfoModels.size()));
        }
        {
            for (NodeScriptModel model : list) {
                JSONObject jsonObject1 = workspaceMap.computeIfAbsent(model.getWorkspaceId(), s -> {
                    JSONObject jsonObject11 = new JSONObject();
                    jsonObject11.put("projectCount", 0);
                    jsonObject11.put("scriptCount", 0);
                    return jsonObject11;
                });
                jsonObject1.merge("scriptCount", 1, (v1, v2) -> Integer.sum((Integer) v1, (Integer) v2));
            }
            jsonObject.put("scriptCount", (list == null ? 0 : list.size()));
        }
        jsonObject.put("workspaceStat", workspaceMap);
        return jsonObject;
    }


    @RequestMapping(value = "processList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<JSONObject>> getProcessList(String processName, Integer count) {
        try {
            processName = (processName == null || processName.isEmpty() ? "java" : processName);
            List<JSONObject> processes = io.voyager1.util.OshiUtils.getProcesses(processName, ConvertUtil.toInt(count, 20));
            processes = processes.stream()
                .peek(jsonObject -> {
                    int processId = jsonObject.getIntValue("processId");
                    String port = projectCommander.getMainPort(processId);
                    jsonObject.put("port", port);
                    //
                })
                .collect(Collectors.toList());
            return ApiResult.success("", processes);
        } catch (Throwable e) {
            log.error("oshi 系统进程监控异常", e);
            throw new IllegalStateException("系统进程监控异常：" + e.getMessage());
        }
    }


    @PostMapping(value = "kill.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> kill(int pid) {
        long voyager1AgentId = Voyager1Manifest.getInstance().getPid();
        Assert.state(!java.util.Objects.equals(String.valueOf(voyager1AgentId), String.valueOf(pid)), "不支持在线关闭 Agent 进程");
        String result = systemCommander.kill(null, pid);
        if ((result == null || result.isEmpty())) {
            result = "成功kill";
        }
        return ApiResult.success(result);
    }

    @PostMapping(value = "disk-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<JSONObject>> diskInfo() {
        try {
            List<JSONObject> list = io.voyager1.util.OshiUtils.fileStores();
            return ApiResult.success("", list);
        } catch (Throwable e) {
            log.error("oshi 文件系统资源监控异常", e);
            throw new IllegalStateException("文件系统监控异常：" + e.getMessage());
        }
    }

    @PostMapping(value = "hw-disk--info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<JSONObject>> hwDiskInfo() {
        try {
            List<JSONObject> list = io.voyager1.util.OshiUtils.diskStores();
            return ApiResult.success("", list);
        } catch (Throwable e) {
            log.error("oshi 硬盘资源监控异常", e);
            throw new IllegalStateException("硬盘资源监控异常：" + e.getMessage());
        }
    }

    @PostMapping(value = "network-interfaces", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<JSONObject>> networkInterfaces() {
        try {
            List<JSONObject> list = io.voyager1.util.OshiUtils.networkInterfaces();
            return ApiResult.success("", list);
        } catch (Throwable e) {
            log.error("oshi 网卡资源监控异常", e);
            throw new IllegalStateException("网卡资源监控异常：" + e.getMessage());
        }
    }
}
