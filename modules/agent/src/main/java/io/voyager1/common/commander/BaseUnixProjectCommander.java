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

package io.voyager1.common.commander;

import io.voyager1.util.StrSplitter;
import io.voyager1.util.StrUtil;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.configuration.ProjectConfig;
import io.voyager1.model.data.NodeProjectInfoModel;
import io.voyager1.service.manage.ProjectInfoService;
import io.voyager1.service.script.DslScriptServer;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.JvmUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * unix
 *
 * @since 2021/12/17
 */
@Slf4j
public abstract class BaseUnixProjectCommander extends AbstractProjectCommander {

    public BaseUnixProjectCommander(SystemCommander systemCommander,
                                    ProjectConfig projectConfig,
                                    DslScriptServer dslScriptServer,
                                    ProjectInfoService projectInfoService) {
        super(systemCommander, projectConfig, dslScriptServer, projectInfoService);
    }

    @Override
    public String buildRunCommand(NodeProjectInfoModel nodeProjectInfoModel) {
        NodeProjectInfoModel infoModel = projectInfoService.resolveModel(nodeProjectInfoModel);
        return this.buildRunCommand(nodeProjectInfoModel, infoModel);
    }


    @Override
    public String buildRunCommand(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel) {
        String lib = projectInfoService.resolveLibPath(originalModel);
        String path = this.getClassPathLib(originalModel, lib);
        if ((path == null || path.isBlank())) {
            return null;
        }
        String tag = nodeProjectInfoModel.getId();
        String absoluteLog = projectInfoService.resolveAbsoluteLog(nodeProjectInfoModel, originalModel);
        return String.format("nohup %s %s %s %s %s %s >> %s 2>&1 &", getRunJavaPath(nodeProjectInfoModel, false), Optional.ofNullable(nodeProjectInfoModel.getJvm()).orElse(""), JvmUtil.getVoyager1PidTag(tag, lib), path, Optional.ofNullable(originalModel.mainClass()).orElse(""), Optional.ofNullable(nodeProjectInfoModel.getArgs()).orElse(""), absoluteLog);
    }

    @Override
    public CommandOpResult stopJava(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel, int pid) {
        File file = projectInfoService.resolveLibFile(originalModel);
        List<String> result = new ArrayList<>();
        boolean success = false;
        String kill = systemCommander.kill(file, pid);
        result.add(kill);
        if (this.loopCheckRun(nodeProjectInfoModel, originalModel, false)) {
            success = true;
        } else {
            // 强制杀进程
            result.add("Kill not completed, test kill -9");
            String cmd = String.format("kill -9 %s", pid);
            try {
                CommandUtil.asyncExeLocalCommand(cmd, file, null, true);
            } catch (Exception e) {
                throw Lombok.sneakyThrow(e);
            }
            //
            if (this.loopCheckRun(nodeProjectInfoModel, originalModel, 5, false)) {
                success = true;
            } else {
                result.add("Kill -9 not completed, kill -9 failed ");
            }
        }
        String tag = nodeProjectInfoModel.getId();
        return CommandOpResult.of(success, status(tag)).appendMsg(result);
//        return status(tag) + " " + kill;
    }

    /**
     * 尝试ps -ef | grep  中查看进程id
     *
     * @param tag 进程标识
     * @return 运行标识
     */
    @Override
    protected String bySystemPs(String tag) {
        String execSystemCommand = CommandUtil.execSystemCommand("ps -ef | grep " + tag);
        log.debug("getPsStatus {} {}", tag, execSystemCommand);
        List<String> list = StrSplitter.splitTrim(execSystemCommand, "\n", true);
        for (String item : list) {
            if (JvmUtil.checkCommandLineIsVoyager1(item, tag)) {
                String[] split = item.split(java.util.regex.Pattern.quote(" "));
                return String.format("%s:%s", AbstractProjectCommander.RUNNING_TAG, split[1]);
            }
        }
        return AbstractProjectCommander.STOP_TAG;
    }
}
