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

import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.Const;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 环境变量管理
 *
 * @since 2023/2/11
 */
public class EnvironmentMapBuilder {


    private final Map<String, Item> map;

    public EnvironmentMapBuilder(int initialCapacity) {
        map = new LinkedHashMap<>(initialCapacity);
    }

    public static EnvironmentMapBuilder builder(Map<String, Item> map) {
        EnvironmentMapBuilder environmentMapBuilder = new EnvironmentMapBuilder(map.size() + 10);
        environmentMapBuilder.put(map);
        return environmentMapBuilder;
    }

    public EnvironmentMapBuilder put(String name, String value) {
        map.put(name, new Item(value, false, false));
        return this;
    }

    /**
     * 系统变量
     *
     * @param name  变量名
     * @param value 值
     * @return this
     */
    public EnvironmentMapBuilder putSystem(String name, String value) {
        boolean privacy = StrUtil.containsAnyIgnoreCase(name, Const.PRIVACY_VARIABLE_KEYWORDS);
        map.put(name, new Item(value, privacy, true));
        return this;
    }

    public EnvironmentMapBuilder put(Map<String, Item> map) {
        if (map != null) {
            this.map.putAll(map);
        }
        return this;
    }

    public EnvironmentMapBuilder putStr(Map<String, String> map) {
        Optional.ofNullable(map).ifPresent(stringMap -> {
            for (Map.Entry<String, String> entry : stringMap.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        });
        return this;
    }

    public EnvironmentMapBuilder putObjectArray(Object... parametersEnv) {
        for (int i = 0; i < parametersEnv.length; i += 2) {
            this.put(String.valueOf(parametersEnv[i]), String.valueOf(parametersEnv[i + 1]));
        }
        return this;
    }

    public Map<String, String> environment() {
        return this.environment(null);
    }

    public Map<String, String> environment(Map<String, Object> appendMap) {
        Map<String, String> map = new LinkedHashMap<>(this.map.size());
        for (Map.Entry<String, Item> entry : this.map.entrySet()) {
            Item entryValue = entry.getValue();
            if (entryValue.value == null || entry.getKey() == null) {
                // 值不能为 null
                continue;
            }
            map.put(entry.getKey(), entryValue.value);
        }
        Optional.ofNullable(appendMap).ifPresent(objectMap -> {
            for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
                Object value = entry.getValue();
                if (value == null || entry.getKey() == null) {
                    continue;
                }
                map.put(entry.getKey(), (value == null ? null : value.toString()));
            }
        });
        return map;
    }

    public void eachStr(Consumer<String> consumer) {
        this.eachStr(consumer, null);
    }

    /**
     * 输出环境变量信息
     *
     * @param consumer  回调
     * @param appendMap 附加的环境变量
     */
    public void eachStr(Consumer<String> consumer, Map<String, Object> appendMap) {
        int allSize = (this.map == null ? 0 : this.map.size()) + (appendMap == null ? 0 : appendMap.size());
        if (allSize <= 0) {
            return;
        }
        consumer.accept("##################################################################################");
        for (Map.Entry<String, Item> entry : map.entrySet()) {
            Item entryValue = entry.getValue();
            String value = entryValue.privacy ? Const.PRIVACY_PLACEHOLDER : entryValue.value;
            consumer.accept(entry.getKey() + "=" + value);
        }
        Optional.ofNullable(appendMap).ifPresent(objectMap -> {
            for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
                consumer.accept(entry.getKey() + "=" + StrUtil.toString(entry.getValue()));
            }
        });
        consumer.accept("##################################################################################");
    }

    /**
     * 获取环境变量的执行
     *
     * @param key 变量名
     * @return 值
     */
    public String get(String key) {
        Item item = map.get(key);
        if (item != null) {
            return item.value;
        }
        return null;
    }

    /**
     * 获取环境变量的执行
     *
     * @param key 变量名
     * @return 值
     */
    public boolean getBool(String key, boolean defaultValue) {
        String value = this.get(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * 将对象转 json
     *
     * @return 原始对象
     */
    public String toDataJsonStr() {
        return JSONObject.toJSONString(map);
    }

    /**
     * 将对象转隐私 json
     *
     * @return 隐私变量自动隐藏对象
     */
    public String toPrivacyDataJsonStr() {
        Map<String, Item> clone = clonePrivacyData();
        return JSONObject.toJSONString(clone);
    }

    /**
     * 将对象转隐私 json
     *
     * @return 隐私变量自动隐藏对象
     */
    public Map<String, Item> clonePrivacyData() {
        Map<String, Item> clone = ObjectUtil.clone(map);
        clone.forEach((k, v) -> {
            if (v.privacy) {
                v.value = Const.PRIVACY_PLACEHOLDER;
            }
        });
        return clone;
    }

    public JSONObject toDataJson() {
        return JSONObject.from(map);
    }

    @AllArgsConstructor
    @Data
    public static class Item {
        /**
         * 值
         */
        private String value;
        /**
         * 隐私
         */
        private boolean privacy;
        /**
         * 是否为系统变量
         */
        private boolean system;
    }
}
