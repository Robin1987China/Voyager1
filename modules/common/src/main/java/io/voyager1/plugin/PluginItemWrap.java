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

import io.voyager1.util.ReflectUtil;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.plugin.IPlugin;
import io.voyager1.plugin.PluginConfig;
import lombok.Getter;

/**
 * 插件端对象
 *
 * @since 2021/12/24
 */
@Getter
public class PluginItemWrap {

    /**
     * 配置相关
     */
    private final PluginConfig pluginConfig;

    /**
     * 插件名
     */
    private final String name;

    /**
     * 插件类名
     */
    private final Class<? extends IPlugin> className;

    /**
     * 插件对象
     */
    private volatile IPlugin plugin;

    public PluginItemWrap(Class<? extends IPlugin> className) {
        this.className = className;
        this.pluginConfig = className.getAnnotation(PluginConfig.class);
        this.name = this.pluginConfig.name();
    }

    public IPlugin getPlugin() {
        if (plugin == null) {
            synchronized (className) {
                if (plugin == null) {
                    //
                    boolean nativeObject = this.pluginConfig.nativeObject();
                    if (nativeObject) {
                        plugin = ReflectUtil.newInstance(className);
                    } else {
                        plugin = SpringContextHolder.getBean(className);
                    }
                }
            }
        }
        return plugin;
    }
}
