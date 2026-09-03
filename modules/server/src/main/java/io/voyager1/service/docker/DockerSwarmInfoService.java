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

package io.voyager1.service.docker;

import io.voyager1.core.entity.DockerSwarmInfoEntity;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.DockerSwarmInfoRepository;
import io.voyager1.model.docker.DockerSwarmInfoMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

/**
 * docker swarm 集群信息服务。
 * <p>
 * 已从承继存储框架（BaseWorkspaceService）搬家到 JPA（JpaWorkspaceService + DockerSwarmInfoRepository），对外契约不变。
 *
 * @since 2022/2/13
 */
@Service
@Slf4j
public class DockerSwarmInfoService extends JpaWorkspaceService<DockerSwarmInfoMode, DockerSwarmInfoEntity> {

    public static final String DOCKER_PLUGIN_NAME = "docker-cli:swarm";

    private final DockerSwarmInfoRepository repository;

    public DockerSwarmInfoService(DockerSwarmInfoRepository repository) {
        this.repository = repository;
    }

    @Override
    protected JpaRepository<DockerSwarmInfoEntity, String> repository() {
        return repository;
    }

    @Override
    protected JpaSpecificationExecutor<DockerSwarmInfoEntity> specExecutor() {
        return repository;
    }

    @Override
    protected Class<DockerSwarmInfoEntity> entityClass() {
        return DockerSwarmInfoEntity.class;
    }

    @Override
    protected Class<DockerSwarmInfoMode> modelClass() {
        return DockerSwarmInfoMode.class;
    }
}
