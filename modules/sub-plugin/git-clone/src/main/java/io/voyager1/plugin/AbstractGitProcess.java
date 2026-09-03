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

package io.voyager1.plugin;

import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;

import java.io.File;
import java.util.Map;

/**
 * GIt执行基类
 * <br>
 * Created By Hong on 2023/3/31
 *
 **/
@Slf4j
public abstract class AbstractGitProcess implements GitProcess {

    private final IWorkspaceEnvPlugin workspaceEnvPlugin;
    protected final Map<String, Object> parameter;

    protected AbstractGitProcess(IWorkspaceEnvPlugin workspaceEnvPlugin, Map<String, Object> parameter) {
        this.workspaceEnvPlugin = workspaceEnvPlugin;
        this.parameter = decryptParameter(parameter);
    }

    /**
     * 解密参数
     *
     * @param parameter 参数
     */
    protected Map<String, Object> decryptParameter(Map<String, Object> parameter) {
        try {
            parameter.put("password", workspaceEnvPlugin.convertRefEnvValue(parameter, "password"));
            parameter.put("username", workspaceEnvPlugin.convertRefEnvValue(parameter, "username"));
        } catch (Exception e) {
            log.error("解密参数失败", e);
        }
        return parameter;
    }


    /**
     * 获取保存路径
     */
    protected File getSaveFile() {
        return (File) parameter.get("savePath");
    }

    /**
     * 获取分支Name
     */
    protected String getBranchName() {
        return (String) parameter.get("branchName");
    }

    /**
     * 获取TagName
     */
    protected String getTagName() {
        return (String) parameter.get("tagName");
    }
}
