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

import io.voyager1.model.BaseIdModel;
import io.voyager1.model.BaseDbModel;
import io.voyager1.model.PageResultDto;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.JakartaServletUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 非工作空间型 JPA 服务基类（清洁室实现，取代 BaseDbService 持久层）。
 * <p>
 * 与 {@link JpaWorkspaceService} 的区别：模型不绑定 {@code workspaceId}，不提供工作空间过滤方法。
 * 用于工作空间本身、机器资产、用户、备份等无工作空间维度的数据。
 *
 * @param <T> 数据模型类型（继承 BaseIdModel）
 * @param <E> JPA 实体类型（无工作空间约束）
 */
public abstract class JpaBaseService<T extends BaseDbModel, E> implements DataService<T> {

    @PersistenceContext
    protected EntityManager entityManager;

    protected abstract JpaRepository<E, String> repository();

    protected abstract JpaSpecificationExecutor<E> specExecutor();

    protected abstract Class<E> entityClass();

    protected abstract Class<T> modelClass();

    @Override
    public T getByKey(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        E entity = repository().findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    @Override
    public T getByKey(String id, boolean fill) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        E entity = repository().findById(id).orElse(null);
        return entity == null ? null : (fill ? toModel(entity) : toModelNoFill(entity));
    }

    public List<T> getByKey(Collection<String> ids) {
        return repository().findAllById(ids).stream().map(this::toModel).collect(Collectors.toList());
    }

    public List<T> listById(Collection<String> ids) {
        return repository().findAllById(ids).stream().map(this::toModel).collect(Collectors.toList());
    }

    @Transactional
    public int insert(T model) {
        long now = System.currentTimeMillis();
        if (model.getId() == null || model.getId().isEmpty()) {
            model.setId(UUID.randomUUID().toString());
        }
        if (model.getCreateTimeMillis() == null) {
            model.setCreateTimeMillis(now);
        }
        model.setModifyTimeMillis(now);
        this.fillInsert(model);
        E entity = BeanUtils.instantiateClass(entityClass());
        this.copyProperties(model, entity);
        repository().save(entity);
        return 1;
    }

    @Transactional
    public void upsert(T model) {
        if (model.getId() != null && !model.getId().isEmpty() && repository().existsById(model.getId())) {
            this.updateById(model);
        } else {
            this.insert(model);
        }
    }

    @Transactional
    public void insert(Collection<T> models) {
        for (T model : models) {
            this.insert(model);
        }
    }

    @Transactional
    public int updateById(T model) {
        E entity = repository().findById(model.getId()).orElse(null);
        if (entity == null) {
            this.insert(model);
            return 0;
        }
        model.setModifyTimeMillis(System.currentTimeMillis());
        this.copyProperties(model, entity);
        repository().save(entity);
        return 1;
    }

    @Transactional
    public int delByKey(String id) {
        if (repository().existsById(id)) {
            repository().deleteById(id);
            return 1;
        }
        return 0;
    }

    @Transactional
    public int delByKey(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<E> toDelete = repository().findAllById(ids);
        repository().deleteAll(toDelete);
        return toDelete.size();
    }

    @Transactional
    public int del(io.voyager1.core.db.Entity where) {
        if (where == null || where.isEmpty()) {
            return 0;
        }
        List<E> toDelete = specExecutor().findAll(JpaQuerySupport.specificationFromEntity(where));
        repository().deleteAll(toDelete);
        return toDelete.size();
    }

    public List<T> list() {
        return repository().findAll().stream().map(this::toModel).collect(Collectors.toList());
    }

    public List<T> listByBean(T model) {
        return specExecutor().findAll(JpaQuerySupport.specificationFromBean(model))
            .stream().map(this::toModel).collect(Collectors.toList());
    }

    public List<T> listByBean(T model, boolean fill) {
        return specExecutor().findAll(JpaQuerySupport.specificationFromBean(model))
            .stream().map(e -> fill ? toModel(e) : toModelNoFill(e)).collect(Collectors.toList());
    }

    public List<T> listByEntity(io.voyager1.core.db.Entity where) {
        return specExecutor().findAll(JpaQuerySupport.specificationFromEntity(where))
            .stream().map(this::toModel).collect(Collectors.toList());
    }

    public List<T> listByEntity(io.voyager1.core.db.Entity where, boolean fill) {
        return this.listByEntity(where);
    }

