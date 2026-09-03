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

package io.voyager1.service.node.ssh;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CharsetUtil;
import io.voyager1.util.IdUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.Task;
import io.voyager1.util.JschUtil;
import io.voyager1.util.SystemUtil;
import io.voyager1.cron.ICron;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.i18n.I18nThreadUtil;
import io.voyager1.cron.CronUtils;
import io.voyager1.core.entity.CommandEntity;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.CommandRepository;
import io.voyager1.func.assets.model.MachineSshModel;
import io.voyager1.func.assets.server.ScriptLibraryServer;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.model.data.CommandExecLogModel;
import io.voyager1.model.data.CommandModel;
import io.voyager1.model.data.SshModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.plugins.JschUtils;
import io.voyager1.script.CommandParam;
import io.voyager1.service.ITriggerToken;
import io.voyager1.service.system.WorkspaceEnvVarService;
import io.voyager1.util.LogRecorder;
import io.voyager1.util.StrictSyncFinisher;
import io.voyager1.util.StringUtil;
import io.voyager1.util.SyncFinisherUtil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;

/**
 * 命令管理
 *
 * @since : 2021/12/6 22:11
 */
@Service
@Slf4j
public class SshCommandService extends JpaWorkspaceService<CommandModel, CommandEntity> implements ICron<CommandModel>, ITriggerToken {

    private final SshService sshService;
    private final CommandExecLogService commandExecLogService;
    private final WorkspaceEnvVarService workspaceEnvVarService;
    private final ScriptLibraryServer scriptLibraryServer;
    private final CommandRepository commandRepository;

    private static final byte[] LINE_BYTES = SystemUtil.getOsInfo().getLineSeparator().getBytes(StandardCharsets.UTF_8);

    public SshCommandService(SshService sshService,
                             CommandExecLogService commandExecLogService,
                             WorkspaceEnvVarService workspaceEnvVarService,
                             ScriptLibraryServer scriptLibraryServer,
                             CommandRepository commandRepository) {
        this.sshService = sshService;
        this.commandExecLogService = commandExecLogService;
        this.workspaceEnvVarService = workspaceEnvVarService;
        this.scriptLibraryServer = scriptLibraryServer;
        this.commandRepository = commandRepository;
    }

    @Override
    protected JpaRepository<CommandEntity, String> repository() {
        return commandRepository;
    }

    @Override
    protected JpaSpecificationExecutor<CommandEntity> specExecutor() {
        return commandRepository;
    }

    @Override
    protected Class<CommandEntity> entityClass() {
        return CommandEntity.class;
    }

    @Override
    protected Class<CommandModel> modelClass() {
        return CommandModel.class;
    }

    @Override
    public void insert(CommandModel commandModel) {
        super.insert(commandModel);
        this.checkCron(commandModel);
    }

    @Override
    public void updateById(CommandModel info, HttpServletRequest request) {
        super.updateById(info, request);
        this.checkCron(info);
    }

    @Override
    public int delByKey(String keyValue, HttpServletRequest request) {
        int delByKey = super.delByKey(keyValue, request);
        if (delByKey > 0) {
            String taskId = "ssh_command:" + keyValue;
            CronUtils.remove(taskId);
        }
        return delByKey;
    }

    /**
     * 检查定时任务 状态
     *
     * @param buildInfoModel 构建信息
     */
    @Override
    public boolean checkCron(CommandModel buildInfoModel) {
        String id = buildInfoModel.getId();
        String taskId = "ssh_command:" + id;
        String autoExecCron = buildInfoModel.getAutoExecCron();
        autoExecCron = StringUtil.parseCron(autoExecCron);
        if ((autoExecCron == null || autoExecCron.isEmpty())) {
            CronUtils.remove(taskId);
            return false;
        }
        log.debug("start ssh command cron {} {} {}", id, buildInfoModel.getName(), autoExecCron);
        CronUtils.upsert(taskId, autoExecCron, new SshCommandService.CronTask(id));
        return true;
    }

    /**
     * 开启定时构建任务
     */
    @Override
    public List<CommandModel> queryStartingList() {
        List<CommandEntity> entities = commandRepository.findByAutoExecCronIsNotNullAndAutoExecCronNot("");
        return entities.stream().map(this::toModel).collect(java.util.stream.Collectors.toList());
    }

    private class CronTask implements Task {

        private final String id;

