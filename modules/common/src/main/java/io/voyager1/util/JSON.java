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
 *  {@code io.voyager1.util.JSON} 接口的兼容实现。
 *
 * <p>作为 {@link JSONObject} 与 {@link JSONArray} 的公共父接口，
 * 供 {@link JSONUtil#parse(String)} 返回对象/数组二义类型。</p>
 */
public interface JSON {

    /**
     * 通过表达式获取指定路径下的值。
     *
     * @param expression 点分隔的路径表达式，如 {@code data.items}；空字符串表示根节点自身
     * @param resultType 目标类型
     * @param <T>        目标泛型
     * @return 转换后的值
     */
    <T> T getByPath(String expression, Class<T> resultType);
}
