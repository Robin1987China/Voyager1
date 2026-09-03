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

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * CSV 写出器 {@code io.voyager1.util.CsvWriter}。
 */
public final class CsvWriter implements Closeable, Flushable {

    private static final char DEFAULT_FIELD_SEPARATOR = ',';
    private static final char DEFAULT_TEXT_DELIMITER = '"';
    private static final char[] DEFAULT_LINE_DELIMITER = {'\r', '\n'};

    private final Writer writer;
    private final CsvWriteConfig config;
    private boolean newline = true;
    private boolean isFirstLine = true;

    public CsvWriter(File file) {
        this(file, StandardCharsets.UTF_8);
    }

    public CsvWriter(String filePath, Charset charset) {
        this(new File(filePath), charset);
    }

    public CsvWriter(File file, Charset charset) {
        this(file, charset, false);
    }

    public CsvWriter(File file, Charset charset, boolean isAppend) {
        this(openWriter(file, charset, isAppend), null);
    }

    public CsvWriter(Writer writer) {
        this(writer, null);
    }

    public CsvWriter(Writer writer, CsvWriteConfig config) {
        this.writer = (writer instanceof BufferedWriter) ? writer : new BufferedWriter(writer);
        this.config = (config != null ? config : CsvWriteConfig.defaultConfig());
    }

    private static Writer openWriter(File file, Charset charset, boolean isAppend) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, isAppend), charset));
        } catch (IOException e) {
            throw new RuntimeException("创建 CSV 写出器失败: " + file, e);
        }
    }

    /**
     * 将多行写出到 Writer。
     *
     * @param lines 多行数据
     * @return this
     */
    public CsvWriter write(String[]... lines) {
        if (lines != null) {
            for (String[] line : lines) {
                appendLine(line);
            }
            flush();
        }
        return this;
    }

    /**
     * 写出一行。
     *
     * @param fields 字段列表，{@code null} 值会被作为空值写出
     * @return this
     */
    public CsvWriter writeLine(String... fields) {
        if (fields == null || fields.length == 0) {
            return writeLine();
        }
        appendLine(fields);
        return this;
    }

    /**
     * 追加新行（换行）。
     *
     * @return this
     */
    public CsvWriter writeLine() {
        try {
            writer.write(lineDelimiter());
        } catch (IOException e) {
            throw new RuntimeException("写出 CSV 失败", e);
        }
        newline = true;
        return this;
    }

    @Override
    public void close() {
        if (endingLineBreak()) {
            writeLine();
        }
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("关闭 CSV 写出器失败", e);
        }
    }

    @Override
    public void flush() {
        try {
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("写出 CSV 失败", e);
        }
    }

    private void appendLine(String... fields) {
        try {
            doAppendLine(fields);
        } catch (IOException e) {
            throw new RuntimeException("写出 CSV 失败", e);
        }
    }

    private void doAppendLine(String... fields) throws IOException {
        if (fields == null) {
            return;
        }
        if (isFirstLine) {
            isFirstLine = false;
        } else {
            writer.write(lineDelimiter());
        }
        for (String field : fields) {
            appendField(field);
        }
        newline = true;
    }

    private void appendField(String value) throws IOException {
        boolean alwaysDelimitText = config.alwaysDelimitText;
        char textDelimiter = DEFAULT_TEXT_DELIMITER;
        char fieldSeparator = DEFAULT_FIELD_SEPARATOR;

        if (!newline) {
            writer.write(fieldSeparator);
        } else {
            newline = false;
        }

        if (value == null) {
            if (alwaysDelimitText) {
                writer.write(new char[]{textDelimiter, textDelimiter});
            }
            return;
        }

        char[] valueChars = value.toCharArray();
        boolean needsTextDelimiter = alwaysDelimitText;
        boolean containsTextDelimiter = false;
        for (char c : valueChars) {
            if (c == textDelimiter) {
                containsTextDelimiter = true;
                needsTextDelimiter = true;
                break;
            } else if (c == fieldSeparator || c == '\n' || c == '\r') {
                needsTextDelimiter = true;
            }
        }

        if (needsTextDelimiter) {
            writer.write(textDelimiter);
        }
        if (containsTextDelimiter) {
            for (char c : valueChars) {
                if (c == textDelimiter) {
                    writer.write(textDelimiter);
                }
                writer.write(c);
            }
        } else {
            writer.write(valueChars);
        }
        if (needsTextDelimiter) {
            writer.write(textDelimiter);
        }
    }

    private char[] lineDelimiter() {
        return DEFAULT_LINE_DELIMITER;
    }

    private boolean endingLineBreak() {
        return false;
    }
}
