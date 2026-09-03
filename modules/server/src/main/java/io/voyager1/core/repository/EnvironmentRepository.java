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

package io.voyager1.core.repository;

import io.voyager1.core.entity.EnvironmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 环境定义 JPA 仓库。
 */
public interface EnvironmentRepository extends JpaRepository<EnvironmentEntity, String> {

    /**
     * 按启用状态查询，先按排序、再按创建时间升序。
     */
    List<EnvironmentEntity> findByEnabledOrderBySortValueAscCreateTimeMillisAsc(Integer enabled);
}
