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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseUserModifyDbModel;

/**
 * 工作空间
 *
 * @since 2021/12/3
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "SYS_WORKSPACE",
    nameKey = "工作空间")
@Data
@NoArgsConstructor
public class WorkspaceModel extends BaseUserModifyDbModel {

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;
    /**
     * 分组
     */
    private String group;
    /**
     * 集群信息Id
     *
     * @see io.voyager1.func.system.model.ClusterInfoModel
     */
    private String clusterInfoId;

    public WorkspaceModel(String id) {
        this.setId(id);
    }
}
