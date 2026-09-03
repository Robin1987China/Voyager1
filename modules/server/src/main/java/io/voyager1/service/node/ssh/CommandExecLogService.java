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

import io.voyager1.util.FileUtil;
import io.voyager1.core.entity.CommandExecLogEntity;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.CommandExecLogRepository;
import io.voyager1.model.data.CommandExecLogModel;
import io.voyager1.util.CommandUtil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 命令执行记录。
 * <p>
 * 已从承继存储框架（BaseWorkspaceService）搬家到 JPA（JpaWorkspaceService + CommandExecLogRepository），对外契约不变。
 *
 * @since 2021/12/22
 */
@Service
public class CommandExecLogService extends JpaWorkspaceService<CommandExecLogModel, CommandExecLogEntity> {

    private final CommandExecLogRepository repository;

    public CommandExecLogService(CommandExecLogRepository repository) {
        this.repository = repository;
    }

    @Override
    protected JpaRepository<CommandExecLogEntity, String> repository() {
        return repository;
    }

    @Override
    protected JpaSpecificationExecutor<CommandExecLogEntity> specExecutor() {
        return repository;
    }

    @Override
    protected Class<CommandExecLogEntity> entityClass() {
        return CommandExecLogEntity.class;
    }

    @Override
    protected Class<CommandExecLogModel> modelClass() {
        return CommandExecLogModel.class;
    }

    @Override
    protected void fillSelectResult(CommandExecLogModel data) {
        if (data == null) {
            return;
        }
        data.setHasLog(FileUtil.exist(data.logFile()));
    }

    @Override
    protected void executeClearImpl(int h2DbLogStorageCount) {
        super.autoLoopClear("createTimeMillis", h2DbLogStorageCount, null, commandExecLogModel -> {
            File file = commandExecLogModel.logFile();
            CommandUtil.systemFastDel(file);
            File parentFile = file.getParentFile();
            boolean empty = FileUtil.isEmpty(parentFile);
            if (empty) {
                CommandUtil.systemFastDel(parentFile);
            }
            return true;
        });
    }

    @Override
    protected String[] clearTimeColumns() {
        return super.clearTimeColumns();
    }
}
