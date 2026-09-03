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

package io.voyager1.service.finops;

import io.voyager1.core.entity.CostTagRuleEntity;
import io.voyager1.core.repository.CostTagRuleRepository;
import io.voyager1.model.data.CostTagRuleModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 标签分摊规则服务。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA 仓库（CostTagRuleRepository），对外契约不变。
 *
 * @since 2026/8/31
 */
@Service
public class CostTagRuleService {

    private final CostTagRuleRepository repository;

    public CostTagRuleService(CostTagRuleRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public String save(CostTagRuleModel rule) {
        long now = System.currentTimeMillis();
        CostTagRuleEntity entity;
        if (rule.getId() == null || rule.getId().isEmpty()) {
            entity = new CostTagRuleEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setCreateTimeMillis(now);
        } else {
            entity = repository.findById(rule.getId()).orElse(null);
            if (entity == null) {
                entity = new CostTagRuleEntity();
                entity.setId(rule.getId());
                entity.setCreateTimeMillis(now);
            }
        }
        entity.setModifyTimeMillis(now);
        entity.setVendor(rule.getVendor());
        entity.setTagKey(rule.getTagKey());
        entity.setTagValue(rule.getTagValue());
        entity.setProjectId(rule.getProjectId());
        entity.setProjectName(rule.getProjectName());
        repository.save(entity);
        return entity.getId();
    }

    public List<CostTagRuleModel> list() {
        return repository.findAll().stream().map(this::toModel).collect(Collectors.toList());
    }

    @Transactional
    public void delete(String id) {
        repository.deleteById(id);
    }

    private CostTagRuleModel toModel(CostTagRuleEntity entity) {
        CostTagRuleModel model = CostTagRuleModel.builder()
            .vendor(entity.getVendor())
            .tagKey(entity.getTagKey())
            .tagValue(entity.getTagValue())
            .projectId(entity.getProjectId())
            .projectName(entity.getProjectName())
            .build();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        return model;
    }
}
