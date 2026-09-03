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

import io.voyager1.util.CollStreamUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.JarClassLoader;

import io.voyager1.util.ClassUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.plugin.IPlugin;
import io.voyager1.plugin.PluginConfig;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.Voyager1Manifest;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.system.ExtConfigBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;

import java.io.File;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * 插件工厂
 *
 * @since 2019/8/13
 */
@Slf4j
public class PluginFactory implements ApplicationContextInitializer<ConfigurableApplicationContext>, ApplicationListener<ApplicationEvent> {

    //    private static final List<FeatureCallback> FEATURE_CALLBACKS = new ArrayList<>();
    private static final Map<String, List<PluginItemWrap>> PLUGIN_MAP = new java.util.concurrent.ConcurrentHashMap<>();

//    /**
//     * 添加回调事件
//     *
//     * @param featureCallback 回调
//     */
//    public static void addFeatureCallback(FeatureCallback featureCallback) {
//        FEATURE_CALLBACKS.add(featureCallback);
//    }
//
//    public static List<FeatureCallback> getFeatureCallbacks() {
//        return FEATURE_CALLBACKS;
//    }

    /**
     * 获取插件端
     *
     * @param name 插件名
     * @return 插件对象
     */
    public static IPlugin getPlugin(String name) {
        List<PluginItemWrap> pluginItemWraps = PLUGIN_MAP.get(name);
        PluginItemWrap first = (pluginItemWraps == null || pluginItemWraps.isEmpty() ? null : pluginItemWraps.get(0));
        Assert.notNull(first, "对应找到对应到插件：" + name);
        return first.getPlugin();
    }

    /**
     * 判断是否包含某个插件
     *
     * @param name 插件名
     * @return true 包含
     */
    public static boolean contains(String name) {
        return PLUGIN_MAP.containsKey(name);
    }

    /**
     * 插件数量
     *
     * @return 当前加载的插件数量
     */
    public static int size() {
        return PLUGIN_MAP.size();
    }

    /**
     * 正式环境添加依赖
     */
    private static void init() {
        File runPath = Voyager1Manifest.getRunPath().getParentFile();
        File plugin = FileUtil.file(runPath, "plugin");
        if (!plugin.exists() || plugin.isFile()) {
            return;
        }
        // 加载二级插件包
        File[] dirFiles = plugin.listFiles(File::isDirectory);
        if (dirFiles != null) {
            for (File file : dirFiles) {
                File lib = FileUtil.file(file, "lib");
                if (!lib.exists() || lib.isFile()) {
                    continue;
                }
                File[] listFiles = lib.listFiles((dir, name) -> (name != null && (name != null && name.toLowerCase().endsWith(FileUtil.JAR_FILE_EXT.toLowerCase()))));
                if (listFiles == null || listFiles.length == 0) {
                    continue;
                }
                addPlugin(file.getName(), lib);
            }
        }
        // 加载一级独立插件端包
        File[] files = plugin.listFiles(pathname -> FileUtil.isFile(pathname) && FileUtil.JAR_FILE_EXT.equalsIgnoreCase(FileUtil.extName(pathname)));
        if (files != null) {
            for (File file : files) {
                addPlugin(file.getName(), file);
            }
        }
    }

    private static void addPlugin(String pluginName, File file) {
        log.info("加载：{} 插件", pluginName);
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        JarClassLoader.loadJar((URLClassLoader) contextClassLoader, file);
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        //init();
        // 扫描插件 实现
        Set<Class<?>> classes = ClassUtil.scanPackage("io.voyager1", IPlugin.class::isAssignableFrom);
        List<PluginItemWrap> pluginItemWraps = classes
            .stream()
            .filter(aClass -> ClassUtil.isNormalClass(aClass) && aClass.isAnnotationPresent(PluginConfig.class))
            .map(aClass -> new PluginItemWrap((Class<? extends IPlugin>) aClass))
            .filter(pluginItemWrap -> {
                if ((pluginItemWrap.getName() == null || pluginItemWrap.getName().isEmpty())) {
                    log.warn("plugin config name error:{}", pluginItemWrap.getClassName());
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
        //
        Map<String, List<PluginItemWrap>> pluginMap = CollStreamUtil.groupByKey(pluginItemWraps, PluginItemWrap::getName);
        pluginMap.forEach((key, value) -> {
            // 排序
            value.sort((o1, o2) -> Comparator.comparingInt((ToIntFunction<PluginItemWrap>) value1 -> {
                Order order = value1.getClassName().getAnnotation(Order.class);
                if (order == null) {
                    return 0;
                }
                return order.value();
            }).compare(o1, o2));
            PLUGIN_MAP.put(key, value);
        });
        log.debug("load plugin count:{}", pluginMap.keySet().size());
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
//         <ContextClosedEvent>, ApplicationListener<ApplicationReadyEvent>
        if (event instanceof ContextClosedEvent) {
            Collection<List<PluginItemWrap>> values = PLUGIN_MAP.values();
            for (List<PluginItemWrap> value : values) {
                for (PluginItemWrap pluginItemWrap : value) {
                    IPlugin plugin = pluginItemWrap.getPlugin();
                    IoUtil.close(plugin);
                }
            }
        } else if (event instanceof ApplicationReadyEvent) {
            System.setProperty(IPlugin.DATE_PATH_KEY, ExtConfigBean.getPath());
            System.setProperty(IPlugin.VOYAGER1_VERSION_KEY, Voyager1Manifest.getInstance().getVersion());
        }

    }
}
