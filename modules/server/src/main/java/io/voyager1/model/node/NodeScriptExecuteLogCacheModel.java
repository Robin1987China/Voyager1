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

package io.voyager1.model.node;

import io.voyager1.util.PropIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseNodeModel;

/**
 * @since 2021/12/12
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "OPS_SCRIPT_LOG",
    nameKey = "节点脚本模版执行记录", parents = NodeScriptCacheModel.class)
@Data
public class NodeScriptExecuteLogCacheModel extends BaseNodeModel {

    /**
     *
     */
    @PropIgnore
    private String name;
    /**
     * 脚本ID
     */
    private String scriptId;
    /**
     * 脚本名称
     */
    private String scriptName;
    /**
     * 触发类型 {0，手动，1 自动触发}
     */
    private Integer triggerExecType;

    @Override
    public String fullId() {
        throw new IllegalStateException("NO implements");
    }

    public void setName(String name) {
        this.name = name;
        this.scriptName = name;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
        this.name = scriptName;
    }

    @Override
    public String dataId() {
        return getScriptId();
    }

    @Override
    public void dataId(String id) {
        setScriptId(id);
    }
}
