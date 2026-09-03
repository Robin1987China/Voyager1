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

package io.voyager1.func.assets.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseUserModifyDbModel;

/**
 * @since 2024/6/1
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "OPS_SCRIPT_LIBRARY", nameKey = "脚本库信息")
@Data
public class ScriptLibraryModel extends BaseUserModifyDbModel {
    /**
     * 脚本唯一的标记
     */
    private String tag;
    /**
     * 脚本内容
     */
    private String script;
    /**
     * 描述
     */
    private String description;
    /**
     * 版本
     */
    private String version;
    /**
     * 关联的资产机器节点
     */
    private String machineIds;

    @Override
    protected boolean hasCreateUser() {
        return true;
    }
}
