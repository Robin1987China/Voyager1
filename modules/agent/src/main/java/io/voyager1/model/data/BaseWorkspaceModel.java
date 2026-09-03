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

import io.voyager1.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.common.Const;
import io.voyager1.model.BaseModel;

/**
 * 插件端 工作空间相关的数据
 *
 * @since 2021/12/12
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseWorkspaceModel extends BaseModel {

    /**
     * 数据关联的工作空间
     */
    private String workspaceId;
    /**
     * 数据跟随的节点 ID
     */
    private String nodeId;
    /**
     * 最后修改人
     */
    private String modifyUser;

    private String createTime;

    private String modifyTime;

    /**
     * 创建人
     */
    private String createUser;

    public boolean global() {
        return java.util.Objects.equals(this.workspaceId, Const.WORKSPACE_GLOBAL);
    }
}
