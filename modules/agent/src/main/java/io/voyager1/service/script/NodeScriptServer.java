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
import io.voyager1.util.IdUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.Task;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.cron.ICron;
import io.voyager1.Voyager1Application;
import io.voyager1.common.AgentConst;
import io.voyager1.common.Const;
import io.voyager1.cron.CronUtils;
import io.voyager1.model.data.NodeScriptExecLogModel;
import io.voyager1.model.data.NodeScriptModel;
import io.voyager1.script.NodeScriptProcessBuilder;
import io.voyager1.service.BaseWorkspaceOptService;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.FileUtils;
import io.voyager1.util.StringUtil;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 脚本模板管理
 *
 * @since 2019/4/24
 */
@Service
public class NodeScriptServer extends BaseWorkspaceOptService<NodeScriptModel> implements ICron<NodeScriptModel> {
    private final NodeScriptExecLogServer execLogServer;
    private final Voyager1Application voyager1Application;
    private final ScriptLibraryService scriptLibraryService;

    public NodeScriptServer(NodeScriptExecLogServer execLogServer,
                            Voyager1Application voyager1Application,
                            ScriptLibraryService scriptLibraryService) {
        super(AgentConst.SCRIPT);
        this.execLogServer = execLogServer;
        this.voyager1Application = voyager1Application;
        this.scriptLibraryService = scriptLibraryService;
    }

    @Override
    public void addItem(NodeScriptModel nodeScriptModel) {
        super.addItem(nodeScriptModel);
        this.checkCron(nodeScriptModel);
    }

    @Override
    public void updateItem(NodeScriptModel nodeScriptModel) {
        super.updateItem(nodeScriptModel);
        this.checkCron(nodeScriptModel);
    }

    /**
     * @param id 数据id
     * @see NodeScriptModel#logFile(String)
     */
    @Override
    public void deleteItem(String id) {
        NodeScriptModel nodeScriptModel = getItem(id);
        if (nodeScriptModel != null) {
            File file = nodeScriptModel.scriptPath();
            FileUtil.del(file);
        }
        super.deleteItem(id);
        String taskId = "script:" + id;
        CronUtils.remove(taskId);
    }

    public File toExecuteFile(NodeScriptModel nodeScriptModel) {
        String dataPath = voyager1Application.getDataPath();
        File scriptFile = FileUtil.file(dataPath, Const.SCRIPT_RUN_CACHE_DIRECTORY, String.format("%s.%s", java.util.UUID.randomUUID().toString().replace("-", ""), CommandUtil.SUFFIX));
        String context = nodeScriptModel.getContext();
        //
        context = scriptLibraryService.referenceReplace(context);
        FileUtils.writeScript(context, scriptFile, ExtConfigBean.getConsoleLogCharset());
        return scriptFile;
    }

    @Override
    public boolean checkCron(NodeScriptModel nodeScriptModel) {
        String id = "script:" + nodeScriptModel.getId();
        String autoExecCron = nodeScriptModel.getAutoExecCron();
        autoExecCron = StringUtil.parseCron(autoExecCron);
        if ((autoExecCron == null || autoExecCron.isEmpty())) {
            CronUtils.remove(id);
            return false;
        } else {
            CronUtils.upsert(id, autoExecCron, new CronTask(nodeScriptModel.getId()));
            return true;
        }
    }

    @Override
    public List<NodeScriptModel> queryStartingList() {
        return this.list();
    }

    private static class CronTask implements Task {
        private final String id;

        public CronTask(String id) {
            this.id = id;
        }

        @Override
        public void execute() {
            NodeScriptServer nodeScriptServer = SpringContextHolder.getBean(NodeScriptServer.class);
            NodeScriptModel scriptServerItem = nodeScriptServer.getItem(id);
            if (scriptServerItem == null) {
                return;
            }
            // 创建记录
            NodeScriptExecLogServer execLogServer = SpringContextHolder.getBean(NodeScriptExecLogServer.class);
            NodeScriptExecLogModel nodeScriptExecLogModel = new NodeScriptExecLogModel();
            nodeScriptExecLogModel.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
            nodeScriptExecLogModel.setCreateTimeMillis(System.currentTimeMillis());
            nodeScriptExecLogModel.setScriptId(scriptServerItem.getId());
            nodeScriptExecLogModel.setScriptName(scriptServerItem.getName());
            nodeScriptExecLogModel.setWorkspaceId(scriptServerItem.getWorkspaceId());
            nodeScriptExecLogModel.setTriggerExecType(1);
            execLogServer.addItem(nodeScriptExecLogModel);
            // 执行
            NodeScriptProcessBuilder.create(scriptServerItem, nodeScriptExecLogModel.getId(), scriptServerItem.getDefArgs(), null);
        }
    }

    /**
     * 执行脚本
     *
     * @param scriptServerItem 脚本
     * @param type             类型
     * @param args             参数
     * @return 执行记录ID
     */
    public String execute(NodeScriptModel scriptServerItem, int type, String uerName, String workspaceId, String args, Map<String, String> paramMap) {
        NodeScriptExecLogModel nodeScriptExecLogModel = new NodeScriptExecLogModel();
        nodeScriptExecLogModel.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
        nodeScriptExecLogModel.setCreateTimeMillis(System.currentTimeMillis());
        nodeScriptExecLogModel.setScriptId(scriptServerItem.getId());
        nodeScriptExecLogModel.setScriptName(scriptServerItem.getName());
        nodeScriptExecLogModel.setModifyUser(uerName);
        nodeScriptExecLogModel.setWorkspaceId((workspaceId == null || workspaceId.isEmpty() ? scriptServerItem.getWorkspaceId() : workspaceId));
        nodeScriptExecLogModel.setTriggerExecType(type);
        execLogServer.addItem(nodeScriptExecLogModel);
        String userArgs = (args == null || args.isEmpty() ? scriptServerItem.getDefArgs() : args);
        // 执行
        NodeScriptProcessBuilder.create(scriptServerItem, nodeScriptExecLogModel.getId(), userArgs, paramMap);
        return nodeScriptExecLogModel.getId();
    }
}
