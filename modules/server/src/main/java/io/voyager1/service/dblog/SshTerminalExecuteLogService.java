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

package io.voyager1.service.dblog;

import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerConst;
import io.voyager1.core.entity.SshTerminalExecuteLogEntity;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.SshTerminalExecuteLogRepository;
import io.voyager1.func.assets.model.MachineSshModel;
import io.voyager1.model.data.SshModel;
import io.voyager1.model.log.SshTerminalExecuteLog;
import io.voyager1.model.user.UserModel;
import io.voyager1.util.StrUtil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SSH 终端操作记录服务。
 */
@Service
public class SshTerminalExecuteLogService extends JpaWorkspaceService<SshTerminalExecuteLog, SshTerminalExecuteLogEntity> {

    private final SshTerminalExecuteLogRepository repository;

    public SshTerminalExecuteLogService(SshTerminalExecuteLogRepository repository) {
        this.repository = repository;
    }

    @Override
    protected JpaRepository<SshTerminalExecuteLogEntity, String> repository() { return repository; }

    @Override
    protected JpaSpecificationExecutor<SshTerminalExecuteLogEntity> specExecutor() { return repository; }

    @Override
    protected Class<SshTerminalExecuteLogEntity> entityClass() { return SshTerminalExecuteLogEntity.class; }

    @Override
    protected Class<SshTerminalExecuteLog> modelClass() { return SshTerminalExecuteLog.class; }

    @Override
    protected String[] clearTimeColumns() {
        return new String[]{"createTimeMillis"};
    }

    @org.springframework.transaction.annotation.Transactional
    public int delBySshId(String sshId) {
        java.util.List<SshTerminalExecuteLogEntity> list = repository.findAll();
        repository.deleteBySshId(sshId);
        return 0;
    }

    public void batch(UserModel userInfo, MachineSshModel machineSshModel, SshModel sshItem, String ip, String userAgent, boolean refuse, List<String> commands) {
        if (machineSshModel == null) {
            return;
        }
        long optTime = System.currentTimeMillis();
        try {
            BaseServerController.resetInfo(userInfo);
            List<SshTerminalExecuteLog> executeLogs = commands.stream()
                .filter(StrUtil::isNotEmpty)
                .map(s -> {
                    SshTerminalExecuteLog sshTerminalExecuteLog = new SshTerminalExecuteLog();
                    if (sshItem != null) {
                        sshTerminalExecuteLog.setSshId(sshItem.getId());
                        sshTerminalExecuteLog.setSshName(sshItem.getName());
                        sshTerminalExecuteLog.setWorkspaceId(sshItem.getWorkspaceId());
                    } else {
                        sshTerminalExecuteLog.setWorkspaceId(ServerConst.WORKSPACE_GLOBAL);
                    }
                    sshTerminalExecuteLog.setMachineSshId(machineSshModel.getId());
                    sshTerminalExecuteLog.setMachineSshName(machineSshModel.getName());
                    sshTerminalExecuteLog.setCommands(s);
                    sshTerminalExecuteLog.setRefuse(refuse);
                    sshTerminalExecuteLog.setCreateTimeMillis(optTime);
                    sshTerminalExecuteLog.setIp(ip);
                    sshTerminalExecuteLog.setUserAgent(userAgent);
                    return sshTerminalExecuteLog;
                }).collect(Collectors.toList());
            this.insert(executeLogs);
        } finally {
            BaseServerController.removeAll();
        }
    }
}
