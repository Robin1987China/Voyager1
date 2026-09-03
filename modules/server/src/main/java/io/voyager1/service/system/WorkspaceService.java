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

package io.voyager1.service.system;

import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.Const;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.core.db.TableName;
import io.voyager1.core.entity.WorkspaceInfoEntity;
import io.voyager1.core.jpa.JpaBaseService;
import io.voyager1.core.repository.WorkspaceInfoRepository;
import io.voyager1.model.BaseWorkspaceModel;
import io.voyager1.model.data.WorkspaceModel;
import io.voyager1.service.IStatusRecover;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 工作空间。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA（JpaBaseService + WorkspaceInfoRepository），对外契约不变。
 *
 * @since 2021/12/3
 */
@Service
@Slf4j
public class WorkspaceService extends JpaBaseService<WorkspaceModel, WorkspaceInfoEntity> implements IStatusRecover {

    private final WorkspaceInfoRepository workspaceInfoRepository;
    private final EntityManager entityManager;

    public WorkspaceService(WorkspaceInfoRepository workspaceInfoRepository, EntityManager entityManager) {
        this.workspaceInfoRepository = workspaceInfoRepository;
        this.entityManager = entityManager;
    }

    @Override
    protected JpaRepository<WorkspaceInfoEntity, String> repository() {
        return workspaceInfoRepository;
    }

    @Override
    protected JpaSpecificationExecutor<WorkspaceInfoEntity> specExecutor() {
        return workspaceInfoRepository;
    }

    @Override
    protected Class<WorkspaceInfoEntity> entityClass() {
        return WorkspaceInfoEntity.class;
    }

    @Override
    protected Class<WorkspaceModel> modelClass() {
        return WorkspaceModel.class;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public int statusRecover() {
        WorkspaceModel workspaceModel = this.getByKey(Const.WORKSPACE_DEFAULT_ID);
        if (workspaceModel == null) {
            WorkspaceModel defaultWorkspace = new WorkspaceModel();
            defaultWorkspace.setId(Const.WORKSPACE_DEFAULT_ID);
            defaultWorkspace.setName(Const.DEFAULT_GROUP_NAME.get());
            defaultWorkspace.setDescription("系统默认的工作空间,不能删除");
            this.insert(defaultWorkspace);
            log.info("初始化{}工作空间", Const.DEFAULT_GROUP_NAME.get());
        }

        Set<Class<?>> classes = BaseWorkspaceModel.allClass();
        int total = 0;
        for (Class<?> aClass : classes) {
            TableName tableName = aClass.getAnnotation(TableName.class);
            if (tableName == null) {
                continue;
            }
            String sql = "update " + tableName.value() + " set workspaceId=?1 where (workspaceId is null or workspaceId='' or workspaceId='null')";
            jakarta.persistence.Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, Const.WORKSPACE_DEFAULT_ID);
            int execute = query.executeUpdate();
            if (execute > 0) {
                log.info("修复工作空间为 null 的数据 {} {}", tableName.value(), execute);
            }
            total += execute;
        }
        return total;
    }
}
