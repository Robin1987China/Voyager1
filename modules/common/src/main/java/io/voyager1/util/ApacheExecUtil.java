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


import io.voyager1.system.ExtConfigBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.*;
import org.apache.commons.exec.environment.EnvironmentUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

@Slf4j
public class ApacheExecUtil {

    private static final ShutdownHookProcessDestroyer shutdownHookProcessDestroyer = new ShutdownHookProcessDestroyer();
    private static final Map<String, Process> processMap = new java.util.concurrent.ConcurrentHashMap<>();

    public static void addProcess(Process process) {
        shutdownHookProcessDestroyer.add(process);
    }

    /**
     * 关闭 Process
     *
     * @param execId 执行Id
     */
    public static void kill(String execId) {
        Process process = processMap.remove(execId);
        if (process == null) {
            return;
        }
        CommandUtil.kill(process);
    }

    /**
     * 执行脚本
     *
     * @param scriptFile  脚本文件
     * @param baseDir     基础目录
     * @param env         环境变量
     * @param args        参数
     * @param logRecorder 日志记录
     * @return 退出码
     * @throws IOException io
     */
    public static int exec(String execId, File scriptFile, File baseDir, Map<String, String> env, String args, LogRecorder logRecorder) throws IOException {
        List<String> build = CommandUtil.build(scriptFile, args);
        String join = String.join(" ", build);
        CommandLine commandLine = CommandLine.parse(join);
        log.debug(join);
        Charset charset;
        try {
            charset = ExtConfigBean.getConsoleLogCharset();
        } catch (Exception e) {
            // 直接执行，使用默认编码格式
            charset = java.nio.charset.Charset.defaultCharset();
        }
        Map<String, String> procEnvironment = EnvironmentUtils.getProcEnvironment();
        procEnvironment.putAll(env);
        final LogOutputStream logOutputStream = new LogOutputStream(1) {
            @Override
            protected void processLine(String line, int logLevel) {
                logRecorder.info(line);
            }
        };
        // 重定向stdout和stderr到文件
        PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(logOutputStream, logOutputStream);

        // 创建执行器
        DefaultExecutor executor = DefaultExecutor.builder()
            .setExecuteStreamHandler(pumpStreamHandler)
            .setWorkingDirectory(baseDir)
            .get();
        //
        executor.setProcessDestroyer(new ProcessDestroyer() {
            private int size = 0;

            @Override
            public boolean add(Process process) {
                processMap.put(execId, process);
                size++;
                return shutdownHookProcessDestroyer.add(process);
            }

            @Override
            public boolean remove(Process process) {
                processMap.remove(execId);
                size--;
                return shutdownHookProcessDestroyer.remove(process);
            }

            @Override
            public int size() {
                return size;
            }
        });
        pumpStreamHandler.stop();
        // 执行，打印退出码
        try {
            return executor.execute(commandLine, procEnvironment);
        } catch (ExecuteException executeException) {
            logRecorder.systemWarning("执行异常：{}", executeException.getMessage());
            return executeException.getExitValue();
        }
    }
}
