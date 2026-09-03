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

import io.voyager1.util.ConvertUtil;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.commander.AbstractSystemCommander;
import io.voyager1.common.commander.Commander;
import io.voyager1.util.CommandUtil;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 */
@Slf4j
@Conditional(Commander.Mac.class)
@Service
public class MacOsSystemCommander extends AbstractSystemCommander {


    @Override
    public String emptyLogFile(File file) {
        return CommandUtil.execSystemCommand("cp /dev/null " + file.getAbsolutePath());
    }


//    @Override
//    public boolean getServiceStatus(String serviceName) {
//        if ((serviceName != null && serviceName.startsWith("/"))) {
//            String ps = getPs(serviceName);
//            return (ps != null && !ps.isEmpty());
//        }
//        /**
//         * Mac OS 里面查询服务的命令是 launchctl list | grep serverName
//         * 第一个数字是进程的 PID，如果进程正在运行，如果它不在运行，则显示 "-"
//         * 第二个数字是进程的退出代码（如果已完成）。如果为负，则为终止信号的数量
//         * 第三列进程名称
//         */
//        String format = String.format("service %s status", serviceName);
//        String result = CommandUtil.execSystemCommand(format);
//        return (result != null && result.toLowerCase().contains("RUNNING".toLowerCase()));
//    }
//
//    private String getPs(final String serviceName) {
//        String ps = String.format(" ps -ef |grep -w %s | grep -v grep", serviceName);
//        return CommandUtil.execSystemCommand(ps);
//    }
//
//    @Override
//    public String startService(String serviceName) {
//        if ((serviceName != null && serviceName.startsWith("/"))) {
//            try {
//                CommandUtil.asyncExeLocalCommand(FileUtil.file(SystemUtil.getUserInfo().getHomeDir()), serviceName);
//                return "ok";
//            } catch (Exception e) {
//                log.error("执行异常", e);
//                return "执行异常：" + e.getMessage();
//            }
//        }
//        /**
//         * Mac OS 里面启动服务命令是 launchctl start serverName
//         */
//        String format = String.format("service %s start", serviceName);
//        return CommandUtil.execSystemCommand(format);
//    }
//
//    @Override
//    public String stopService(String serviceName) {
//        if ((serviceName != null && serviceName.startsWith("/"))) {
//            String ps = getPs(serviceName);
//            List<String> list = io.voyager1.util.ConvertUtil.splitTrim(ps, "\n");
//            if (list == null || list.isEmpty()) {
//                return "stop";
//            }
//            String s = list.get(0);
//            list = io.voyager1.util.ConvertUtil.splitTrim(s, " ");
//            if (list == null || list.size() < 2) {
//                return "stop";
//            }
//            File file = new File(SystemUtil.getUserInfo().getHomeDir());
//            int pid = ConvertUtil.toInt(list.get(1), 0);
//            if (pid <= 0) {
//                return "error stop";
//            }
//            return kill(file, pid);
//        }
//        /**
//         * Mac OS 里面启动服务命令是 launchctl stop serverName
//         */
//        String format = String.format("service %s stop", serviceName);
//        return CommandUtil.execSystemCommand(format);
//    }

    @Override
    public String buildKill(int pid) {
        return String.format("kill  %s", pid);
    }
}
