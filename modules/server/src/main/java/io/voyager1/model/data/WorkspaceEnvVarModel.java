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
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseWorkspaceModel;

/**
 * 工作空间环境变量
 *
 * @since 2021/12/10
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "WORKSPACE_ENV_VAR",
    nameKey = "工作空间环境变量", workspaceBind = 2)
@Data
public class WorkspaceEnvVarModel extends BaseWorkspaceModel {

    /**
     * 名称
     */
    private String name;
    /**
     * 值
     */
    private String value;
    /**
     * 描述
     */
    private String description;
    /**
     * 节点ID
     */
    private String nodeIds;
    /**
     * 隐私变量{1，隐私变量，0 非隐私变量（明文回显）}
     */
    private Integer privacy;

    /**
     * 触发器 token
     */
    private String triggerToken;
}