    public boolean exists(String id) {
        return repository().existsById(id);
    }

    public boolean exists(T model) {
        return !this.listByBean(model).isEmpty();
    }

    public boolean exists(io.voyager1.core.db.Entity where) {
        return this.count(where) > 0;
    }

    public long count() {
        return repository().count();
    }

    public long count(io.voyager1.core.db.Entity where) {
        if (where == null || where.isEmpty()) {
            return repository().count();
        }
        return specExecutor().count(JpaQuerySupport.specificationFromEntity(where));
    }

    public long count(T model) {
        return specExecutor().count(JpaQuerySupport.specificationFromBean(model));
    }

    public T queryByBean(T model) {
        List<T> list = this.listByBean(model);
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    @Transactional
    public int delByBean(T model) {
        io.voyager1.core.db.Entity where = this.dataBeanToEntity(model);
        if (where.isEmpty()) {
            return 0;
        }
        List<E> toDelete = specExecutor().findAll(JpaQuerySupport.specificationFromEntity(where));
        repository().deleteAll(toDelete);
        return toDelete.size();
    }

    public PageResultDto<T> listPage(Map<String, String> paramMap) {
        Page<E> page = specExecutor().findAll(
            JpaQuerySupport.specification(paramMap), JpaQuerySupport.pageable(paramMap));
        List<T> result = page.getContent().stream().map(this::toModel).collect(Collectors.toList());
        return JpaQuerySupport.toPageResult(page, result);
    }

    public PageResultDto<T> listPage(Map<String, String> paramMap, boolean fill) {
        return this.listPage(paramMap);
    }

    public PageResultDto<T> listPage(io.voyager1.core.db.Entity where, Pageable pageable) {
        Page<E> result = specExecutor().findAll(JpaQuerySupport.specificationFromEntity(where), pageable);
        List<T> models = result.getContent().stream().map(this::toModel).collect(Collectors.toList());
        return JpaQuerySupport.toPageResult(result, models);
    }

    public List<T> queryList(io.voyager1.core.db.Entity where, int count) {
        return this.queryList(where, count, Sort.unsorted());
    }

    public List<T> queryList(T data, int count) {
        return this.queryList(data, count, Sort.unsorted());
    }

    public List<T> queryList(io.voyager1.core.db.Entity where, int count, Sort sort) {
        Pageable pageable = PageRequest.of(0, Math.max(count, 1), sort);
        PageResultDto<T> result = this.listPage(where, pageable);
        return result.getResult();
    }

    public List<T> queryList(T data, int count, Sort sort) {
        io.voyager1.core.db.Entity where = this.dataBeanToEntity(data);
        return this.queryList(where, count, sort);
    }

    @Transactional
    public int update(io.voyager1.core.db.Entity data, io.voyager1.core.db.Entity where) {
        if (where == null || where.isEmpty()) {
            return 0;
        }
        List<E> list = specExecutor().findAll(JpaQuerySupport.specificationFromEntity(where));
        for (E e : list) {
            BeanWrapperImpl bw = new BeanWrapperImpl(e);
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                try {
                    bw.setPropertyValue(entry.getKey(), entry.getValue());
                } catch (Exception ignore) {
                    // 忽略不存在或类型不匹配的字段
                }
            }
            repository().save(e);
        }
        return list.size();
    }

