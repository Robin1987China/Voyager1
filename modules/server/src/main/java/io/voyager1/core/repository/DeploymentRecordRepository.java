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

import io.voyager1.core.entity.DeploymentRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 部署记录 JPA 仓库。
 */
public interface DeploymentRecordRepository extends JpaRepository<DeploymentRecordEntity, String> {

    List<DeploymentRecordEntity> findByVersionIdOrderByCreateTimeMillisDesc(String versionId);

    List<DeploymentRecordEntity> findByEnvironmentOrderByCreateTimeMillisDesc(String environment);

    DeploymentRecordEntity findFirstByEnvironmentAndStatusOrderByCreateTimeMillisDesc(String environment, Integer status);

    /**
     * 按构建配置查询部署记录（创建时间倒序）。
     */
    List<DeploymentRecordEntity> findByBuildIdOrderByCreateTimeMillisDesc(String buildId);

    /**
     * 按构建配置 + 环境查询最新一条部署记录。
     */
    DeploymentRecordEntity findFirstByBuildIdAndEnvironmentOrderByCreateTimeMillisDesc(String buildId, String environment);

    /**
     * 按构建配置 + 环境 + 状态查询最新一条部署记录（泳道"当前版本"只取成功记录）。
     */
    DeploymentRecordEntity findFirstByBuildIdAndEnvironmentAndStatusOrderByCreateTimeMillisDesc(String buildId, String environment, Integer status);
}
