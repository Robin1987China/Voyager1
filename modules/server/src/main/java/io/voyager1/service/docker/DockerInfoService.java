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

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.entity.DockerInfoEntity;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.DockerInfoRepository;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.docker.DockerInfoModel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @since 2022/1/26
 */
@Service
@Slf4j
public class DockerInfoService extends JpaWorkspaceService<DockerInfoModel, DockerInfoEntity> {

    private final DockerInfoRepository dockerInfoRepository;
    private final EntityManager entityManager;

    public DockerInfoService(DockerInfoRepository dockerInfoRepository, EntityManager entityManager) {
        this.dockerInfoRepository = dockerInfoRepository;
        this.entityManager = entityManager;
    }

    @Override
    protected JpaRepository<DockerInfoEntity, String> repository() {
        return dockerInfoRepository;
    }

    @Override
    protected JpaSpecificationExecutor<DockerInfoEntity> specExecutor() {
        return dockerInfoRepository;
    }

    @Override
    protected Class<DockerInfoEntity> entityClass() {
        return DockerInfoEntity.class;
    }

    @Override
    protected Class<DockerInfoModel> modelClass() {
        return DockerInfoModel.class;
    }

    public static final String DOCKER_CHECK_PLUGIN_NAME = "docker-cli:check";

    public static final String DOCKER_PLUGIN_NAME = "docker-cli";

    @Override
    protected void fillSelectResult(DockerInfoModel data) {
        //data.setRegistryPassword(null);
    }

    @Override
    protected void fillInsert(DockerInfoModel dockerInfoModel) {
        super.fillInsert(dockerInfoModel);
    }

    /**
     * 根据 tag 查询 容器
     *
     * @param workspaceId 工作空间
     * @param tag         tag
     * @return list
     */
    public List<DockerInfoModel> queryByTag(String workspaceId, String tag) {
        if ((tag == null || tag.isEmpty())) {
            return this.listByEntity(new io.voyager1.core.db.Entity().set("workspaceId", workspaceId));
        } else {
            Query query = entityManager.createNativeQuery("SELECT * FROM INFRA_DOCKER WHERE workspaceId = ?1 AND instr(tags, ?2)", DockerInfoEntity.class);
            query.setParameter(1, workspaceId);
            query.setParameter(2, ":" + tag + ":");
            @SuppressWarnings("unchecked")
            List<DockerInfoEntity> list = query.getResultList();
            return list.stream().map(this::toModel).collect(Collectors.toList());
        }
    }

    /**
     * 根据 tag 查询 容器
     *
     * @param workspaceId 工作空间
     * @param tag         tag
     * @return count
     */
    public int countByTag(String workspaceId, String tag) {
        Query query = entityManager.createNativeQuery("SELECT COUNT(*) FROM INFRA_DOCKER WHERE workspaceId = ?1 AND instr(tags, ?2)");
        query.setParameter(1, workspaceId);
        query.setParameter(2, ":" + tag + ":");
        Object result = query.getSingleResult();
        return result == null ? 0 : ((Number) result).intValue();
    }

    /**
     * 根据 tag 查询 容器
     *
     * @param workspaceId 工作空间
     * @return count
     */
    public List<String> allTag(String workspaceId) {
        Query query = entityManager.createNativeQuery("SELECT tags FROM INFRA_DOCKER WHERE workspaceId = ?1");
        query.setParameter(1, workspaceId);
        List<?> list = query.getResultList();
        if ((list == null || list.isEmpty())) {
            return new ArrayList<>();
        }
        return list.stream()
            .map(Object::toString)
            .flatMap((Function<String, Stream<String>>) s -> io.voyager1.util.ConvertUtil.splitTrim(s, ":").stream())
            .filter(StrUtil::isNotEmpty)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 将节点信息同步到其他工作空间
     *
     * @param ids            多给节点ID
     * @param nowWorkspaceId 当前的工作空间ID
     * @param workspaceId    同步到哪个工作空间
     */
    public void syncToWorkspace(String ids, String nowWorkspaceId, String workspaceId) {
        io.voyager1.util.ConvertUtil.splitTrim(ids, ",").forEach(id -> {
            DockerInfoModel data = super.getByKey(id, false, entity -> entity.set("workspaceId", nowWorkspaceId));
            Assert.notNull(data, "没有对应到docker信息");
            //
            DockerInfoModel where = new DockerInfoModel();
            where.setWorkspaceId(workspaceId);
            where.setMachineDockerId(data.getMachineDockerId());
            DockerInfoModel exits = super.queryByBean(where);
            Assert.isNull(exits, "对应工作空间已经存在对应的 docker 啦");
            // 不存在则添加节点
            data.setId(null);
            data.setWorkspaceId(workspaceId);
            data.setCreateTimeMillis(null);
            data.setModifyTimeMillis(null);
            data.setModifyUser(null);
            // 集群 不同步
            data.setSwarmId(null);
            data.setSwarmNodeId(null);
            this.insert(data);
        });
    }
}
