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

import io.voyager1.util.StrUtil;
import io.voyager1.util.OsInfo;
import io.voyager1.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.util.CommandUtil;

import java.util.Map;

/**
 * GIt执行器
 * <br>
 * Created By Hong on 2023/3/31
 *
 **/
@Slf4j
public class GitProcessFactory {

    private static final String WIN_EXISTS_GIT = "where git";
    private static final String LINUX_EXISTS_GIT = "which git";

    private static Boolean result;

    private static final String DEFAULT_GIT_PROCESS = "JGit";
    private static final String SYSTEM_GIT_PROCESS = "SystemGit";

    public static GitProcess get(Map<String, Object> parameter, IWorkspaceEnvPlugin workspaceEnvPlugin) {
        String processType = (String) parameter.getOrDefault("gitProcessType", DEFAULT_GIT_PROCESS);
        if (SYSTEM_GIT_PROCESS.equalsIgnoreCase(processType) && GitProcessFactory.existsSystemGit()) {
            return new SystemGitProcess(workspaceEnvPlugin, parameter);
        } else {
            return new JGitProcess(workspaceEnvPlugin, parameter);
        }
    }


    /**
     * 操作系统是否有GIT环境
     */
    public static boolean existsSystemGit() {
        if (result == null) {
            result = existsSystemGit2();
        }
        return result;
    }

    /**
     * 操作系统是否有GIT环境
     */
    private static boolean existsSystemGit2() {
        String result;
        OsInfo osInfo = SystemUtil.getOsInfo();
        if (osInfo.isWindows()) {
            result = CommandUtil.execSystemCommand(WIN_EXISTS_GIT);
            if ((result != null && result.contains(".exe"))) {
                log.info("git安装位置：{}", result);
                return true;
            }
        } else if (osInfo.isLinux() || osInfo.isMac()) {
            result = CommandUtil.execSystemCommand(LINUX_EXISTS_GIT);
            if (StrUtil.containsAny(result, "no git", "not found")) {
                return false;
            }
            log.info("git安装位置：{}", result);
            return true;
        } else {
            log.warn("不支持的系统类型：{}", osInfo.getName());
            return false;
        }
        return false;
    }

}
