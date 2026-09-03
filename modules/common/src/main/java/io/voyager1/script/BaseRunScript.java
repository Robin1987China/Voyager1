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

import io.voyager1.util.DateTime;
import io.voyager1.util.DateUnit;
import io.voyager1.util.DateUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.log.ILogRecorder;
import io.voyager1.Voyager1Application;
import io.voyager1.common.Const;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.LogRecorder;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Optional;

/**
 * 脚本模版执行父类
 */
public abstract class BaseRunScript implements AutoCloseable, ILogRecorder {

    /**
     * 日志文件
     */
    protected final LogRecorder logRecorder;
    protected final File logFile;
    protected Process process;
    protected InputStream inputStream;

    protected BaseRunScript(File logFile, Charset charset) {
        if (logFile == null) {
            this.logFile = null;
            this.logRecorder = null;
        } else {
            this.logFile = logFile;
            this.logRecorder = LogRecorder.builder().file(logFile).charset(charset).build();
        }
    }

    @Override
    public String info(String info, Object... vals) {
        String msg = logRecorder.info(info, vals);
        this.msgCallback(msg);
        return msg;
    }

    @Override
    public String system(String info, Object... vals) {
        String msg = logRecorder.system(info, vals);
        this.msgCallback(msg);
        return msg;
    }

    @Override
    public String systemError(String info, Object... vals) {
        String msg = logRecorder.systemError(info, vals);
        this.msgCallback(msg);
        return msg;
    }

    @Override
    public String systemWarning(String info, Object... vals) {
        String msg = logRecorder.systemWarning(info, vals);
        this.msgCallback(msg);
        return msg;
    }

    /**
     * 输出消息后的回调
     *
     * @param msg 消息
     */
    protected abstract void msgCallback(String msg);

    /**
     * 结束执行
     *
     * @param msg 异常方法
     */
    protected abstract void end(String msg);

    @Override
    public void close() {
        // windows 中不能正常关闭
        IoUtil.close(inputStream);
        CommandUtil.kill(process);
        IoUtil.close(logRecorder);
    }

    /**
     * 清理 脚本文件执行缓存
     */
    public static void clearRunScript() {
        String dataPath = Voyager1Application.getInstance().getDataPath();
        File scriptFile = FileUtil.file(dataPath, Const.SCRIPT_RUN_CACHE_DIRECTORY);
        if (!FileUtil.isDirectory(scriptFile)) {
            return;
        }
        File[] files = scriptFile.listFiles(pathname -> {
            Date lastModifiedTime = FileUtil.lastModifiedTime(pathname);
            DateTime now = DateTime.now();
            long between = DateUtil.between(lastModifiedTime, now, DateUnit.HOUR);
            // 文件大于一个小时才能被删除
            return between > 1;
        });
        Optional.ofNullable(files).ifPresent(files1 -> {
            for (File file : files1) {
                try {
                    FileUtil.del(file);
                } catch (Exception ignored) {
                }
            }
        });
    }
}
