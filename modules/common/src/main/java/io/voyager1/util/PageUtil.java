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

/**
 * 分页工具，"" {@code .core.util.PageUtil}。
 */
public class PageUtil {

    public static final int DEFAULT_PAGE_SIZE = 10;

    public static int getFirstPageNo() {
        return 1;
    }

    public static int totalPage(int totalCount, int pageSize) {
        if (totalCount <= 0) {
            return 0;
        }
        if (pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        return (totalCount + pageSize - 1) / pageSize;
    }

    public static int getStart(int pageNo, int pageSize) {
        if (pageNo < getFirstPageNo()) {
            pageNo = getFirstPageNo();
        }
        return (pageNo - 1) * pageSize;
    }

    public static int getEnd(int pageNo, int pageSize) {
        return getStart(pageNo, pageSize) + pageSize;
    }
}
