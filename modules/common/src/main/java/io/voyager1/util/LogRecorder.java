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

import io.voyager1.util.ExceptionUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.FileWriter;
import io.voyager1.util.CharsetUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.log.ILogRecorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.exception.LogRecorderCloseException;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 日志记录
 *
 * @since 2022/1/26
 */
@Slf4j
@Getter
public class LogRecorder extends OutputStream implements ILogRecorder, AutoCloseable {

    private File file;
    private PrintWriter writer;
    private final Charset charset;

    private LogRecorder(File file, Charset charset) {
        if (file == null) {
            this.writer = null;
            this.file = null;
            this.charset = charset;
            return;
        }
        this.file = file;
        this.charset = charset;
        this.writer = FileWriter.create(file, charset).getPrintWriter(true);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private File file;
        private Charset charset;

        Builder() {
        }

        public Builder file(final File file) {
            this.file = file;
            return this;
        }

        public Builder charset(final Charset charset) {
            this.charset = charset;
            return this;
        }

        public LogRecorder build() {
            Charset charset1 = (this.charset != null ? this.charset : StandardCharsets.UTF_8);
            return new LogRecorder(this.file, charset1);
        }

        public String toString() {
            return "LogRecorder.LogRecorderBuilder(file=" + this.file + ", charset=" + this.charset + ")";
        }
    }


    /**
     * 记录错误信息
     *
     * @param title     错误描述
     * @param throwable 堆栈信息
     */
    public void error(String title, Throwable throwable) {
        log.error(title, throwable);
        if (writer == null) {
            throw new LogRecorderCloseException();
        }
        writer.println(title);
        String s = java.util.Arrays.toString(throwable.getStackTrace());
        writer.println(s);
        writer.flush();
    }

    /**
     * 记录单行日志
     *
     * @param info 日志
     */
    public String info(String info, Object... vals) {
        if (writer == null) {
            throw new LogRecorderCloseException();
        }
        String format = String.format(info, vals);
        writer.println(format);
        writer.flush();
        return format;
    }

    /**
     * 记录单行日志
     *
     * @param info 日志
     */
    public String system(String info, Object... vals) {
        return this.info("[SYSTEM-INFO] " + info, vals);
    }

    /**
     * 记录单行日志
     *
     * @param info 日志
     */
    public String systemError(String info, Object... vals) {
        return this.info("[SYSTEM-ERROR] " + info, vals);
    }

    /**
     * 记录单行日志
     *
     * @param info 日志
     */
    public String systemWarning(String info, Object... vals) {
        return this.info("[SYSTEM-WARNING] " + info, vals);
    }

    /**
     * 记录单行日志 (不还行)
     *
     * @param info 日志
     */
    public void append(String info, Object... vals) {
        if (writer == null) {
            throw new LogRecorderCloseException();
        }
        writer.append(String.format(info, vals));
        writer.flush();

    }

    /**
     * 获取 文件输出流
     *
     * @return Writer
     */
    public PrintWriter getPrintWriter() {
        return writer;
    }

    @Override
    public void close() {
        IoUtil.close(writer);
        this.writer = null;
        this.file = null;
    }

    public long size() {
        Assert.notNull(writer, "日志记录器未启用");
        return FileUtil.size(this.file);
    }

    @Override
    public void write(int b) throws IOException {
        if (writer == null) {
            throw new LogRecorderCloseException();
        }
        writer.write((byte) b);
    }
}
