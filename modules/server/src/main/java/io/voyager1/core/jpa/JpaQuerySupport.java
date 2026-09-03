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

package io.voyager1.core.jpa;

import io.voyager1.core.db.Entity;
import io.voyager1.model.PageResultDto;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.DateUtil;
import io.voyager1.util.DateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.BeanWrapperImpl;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * JPA 动态查询/分页支持（清洁室实现，取代承继存储框架的 Entity 动态查询）。
 * <p>
 * 复刻 {@code BaseDbService.listPage(paramMap)} 的核心契约：
 * <ul>
 *   <li>分页：{@code page}（默认 1）、{@code limit}（默认 10，&lt; 200）</li>
 *   <li>排序：{@code order_field} + {@code order}（ascend/descend）</li>
 *   <li>过滤：等值、{@code :in} 列表、{@code ~} 时间区间、{@code %} LIKE</li>
 * </ul>
 */
public final class JpaQuerySupport {

    private static final List<String> RESERVED = Arrays.asList("page", "limit", "order_field", "order", "total");

    private JpaQuerySupport() {
    }

    /**
     * 由请求参数构建 JPA Specification（动态 WHERE）。
     */
    public static <E> Specification<E> specification(Map<String, String> paramMap) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Map.Entry<String, String> entry : paramMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value == null || value.isEmpty()) {
                    continue;
                }
                String cleanKey = key.replace("%", "");
                if (RESERVED.contains(cleanKey)) {
                    continue;
                }
                if (cleanKey.endsWith(":in")) {
                    String field = cleanKey.substring(0, cleanKey.length() - 3);
                    List<String> values = new ArrayList<>(Arrays.asList(value.split(",")));
                    predicates.add(root.get(field).in(values));
                } else if (cleanKey.toLowerCase().contains("time") && value.contains("~")) {
                    String[] vals = value.split("~");
                    if (vals.length == 2) {
                        predicates.add(cb.greaterThanOrEqualTo(root.get(cleanKey), parseTime(vals[0])));
                        predicates.add(cb.lessThanOrEqualTo(root.get(cleanKey), parseTime(vals[1])));
                    }
                } else if (key.startsWith("%") && key.endsWith("%")) {
                    predicates.add(cb.like(root.get(cleanKey), "%" + value + "%"));
                } else if (key.endsWith("%")) {
                    predicates.add(cb.like(root.get(cleanKey), value + "%"));
                } else if (key.startsWith("%")) {
                    predicates.add(cb.like(root.get(cleanKey), "%" + value));
                } else {
                    predicates.add(cb.equal(root.get(cleanKey), value));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 由请求参数构建分页对象（含排序）。
     */
    public static Pageable pageable(Map<String, String> paramMap) {
        int page = ConvertUtil.toInt(paramMap.get("page"), 1);
        int limit = ConvertUtil.toInt(paramMap.get("limit"), 10);
        if (page <= 0) {
            page = 1;
        }
        if (limit <= 0 || limit >= 200) {
            limit = 10;
        }
        String orderField = paramMap.get("order_field");
        String order = paramMap.get("order");
        Sort sort = Sort.unsorted();
        if (orderField != null && !orderField.isEmpty()) {
            String field = orderField.replace("%", "");
            Sort.Direction direction = "ascend".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, field);
        }
        return PageRequest.of(page - 1, limit, sort);
    }

    /**
     * 将 Spring 分页结果转换为 {@link PageResultDto}。
     */
    public static <T> PageResultDto<T> toPageResult(Page<?> page, List<T> result) {
        PageResultDto<T> dto = new PageResultDto<>(page.getNumber() + 1, page.getSize(), (int) page.getTotalElements());
        dto.setResult(result);
        return dto;
    }

    /**
     * 由 bean 的非空字段构建 Specification（复刻 listByBean/exists/count(bean) 契约）。
     */
    public static <E> Specification<E> specificationFromBean(Object bean) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            BeanWrapperImpl bw = new BeanWrapperImpl(bean);
            for (PropertyDescriptor pd : bw.getPropertyDescriptors()) {
                String name = pd.getName();
                if ("class".equals(name)) {
                    continue;
                }
                // 只读属性（无 setter）属于派生 getter（如 isDemoUser），不是可查询字段
                if (pd.getWriteMethod() == null) {
                    continue;
                }
                Object value;
                try {
                    value = bw.getPropertyValue(name);
                } catch (Exception e) {
                    continue;
                }
                if (value == null) {
                    continue;
                }
                if (value instanceof String && ((String) value).isEmpty()) {
                    continue;
                }
                // 模型字段可能不在实体中（填充/展示字段），跳过以避免属性解析异常
                jakarta.persistence.criteria.Path<E> path;
                try {
                    path = root.get(name);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                predicates.add(cb.equal(path, coerceValue(path.getJavaType(), value)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 由承继存储框架的 {@link Entity}（条件 Map）构建 Specification。
     * <p>
     * 复刻 {@code BaseDbCommonService.buildWhere/appendWhereValue} 的运算符契约：集合值 = IN、
     * 字符串前缀运算符（like / &gt;= / &lt;= / &lt;&gt; / != / &gt; / &lt;），其余等值。
     */
    public static <E> Specification<E> specificationFromEntity(Entity entity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Map.Entry<String, Object> entry : entity.entrySet()) {
                Object value = entry.getValue();
                if (value == null) {
                    continue;
                }
                String field = entry.getKey();
                if (field == null || field.isEmpty()) {
                    continue;
                }
                if (value instanceof Collection) {
                    Collection<?> coll = (Collection<?>) value;
                    if (coll.isEmpty()) {
                        continue;
                    }
                    List<Object> coerced = coll.stream()
                        .map(v -> coerceValue(root.get(field).getJavaType(), v))
                        .collect(java.util.stream.Collectors.toList());
                    predicates.add(root.get(field).in(coerced));
                } else if (value instanceof String) {
                    String s = ((String) value).trim();
                    if (s.isEmpty()) {
                        continue;
                    }
                    if (s.startsWith("like ")) {
                        predicates.add(cb.like(root.get(field), s.substring(5)));
                    } else if (s.startsWith(">=")) {
                        predicates.add(cb.greaterThanOrEqualTo((jakarta.persistence.criteria.Expression) root.get(field), coerce(root.get(field).getJavaType(), s.substring(2).trim())));
                    } else if (s.startsWith("<=")) {
                        predicates.add(cb.lessThanOrEqualTo((jakarta.persistence.criteria.Expression) root.get(field), coerce(root.get(field).getJavaType(), s.substring(2).trim())));
                    } else if (s.startsWith("<>")) {
                        predicates.add(cb.notEqual((jakarta.persistence.criteria.Expression) root.get(field), coerce(root.get(field).getJavaType(), s.substring(2).trim())));
                    } else if (s.startsWith("!=")) {
                        predicates.add(cb.notEqual((jakarta.persistence.criteria.Expression) root.get(field), coerce(root.get(field).getJavaType(), s.substring(2).trim())));
                    } else if (s.startsWith(">")) {
                        predicates.add(cb.greaterThan((jakarta.persistence.criteria.Expression) root.get(field), coerce(root.get(field).getJavaType(), s.substring(1).trim())));
                    } else if (s.startsWith("<")) {
                        predicates.add(cb.lessThan((jakarta.persistence.criteria.Expression) root.get(field), coerce(root.get(field).getJavaType(), s.substring(1).trim())));
                    } else {
                        predicates.add(cb.equal(root.get(field), coerceValue(root.get(field).getJavaType(), value)));
                    }
                } else {
                    predicates.add(cb.equal(root.get(field), coerceValue(root.get(field).getJavaType(), value)));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Long parseTime(String value) {
        try {
            DateTime dateTime = DateUtil.parse(value.trim(), "yyyy-MM-dd HH:mm:ss");
            return dateTime.getTime();
        } catch (Exception e) {
            return 0L;
        }
    }

    private static Comparable coerce(Class<?> type, String s) {
        try {
            if (type == Long.class || type == long.class) return Long.parseLong(s);
            if (type == Integer.class || type == int.class) return Integer.parseInt(s);
            if (type == Double.class || type == double.class) return Double.parseDouble(s);
            if (type == Float.class || type == float.class) return Float.parseFloat(s);
            if (type == Short.class || type == short.class) return Short.parseShort(s);
            if (type == Boolean.class || type == boolean.class) return Boolean.parseBoolean(s);
        } catch (NumberFormatException ignore) {
            // 转换失败时保留字符串
        }
        return s;
    }

    private static Object coerceValue(Class<?> type, Object value) {
        if (value == null) {
            return null;
        }
        if (type == Integer.class && value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        }
        if (type == Boolean.class && value instanceof Integer) {
            return ((Integer) value) != 0;
        }
        if (value instanceof String && Number.class.isAssignableFrom(type)) {
            return coerce(type, (String) value);
        }
        if (type == String.class && value instanceof Number) {
            return value.toString();
        }
        return value;
    }
}
