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

package io.voyager1.core.db;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 数据库实体。
 * 内部是一个有序 Map（字段名 -> 值），字段名通常已按方言包裹（wrap）。
 *
 */
public class Entity extends LinkedHashMap<String, Object> {

    /**
     * 表名
     */
    private String tableName;
    /**
     * 查询字段名（null 表示查询所有字段）
     */
    private Set<String> fieldNames;

    public Entity() {
    }

    public Entity(String tableName) {
        this.tableName = tableName;
    }

    /**
     * 创建空实体
     */
    public static Entity create() {
        return new Entity();
    }

    /**
     * 创建指定表名的实体
     */
    public static Entity create(String tableName) {
        return new Entity(tableName);
    }

    /**
     * 获取表名
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * 设置表名
     */
    public Entity setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    /**
     * 设置字段值
     */
    public Entity set(String field, Object value) {
        this.put(field, value);
        return this;
    }

    /**
     * 获取字段值（原始对象）
     */
    @Override
    public Object get(Object field) {
        Object value = super.get(field);
        if (value != null || !(field instanceof String)) {
            return value;
        }
        // 大小写不敏感兜底：H2 等数据库未加引号的列名会被转大写（password -> PASSWORD）
        String fieldStr = (String) field;
        for (Map.Entry<String, Object> entry : entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(fieldStr)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 获取字符串值
     */
    public String getStr(String field) {
        Object value = get(field);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * 获取 Long 值
     */
    public Long getLong(String field) {
        Object value = get(field);
        return toLong(value);
    }

    /**
     * 获取 Integer 值
     */
    public Integer getInt(String field) {
        Object value = get(field);
        return toInt(value);
    }

    /**
     * 获取 Short 值
     */
    public Short getShort(String field) {
        Object value = get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).shortValue();
        }
        return Short.parseShort(value.toString());
    }

    /**
     * 获取 Boolean 值
     */
    public Boolean getBool(String field) {
        Object value = get(field);
        return toBool(value);
    }

    /**
     * 获取 Double 值
     */
    public Double getDouble(String field) {
        Object value = get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    /**
     * 获取 Float 值
     */
    public Float getFloat(String field) {
        Object value = get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return Float.parseFloat(value.toString());
    }

    /**
     * 获取 BigDecimal 值
     */
    public BigDecimal getBigDecimal(String field) {
        Object value = get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

    /**
     * 获取 Date 值
     */
    public Date getDate(String field) {
        Object value = get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof LocalDateTime) {
            return Timestamp.valueOf((LocalDateTime) value);
        }
        if (value instanceof LocalDate) {
            return Timestamp.valueOf(((LocalDate) value).atStartOfDay());
        }
        // 兼容时间戳（毫秒）
        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }
        throw new IllegalArgumentException("字段 [" + field + "] 无法转换为 Date: " + value);
    }

    /**
     * 获取 Timestamp 值
     */
    public Timestamp getTimestamp(String field) {
        Object value = get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }
        if (value instanceof Date) {
            return new Timestamp(((Date) value).getTime());
        }
        if (value instanceof LocalDateTime) {
            return Timestamp.valueOf((LocalDateTime) value);
        }
        throw new IllegalArgumentException("字段 [" + field + "] 无法转换为 Timestamp: " + value);
    }

    /**
     * 获取 byte[] 值（BLOB）
     */
    public byte[] getBytes(String field) {
        Object value = get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        if (value instanceof java.sql.Blob) {
            try {
                java.sql.Blob blob = (java.sql.Blob) value;
                return blob.getBytes(1, (int) blob.length());
            } catch (Exception e) {
                throw new RuntimeException("读取 BLOB 字段 [" + field + "] 失败", e);
            }
        }
        throw new IllegalArgumentException("字段 [" + field + "] 不是 byte[] 类型: " + value.getClass());
    }

    /**
     * 获取原始对象值
     */
    public Object getObject(String field) {
        return get(field);
    }

    /**
     * 设置查询字段名（用于 SELECT 指定字段）
     */
    public Entity setFieldNames(String... fieldNames) {
        if (fieldNames == null || fieldNames.length == 0) {
            this.fieldNames = null;
        } else {
            this.fieldNames = new LinkedHashSet<>(Arrays.asList(fieldNames));
        }
        return this;
    }

    /**
     * 获取查询字段名集合（未设置时返回 null，表示查询所有字段）
     */
    public Set<String> getFieldNames() {
        return fieldNames;
    }

    /**
     * 浅拷贝
     */
    @Override
    public Entity clone() {
        Entity entity = new Entity(this.tableName);
        entity.putAll(this);
        return entity;
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1L : 0L;
        }
        return new BigDecimal(value.toString()).longValue();
    }

    private static Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        }
        return new BigDecimal(value.toString()).intValue();
    }

    private static Boolean toBool(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        String s = value.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        if ("1".equals(s) || "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s) || "y".equalsIgnoreCase(s) || "on".equalsIgnoreCase(s)) {
            return Boolean.TRUE;
        }
        if ("0".equals(s) || "false".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s) || "n".equalsIgnoreCase(s) || "off".equalsIgnoreCase(s)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("无法转换为 Boolean: " + value);
    }

    private static BigInteger toBigInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigInteger) {
            return (BigInteger) value;
        }
        if (value instanceof Number) {
            return BigInteger.valueOf(((Number) value).longValue());
        }
        return new BigInteger(value.toString());
    }

    /**
     * 复制为普通 Map
     */
    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(this);
    }
}
