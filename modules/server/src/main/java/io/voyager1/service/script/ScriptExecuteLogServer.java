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
import io.voyager1.util.IoUtil;
import io.voyager1.util.IdUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.Voyager1Application;
import io.voyager1.common.Const;
import io.voyager1.core.entity.ScriptExecuteLogEntity;
import io.voyager1.core.jpa.JpaGlobalOrWorkspaceService;
import io.voyager1.core.repository.ScriptExecuteLogRepository;
import io.voyager1.func.assets.server.ScriptLibraryServer;
import io.voyager1.model.data.CommandExecLogModel;
import io.voyager1.model.script.ScriptExecuteLogModel;
import io.voyager1.model.script.ScriptModel;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.FileUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;

/**
 * @since 2022/1/19
 */
@Service
public class ScriptExecuteLogServer extends JpaGlobalOrWorkspaceService<ScriptExecuteLogModel, ScriptExecuteLogEntity> {

    private final ScriptLibraryServer scriptLibraryServer;
    private final Voyager1Application voyager1Application;
    private final ScriptExecuteLogRepository scriptExecuteLogRepository;

    public ScriptExecuteLogServer(ScriptLibraryServer scriptLibraryServer,
                                  Voyager1Application voyager1Application,
                                  ScriptExecuteLogRepository scriptExecuteLogRepository) {
        this.scriptLibraryServer = scriptLibraryServer;
        this.voyager1Application = voyager1Application;
        this.scriptExecuteLogRepository = scriptExecuteLogRepository;
    }

    @Override
    protected JpaRepository<ScriptExecuteLogEntity, String> repository() {
        return scriptExecuteLogRepository;
    }

    @Override
    protected JpaSpecificationExecutor<ScriptExecuteLogEntity> specExecutor() {
        return scriptExecuteLogRepository;
    }

    @Override
    protected Class<ScriptExecuteLogEntity> entityClass() {
        return ScriptExecuteLogEntity.class;
    }

    @Override
    protected Class<ScriptExecuteLogModel> modelClass() {
        return ScriptExecuteLogModel.class;
    }

    /**
     * 创建执行记录
     *
     * @param scriptModel 脚本
     * @param type        执行类型
     * @return 对象
     */
    public ScriptExecuteLogModel create(ScriptModel scriptModel, int type) {
        return this.create(scriptModel, type, scriptModel.getWorkspaceId());
    }


    /**
     * 创建执行记录
     *
     * @param scriptModel 脚本
     * @param type        执行类型
     * @return 对象
     */
    public ScriptExecuteLogModel create(ScriptModel scriptModel, int type, String workspaceId) {
        ScriptExecuteLogModel scriptExecuteLogModel = new ScriptExecuteLogModel();
        scriptExecuteLogModel.setScriptId(scriptModel.getId());
        scriptExecuteLogModel.setScriptName(scriptModel.getName());
        scriptExecuteLogModel.setTriggerExecType(type);
        scriptExecuteLogModel.setWorkspaceId(workspaceId);
        super.insert(scriptExecuteLogModel);
        return scriptExecuteLogModel;
    }

    /**
     * 修改执行状态
     *
     * @param id     ID
     * @param status 状态
     */
    public void updateStatus(String id, CommandExecLogModel.Status status) {
        this.updateStatus(id, status, null);
    }

    /**
     * 修改执行状态
     *
     * @param id       ID
     * @param status   状态
     * @param exitCode 退出码
     */
    public void updateStatus(String id, CommandExecLogModel.Status status, Integer exitCode) {
        ScriptExecuteLogModel model = new ScriptExecuteLogModel();
        model.setId(id);
        model.setExitCode(exitCode);
        model.setStatus(status.getCode());
        this.updateById(model);
    }

    /**
     * 加载脚本文件
     *
     * @param scriptModel 脚本对象
     * @return file
     */
    public File toExecLogFile(ScriptModel scriptModel) {
        InputStream templateInputStream = ExtConfigBean.getConfigResourceInputStream("/exec/template." + CommandUtil.SUFFIX);
        String defaultTemplate = IoUtil.readUtf8(templateInputStream);
        String context = defaultTemplate + scriptModel.getContext();
        // 替换全局变量
        context = scriptLibraryServer.referenceReplace(context);
        //
        String dataPath = voyager1Application.getDataPath();
        File scriptFile = FileUtil.file(dataPath, Const.SCRIPT_RUN_CACHE_DIRECTORY, String.format("%s.%s", java.util.UUID.randomUUID().toString().replace("-", ""), CommandUtil.SUFFIX));
        FileUtils.writeScript(context, scriptFile, ExtConfigBean.getConsoleLogCharset());
        return scriptFile;
    }


    @Override
    protected void executeClearImpl(int h2DbLogStorageCount) {
        super.autoLoopClear("createTimeMillis", h2DbLogStorageCount, null, scriptExecuteLogModel -> {
            File logFile = ScriptModel.logFile(scriptExecuteLogModel.getScriptId(), scriptExecuteLogModel.getId());
            boolean fastDel = CommandUtil.systemFastDel(logFile);
            return !fastDel;
        });
    }

    @Override
    protected String[] clearTimeColumns() {
        return super.clearTimeColumns();
    }
}
