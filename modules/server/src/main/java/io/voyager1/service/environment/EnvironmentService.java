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

package io.voyager1.service.environment;

import io.voyager1.core.entity.EnvironmentEntity;
import io.voyager1.core.repository.EnvironmentRepository;
import io.voyager1.model.data.EnvironmentModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 环境服务（dev/test/prod 定义与预置）。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA 仓库（EnvironmentRepository），对外契约不变。
 *
 * @since 2026/8/8
 */
@Service
@Slf4j
public class EnvironmentService {

    public static final List<String> DEFAULT_ENVIRONMENTS = Arrays.asList("dev", "test", "prod");

    private final EnvironmentRepository repository;

    public EnvironmentService(EnvironmentRepository repository) {
        this.repository = repository;
    }

    /**
     * 初始化预置环境（首次启动）。
     */
    @Transactional
    public void initDefaultEnvironments() {
        if (!this.listEnabled().isEmpty()) {
            return;
        }
        for (int i = 0; i < DEFAULT_ENVIRONMENTS.size(); i++) {
            this.saveEnvironment(null, DEFAULT_ENVIRONMENTS.get(i), i, true);
        }
        log.info("预置环境: {}", DEFAULT_ENVIRONMENTS);
    }

    /**
     * 创建/更新环境。
     */
    @Transactional
    public String saveEnvironment(String id, String name, Integer sortValue, Boolean enabled) {
        Assert.hasText(name, "环境名称不能为空");
        long now = System.currentTimeMillis();
        EnvironmentEntity entity;
        if (id == null || id.isEmpty()) {
            entity = new EnvironmentEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setCreateTimeMillis(now);
            entity.setModifyTimeMillis(now);
        } else {
            entity = repository.findById(id).orElse(null);
            Assert.notNull(entity, "环境不存在: " + id);
            entity.setModifyTimeMillis(now);
        }
        entity.setName(name);
        entity.setSortValue(sortValue);
        entity.setEnabled(enabled == null || enabled ? 1 : 0);
        repository.save(entity);
        return entity.getId();
    }

    /**
     * 按主键查询环境。
     */
    public EnvironmentModel getByKey(String id) {
        EnvironmentEntity entity = repository.findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    /**
     * 查询启用环境列表（按排序、创建时间）。
     */
    public List<EnvironmentModel> listEnabled() {
        return repository.findByEnabledOrderBySortValueAscCreateTimeMillisAsc(1)
            .stream()
            .map(this::toModel)
            .collect(Collectors.toList());
    }

    private EnvironmentModel toModel(EnvironmentEntity entity) {
        EnvironmentModel model = EnvironmentModel.builder()
            .name(entity.getName())
            .sortValue(entity.getSortValue())
            .enabled(entity.getEnabled())
            .build();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        return model;
    }
}
