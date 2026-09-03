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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * CSV 写出配置项 {@code .core.text.csv.CsvWriteConfig}。
 */
public class CsvWriteConfig {

    protected char fieldSeparator = ',';
    protected char textDelimiter = '"';
    protected char[] lineDelimiter = {'\r', '\n'};
    protected boolean alwaysDelimitText = false;
    protected Charset charset = StandardCharsets.UTF_8;

    public static CsvWriteConfig defaultConfig() {
        return new CsvWriteConfig();
    }

    public CsvWriteConfig setAlwaysDelimitText(boolean alwaysDelimitText) {
        this.alwaysDelimitText = alwaysDelimitText;
        return this;
    }

    public CsvWriteConfig setFieldSeparator(char fieldSeparator) {
        this.fieldSeparator = fieldSeparator;
        return this;
    }

    public CsvWriteConfig setTextDelimiter(char textDelimiter) {
        this.textDelimiter = textDelimiter;
        return this;
    }

    public CsvWriteConfig setLineDelimiter(char[] lineDelimiter) {
        this.lineDelimiter = lineDelimiter;
        return this;
    }

    public CsvWriteConfig setCharset(Charset charset) {
        this.charset = charset;
        return this;
    }
}