        public CronTask(String id) {
            this.id = id;
        }

        @Override
        public void execute() {
            try {
                BaseServerController.resetInfo(UserModel.EMPTY);
                CommandModel commandModel = SshCommandService.this.getByKey(this.id);
                SshCommandService.this.executeBatch(commandModel, commandModel.getDefParams(), commandModel.getSshIds(), 1);
            } catch (Exception e) {
                log.error("触发自动执行命令模版异常", e);
            } finally {
                BaseServerController.removeEmpty();
            }
        }
    }

    /**
     * 批量执行命令
     *
     * @param id     命令 id
     * @param nodes  ssh节点
     * @param params 参数
     * @return 批次ID
     */
    public String executeBatch(String id, String params, String nodes) {
        CommandModel commandModel = this.getByKey(id);
        return this.executeBatch(commandModel, params, nodes, 0);
    }

    /**
     * 批量执行命令
     *
     * @param commandModel 命令模版
     * @param nodes        ssh节点
     * @param params       参数
     * @return 批次ID
     */
    public String executeBatch(CommandModel commandModel, String params, String nodes, int triggerExecType) {
        return executeBatch(commandModel, params, nodes, triggerExecType, null);
    }

    /**
     * 批量执行命令
     *
     * @param commandModel    命令模版
     * @param nodes           ssh节点
     * @param params          参数
     * @param envMap          环境变量
     * @param triggerExecType 触发方式
     * @return 批次ID
     */
    public String executeBatch(CommandModel commandModel, String params, String nodes, int triggerExecType, Map<String, String> envMap) {
        Assert.notNull(commandModel, "没有对应对命令");
        List<String> sshIds = java.util.Arrays.asList(nodes.split(","));
        Assert.notEmpty(sshIds, "请选择 ssh 节点");
        String batchId = java.util.UUID.randomUUID().toString().replace("-", "");
        String name = "ssh-command-batch:" + batchId;
        StrictSyncFinisher syncFinisher = SyncFinisherUtil.create(name, sshIds.size());
        for (String sshId : sshIds) {
            this.executeItem(syncFinisher, commandModel, params, sshId, batchId, triggerExecType, envMap);
        }
        I18nThreadUtil.execute(() -> {
            try {
                syncFinisher.start();
            } catch (Exception e) {
                log.error("ssh 批量执行命令异常", e);
            } finally {
                SyncFinisherUtil.close(name);
            }
        });
        return batchId;
    }

    /**
     * 准备执行 某一个
     *
     * @param syncFinisher  线程同步器
     * @param commandModel  命令模版
     * @param commandParams 参数
     * @param sshId         ssh id
     * @param batchId       批次ID
     */
    private void executeItem(StrictSyncFinisher syncFinisher, CommandModel commandModel, String commandParams, String sshId, String batchId, int triggerExecType, Map<String, String> envMap) {
        SshModel sshModel = sshService.getByKey(sshId, false);

        CommandExecLogModel commandExecLogModel = new CommandExecLogModel();
        commandExecLogModel.setCommandId(commandModel.getId());
        commandExecLogModel.setCommandName(commandModel.getName());
        commandExecLogModel.setBatchId(batchId);
        commandExecLogModel.setSshId(sshId);
        commandExecLogModel.setWorkspaceId(commandModel.getWorkspaceId());
        commandExecLogModel.setTriggerExecType(triggerExecType);
        if (sshModel != null) {
            commandExecLogModel.setSshName(sshModel.getName());
        } else {
            commandExecLogModel.setSshName("SSH不存在");
        }
        commandExecLogModel.setStatus(CommandExecLogModel.Status.ING.getCode());
        // 拼接参数
        String commandParamsLine = CommandParam.toCommandLine(commandParams);
        commandExecLogService.insert(commandExecLogModel);

        syncFinisher.addWorker(() -> {
            try {
                this.execute(commandModel, commandExecLogModel, sshModel, commandParamsLine, envMap);
            } catch (Exception e) {
                log.error("命令模版执行链接异常", e);
                this.updateStatus(commandExecLogModel.getId(), CommandExecLogModel.Status.SESSION_ERROR);
            }
        });
    }


