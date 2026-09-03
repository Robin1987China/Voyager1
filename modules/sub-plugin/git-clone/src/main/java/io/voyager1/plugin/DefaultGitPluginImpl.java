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

import io.voyager1.plugin.PluginConfig;
import org.eclipse.jgit.api.Git;

import java.util.Map;

/**
 * @since 2022/2/22
 */
@PluginConfig(name = "git-clone")
public class DefaultGitPluginImpl implements IWorkspaceEnvPlugin {

    @Override
    public Object execute(Object main, Map<String, Object> parameter) throws Exception {
        String type = main.toString();
        GitProcess gitProcess = GitProcessFactory.get(parameter, this);
        switch (type) {
            case "branchAndTagList":
                return gitProcess.branchAndTagList();
            case "pull": {
                return gitProcess.pull();
            }
            case "pullByTag": {
                return gitProcess.pullByTag();
            }
            case "systemGit": {
                return GitProcessFactory.existsSystemGit();
            }
            default:
                break;
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        Git.shutdown();
    }
}
