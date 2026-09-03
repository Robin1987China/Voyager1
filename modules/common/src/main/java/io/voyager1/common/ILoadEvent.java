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

package io.voyager1.common;

import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;

/**
 * voyager1 加载事件
 * <p>
 * 保证在容器的 bean 加载完成之后
 *
 * @since 2022/12/25
 */
public interface ILoadEvent extends Ordered {

    /**
     * 初始化成功后执行
     *
     * @param applicationContext 应用上下文
     * @throws Exception 异常
     */
    void afterPropertiesSet(ApplicationContext applicationContext) throws Exception;

    /**
     * 排序只
     *
     * @return 0 是默认
     */
    @Override
    default int getOrder() {
        return 0;
    }
}
