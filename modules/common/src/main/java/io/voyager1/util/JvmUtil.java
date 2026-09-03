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

package io.voyager1.util;

import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.StrSplitter;
import io.voyager1.util.StrUtil;
import io.voyager1.common.Voyager1Manifest;
import io.voyager1.common.i18n.I18nMessageUtil;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * jvm jmx 工具
 *
 * @since 2019/4/13
 */
public class JvmUtil {

    /**
     * 状态服务器 jps 命令执行是否正常
     */
    public static boolean jpsNormal = false;

    /**
     * Jps 异常消息回调
     */
    public final static Supplier<String> JPS_ERROR_MSG = () -> {
        checkJpsNormal();
        return "当前服务器 jps 命令异常,请检查 jdk 是否完整,以及 java 环境变量是否配置正确";
    };

    /**
     * 支持的标签数组
     */
    private static final String[] VOYAGER1_PID_TAG = new String[]{"DVoyager1.application", "Voyager1.application"};

    /**
     * 检查 jps 命令是否正常
     */
    public static void checkJpsNormal() {
        JvmUtil.jpsNormal = JvmUtil.exist(Voyager1Manifest.getInstance().getPid());
    }

    /**
     * 获取进程标识
     *
     * @param id   i
     * @param path 路径
     * @return str
     */
    public static String getVoyager1PidTag(String id, String path) {
        return String.format("-%s=%s -DVoyager1.basedir=%s", VOYAGER1_PID_TAG[0], id, path);
    }


    /**
     * 获取当前系统运行的java 程序个数
     *
     * @return 如果发生异常则返回0
     */
    public static int getJavaVirtualCount() {
        String execSystemCommand = CommandUtil.execSystemCommand("jps -l");
        List<String> list = StrSplitter.splitTrim(execSystemCommand, "\n", true);
        return Math.max((list == null ? 0 : list.size()) - 1, 0);
    }

    /**
     * 执行 jps 判断是否存在 对应的进程
     *
     * @return true 存在
     */
    public static boolean exist(long pid) {
        String execSystemCommand = CommandUtil.execSystemCommand("jps -l");
        List<String> list = StrSplitter.splitTrim(execSystemCommand, "\n", true);
        String pidCommandInfo = list.stream()
            .filter(s -> {
                List<String> split = StrSplitter.splitTrim(s, " ", true);
                return java.util.Objects.equals(pid + "", (split == null || split.isEmpty() ? null : split.get(0)));
            })
            .findAny()
            .orElse(null);
        return (pidCommandInfo != null && !pidCommandInfo.isEmpty());
    }

    /**
     * 工具Voyager1运行项目的id 获取进程ID
     *
     * @param tag 项目id
     * @return 进程ID
     */
    public static Integer getPidByTag(String tag) {
        String execSystemCommand = CommandUtil.execSystemCommand("jps -mv");
        List<String> list = StrSplitter.splitTrim(execSystemCommand, "\n", true);
        return list.stream()
            .filter(s -> checkCommandLineIsVoyager1(s, tag))
            .map(s -> {
                List<String> split = java.util.Arrays.asList(s.split(java.util.regex.Pattern.quote(" ")));
                return (split == null || split.isEmpty() ? null : split.get(0));
            })
            .findAny()
            .map(ConvertUtil::toInt)
            .orElse(null);
    }

    /**
     * 判断命令行是否为voyager1 标识
     *
     * @param commandLine 命令行
     * @param tag         标识
     * @return true
     */
    public static boolean checkCommandLineIsVoyager1(String commandLine, String tag) {
        if ((commandLine == null || commandLine.isEmpty())) {
            return false;
        }
        String[] split = commandLine.split(java.util.regex.Pattern.quote(" "));
        String[] tags = Arrays.stream(VOYAGER1_PID_TAG)
            .map(s -> String.format("-%s=%s", s, tag))
            .collect(Collectors.toList())
            .toArray(new String[]{});
        for (String item : split) {
            if (StrUtil.equalsAnyIgnoreCase(item, tags)) {
                return true;
            }
        }
        return false;
    }
}
