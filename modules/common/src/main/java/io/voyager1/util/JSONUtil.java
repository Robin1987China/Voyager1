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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 *  {@code io.voyager1.util.JSONUtil} 的兼容实现。
 *
 * <p>基于 Jackson，仅覆盖代码库实际使用到的 API。</p>
 */
public final class JSONUtil {

    static final ObjectMapper MAPPER = new ObjectMapper();

    private JSONUtil() {
    }

    // ------------------------------------------------------------------ 构造

    public static JSONObject createObj() {
        return new JSONObject();
    }

    public static JSONArray createArray() {
        return new JSONArray();
    }

    // ------------------------------------------------------------------ 解析

    /**
     * 解析 JSON 字符串，返回 {@link JSONObject} 或 {@link JSONArray}。
     */
    public static JSON parse(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(jsonStr);
            if (node == null || node.isNull()) {
                return null;
            }
            if (node.isObject()) {
                return new JSONObject((ObjectNode) node);
            }
            if (node.isArray()) {
                return new JSONArray((ArrayNode) node);
            }
            return null;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("解析 JSON 失败：" + jsonStr, e);
        }
    }

    public static JSONObject parseObj(String jsonStr) {
        JSON json = parse(jsonStr);
        return json instanceof JSONObject ? (JSONObject) json : null;
    }

    public static JSONArray parseArray(String jsonStr) {
        JSON json = parse(jsonStr);
        return json instanceof JSONArray ? (JSONArray) json : null;
    }

    // ------------------------------------------------------------------ 序列化

    public static String toJsonStr(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof JSONObject || obj instanceof JSONArray) {
            return obj.toString();
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 序列化失败", e);
        }
    }

    // ------------------------------------------------------------------ 转换

    public static <T> T toBean(String jsonStr, Class<T> cls) {
        try {
            return MAPPER.readValue(jsonStr, cls);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 转 Bean 失败", e);
        }
    }

    // ------------------------------------------------------------------ 内部工具

    /**
     * 将任意 Java 值转换为 {@link JsonNode}（可处理嵌套的 JSONObject/JSONArray）。
     */
    static JsonNode toNode(Object value) {
        if (value == null) {
            return MAPPER.nullNode();
        }
        if (value instanceof JSONObject) {
            return ((JSONObject) value).node();
        }
        if (value instanceof JSONArray) {
            return ((JSONArray) value).node();
        }
        if (value instanceof JsonNode) {
            return (JsonNode) value;
        }
        if (value instanceof Map) {
            ObjectNode objectNode = MAPPER.createObjectNode();
            ((Map<?, ?>) value).forEach((k, v) -> objectNode.set(String.valueOf(k), toNode(v)));
            return objectNode;
        }
        if (value instanceof Collection) {
            ArrayNode arrayNode = MAPPER.createArrayNode();
            for (Object item : (Collection<?>) value) {
                arrayNode.add(toNode(item));
            }
            return arrayNode;
        }
        if (value.getClass().isArray()) {
            ArrayNode arrayNode = MAPPER.createArrayNode();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                arrayNode.add(toNode(Array.get(value, i)));
            }
            return arrayNode;
        }
        return MAPPER.valueToTree(value);
    }

    /**
     * 将 {@link JsonNode} 转换为普通 Java 值（对象 -> JSONObject，数组 -> JSONArray）。
     */
    static Object toValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            return new JSONObject((ObjectNode) node);
        }
        if (node.isArray()) {
            return new JSONArray((ArrayNode) node);
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        return node.textValue();
    }

    /**
     * 按目标类型转换 {@link JsonNode}。
     */
    static <T> T convert(JsonNode node, Class<T> type) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (type == String.class) {
            return type.cast(node.isTextual() ? node.textValue() : node.toString());
        }
        if (type == JSONObject.class) {
            return type.cast(node.isObject() ? new JSONObject((ObjectNode) node) : null);
        }
        if (type == JSONArray.class) {
            return type.cast(node.isArray() ? new JSONArray((ArrayNode) node) : null);
        }
        if (type == Object.class) {
            return type.cast(toValue(node));
        }
        try {
            return MAPPER.treeToValue(node, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("无法将 JSON 转换为 " + type.getName(), e);
        }
    }

    /**
     * 按点分隔路径导航并转换为目标类型。
     */
    static <T> T getByPath(JsonNode root, String expression, Class<T> type) {
        JsonNode current = root;
        if (expression != null && !expression.isEmpty()) {
            for (String part : expression.split("\\.")) {
                if (part == null || part.isEmpty()) {
                    continue;
                }
                if (current == null || current.isNull() || current.isMissingNode()) {
                    return null;
                }
                if (current.isArray()) {
                    try {
                        current = current.path(Integer.parseInt(part));
                    } catch (NumberFormatException e) {
                        current = null;
                    }
                } else {
                    current = current.path(part);
                }
            }
        }
        return convert(current, type);
    }
}
