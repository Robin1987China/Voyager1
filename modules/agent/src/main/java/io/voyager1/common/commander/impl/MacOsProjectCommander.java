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

package io.voyager1.common.commander.impl;

import io.voyager1.util.CollUtil;
import io.voyager1.util.StrSplitter;
import io.voyager1.util.StrUtil;
import io.voyager1.common.commander.BaseUnixProjectCommander;
import io.voyager1.common.commander.Commander;
import io.voyager1.common.commander.SystemCommander;
import io.voyager1.configuration.AgentConfig;
import io.voyager1.model.system.NetstatModel;
import io.voyager1.service.manage.ProjectInfoService;
import io.voyager1.service.script.DslScriptServer;
import io.voyager1.util.CommandUtil;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * MacOSProjectCommander
 * <p>
 * some commands cannot execute success on Mac OS
 *
 */
@Conditional(Commander.Mac.class)
@Service
public class MacOsProjectCommander extends BaseUnixProjectCommander {

    public MacOsProjectCommander(AgentConfig agentConfig,
                                 SystemCommander systemCommander,
                                 DslScriptServer dslScriptServer,
                                 ProjectInfoService projectInfoService) {
        super(systemCommander, agentConfig.getProject(), dslScriptServer, projectInfoService);
    }

    @Override
    public List<NetstatModel> listNetstat(int pId, boolean listening) {
        String cmd;
        if (listening) {
            cmd = "lsof -n -P -iTCP -sTCP:LISTEN |grep " + pId + " | head -20";
        } else {
            cmd = "lsof -n -P -iTCP -sTCP:CLOSE_WAIT |grep " + pId + " | head -20";
        }
        return this.listNetstat(cmd);
    }

    protected List<NetstatModel> listNetstat(String cmd) {
        String result = CommandUtil.execSystemCommand(cmd);
        List<String> netList = StrSplitter.splitTrim(result, "\n", true);
        if ((netList == null || netList.isEmpty())) {
            return null;
        }
        return netList.stream()
            .map(str -> {
                List<String> list = StrSplitter.splitTrim(str, " ", true);
                if (list.size() < 10) {
                    return null;
                }
                NetstatModel netstatModel = new NetstatModel();
                netstatModel.setProtocol(list.get(7));
                //netstatModel.setReceive(list.get(1));
                //netstatModel.setSend(list.get(2));
                netstatModel.setLocal(list.get(8));
                netstatModel.setForeign(list.get(4));
                if ("tcp".equalsIgnoreCase(netstatModel.getProtocol())) {
                    netstatModel.setStatus((9 < list.size() ? list.get(9) : null));
                    netstatModel.setName((0 < list.size() ? list.get(0) : null));
                } else {
                    netstatModel.setStatus("-");
                    netstatModel.setName((5 < list.size() ? list.get(5) : null));
                }

                return netstatModel;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
