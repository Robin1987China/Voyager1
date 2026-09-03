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

package io.voyager1.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成本预算 JPA 实体。
 * <p>
 * 映射表 {@code COST_BUDGET}，列名与旧 CSV 建表一致（camelCase）。
 */
@Entity
@Table(name = "COST_BUDGET")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CostBudgetEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "createTimeMillis")
    private Long createTimeMillis;

    @Column(name = "modifyTimeMillis")
    private Long modifyTimeMillis;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "scopeType", length = 20)
    private String scopeType;

    @Column(name = "scopeValue", length = 100)
    private String scopeValue;

    @Column(name = "monthlyLimit")
    private Double monthlyLimit;
}
