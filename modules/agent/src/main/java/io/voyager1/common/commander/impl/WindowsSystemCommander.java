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

import io.voyager1.common.commander.AbstractSystemCommander;
import io.voyager1.common.commander.Commander;
import io.voyager1.util.CommandUtil;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * windows 系统查询命令
 *
 * @since 2019/4/16
 */
@Conditional(Commander.Windows.class)
@Service
public class WindowsSystemCommander extends AbstractSystemCommander {

    @Override
    public String emptyLogFile(File file) {
        return CommandUtil.execSystemCommand("echo  \"\" > " + file.getAbsolutePath());
    }


//    @Override
//    public boolean getServiceStatus(String serviceName) {
//        String result = CommandUtil.execSystemCommand("sc query " + serviceName);
//        return (result != null && result.toLowerCase().contains("RUNNING".toLowerCase()));
//    }
//
//    @Override
//    public String startService(String serviceName) {
//        String format = String.format("net start %s", serviceName);
//        return CommandUtil.execSystemCommand(format);
//    }
//
//    @Override
//    public String stopService(String serviceName) {
//        String format = String.format("net stop %s", serviceName);
//        return CommandUtil.execSystemCommand(format);
//    }

    @Override
    public String buildKill(int pid) {
        return String.format("taskkill /F /PID %s", pid);
    }
}
