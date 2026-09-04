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

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *  {@code io.voyager1.util.JSONObject} 的兼容实现。
 *
 * <p>可变的 {@link ObjectNode} 包装器，仅覆盖代码库实际使用到的 API。</p>
 */
public class JSONObject implements JSON {

    private final ObjectNode node;

    public JSONObject() {
        this.node = JSONUtil.MAPPER.createObjectNode();
    }

    JSONObject(ObjectNode node) {
        this.node = node;
    }

    /**
     * Jackson 序列化入口：直接输出内部 ObjectNode 内容，
     * 避免把本包装类当作普通 POJO（会多出 node/empty 等字段）。
     */
    @JsonValue
    ObjectNode node() {
        return node;
    }

    // ------------------------------------------------------------------ 写入

    public JSONObject set(String key, Object value) {
        node.set(key, JSONUtil.toNode(value));
        return this;
    }

    public JSONObject put(String key, Object value) {
        node.set(key, JSONUtil.toNode(value));
        return this;
    }

    public JSONObject putOpt(String key, Object value) {
        if (value != null) {
            node.set(key, JSONUtil.toNode(value));
        }
        return this;
    }

    // ------------------------------------------------------------------ 读取

    public Object get(String key) {
        return JSONUtil.toValue(node.get(key));
    }

    public String getStr(String key) {
        JsonNode n = node.get(key);
        if (n == null || n.isNull()) {
            return null;
        }
        return n.isTextual() ? n.textValue() : n.toString();
    }

    public String getString(String key) {
        return getStr(key);
    }

    public java.util.Map<String, Object> toMap() {
        return JSONUtil.MAPPER.convertValue(node, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
    }

    public Integer getInteger(String key) {
        JsonNode n = node.get(key);
        return (n == null || n.isNull()) ? null : n.asInt();
    }

    public int getIntValue(String key) {
        return node.path(key).asInt();
    }

    public Long getLong(String key) {
        JsonNode n = node.get(key);
        return (n == null || n.isNull()) ? null : n.asLong();
    }

    public Boolean getBoolean(String key) {
        JsonNode n = node.get(key);
        return (n == null || n.isNull()) ? null : n.asBoolean();
    }

    public JSONObject getJSONObject(String key) {
        JsonNode n = node.get(key);
        return (n != null && n.isObject()) ? new JSONObject((ObjectNode) n) : null;
    }

    public JSONArray getJSONArray(String key) {
        JsonNode n = node.get(key);
        return (n != null && n.isArray()) ? new JSONArray((com.fasterxml.jackson.databind.node.ArrayNode) n) : null;
    }

    // ------------------------------------------------------------------ 结构

    public boolean containsKey(String key) {
        return node.has(key);
    }

    public boolean isEmpty() {
        return node.isEmpty();
    }

    public int size() {
        return node.size();
    }

    public Set<String> keySet() {
        Set<String> keys = new LinkedHashSet<>();
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            keys.add(it.next());
        }
        return keys;
    }

    // ------------------------------------------------------------------ 路径

    @Override
    public <T> T getByPath(String expression, Class<T> resultType) {
        return JSONUtil.getByPath(node, expression, resultType);
    }

    // ------------------------------------------------------------------ 序列化

    @Override
    public String toString() {
        return node.toString();
    }

    // ------------------------------------------------------------------ 静态

    public static JSONObject parseObject(String jsonStr) {
        return JSONUtil.parseObj(jsonStr);
    }

    public static JSONObject parse(String jsonStr) {
        return JSONUtil.parseObj(jsonStr);
    }

    public static <T> T parseObject(String jsonStr, Class<T> cls) {
        return JSONUtil.toBean(jsonStr, cls);
    }

    public static JSONObject from(Object bean) {
        if (bean == null) {
            return new JSONObject();
        }
        JsonNode n = JSONUtil.toNode(bean);
        if (n instanceof ObjectNode) {
            return new JSONObject((ObjectNode) n);
        }
        throw new IllegalArgumentException("无法将对象转换为 JSONObject");
    }

    public static String toJSONString(Object obj) {
        return JSONUtil.toJsonStr(obj);
    }
}
