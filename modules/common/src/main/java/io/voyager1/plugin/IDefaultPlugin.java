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

package io.voyager1.plugin;

import io.voyager1.plugin.IPlugin;
import io.voyager1.system.ExtConfigBean;

import java.io.InputStream;

/**
 * 插件模块接口
 *
 * @since 2021/12/22
 */
public interface IDefaultPlugin extends IPlugin, AutoCloseable {

    /**
     * 获取配置文件流
     *
     * @param name 配置文件名称
     * @return InputStream
     */
    default InputStream getConfigResourceInputStream(String name) {
        return ExtConfigBean.tryGetConfigResourceInputStream(name);
    }
}
