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

import io.voyager1.core.entity.OutGivingLogEntity;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.OutGivingLogRepository;
import io.voyager1.model.log.OutGivingLog;
import io.voyager1.model.outgiving.OutGivingNodeProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

/**
 * 分发日志服务。
 * <p>
 * 已从承继存储框架（BaseWorkspaceService）搬家到 JPA（JpaWorkspaceService + OutGivingLogRepository），对外契约不变。
 *
 * @since 2019/7/20
 */
@Service
public class DbOutGivingLogService extends JpaWorkspaceService<OutGivingLog, OutGivingLogEntity> {

    private final OutGivingLogRepository outGivingLogRepository;

    public DbOutGivingLogService(OutGivingLogRepository outGivingLogRepository) {
        this.outGivingLogRepository = outGivingLogRepository;
    }

    @Override
    protected JpaRepository<OutGivingLogEntity, String> repository() {
        return outGivingLogRepository;
    }

    @Override
    protected JpaSpecificationExecutor<OutGivingLogEntity> specExecutor() {
        return outGivingLogRepository;
    }

    @Override
    protected Class<OutGivingLogEntity> entityClass() {
        return OutGivingLogEntity.class;
    }

    @Override
    protected Class<OutGivingLog> modelClass() {
        return OutGivingLog.class;
    }

    @org.springframework.transaction.annotation.Transactional
    public void delByOutGivingId(String outGivingId) {
        outGivingLogRepository.deleteByOutGivingId(outGivingId);
    }

    public OutGivingLog getByProject(String outId, OutGivingNodeProject nodeProject) {
        OutGivingLogEntity entity = outGivingLogRepository
            .findFirstByOutGivingIdAndNodeIdAndProjectIdOrderByCreateTimeMillisDesc(
                outId, nodeProject.getNodeId(), nodeProject.getProjectId());
        return entity == null ? null : toModel(entity);
    }
}
