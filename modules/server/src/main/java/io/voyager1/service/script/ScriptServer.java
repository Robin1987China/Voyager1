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

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.Task;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.cron.ICron;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.cron.CronUtils;
import io.voyager1.core.entity.ScriptEntity;
import io.voyager1.core.jpa.JpaGlobalOrWorkspaceService;
import io.voyager1.core.repository.ScriptRepository;
import io.voyager1.model.script.ScriptExecuteLogModel;
import io.voyager1.model.script.ScriptModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.ITriggerToken;
import io.voyager1.socket.ServerScriptProcessBuilder;
import io.voyager1.util.StringUtil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @since 2022/1/19
 */
@Service
@Slf4j
public class ScriptServer extends JpaGlobalOrWorkspaceService<ScriptModel, ScriptEntity> implements ICron<ScriptModel>, ITriggerToken {

    private final ScriptRepository scriptRepository;

    public ScriptServer(ScriptRepository scriptRepository) {
        this.scriptRepository = scriptRepository;
    }

    @Override
    protected JpaRepository<ScriptEntity, String> repository() {
        return scriptRepository;
    }

    @Override
    protected JpaSpecificationExecutor<ScriptEntity> specExecutor() {
        return scriptRepository;
    }

    @Override
    protected Class<ScriptEntity> entityClass() {
        return ScriptEntity.class;
    }

    @Override
    protected Class<ScriptModel> modelClass() {
        return ScriptModel.class;
    }

    @Override
    public List<ScriptModel> queryStartingList() {
        List<ScriptEntity> entities = scriptRepository.findByAutoExecCronIsNotNullAndAutoExecCronNot("");
        return entities.stream().map(this::toModel).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void insert(ScriptModel scriptModel) {
        super.insert(scriptModel);
        this.checkCron(scriptModel);
    }

    @Override
    public void updateById(ScriptModel info, HttpServletRequest request) {
        super.updateById(info, request);
        this.checkCron(info);
    }

    @Override
    public int delByKey(String keyValue, HttpServletRequest request) {
        int delByKey = super.delByKey(keyValue, request);
        if (delByKey > 0) {
            String taskId = "server_script:" + keyValue;
            CronUtils.remove(taskId);
        }
        return delByKey;
    }

    /**
     * 检查定时任务 状态
     *
     * @param scriptModel 构建信息
     */
    @Override
    public boolean checkCron(ScriptModel scriptModel) {
        String id = scriptModel.getId();
        String taskId = "server_script:" + id;
        String autoExecCron = scriptModel.getAutoExecCron();
        autoExecCron = StringUtil.parseCron(autoExecCron);
        if ((autoExecCron == null || autoExecCron.isEmpty())) {
            CronUtils.remove(taskId);
            return false;
        }
        log.debug("start script cron {} {} {}", id, scriptModel.getName(), autoExecCron);
        CronUtils.upsert(taskId, autoExecCron, new CronTask(id));
        return true;
    }


    /**
     * 将服务端 脚本信息同步到其他工作空间
     *
     * @param ids            多给节点ID
     * @param nowWorkspaceId 当前的工作空间ID
     * @param workspaceId    同步到哪个工作空间
     */
    public void syncToWorkspace(String ids, String nowWorkspaceId, String workspaceId) {
        io.voyager1.util.ConvertUtil.splitTrim(ids, ",")
            .forEach(id -> {
                ScriptModel data = super.getByKey(id, false, entity -> entity.set("workspaceId", nowWorkspaceId));
                Assert.notNull(data, "没有对应到脚本信息或者选择全局脚本");
                //
                ScriptModel where = new ScriptModel();
                where.setWorkspaceId(workspaceId);
                where.setName(data.getName());
                ScriptModel exits = super.queryByBean(where);
                if (exits == null) {
                    // 不存在则添加 信息
                    data.setId(null);
                    data.setWorkspaceId(workspaceId);
                    data.setCreateTimeMillis(null);
                    data.setModifyTimeMillis(null);
                    data.setNodeIds(null);
                    data.setModifyUser(null);
                    super.insert(data);
                } else {
                    // 修改信息
                    ScriptModel update = new ScriptModel();
                    update.setId(exits.getId());
                    update.setContext(data.getContext());
                    update.setDefArgs(data.getDefArgs());
                    update.setDescription(data.getDescription());
                    update.setAutoExecCron(data.getAutoExecCron());
                    super.updateById(update);
                }
            });
    }

    private static class CronTask implements Task {

        private final String id;

        public CronTask(String id) {
            this.id = id;
        }

        @Override
        public void execute() {
            try {
                BaseServerController.resetInfo(UserModel.EMPTY);
                ScriptServer nodeScriptServer = SpringContextHolder.getBean(ScriptServer.class);
                ScriptModel scriptServerItem = nodeScriptServer.getByKey(id);
                if (scriptServerItem == null) {
                    return;
                }
                // 创建记录
                ScriptExecuteLogServer execLogServer = SpringContextHolder.getBean(ScriptExecuteLogServer.class);
                ScriptExecuteLogModel nodeScriptExecLogModel = execLogServer.create(scriptServerItem, 1);
                // 执行
                ServerScriptProcessBuilder.create(scriptServerItem, nodeScriptExecLogModel.getId(), scriptServerItem.getDefArgs());
            } finally {
                BaseServerController.removeEmpty();
            }
        }
    }
}
