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

import io.voyager1.util.StrSplitter;
import io.voyager1.util.StrUtil;
import io.voyager1.common.commander.AbstractProjectCommander;
import io.voyager1.common.commander.CommandOpResult;
import io.voyager1.common.commander.Commander;
import io.voyager1.common.commander.SystemCommander;
import io.voyager1.configuration.AgentConfig;
import io.voyager1.model.data.NodeProjectInfoModel;
import io.voyager1.model.system.NetstatModel;
import io.voyager1.service.manage.ProjectInfoService;
import io.voyager1.service.script.DslScriptServer;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.JvmUtil;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * windows 版
 *
 */
@Conditional(Commander.Windows.class)
@Service
public class WindowsProjectCommander extends AbstractProjectCommander {

    public WindowsProjectCommander(AgentConfig agentConfig,
                                   SystemCommander systemCommander,
                                   DslScriptServer dslScriptServer,
                                   ProjectInfoService projectInfoService) {
        super(systemCommander, agentConfig.getProject(), dslScriptServer, projectInfoService);
    }

    @Override
    public String buildRunCommand(NodeProjectInfoModel nodeProjectInfoModel) {
        NodeProjectInfoModel infoModel = projectInfoService.resolveModel(nodeProjectInfoModel);
        return this.buildRunCommand(nodeProjectInfoModel, infoModel);
    }

    @Override
    public String buildRunCommand(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel) {
        String lib = projectInfoService.resolveLibPath(originalModel);
        String classPath = this.getClassPathLib(originalModel, lib);
        if ((classPath == null || classPath.isBlank())) {
            return null;
        }
        // 拼接命令
        String jvm = nodeProjectInfoModel.getJvm();
        String tag = nodeProjectInfoModel.getId();
        String mainClass = originalModel.mainClass();
        String args = nodeProjectInfoModel.getArgs();

        String absoluteLog = projectInfoService.resolveAbsoluteLog(nodeProjectInfoModel, originalModel);
        return String.format("%s %s %s %s %s %s >> %s &", getRunJavaPath(nodeProjectInfoModel, true), Optional.ofNullable(jvm).orElse(""), JvmUtil.getVoyager1PidTag(tag, lib), classPath, Optional.ofNullable(mainClass).orElse(""), Optional.ofNullable(args).orElse(""), absoluteLog);
    }

    @Override
    public CommandOpResult stopJava(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel, int pid) {
        String tag = nodeProjectInfoModel.getId();
        List<String> result = new ArrayList<>();
        boolean success = false;
        // 如果正在运行，则执行杀进程命令
        File file = projectInfoService.resolveLibFile(nodeProjectInfoModel);
        String kill = systemCommander.kill(file, pid);
        result.add(kill);
        if (this.loopCheckRun(nodeProjectInfoModel, originalModel, false)) {
            success = true;
        } else {
            result.add("Kill not completed");
        }
        return CommandOpResult.of(success, status(tag)).appendMsg(result);
        // return status(tag) + " " + kill;
    }

    @Override
    public List<NetstatModel> listNetstat(int pId, boolean listening) {
        String cmd;
        if (listening) {
            cmd = "netstat -nao -p tcp | findstr \"LISTENING\" | findstr " + pId;
        } else {
            cmd = "netstat -nao -p tcp | findstr /V \"CLOSE_WAIT\" | findstr " + pId;
        }
        String result = CommandUtil.execSystemCommand(cmd);
        List<String> netList = StrSplitter.splitTrim(result, "\n", true);
        if (netList == null || netList.isEmpty()) {
            return null;
        }
        List<NetstatModel> array = new ArrayList<>();
        for (String str : netList) {
            List<String> list = StrSplitter.splitTrim(str, " ", true);
            if (list.size() < 5) {
                continue;
            }
            NetstatModel netstatModel = new NetstatModel();
            netstatModel.setProtocol(list.get(0));
            netstatModel.setLocal(list.get(1));
            netstatModel.setForeign(list.get(2));
            netstatModel.setStatus(list.get(3));
            netstatModel.setName(list.get(4));
            array.add(netstatModel);
        }
        return array;
    }

    // tasklist | findstr /s /i "java"
    // wmic process where caption="javaw.exe" get processid,caption,commandline /value
}
