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

/**
 * CSV 数据对象，"" {@code .core.text.csv.CsvData}。
 */
public class CsvData {

    private final List<String> header;
    private final List<CsvRow> rows;

    public CsvData(List<String> header, List<CsvRow> rows) {
        this.header = header;
        this.rows = rows;
    }

    public List<String> getHeader() {
        return header;
    }

    public List<CsvRow> getRows() {
        return rows;
    }

    public CsvRow getRow(int index) {
        return rows.get(index);
    }

    public int getRowCount() {
        return rows.size();
    }
}
