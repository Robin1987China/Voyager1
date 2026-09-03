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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 *  {@code io.voyager1.util.JSONArray} 的兼容实现。
 *
 * <p>可变的 {@link ArrayNode} 包装器，仅覆盖代码库实际使用到的 API。</p>
 */
public class JSONArray implements JSON {

    private final ArrayNode node;

    public JSONArray() {
        this.node = JSONUtil.MAPPER.createArrayNode();
    }

    JSONArray(ArrayNode node) {
        this.node = node;
    }

    ArrayNode node() {
        return node;
    }

    // ------------------------------------------------------------------ 写入

    public JSONArray add(Object value) {
        node.add(JSONUtil.toNode(value));
        return this;
    }

    public JSONArray put(Object value) {
        node.add(JSONUtil.toNode(value));
        return this;
    }

    // ------------------------------------------------------------------ 读取

    public Object get(int index) {
        return JSONUtil.toValue(node.get(index));
    }

    public String getStr(int index) {
        JsonNode n = node.get(index);
        if (n == null || n.isNull()) {
            return null;
        }
        return n.isTextual() ? n.textValue() : n.toString();
    }

    public String getString(int index) {
        return getStr(index);
    }

    public Integer getInteger(int index) {
        JsonNode n = node.get(index);
        return (n == null || n.isNull()) ? null : n.asInt();
    }

    public int getIntValue(int index) {
        JsonNode n = node.get(index);
        return (n == null || n.isNull()) ? 0 : n.asInt();
    }

    public Long getLong(int index) {
        JsonNode n = node.get(index);
        return (n == null || n.isNull()) ? null : n.asLong();
    }

    public Boolean getBoolean(int index) {
        JsonNode n = node.get(index);
        return (n == null || n.isNull()) ? null : n.asBoolean();
    }

    public JSONObject getJSONObject(int index) {
        JsonNode n = node.get(index);
        return (n != null && n.isObject()) ? new JSONObject((com.fasterxml.jackson.databind.node.ObjectNode) n) : null;
    }

    public JSONArray getJSONArray(int index) {
        JsonNode n = node.get(index);
        return (n != null && n.isArray()) ? new JSONArray((ArrayNode) n) : null;
    }

    // ------------------------------------------------------------------ 结构

    public boolean isEmpty() {
        return node.isEmpty();
    }

    public int size() {
        return node.size();
    }

    public Stream<Object> stream() {
        List<Object> list = new ArrayList<>(node.size());
        for (JsonNode n : node) {
            list.add(JSONUtil.toValue(n));
        }
        return list.stream();
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

    public static JSONArray parseArray(String jsonStr) {
        return JSONUtil.parseArray(jsonStr);
    }

    public static JSONArray parse(String jsonStr) {
        return JSONUtil.parseArray(jsonStr);
    }

    public static String toJSONString(Object obj) {
        return JSONUtil.toJsonStr(obj);
    }
}
