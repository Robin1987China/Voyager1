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

package io.voyager1.controller.user;

import lombok.Data;
import io.voyager1.func.system.model.ClusterInfoModel;

/**
 * 用户工作空间配置
 *
 * @since 2023/3/15
 */
@Data
public class UserWorkspaceModel {

    private String id;

    /**
     * 用户自定义名
     */
    private String name;
    /**
     * 原始
     */
    private String originalName;
    private String group;
    /**
     * 自定义排序规则
     */
    private Integer sort;
    /**
     * 集群Id
     *
     * @see ClusterInfoModel#getId()
     */
    private String clusterInfoId;
}
