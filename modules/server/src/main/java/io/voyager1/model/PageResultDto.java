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

package io.voyager1.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;

/**
 * 分页查询结果对象
 *
 * @since 2021/12/3
 */
@Data
public class PageResultDto<T> implements Serializable {

    /**
     * 结果
     */
    private List<T> result;
    /**
     * 页码
     */
    private Integer page;
    /**
     * 每页结果数
     */
    private Integer pageSize;
    /**
     * 总页数
     */
    private Integer totalPage;
    /**
     * 总数
     */
    private Integer total;

    public PageResultDto(int page, int pageSize, int total) {
        this.setPage(page);
        this.setPageSize(pageSize);
        this.setTotalPage(totalPage(total, pageSize));
        this.setTotal(total);
    }

    public void each(Consumer<T> consumer) {
        if (result == null) {
            return;
        }
        result.forEach(consumer);
    }

    public boolean isEmpty() {
        return getResult() == null || getResult().isEmpty();
    }

    public T get(int index) {
        if (getResult() == null) {
            return null;
        }
        return getResult().get(index);
    }

    /**
     * 计算总页数
     */
    public static int totalPage(int total, int pageSize) {
        if (pageSize <= 0) {
            return 0;
        }
        return (total + pageSize - 1) / pageSize;
    }
}