    /**
     * 执行命令
     *
     * @param commandModel        命令模版
     * @param commandExecLogModel 执行记录
     * @param sshModel            ssh
     * @param commandParamsLine   参数
     */
    private void execute(CommandModel commandModel, CommandExecLogModel commandExecLogModel, SshModel sshModel, String commandParamsLine, Map<String, String> envMap) {
        File file = commandExecLogModel.logFile();
        try (LogRecorder logRecorder = LogRecorder.builder().file(file).charset(StandardCharsets.UTF_8).build()) {
            if (sshModel == null) {
                logRecorder.systemError("ssh 不存在");
                this.updateStatus(commandExecLogModel.getId(), CommandExecLogModel.Status.ERROR, -100);
                return;
            }
            EnvironmentMapBuilder environmentMapBuilder = workspaceEnvVarService.getEnv(commandModel.getWorkspaceId());
            environmentMapBuilder.put("VOYAGER1_SSH_ID", sshModel.getId());
            environmentMapBuilder.put("VOYAGER1_COMMAND_ID", commandModel.getId());
            environmentMapBuilder.putStr(envMap);
            environmentMapBuilder.eachStr(logRecorder::system);
            Map<String, String> environment = environmentMapBuilder.environment();
            String commands = StringUtil.formatStrByMap(commandModel.getCommand(), environment);
            // 替换全局脚本
            commands = scriptLibraryServer.referenceReplace(commands);
            MachineSshModel machineSshModel = sshService.getMachineSshModel(sshModel);
            //
            Session session = null;
            try {
                Charset charset = machineSshModel.charset();
                int timeout = machineSshModel.timeout();
                //
                session = sshService.getSessionByModel(machineSshModel);
                int exitCode = JschUtils.execCallbackLine(session, charset, timeout, commands, commandParamsLine, logRecorder::info);
                logRecorder.system("执行退出码：{}", exitCode);
                // 更新状态
                this.updateStatus(commandExecLogModel.getId(), CommandExecLogModel.Status.DONE, exitCode);
            } catch (Exception e) {
                log.error("执行命令错误", e);
                // 更新状态
                this.updateStatus(commandExecLogModel.getId(), CommandExecLogModel.Status.ERROR);
                // 记录错误日志
                logRecorder.error("执行命令错误", e);
            } finally {
                JschUtil.close(session);
            }
        }
    }

    /**
     * 修改执行状态
     *
     * @param id     ID
     * @param status 状态
     */
    private void updateStatus(String id, CommandExecLogModel.Status status) {
        this.updateStatus(id, status, null);
    }

    /**
     * 修改执行状态
     *
     * @param id       ID
     * @param status   状态
     * @param exitCode 退出码
     */
    private void updateStatus(String id, CommandExecLogModel.Status status, Integer exitCode) {
        CommandExecLogModel commandExecLogModel = new CommandExecLogModel();
        commandExecLogModel.setId(id);
        commandExecLogModel.setExitCode(exitCode);
        commandExecLogModel.setStatus(status.getCode());
        commandExecLogService.updateById(commandExecLogModel);
    }

    /**
     * 将ssh 脚本信息同步到其他工作空间
     *
     * @param ids            多给节点ID
     * @param nowWorkspaceId 当前的工作空间ID
     * @param workspaceId    同步到哪个工作空间
     */
    public void syncToWorkspace(String ids, String nowWorkspaceId, String workspaceId) {
        io.voyager1.util.ConvertUtil.splitTrim(ids, ",")
            .forEach(id -> {
                CommandModel data = super.getByKey(id, false, entity -> entity.set("workspaceId", nowWorkspaceId));
                Assert.notNull(data, "没有对应的ssh脚本信息");
                //
                CommandModel where = new CommandModel();
                where.setWorkspaceId(workspaceId);
                where.setName(data.getName());
                CommandModel exits = super.queryByBean(where);
                if (exits == null) {
                    // 不存在则添加 信息
                    data.setId(null);
                    data.setWorkspaceId(workspaceId);
                    data.setCreateTimeMillis(null);
                    data.setModifyTimeMillis(null);
                    data.setSshIds(null);
                    data.setModifyUser(null);
                    super.insert(data);
                } else {
                    // 修改信息
                    CommandModel update = new CommandModel();
                    update.setId(exits.getId());
                    update.setCommand(data.getCommand());
                    update.setDesc(data.getDesc());
                    update.setDefParams(data.getDefParams());
                    update.setAutoExecCron(data.getAutoExecCron());
                    super.updateById(update);
                }
            });
    }

}
