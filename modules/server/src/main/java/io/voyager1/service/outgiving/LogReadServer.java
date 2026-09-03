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

package io.voyager1.service.outgiving;

import io.voyager1.core.entity.LogReadEntity;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.LogReadRepository;
import io.voyager1.model.outgiving.LogReadModel;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 日志阅读服务。
 * <p>
 * 已从承继存储框架（BaseWorkspaceService）搬家到 JPA（JpaWorkspaceService + LogReadRepository），对外契约不变。
 *
 * @since 2022/5/15
 */
@Service
public class LogReadServer extends JpaWorkspaceService<LogReadModel, LogReadEntity> {

    private final LogReadRepository repository;

    public LogReadServer(LogReadRepository repository) {
        this.repository = repository;
    }

    @Override
    protected JpaRepository<LogReadEntity, String> repository() {
        return repository;
    }

    @Override
    protected JpaSpecificationExecutor<LogReadEntity> specExecutor() {
        return repository;
    }

    @Override
    protected Class<LogReadEntity> entityClass() {
        return LogReadEntity.class;
    }

    @Override
    protected Class<LogReadModel> modelClass() {
        return LogReadModel.class;
    }

    public void checkNodeProject(String nodeId, String projectId, HttpServletRequest request, String msg) {
        List<LogReadModel> outGivingModels = this.listByWorkspace(request);
        if (outGivingModels != null) {
            boolean match = outGivingModels.stream().anyMatch(outGivingModel -> outGivingModel.checkContains(nodeId, projectId));
            Assert.state(!match, msg);
        }
    }

    public boolean checkNode(String nodeId, HttpServletRequest request) {
        List<LogReadModel> list = this.listByWorkspace(request);
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (LogReadModel outGivingModel : list) {
            List<LogReadModel.Item> items = outGivingModel.nodeProjectList();
            if (items != null) {
                for (LogReadModel.Item item : items) {
                    if (item.getNodeId().equals(nodeId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
