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

import io.voyager1.core.entity.EnvironmentTargetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 环境-部署目标 JPA 仓库。
 */
public interface EnvironmentTargetRepository extends JpaRepository<EnvironmentTargetEntity, String> {

    /**
     * 按环境查询启用目标，先按排序、再按创建时间升序。
     */
    List<EnvironmentTargetEntity> findByEnvironmentIdAndEnabledOrderBySortValueAscCreateTimeMillisAsc(String environmentId, Integer enabled);

    /**
     * 判断同一目标是否已绑定。
     */
    boolean existsByEnvironmentIdAndTargetTypeAndTargetId(String environmentId, String targetType, String targetId);
}
