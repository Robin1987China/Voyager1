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

package io.voyager1.transport;

import com.alibaba.fastjson2.TypeReference;

/**
 * 消息转换服务
 *
 * @since 2022/12/24
 */
public interface TransformServer {

    /**
     * 数据类型转换
     *
     * @param data           数据
     * @param tTypeReference 类型
     * @param <T>            范型
     * @return data
     */
    <T> T transform(String data, TypeReference<T> tTypeReference);

    /**
     * 数据类型转换,只返回成功的数据
     *
     * @param data   数据
     * @param tClass 类型
     * @param <T>    范型
     * @return data
     */
    <T> T transformOnlyData(String data, Class<T> tClass);

    /**
     * 转换异常
     *
     * @param e        请求的异常
     * @param nodeInfo 节点信息
     * @return 转换后的异常
     */
    default Exception transformException(Exception e, INodeInfo nodeInfo) {
        return e;
    }
}
