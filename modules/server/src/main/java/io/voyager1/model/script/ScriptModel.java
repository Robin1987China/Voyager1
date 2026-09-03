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

import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.Voyager1Application;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseWorkspaceModel;
import io.voyager1.script.CommandParam;

import java.io.File;

/**
 * @since 2022/1/19
 */
@TableName(value = "OPS_SERVER_SCRIPT",
    nameKey = "脚本模版")
@Data
@EqualsAndHashCode(callSuper = true)
public class ScriptModel extends BaseWorkspaceModel {
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

    private String context;
    /**
     * 节点ID
     */
    private String nodeIds;
    /**
     * 触发器 token
     */
    private String triggerToken;

    public void setDefArgs(String defArgs) {
        this.defArgs = CommandParam.convertToParam(defArgs);
    }

    public File scriptPath() {
        return scriptPath(getId());
    }

    public static File scriptPath(String id) {
        if ((id == null || id.isEmpty())) {
            throw new IllegalArgumentException("id 为空");
        }
        File path = Voyager1Application.getInstance().getScriptPath();
        return FileUtil.file(path, id);
    }

    public File logFile(String executeId) {
        //File path = this.scriptPath();
        //return FileUtil.file(path, "log", executeId + ".log");
        return logFile(getId(), executeId);
    }

    public static File logFile(String id, String executeId) {
        File path = scriptPath(id);
        return FileUtil.file(path, "log", executeId + ".log");
    }

    @Override
    protected boolean hasCreateUser() {
        return true;
    }
}
