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

import io.voyager1.core.entity.OutGivingEntity;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.OutGivingRepository;
import io.voyager1.model.outgiving.OutGivingModel;
import io.voyager1.model.outgiving.OutGivingNodeProject;
import io.voyager1.service.IStatusRecover;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 分发管理。
 * <p>
 * 已从承继存储框架（BaseWorkspaceService）搬家到 JPA（JpaWorkspaceService + OutGivingRepository），对外契约不变。
 *
 * @since 2019/4/21
 */
@Service
public class OutGivingServer extends JpaWorkspaceService<OutGivingModel, OutGivingEntity> implements IStatusRecover {

    private final OutGivingRepository outGivingRepository;
    private final EntityManager entityManager;

    public OutGivingServer(OutGivingRepository outGivingRepository, EntityManager entityManager) {
        this.outGivingRepository = outGivingRepository;
        this.entityManager = entityManager;
    }

    @Override
    protected JpaRepository<OutGivingEntity, String> repository() {
        return outGivingRepository;
    }

    @Override
    protected JpaSpecificationExecutor<OutGivingEntity> specExecutor() {
        return outGivingRepository;
    }

    @Override
    protected Class<OutGivingEntity> entityClass() {
        return OutGivingEntity.class;
    }

    @Override
    protected Class<OutGivingModel> modelClass() {
        return OutGivingModel.class;
    }

    public void checkNodeProject(String nodeId, String projectId, HttpServletRequest request, String msg) {
        // 检查节点分发
        List<OutGivingModel> outGivingModels = super.listByWorkspace(request);
        if (outGivingModels != null) {
            boolean match = outGivingModels.stream().anyMatch(outGivingModel -> outGivingModel.checkContains(nodeId, projectId));
            Assert.state(!match, msg);
        }
    }

    public boolean checkNode(String nodeId, HttpServletRequest request) {
        List<OutGivingModel> list = super.listByWorkspace(request);
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (OutGivingModel outGivingModel : list) {
            List<OutGivingNodeProject> outGivingNodeProjectList = outGivingModel.outGivingNodeProjectList();
            if (outGivingNodeProjectList != null) {
                for (OutGivingNodeProject outGivingNodeProject : outGivingNodeProjectList) {
                    if (outGivingNodeProject.getNodeId().equals(nodeId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public int statusRecover() {
        // 恢复异常数据
        jakarta.persistence.Query query = entityManager.createNativeQuery("update OPS_RELEASE set status=?1 where status=?2");
        query.setParameter(1, OutGivingModel.Status.DONE.getCode());
        query.setParameter(2, OutGivingModel.Status.ING.getCode());
        return query.executeUpdate();
    }
}
