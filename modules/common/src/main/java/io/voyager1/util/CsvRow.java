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

import java.util.List;
import java.util.Map;

/**
 * CSV 行对象
 */
public class CsvRow {

    private final long originalLineNumber;
    private final Map<String, Integer> headerMap;
    private final List<String> fields;

    public CsvRow(long originalLineNumber, Map<String, Integer> headerMap, List<String> fields) {
        this.originalLineNumber = originalLineNumber;
        this.headerMap = headerMap;
        this.fields = fields;
    }

    public String getByName(String name) {
        Integer index = headerMap == null ? null : headerMap.get(name);
        if (index == null || index < 0 || index >= fields.size()) {
            return null;
        }
        return fields.get(index);
    }

    public String get(int index) {
        return fields.get(index);
    }

    public List<String> getRawList() {
        return fields;
    }

    public long getOriginalLineNumber() {
        return originalLineNumber;
    }

    public Map<String, Integer> getFieldMap() {
        return headerMap;
    }
}
