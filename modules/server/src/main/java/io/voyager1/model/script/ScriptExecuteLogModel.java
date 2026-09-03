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

package io.voyager1.model.script;

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseWorkspaceModel;
import io.voyager1.model.data.CommandExecLogModel;

/**
 * @since 2022/1/19
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "OPS_SERVER_SCRIPT_LOG",
    nameKey = "脚本模版执行记录", parents = ScriptModel.class)
@Data
public class ScriptExecuteLogModel extends BaseWorkspaceModel {

    /**
     * 脚本ID
     */
    private String scriptId;
    /**
     * 脚本名称
     */
    private String scriptName;
    /**
     * 触发类型 {0，手动，1 自动触发，2 触发器，3 构建事件}
     */
    private Integer triggerExecType;

    /**
     * 退出码
     */
    private Integer exitCode;

    /**
     * @see CommandExecLogModel.Status
     */
    private Integer status;
}
