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

package io.voyager1.script;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.DateTime;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.LineHandler;
import io.voyager1.util.Tuple;
import io.voyager1.util.StrUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.util.CommandUtil;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * dsl 执行脚本
 *
 * @since 2022/1/15
 */
@Setter
@Slf4j
public class DslScriptBuilder extends BaseRunScript implements Runnable {


    private final String args;
    private String action;
    private File scriptFile;
    private boolean autoDelete;
    private EnvironmentMapBuilder environmentMapBuilder;

    public DslScriptBuilder(String action,
                            EnvironmentMapBuilder environmentMapBuilder,
                            String args,
                            String log,
                            Charset charset) {
        super(FileUtil.file(log), charset);
        this.action = action;
        this.environmentMapBuilder = environmentMapBuilder;
        this.args = args;
    }

    /**
     * 初始化
     */
    private ProcessBuilder init() {
        //
        String script = FileUtil.getAbsolutePath(scriptFile);
        ProcessBuilder processBuilder = new ProcessBuilder();
        List<String> command = io.voyager1.util.ConvertUtil.splitTrim(args, " ");
        command.add(0, script);
        CommandUtil.paddingPrefix(command);
        log.debug(String.join(" ", command));
        processBuilder
            .environment()
            .putAll(environmentMapBuilder.environment());
        //
        String voyager1ExecPath = environmentMapBuilder.get("VOYAGER1_EXEC_PATH");
        String projectPath = environmentMapBuilder.get("PROJECT_PATH");
        if ((voyager1ExecPath != null && !voyager1ExecPath.isEmpty()) && (projectPath != null && !projectPath.isEmpty())) {
            boolean absolutePath = FileUtil.isAbsolutePath(voyager1ExecPath);
            if (absolutePath) {
                processBuilder.directory(FileUtil.file(projectPath));
            } else {
                processBuilder.directory(FileUtil.file(projectPath, voyager1ExecPath));
            }
        } else {
            processBuilder.directory(FileUtil.getParent(scriptFile, 1));
        }
        processBuilder.redirectErrorStream(true);
        processBuilder.command(command);
        return processBuilder;
    }

    @Override
    public void run() {
        try {
            ProcessBuilder processBuilder = this.init();
            environmentMapBuilder.eachStr(this::info);
            //
            this.system("开始执行: {}", this.action);
            process = processBuilder.start();
            inputStream = process.getInputStream();
            IoUtil.readLines(inputStream, ExtConfigBean.getConsoleLogCharset(), (LineHandler) line -> {
                String formatLine = formatLine(line);
                this.info(formatLine);
            });
            //
            int waitFor = process.waitFor();
            //
            this.system("执行结束: {} {}", this.action, waitFor);
        } catch (Exception e) {
            log.error("执行异常", e);
            this.systemError("执行异常：" + e.getMessage());
        } finally {
            this.close();
        }
    }


    private String formatLine(String line) {
        return String.format("%s [%s] - %s", DateTime.now().toString("yyyy-MM-dd HH:mm:ss.SSS"), this.action, line);
    }

    /**
     * 执行
     * <p>
     * 0 退出码
     * 1 日志
     */
    public Tuple syncExecute() {
        ProcessBuilder processBuilder = this.init();
        List<String> result = new ArrayList<>();
        int waitFor = -100;
        try {
            //
            process = processBuilder.start();
            inputStream = process.getInputStream();

            IoUtil.readLines(inputStream, ExtConfigBean.getConsoleLogCharset(), (LineHandler) line -> result.add(this.formatLine(line)));
            //
            waitFor = process.waitFor();
            // 插入第一行
            result.add(0, this.formatLine(String.format("本次执行退出码: %s", waitFor)));
            //
        } catch (Exception e) {
            log.error("执行异常", e);
            result.add(this.formatLine(String.format("执行异常：", e.getMessage())));
        } finally {
            this.close();
        }
        return new Tuple(waitFor, result);
    }

    @Override
    protected void end(String msg) {

    }

    @Override
    protected void msgCallback(String msg) {

    }

    @Override
    public void close() {
        super.close();
        //
        if (autoDelete) {
            try {
                FileUtil.del(this.scriptFile);
            } catch (Exception ignored) {
            }
        }
    }


}
