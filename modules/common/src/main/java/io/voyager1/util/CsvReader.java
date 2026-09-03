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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Closeable;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV 读取器 {@code io.voyager1.util.CsvReader}。
 * <p>手写简单 CSV 解析器，支持引号内换行、转义引号与注释行。</p>
 */
public class CsvReader implements Closeable {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Reader reader;
    private final CsvReadConfig config;

    public CsvReader() {
        this(null, null);
    }

    public CsvReader(CsvReadConfig config) {
        this(null, config);
    }

    public CsvReader(Reader reader, CsvReadConfig config) {
        this.reader = reader;
        this.config = (config != null ? config : CsvReadConfig.defaultConfig());
    }

    /**
     * 读取 CSV 数据，此方法不会关闭 Reader。
     *
     * @return {@link CsvData}
     */
    public CsvData read() {
        return read(this.reader, false);
    }

    /**
     * 从 Reader 中读取 CSV 数据，读取后关闭 Reader。
     *
     * @param reader Reader
     * @return {@link CsvData}
     */
    public CsvData read(Reader reader) {
        return read(reader, true);
    }

    /**
     * 从 Reader 中读取 CSV 数据。
     *
     * @param reader Reader
     * @param close  读取结束是否关闭 Reader
     * @return {@link CsvData}
     */
    public CsvData read(Reader reader, boolean close) {
        try {
            Parsed parsed = parse(reader, config);
            List<CsvRow> rows = new ArrayList<>(parsed.rows.size());
            for (Row row : parsed.rows) {
                rows.add(new CsvRow(row.lineNo, parsed.headerMap, row.fields));
            }
            List<String> header = config.headerLineNo > -1 ? parsed.header : null;
            return new CsvData(header, rows);
        } finally {
            if (close) {
                closeQuietly(reader);
            }
        }
    }

    /**
     * 从 Reader 中读取 CSV 数据并转换为 Bean 列表，首行作为标题行。
     *
     * @param reader Reader
     * @param clazz  Bean 类型
     * @param <T>    Bean 类型
     * @return Bean 列表
     */
    public <T> List<T> read(Reader reader, Class<T> clazz) {
        // 此方法必须包含标题
        config.headerLineNo = config.beginLineNo;
        try {
            Parsed parsed = parse(reader, config);
            List<T> result = new ArrayList<>(parsed.rows.size());
            for (Row row : parsed.rows) {
                Map<String, String> fieldMap = new LinkedHashMap<>();
                if (parsed.headerMap != null) {
                    for (Map.Entry<String, Integer> entry : parsed.headerMap.entrySet()) {
                        fieldMap.put(entry.getKey(), row.get(entry.getValue()));
                    }
                }
                result.add(MAPPER.convertValue(fieldMap, clazz));
            }
            return result;
        } finally {
            closeQuietly(reader);
        }
    }

    @Override
    public void close() {
        closeQuietly(this.reader);
    }

