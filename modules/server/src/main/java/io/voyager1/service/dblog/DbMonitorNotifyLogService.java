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
import io.voyager1.core.entity.MonitorNotifyLogEntity;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.MonitorNotifyLogRepository;
import io.voyager1.model.log.MonitorNotifyLog;
import io.voyager1.model.user.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 监控通知日志服务。
 */
@Service
public class DbMonitorNotifyLogService extends JpaWorkspaceService<MonitorNotifyLog, MonitorNotifyLogEntity> {

    private final MonitorNotifyLogRepository repository;

    public DbMonitorNotifyLogService(MonitorNotifyLogRepository repository) {
        this.repository = repository;
    }

    @Override
    protected JpaRepository<MonitorNotifyLogEntity, String> repository() { return repository; }

    @Override
    protected JpaSpecificationExecutor<MonitorNotifyLogEntity> specExecutor() { return repository; }

    @Override
    protected Class<MonitorNotifyLogEntity> entityClass() { return MonitorNotifyLogEntity.class; }

    @Override
    protected Class<MonitorNotifyLog> modelClass() { return MonitorNotifyLog.class; }

    @Override
    @Transactional
    public void insert(MonitorNotifyLog monitorNotifyLog) {
        try {
            BaseServerController.resetInfo(UserModel.EMPTY);
            super.insert(monitorNotifyLog);
        } finally {
            BaseServerController.removeEmpty();
        }
    }

    @Override
    protected String[] clearTimeColumns() {
        return new String[]{"createTime", "createTimeMillis"};
    }

    public MonitorNotifyLog getByMonitorId(String monitorId) {
        MonitorNotifyLogEntity e = repository.findFirstByMonitorIdOrderByCreateTimeDesc(monitorId);
        return e == null ? null : toModel(e);
    }

    @Transactional
    public void delByMonitorId(String monitorId) {
        repository.deleteByMonitorId(monitorId);
    }

    @Transactional
    public void updateStatus(String logId, boolean status, String errorMsg) {
        MonitorNotifyLog monitorNotifyLog = new MonitorNotifyLog();
        monitorNotifyLog.setId(logId);
        monitorNotifyLog.setNotifyStatus(status);
        monitorNotifyLog.setNotifyError(errorMsg);
        this.updateById(monitorNotifyLog);
    }
}
