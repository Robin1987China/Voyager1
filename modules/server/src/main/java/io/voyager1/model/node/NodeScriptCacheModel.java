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

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseNodeModel;
import io.voyager1.script.CommandParam;

/**
 * 脚本模版实体
 *
 * @since 2021/12/12
 **/
@TableName(value = "OPS_SCRIPT",
    nameKey = "节点脚本模版")
@Data
@EqualsAndHashCode(callSuper = true)
public class NodeScriptCacheModel extends BaseNodeModel {
    /**
     * 脚本ID
     */
    private String scriptId;
    /**
     * 模版名称
     */
    private String name;
    /**
     * 最后执行人员
     */
    private String lastRunUser;
    /**
     * 定时执行
     */
    private String autoExecCron;
    /**
     * 默认参数
     */
    private String defArgs;
    /**
     * 描述
     */
    private String description;
    /**
     * 脚本类型
     */
    private String scriptType;
    /**
     * 触发器 token
     */
    private String triggerToken;

    @Override
    public String dataId() {
        return getScriptId();
    }

    @Override
    public void dataId(String id) {
        setScriptId(id);
    }

    public void setDefArgs(String defArgs) {
        this.defArgs = CommandParam.convertToParam(defArgs);
    }

    @Override
    protected boolean hasCreateUser() {
        return true;
    }
}
