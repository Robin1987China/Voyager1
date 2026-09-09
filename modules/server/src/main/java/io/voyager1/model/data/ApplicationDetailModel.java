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

package io.voyager1.model.data;

import io.voyager1.model.log.BuildHistoryLog;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 应用详情聚合视图：应用信息 + 环境泳道 + 构建历史 + 部署记录。
 * <p>
 * 纯视图模型，非持久化，供「应用交付 → 应用」详情页一次拉全。
 *
 * @since 2026/9/7
 */
@Data
@Builder
public class ApplicationDetailModel {

    /**
     * 应用基本信息
     */
    private ApplicationModel application;

    /**
     * 环境泳道（dev/test/prod 各自当前部署版本）
     */
    private List<EnvironmentLaneModel> environments;

    /**
     * 构建历史（按构建编号倒序）
     */
    private List<BuildHistoryLog> buildHistory;

    /**
     * 部署记录（按创建时间倒序）
     */
    private List<DeploymentRecordModel> deploymentRecords;
}
