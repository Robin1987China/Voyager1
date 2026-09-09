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

import lombok.Builder;
import lombok.Data;

/**
 * 应用详情里的环境泳道（每环境当前部署版本）。
 * <p>
 * 纯视图模型，非持久化：把环境策略与当前部署记录拼到一起，供前端泳道晋升 UI 使用。
 *
 * @since 2026/9/7
 */
@Data
@Builder
public class EnvironmentLaneModel {

    /**
     * 环境名（dev/test/prod）
     */
    private String name;

    /**
     * 环境类型（dev/test/prod）
     */
    private String type;

    /**
     * 部署策略（CI_CD / CD_ONLY）
     */
    private String strategy;

    /**
     * 是否需要审批
     */
    private Boolean approvalRequired;

    /**
     * 当前部署的版本（该应用在该环境最新一条部署记录），无则 null
     */
    private DeploymentRecordModel current;
}
