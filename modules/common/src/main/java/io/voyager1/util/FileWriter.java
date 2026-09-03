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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 文件写入器，"" {@code io.voyager1.util.FileWriter}。
 */
public class FileWriter {

    private final File file;
    private final Charset charset;

    /**
     * 创建 FileWriter。
     *
     * @param file    文件
     * @param charset 编码
     * @return FileWriter
     */
    public static FileWriter create(File file, Charset charset) {
        return new FileWriter(file, charset);
    }

    /**
     * 创建 FileWriter，编码 UTF-8。
     *
     * @param file 文件
     * @return FileWriter
     */
    public static FileWriter create(File file) {
        return new FileWriter(file, StandardCharsets.UTF_8);
    }

    public FileWriter(File file, Charset charset) {
        if (file == null) {
            throw new IllegalArgumentException("File to write content is null !");
        }
        if (file.exists() && !file.isFile()) {
            throw new IllegalArgumentException("File [" + file.getAbsolutePath() + "] is not a file !");
        }
        this.file = file;
        this.charset = (charset != null ? charset : StandardCharsets.UTF_8);
    }

    public File getFile() {
        return file;
    }

    /**
     * 获得带缓存的写入对象。
     *
     * @param isAppend 是否追加
     * @return BufferedWriter
     */
    public BufferedWriter getWriter(boolean isAppend) {
        try {
            touch();
            return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, isAppend), charset));
        } catch (IOException e) {
            throw new RuntimeException("获取写入器失败: " + file, e);
        }
    }

    /**
     * 获得打印写入对象。
     *
     * @param isAppend 是否追加
     * @return PrintWriter
     */
    public PrintWriter getPrintWriter(boolean isAppend) {
        return new PrintWriter(getWriter(isAppend));
    }

    /**
     * 将字符串写入文件（覆盖）。
     *
     * @param content 内容
     * @return 目标文件
     */
    public File write(String content) {
        return write(content, false);
    }

    /**
     * 将字符串写入文件。
     *
     * @param content  内容
     * @param isAppend 是否追加
     * @return 目标文件
     */
    public File write(String content, boolean isAppend) {
        BufferedWriter writer = null;
        try {
            writer = getWriter(isAppend);
            writer.write(content == null ? "" : content);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("写入文件失败: " + file, e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
        return file;
    }

    /**
     * 将字符串追加写入文件。
     *
     * @param content 内容
     * @return 目标文件
     */
    public File append(String content) {
        return write(content, true);
    }

    private void touch() throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
    }
}
