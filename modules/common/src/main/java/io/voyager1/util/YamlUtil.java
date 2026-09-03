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

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.Map;

/**
 * {@code io.voyager1.util.YamlUtil} 的兼容实现。
 *
 * <p>基于 SnakeYAML，仅覆盖代码库实际使用到的 API。</p>
 */
public final class YamlUtil {

    private YamlUtil() {
    }

    /**
     * 从流中加载 YAML 并转换为指定类型。
     */
    public static <T> T load(InputStream inputStream, Class<T> cls) {
        LoaderOptions loaderOptions = new LoaderOptions();
        Constructor constructor = new Constructor(cls, loaderOptions);
        Yaml yaml = new Yaml(constructor);
        return yaml.load(inputStream);
    }

    /**
     * 从字符串加载 YAML 并转换为指定类型。
     */
    public static <T> T load(String content, Class<T> cls) {
        LoaderOptions loaderOptions = new LoaderOptions();
        Constructor constructor = new Constructor(cls, loaderOptions);
        Yaml yaml = new Yaml(constructor);
        return yaml.load(content);
    }

    /**
     * 加载为 Map。
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> load(InputStream inputStream) {
        Yaml yaml = new Yaml();
        return yaml.load(inputStream);
    }

    /**
     * 别名，与 {@link #load(String, Class)} 等价。
     */
    public static <T> T loadAs(String content, Class<T> cls) {
        return load(content, cls);
    }

    /**
     * 将对象序列化为 YAML 字符串。
     */
    public static String dump(Object obj) {
        Yaml yaml = new Yaml();
        return yaml.dump(obj);
    }
}
