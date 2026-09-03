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

import io.voyager1.common.ServerConst;
import io.voyager1.core.entity.RepositoryEntity;
import io.voyager1.core.jpa.JpaGlobalOrWorkspaceService;
import io.voyager1.core.repository.RepositoryRepository;
import io.voyager1.model.data.RepositoryModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 仓库信息服务。
 * <p>
 * 已从承继存储框架（BaseGlobalOrWorkspaceService）搬家到 JPA（JpaGlobalOrWorkspaceService + RepositoryRepository），对外契约不变。
 *
 */
@Service
public class RepositoryService extends JpaGlobalOrWorkspaceService<RepositoryModel, RepositoryEntity> {

    private final RepositoryRepository repository;

    public RepositoryService(RepositoryRepository repository) {
        this.repository = repository;
    }

    @Override
    protected JpaRepository<RepositoryEntity, String> repository() {
        return repository;
    }

    @Override
    protected JpaSpecificationExecutor<RepositoryEntity> specExecutor() {
        return repository;
    }

    @Override
    protected Class<RepositoryEntity> entityClass() {
        return RepositoryEntity.class;
    }

    @Override
    protected Class<RepositoryModel> modelClass() {
        return RepositoryModel.class;
    }

    @Override
    protected void fillSelectResult(RepositoryModel repositoryModel) {
        if (repositoryModel == null) {
            return;
        }
        if (!(repositoryModel.getPassword() != null && repositoryModel.getPassword().toLowerCase().startsWith(ServerConst.REF_WORKSPACE_ENV.toLowerCase()))) {
            repositoryModel.setPassword(null);
        }
        repositoryModel.setRsaPrv(null);
    }

    public boolean existsByGitUrl(String workspaceId, String excludeId, String gitUrl) {
        List<String> allowed = Arrays.asList(workspaceId, ServerConst.WORKSPACE_GLOBAL);
        List<RepositoryEntity> list = repository.findByGitUrlAndWorkspaceIdIn(gitUrl, allowed);
        for (RepositoryEntity e : list) {
            if (excludeId == null || excludeId.isEmpty() || !excludeId.equals(e.getId())) {
                return true;
            }
        }
        return false;
    }
}
