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

import io.voyager1.core.entity.CostBudgetEntity;
import io.voyager1.core.repository.CostBudgetRepository;
import io.voyager1.model.data.CostBudgetModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 成本预算服务。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA 仓库（CostBudgetRepository），对外契约不变。
 *
 * @since 2026/8/31
 */
@Service
public class CostBudgetService {

    private final CostBudgetRepository repository;

    public CostBudgetService(CostBudgetRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public String save(CostBudgetModel budget) {
        long now = System.currentTimeMillis();
        CostBudgetEntity entity;
        if (budget.getId() == null || budget.getId().isEmpty()) {
            entity = new CostBudgetEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setCreateTimeMillis(now);
        } else {
            entity = repository.findById(budget.getId()).orElse(null);
            if (entity == null) {
                entity = new CostBudgetEntity();
                entity.setId(budget.getId());
                entity.setCreateTimeMillis(now);
            }
        }
        entity.setModifyTimeMillis(now);
        entity.setName(budget.getName());
        entity.setScopeType(budget.getScopeType());
        entity.setScopeValue(budget.getScopeValue());
        entity.setMonthlyLimit(budget.getMonthlyLimit());
        repository.save(entity);
        return entity.getId();
    }

    public List<CostBudgetModel> list() {
        return repository.findAll().stream().map(this::toModel).collect(Collectors.toList());
    }

    @Transactional
    public void delete(String id) {
        repository.deleteById(id);
    }

    private CostBudgetModel toModel(CostBudgetEntity entity) {
        CostBudgetModel model = CostBudgetModel.builder()
            .name(entity.getName())
            .scopeType(entity.getScopeType())
            .scopeValue(entity.getScopeValue())
            .monthlyLimit(entity.getMonthlyLimit())
            .build();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        return model;
    }
}