    private static void closeQuietly(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignore) {
                // 忽略
            }
        }
    }

    //--------------------------------------------------------------------------------------------- 解析

    private static Parsed parse(Reader reader, CsvReadConfig config) {
        if (reader == null) {
            throw new IllegalArgumentException("Reader must be not null!");
        }
        char fieldSeparator = config.fieldSeparator;
        char textDelimiter = config.textDelimiter;
        Character commentCharacter = config.commentCharacter;

        PushbackReader in = new PushbackReader(reader, 1);
        List<Row> rows = new ArrayList<>();
        Map<String, Integer> headerMap = null;
        List<String> header = null;

        long lineNo = -1;
        int preChar = -1;
        boolean inQuotes = false;
        boolean finished = false;
        int firstLineFieldCount = -1;

        try {
            while (!finished) {
                List<String> currentFields = new ArrayList<>();
                StringBuilder currentField = new StringBuilder();
                boolean inComment = false;
                int c;
                while (true) {
                    c = in.read();
                    if (c < 0) {
                        if (currentField.length() > 0 || preChar == fieldSeparator) {
                            if (inQuotes) {
                                currentField.append(textDelimiter);
                            }
                            addField(currentFields, currentField.toString(), textDelimiter, config.trimField);
                            currentField.setLength(0);
                        }
                        finished = true;
                        break;
                    }

                    // 注释行处理
                    if ((preChar < 0 || preChar == '\r' || preChar == '\n') && !inQuotes
                            && commentCharacter != null && c == commentCharacter) {
                        inComment = true;
                    }
                    if (inComment) {
                        if (c == '\r' || c == '\n') {
                            lineNo++;
                            inComment = false;
                        }
                        continue;
                    }

                    if (inQuotes) {
                        if (c == textDelimiter) {
                            int next = in.read();
                            if (next != textDelimiter) {
                                inQuotes = false;
                                if (next >= 0) {
                                    in.unread(next);
                                }
                            }
                        }
                        currentField.append((char) c);
                    } else {
                        if (c == fieldSeparator) {
                            addField(currentFields, currentField.toString(), textDelimiter, config.trimField);
                            currentField.setLength(0);
                        } else if (c == textDelimiter && isFieldBegin(preChar, fieldSeparator)) {
                            inQuotes = true;
                            currentField.append((char) c);
                        } else if (c == '\r') {
                            addField(currentFields, currentField.toString(), textDelimiter, config.trimField);
                            currentField.setLength(0);
                            preChar = c;
                            break;
                        } else if (c == '\n') {
                            if (preChar != '\r') {
                                addField(currentFields, currentField.toString(), textDelimiter, config.trimField);
                                currentField.setLength(0);
                                preChar = c;
                                break;
                            }
                        } else {
                            currentField.append((char) c);
                        }
                    }
                    preChar = c;
                }
                lineNo++;

                if (finished && currentFields.isEmpty()) {
                    break;
                }
                if (lineNo < config.beginLineNo) {
                    continue;
                }
                if (lineNo > config.endLineNo) {
                    break;
                }
                if (config.skipEmptyRows && currentFields.size() == 1 && currentFields.get(0).isEmpty()) {
                    continue;
                }
                if (config.errorOnDifferentFieldCount) {
                    if (firstLineFieldCount < 0) {
                        firstLineFieldCount = currentFields.size();
                    } else if (currentFields.size() != firstLineFieldCount) {
                        throw new RuntimeException("CSV 行字段数不一致: line " + lineNo);
                    }
                }
                if (lineNo == config.headerLineNo && headerMap == null) {
                    headerMap = new LinkedHashMap<>();
                    header = new ArrayList<>();
                    for (int i = 0; i < currentFields.size(); i++) {
                        String field = currentFields.get(i);
                        if (field != null && !field.isEmpty() && !headerMap.containsKey(field)) {
                            headerMap.put(field, i);
                        }
                        header.add(field);
                    }
                    continue;
                }
                rows.add(new Row(lineNo, new ArrayList<>(currentFields)));
            }
        } catch (IOException e) {
            throw new RuntimeException("解析 CSV 失败", e);
        }
        return new Parsed(header, headerMap, rows);
    }

    private static boolean isFieldBegin(int preChar, char fieldSeparator) {
        return preChar == -1 || preChar == fieldSeparator || preChar == '\n' || preChar == '\r';
    }

    private static void addField(List<String> fields, String field, char textDelimiter, boolean trimField) {
        String f = field;
        if (f.length() > 0 && (f.charAt(0) == '\n' || f.charAt(0) == '\r')) {
            f = f.substring(1);
        }
        if (f.length() > 0 && (f.charAt(f.length() - 1) == '\n' || f.charAt(f.length() - 1) == '\r')) {
            f = f.substring(0, f.length() - 1);
        }
        if (f.length() >= 2 && f.charAt(0) == textDelimiter && f.charAt(f.length() - 1) == textDelimiter) {
            f = f.substring(1, f.length() - 1);
        }
        if (trimField) {
            f = f.trim();
        }
        fields.add(f);
    }

    private static final class Row {
        final long lineNo;
        final List<String> fields;

        Row(long lineNo, List<String> fields) {
            this.lineNo = lineNo;
            this.fields = fields;
        }

        String get(int index) {
            if (index < 0 || index >= fields.size()) {
                return null;
            }
            return fields.get(index);
        }
    }

    private static final class Parsed {
        final List<String> header;
        final Map<String, Integer> headerMap;
        final List<Row> rows;

        Parsed(List<String> header, Map<String, Integer> headerMap, List<Row> rows) {
            this.header = header;
            this.headerMap = headerMap;
            this.rows = rows;
        }
    }
}
