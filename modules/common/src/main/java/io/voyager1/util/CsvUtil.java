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

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * CSV 工具类 {@code io.voyager1.util.CsvUtil}。
 */
public class CsvUtil {

    private CsvUtil() {
    }

    //----------------------------------------------------------------------------------------------------------- Reader

    /**
     * 获取 CSV 读取器，须自行指定读取资源。
     *
     * @param config 配置，允许为空
     * @return {@link CsvReader}
     */
    public static CsvReader getReader(CsvReadConfig config) {
        return new CsvReader(config);
    }

    /**
     * 获取 CSV 读取器（默认配置）。
     *
     * @return {@link CsvReader}
     */
    public static CsvReader getReader() {
        return new CsvReader();
    }

    /**
     * 获取 CSV 读取器。
     *
     * @param reader Reader
     * @param config 配置，允许为空
     * @return {@link CsvReader}
     */
    public static CsvReader getReader(Reader reader, CsvReadConfig config) {
        return new CsvReader(reader, config);
    }

    /**
     * 获取 CSV 读取器。
     *
     * @param reader Reader
     * @return {@link CsvReader}
     */
    public static CsvReader getReader(Reader reader) {
        return getReader(reader, null);
    }

    //----------------------------------------------------------------------------------------------------------- Writer

    /**
     * 获取 CSV 写出器。
     *
     * @param filePath CSV 文件路径
     * @param charset  编码
     * @return {@link CsvWriter}
     */
    public static CsvWriter getWriter(String filePath, Charset charset) {
        return new CsvWriter(filePath, charset);
    }

    /**
     * 获取 CSV 写出器。
     *
     * @param file    CSV 文件
     * @param charset 编码
     * @return {@link CsvWriter}
     */
    public static CsvWriter getWriter(File file, Charset charset) {
        return new CsvWriter(file, charset);
    }

    /**
     * 获取 CSV 写出器。
     *
     * @param writer Writer
     * @return {@link CsvWriter}
     */
    public static CsvWriter getWriter(Writer writer) {
        return new CsvWriter(writer);
    }

    /**
     * 获取 CSV 写出器。
     *
     * @param writer Writer
     * @param config 写出配置，null 则使用默认配置
     * @return {@link CsvWriter}
     */
    public static CsvWriter getWriter(Writer writer, CsvWriteConfig config) {
        return new CsvWriter(writer, config);
    }
}