    public io.voyager1.core.db.Entity dataBeanToEntity(T data) {
        io.voyager1.core.db.Entity entity = new io.voyager1.core.db.Entity();
        if (data == null) {
            return entity;
        }
        for (java.lang.reflect.Field field : io.voyager1.util.ReflectUtil.getFields(data.getClass())) {
            int mod = field.getModifiers();
            if (java.lang.reflect.Modifier.isStatic(mod) || java.lang.reflect.Modifier.isTransient(mod)) {
                continue;
            }
            if (field.isAnnotationPresent(io.voyager1.util.PropIgnore.class)) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(data);
                if (value != null) {
                    entity.set(field.getName(), value);
                }
            } catch (Exception ignore) {
                // 忽略不可读字段
            }
        }
        return entity;
    }

    public T getByKey(String keyValue, boolean fill, Consumer<io.voyager1.core.db.Entity> consumer) {
        if (keyValue == null || keyValue.isEmpty()) {
            return null;
        }
        io.voyager1.core.db.Entity where = new io.voyager1.core.db.Entity();
        where.set("id", keyValue);
        if (consumer != null) {
            consumer.accept(where);
        }
        List<E> list = specExecutor().findAll(JpaQuerySupport.specificationFromEntity(where));
        if (list.isEmpty()) {
            return null;
        }
        E e = list.get(0);
        return fill ? toModel(e) : toModelNoFill(e);
    }

    protected long getLastTimeValue(String timeColumn, int maxCount, Consumer<io.voyager1.core.db.Entity> whereCon) {
        io.voyager1.core.db.Entity entity = new io.voyager1.core.db.Entity();
        if (whereCon != null) {
            whereCon.accept(entity);
        }
        Pageable pageable = PageRequest.of(Math.max(maxCount - 1, 0), 1, Sort.by(Sort.Order.desc(timeColumn)));
        try {
            PageResultDto<T> pageResult = this.listPage(entity, pageable);
            if (pageResult.isEmpty()) {
                return 0L;
            }
            T first = pageResult.get(0);
            Object fieldValue = new BeanWrapperImpl(first).getPropertyValue(timeColumn);
            return ConvertUtil.toLong(fieldValue, 0L);
        } catch (Exception e) {
            return 0L;
        }
    }

    protected void autoLoopClear(String timeClo, int maxCount, Consumer<io.voyager1.core.db.Entity> whereCon, Predicate<T> predicate) {
        if (maxCount <= 0) {
            return;
        }
        io.voyager1.util.ThreadUtil.execute(() -> {
            io.voyager1.core.db.Entity entity = new io.voyager1.core.db.Entity();
            long timeValue = this.getLastTimeValue(timeClo, maxCount, whereCon);
            if (timeValue <= 0) {
                return;
            }
            if (whereCon != null) {
                whereCon.accept(entity);
            }
            entity.set(timeClo, "< " + timeValue);
            while (true) {
                Pageable pageable = PageRequest.of(0, 50, Sort.by(Sort.Order.desc(timeClo)));
                PageResultDto<T> pageResult = this.listPage(entity, pageable);
                if (pageResult.isEmpty()) {
                    return;
                }
                List<String> ids = pageResult.getResult().stream().filter(predicate).map(BaseIdModel::getId).collect(Collectors.toList());
                this.delByKey(ids);
            }
        });
    }

    public Pageable parsePage(Map<String, String> paramMap) {
        int page = ConvertUtil.toInt(paramMap.get("page"), 1);
        int limit = ConvertUtil.toInt(paramMap.get("limit"), 10);
        if (page <= 0) {
            page = 1;
        }
        if (limit <= 0 || limit >= 200) {
            limit = 10;
        }
        return PageRequest.of(page - 1, limit);
    }

    protected void fillInsert(T model) {
    }

    protected void fillSelectResult(T model) {
    }

    protected String[] clearTimeColumns() {
        return new String[]{};
    }

    protected void executeClearImpl(int logStorageCount) {
    }

    protected void copyProperties(Object src, Object dst) {
        BeanWrapperImpl srcW = new BeanWrapperImpl(src);
        BeanWrapperImpl dstW = new BeanWrapperImpl(dst);
        java.util.List<String> nullNames = new java.util.ArrayList<>();
        for (PropertyDescriptor pd : srcW.getPropertyDescriptors()) {
            String name = pd.getName();
            if ("class".equals(name)) {
                continue;
            }
            if (srcW.getPropertyValue(name) == null) {
                nullNames.add(name);
            }
        }
        BeanUtils.copyProperties(src, dst, nullNames.toArray(new String[0]));
        for (PropertyDescriptor pd : dstW.getPropertyDescriptors()) {
            String name = pd.getName();
            if ("class".equals(name)) {
                continue;
            }
            Class<?> srcType = srcW.getPropertyType(name);
            Class<?> dstType = pd.getPropertyType();
            if (srcType == null || dstType == null) {
                continue;
            }
            boolean toInteger = srcType == Boolean.class && dstType == Integer.class;
            boolean toBoolean = srcType == Integer.class && dstType == Boolean.class;
            if (toInteger || toBoolean) {
                Object v = srcW.getPropertyValue(name);
                if (v != null) {
                    dstW.setPropertyValue(name, toInteger ? (((Boolean) v) ? 1 : 0) : (((Integer) v) != 0));
                }
            } else if (dstType == String.class && Number.class.isAssignableFrom(srcType)) {
                Object v = srcW.getPropertyValue(name);
                if (v != null) {
                    dstW.setPropertyValue(name, v.toString());
                }
            } else if (srcType == String.class && Number.class.isAssignableFrom(dstType)) {
                Object v = srcW.getPropertyValue(name);
                if (v != null && !v.toString().isEmpty()) {
                    try {
                        if (dstType == Long.class) {
                            dstW.setPropertyValue(name, Long.parseLong(v.toString()));
                        } else if (dstType == Integer.class) {
                            dstW.setPropertyValue(name, Integer.parseInt(v.toString()));
                        } else if (dstType == Double.class) {
                            dstW.setPropertyValue(name, Double.parseDouble(v.toString()));
                        } else if (dstType == Float.class) {
                            dstW.setPropertyValue(name, Float.parseFloat(v.toString()));
                        }
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }
    }

    protected T toModel(E entity) {
        T model = this.toModelNoFill(entity);
        this.fillSelectResult(model);
        return model;
    }

    protected T toModelNoFill(E entity) {
        T model = BeanUtils.instantiateClass(modelClass());
        this.copyProperties(entity, model);
        return model;
    }

    public String getTableName() {
        io.voyager1.core.db.TableName annotation = modelClass().getAnnotation(io.voyager1.core.db.TableName.class);
        return annotation == null ? null : annotation.value();
    }

    public String typeName() {
        return getTableName();
    }

    public String getDataDesc() {
        io.voyager1.core.db.TableName annotation = modelClass().getAnnotation(io.voyager1.core.db.TableName.class);
        org.springframework.util.Assert.notNull(annotation, "请配置 table Name");
        return io.voyager1.common.i18n.I18nMessageUtil.get(annotation.nameKey());
    }

    public PageResultDto<T> listPage(HttpServletRequest request) {
        return this.listPage(JakartaServletUtil.getParamMap(request));
    }

    public List<String> listGroup() {
        return repository().findAll().stream()
            .map(e -> {
                try {
                    Object g = new BeanWrapperImpl(e).getPropertyValue("group");
                    return g == null ? null : g.toString();
                } catch (Exception ex) {
                    return null;
                }
            })
            .filter(java.util.Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    public List<String> listGroupName() {
        return repository().findAll().stream()
            .map(e -> {
                try {
                    Object g = new BeanWrapperImpl(e).getPropertyValue("groupName");
                    return g == null ? null : g.toString();
                } catch (Exception ex) {
                    return null;
                }
            })
            .filter(java.util.Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    public Sort defaultSort() {
        return Sort.by(
            Sort.Order.desc("createTimeMillis"),
            Sort.Order.desc("modifyTimeMillis"),
            Sort.Order.desc("id"));
    }

    public Number queryNumber(String sql, Object... params) {
        Query query = entityManager.createNativeQuery(sql);
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        List<?> list = query.getResultList();
        if (list == null || list.isEmpty()) {
            return null;
        }
        Object first = list.get(0);
        if (first instanceof Number) {
            return (Number) first;
        }
        if (first instanceof Object[]) {
            Object[] arr = (Object[]) first;
            return arr.length > 0 && arr[0] instanceof Number ? (Number) arr[0] : null;
        }
        return null;
    }

    @Transactional
    public int execute(String sql, Object... params) {
        Query query = entityManager.createNativeQuery(sql);
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        return query.executeUpdate();
    }

    /**
     * 原生 SQL 查询，按列名映射为承继存储框架的 {@link io.voyager1.core.db.Entity}（复刻 BaseDbCommonService.query）。
     */
    public List<io.voyager1.core.db.Entity> query(String sql, Object... params) {
        org.hibernate.Session session = entityManager.unwrap(org.hibernate.Session.class);
        final List<io.voyager1.core.db.Entity> result = new java.util.ArrayList<>();
        session.doWork(connection -> {
            try (java.sql.PreparedStatement ps = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    java.sql.ResultSetMetaData meta = rs.getMetaData();
                    while (rs.next()) {
                        io.voyager1.core.db.Entity entity = new io.voyager1.core.db.Entity();
                        for (int c = 1; c <= meta.getColumnCount(); c++) {
                            entity.put(meta.getColumnLabel(c), rs.getObject(c));
                        }
                        result.add(entity);
                    }
                }
            }
        });
        return result;
    }
}
