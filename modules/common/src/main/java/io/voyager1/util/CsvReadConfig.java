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

import java.io.Serializable;

/**
 * CSV 读取配置项，"" {@code io.voyager1.util.CsvReadConfig}。
 */
public class CsvReadConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 字段分隔符，默认逗号 ',' */
    protected char fieldSeparator = ',';
    /** 文本包装符，默认双引号 '"' */
    protected char textDelimiter = '"';
    /** 注释符号，默认 '#' */
    protected Character commentCharacter = '#';
    /** 指定标题行号，-1 表示无标题行 */
    protected long headerLineNo = -1;
    /** 是否跳过空白行，默认 true */
    protected boolean skipEmptyRows = true;
    /** 每行字段个数不同时是否抛出异常，默认 false */
    protected boolean errorOnDifferentFieldCount;
    /** 开始的行（包括），此处为原始文件行号 */
    protected long beginLineNo;
    /** 结束的行（包括），此处为原始文件行号 */
    protected long endLineNo = Long.MAX_VALUE - 1;
    /** 每个字段是否去除两边空白符 */
    protected boolean trimField;

    /**
     * 默认配置。
     *
     * @return 默认配置
     */
    public static CsvReadConfig defaultConfig() {
        return new CsvReadConfig();
    }

    public CsvReadConfig setFieldSeparator(char fieldSeparator) {
        this.fieldSeparator = fieldSeparator;
        return this;
    }

    public CsvReadConfig setTextDelimiter(char textDelimiter) {
        this.textDelimiter = textDelimiter;
        return this;
    }

    public CsvReadConfig setCommentCharacter(Character commentCharacter) {
        this.commentCharacter = commentCharacter;
        return this;
    }

    /**
     * 设置是否首行作为标题行，默认 false。
     *
     * @param containsHeader 是否首行作为标题行
     * @return this
     */
    public CsvReadConfig setContainsHeader(boolean containsHeader) {
        return setHeaderLineNo(containsHeader ? beginLineNo : -1);
    }

    /**
     * 设置标题行行号，-1 表示无标题行。
     *
     * @param headerLineNo 标题行行号
     * @return this
     */
    public CsvReadConfig setHeaderLineNo(long headerLineNo) {
        this.headerLineNo = headerLineNo;
        return this;
    }

    public CsvReadConfig setSkipEmptyRows(boolean skipEmptyRows) {
        this.skipEmptyRows = skipEmptyRows;
        return this;
    }

    public CsvReadConfig setErrorOnDifferentFieldCount(boolean errorOnDifferentFieldCount) {
        this.errorOnDifferentFieldCount = errorOnDifferentFieldCount;
        return this;
    }

    public CsvReadConfig setBeginLineNo(long beginLineNo) {
        this.beginLineNo = beginLineNo;
        return this;
    }

    public CsvReadConfig setEndLineNo(long endLineNo) {
        this.endLineNo = endLineNo;
        return this;
    }

    public CsvReadConfig setTrimField(boolean trimField) {
        this.trimField = trimField;
        return this;
    }
}
