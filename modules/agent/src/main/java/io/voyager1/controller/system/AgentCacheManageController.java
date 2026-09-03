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

import io.voyager1.util.DateTime;
import io.voyager1.util.FileUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.event.ICacheTask;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.Voyager1Application;
import io.voyager1.common.BaseAgentController;
import io.voyager1.common.Voyager1Manifest;
import io.voyager1.common.commander.AbstractProjectCommander;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.configuration.AgentConfig;
import io.voyager1.configuration.SystemConfig;
import io.voyager1.cron.CronUtils;
import io.voyager1.model.data.ScriptLibraryModel;
import io.voyager1.model.system.WorkspaceEnvVarModel;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.script.NodeScriptExecLogServer;
import io.voyager1.service.script.ScriptLibraryService;
import io.voyager1.service.system.AgentWorkspaceEnvVarService;
import io.voyager1.socket.AgentFileTailWatcher;
import io.voyager1.util.CommandUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * 缓存管理
 *
 * @since 2019/7/20
 */
@RestController
@RequestMapping(value = "system")
public class AgentCacheManageController extends BaseAgentController implements ICacheTask {

    private final AgentWorkspaceEnvVarService agentWorkspaceEnvVarService;
    private final Voyager1Application configBean;
    private final NodeScriptExecLogServer nodeScriptExecLogServer;
    private final SystemConfig systemConfig;
    private final ScriptLibraryService scriptLibraryService;

    private long dataSize;
    private long oldJarsSize;
    private long tempFileSize;

    public AgentCacheManageController(AgentWorkspaceEnvVarService agentWorkspaceEnvVarService,
                                      Voyager1Application configBean,
                                      NodeScriptExecLogServer nodeScriptExecLogServer,
                                      AgentConfig agentConfig,
                                      ScriptLibraryService scriptLibraryService) {
        this.agentWorkspaceEnvVarService = agentWorkspaceEnvVarService;
        this.configBean = configBean;
        this.nodeScriptExecLogServer = nodeScriptExecLogServer;
        this.systemConfig = agentConfig.getSystem();
        this.scriptLibraryService = scriptLibraryService;
    }

    /**
     * 缓存信息
     *
     * @return json
     */
    @PostMapping(value = "cache", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> cache() {
        JSONObject jsonObject = new JSONObject();
        //
        jsonObject.put("fileSize", this.tempFileSize);
        jsonObject.put("dataSize", this.dataSize);
        jsonObject.put("oldJarsSize", this.oldJarsSize);
        jsonObject.put("pidPort", AbstractProjectCommander.PID_PORT.size());

        int oneLineCount = AgentFileTailWatcher.getOneLineCount();
        jsonObject.put("readFileOnLineCount", oneLineCount);
        jsonObject.put("taskList", CronUtils.list());
        jsonObject.put("pluginSize", PluginFactory.size());
        //
        WorkspaceEnvVarModel item = agentWorkspaceEnvVarService.getItem(getWorkspaceId());
        if (item != null) {
            Map<String, WorkspaceEnvVarModel.WorkspaceEnvVarItemModel> varData = item.getVarData();
            if (varData != null) {
                jsonObject.put("envVarKeys", varData.keySet());
            }
        }
        //
        List<ScriptLibraryModel> scriptLibraryModels = scriptLibraryService.list();
        Map<String, String> scriptLibraryTagMap = scriptLibraryModels.stream()
            .collect(Collectors.toMap(ScriptLibraryModel::getTag, ScriptLibraryModel::getVersion));
        jsonObject.put("scriptLibraryTagMap", scriptLibraryTagMap);
        //
        jsonObject.put("dateTime", DateTime.now().toString());
        jsonObject.put("timeZoneId", TimeZone.getDefault().getID());
        // 待同步待日志数
        int size = nodeScriptExecLogServer.size();
        jsonObject.put("scriptExecLogSize", size);
        jsonObject.put("timerMatchSecond", systemConfig.isTimerMatchSecond());
        //
        return ApiResult.success("", jsonObject);
    }

    /**
     * 清空缓存
     *
     * @param type 缓存类型
     * @return json
     */
    @RequestMapping(value = "clearCache", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> clearCache(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "类型错误") String type) {
        switch (type) {
            case "pidPort":
                AbstractProjectCommander.PID_PORT.clear();
                break;
            case "oldJarsSize": {
                File oldJarsPath = Voyager1Manifest.getOldJarsPath();
                boolean clean = CommandUtil.systemFastDel(oldJarsPath);
                Assert.state(!clean, "清空旧版本重新包失败");
                break;
            }
            case "fileSize": {
                File tempPath = configBean.getTempPath();
                boolean clean = CommandUtil.systemFastDel(tempPath);
                Assert.state(!clean, "清空文件缓存失败");
                break;
            }
            default:
                return new ApiResult<>(405, "没有对应类型：" + type);

        }
        return ApiResult.success("清空成功");
    }

    @Override
    public void refreshCache() {
        File file = configBean.getTempPath();
        this.tempFileSize = FileUtil.size(file);
        this.dataSize = configBean.dataSize();
        File oldJarsPath = Voyager1Manifest.getOldJarsPath();
        this.oldJarsSize = FileUtil.size(oldJarsPath);
    }
}
