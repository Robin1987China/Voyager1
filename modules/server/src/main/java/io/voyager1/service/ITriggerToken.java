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

package io.voyager1.service;

import io.voyager1.model.BaseIdModel;

/**
 * 带有触发器 token 相关实现服务
 *
 * @since 2022/7/22
 */
public interface ITriggerToken {

    /**
     * 类型 名称
     *
     * @return 数据分类名称
     */
    String typeName();

    /**
     * 数据描述
     *
     * @return 描述
     */
    String getDataDesc();

    /**
     * 判断是否存在
     *
     * @param dataId 数据id
     * @return true 存在
     */
    boolean exists(String dataId);

    BaseIdModel getByKey(String keyValue);
}
