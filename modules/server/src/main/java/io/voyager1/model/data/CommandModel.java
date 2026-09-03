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
 * 指令信息
 *
 * @since : 2021/12/4 18:38
 */
@TableName(value = "OPS_COMMAND",
    nameKey = "命令管理")
@Data
@EqualsAndHashCode(callSuper = true)
public class CommandModel extends BaseWorkspaceModel {
    /**
     * 命令名称
     */
    private String name;
    /**
     * 命令描述
     */
    private String desc;
    /**
     * 指令内容
     */
    private String command;
    /**
     * 命令默认参数
     */
    private String defParams;
    /**
     * 默认关联大 ssh id
     */
    private String sshIds;
    /**
     * 自动执行的 cron
     */
    private String autoExecCron;
    /**
     * 触发器 token
     */
    private String triggerToken;
}
