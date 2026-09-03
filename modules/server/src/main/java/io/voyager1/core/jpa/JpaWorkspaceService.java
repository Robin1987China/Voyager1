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

import io.voyager1.common.Const;
import io.voyager1.model.BaseWorkspaceModel;
import io.voyager1.model.PageResultDto;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.util.ConvertUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import java.beans.PropertyDescriptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 工作空间型 JPA 服务基类（清洁室实现，取代承继的 BaseWorkspaceService 持久层）。
 * <p>
 * 提供与 {@code BaseWorkspaceService} 一致的对外契约：CRUD + 工作空间过滤的分页/列表/按ID，
 * 但内部走 JPA（仓库 + Specification + Pageable）。子类只需实现 4 个抽象方法。
 *
 * @param <T> 数据模型类型（继承 BaseWorkspaceModel）
 * @param <E> JPA 实体类型（实现 WorkspaceEntity）
 */
public abstract class JpaWorkspaceService<T extends BaseWorkspaceModel, E extends WorkspaceEntity> implements DataService<T> {

    protected abstract JpaRepository<E, String> repository();

    protected abstract JpaSpecificationExecutor<E> specExecutor();

    protected abstract Class<E> entityClass();

    protected abstract Class<T> modelClass();

    protected String defaultWorkspaceId() {
        return Const.WORKSPACE_DEFAULT_ID;
    }

    @Override
    public T getByKey(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        E entity = repository().findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    public T getByKey(String id, HttpServletRequest request) {
        return this.getByKey(id, request, true);
    }

    public T getByKey(String id, HttpServletRequest request, boolean fill) {
        String workspaceId = WorkspaceContext.getWorkspaceId(request);
        E entity = repository().findById(id).orElse(null);
        if (entity == null || !java.util.Objects.equals(entity.getWorkspaceId(), workspaceId)) {
            return null;
        }
        return fill ? toModel(entity) : toModelNoFill(entity);
    }

    @Transactional
    public void insert(T model) {
        long now = System.currentTimeMillis();
        if (model.getId() == null || model.getId().isEmpty()) {
            model.setId(UUID.randomUUID().toString());
        }
        if (model.getCreateTimeMillis() == null) {
            model.setCreateTimeMillis(now);
        }
        model.setModifyTimeMillis(now);
        if (model.getWorkspaceId() == null || model.getWorkspaceId().isEmpty()) {
            model.setWorkspaceId(defaultWorkspaceId());
        }
        this.fillInsert(model);
        E entity = BeanUtils.instantiateClass(entityClass());
        this.copyProperties(model, entity);
        repository().save(entity);
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
    public void updateById(T model, HttpServletRequest request) {
        String workspaceId = WorkspaceContext.getWorkspaceId(request);
        E entity = repository().findById(model.getId()).orElse(null);
        if (entity == null || !java.util.Objects.equals(entity.getWorkspaceId(), workspaceId)) {
            return;
        }
        model.setModifyTimeMillis(System.currentTimeMillis());
        this.copyProperties(model, entity);
        repository().save(entity);
    }

    @Transactional
    public void updateById(T model) {
        E entity = repository().findById(model.getId()).orElse(null);
        if (entity == null) {
            this.insert(model);
            return;
        }
        model.setModifyTimeMillis(System.currentTimeMillis());
        this.copyProperties(model, entity);
        repository().save(entity);
    }

    @Transactional
    public int delByKey(String id, HttpServletRequest request) {
        if (this.getByKey(id, request) != null) {
            repository().deleteById(id);
            return 1;
        }
        return 0;
    }

    @Transactional
    public int delByKey(String id) {
        if (repository().existsById(id)) {
            repository().deleteById(id);
            return 1;
        }
        return 0;
    }

    public List<T> list(boolean fill) {
        return this.list();
    }

    public List<T> getByKey(Collection<String> ids, HttpServletRequest request) {
        String workspaceId = WorkspaceContext.getWorkspaceId(request);
        return repository().findAllById(ids).stream()
            .filter(e -> java.util.Objects.equals(e.getWorkspaceId(), workspaceId))
            .map(this::toModel)
            .collect(Collectors.toList());
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

    /**
     * 模型转承继存储框架的 {@code Entity}（仅遍历真实字段，跳过 static/transient/@PropIgnore，复刻 BaseDbCommonService.dataBeanToEntity）。
     */
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

    public List<T> listByEntity(io.voyager1.core.db.Entity where) {
        return specExecutor().findAll(JpaQuerySupport.specificationFromEntity(where))
            .stream().map(this::toModel).collect(Collectors.toList());
    }

    public List<T> listByEntity(io.voyager1.core.db.Entity where, boolean fill) {
        return this.listByEntity(where);
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

    public boolean exists(String id) {
        return repository().existsById(id);
    }

    public List<T> getByKey(Collection<String> ids) {
        return repository().findAllById(ids).stream().map(this::toModel).collect(Collectors.toList());
    }

    public boolean exists(T model) {
        return !this.listByBean(model).isEmpty();
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

    public boolean exists(io.voyager1.core.db.Entity where) {
        return this.count(where) > 0;
    }

    public long count(T model) {
        return specExecutor().count(JpaQuerySupport.specificationFromBean(model));
    }

    public String getCheckUserWorkspace(HttpServletRequest request) {
        return WorkspaceContext.getWorkspaceId(request);
    }

    public List<T> listById(Collection<String> ids) {
        return repository().findAllById(ids).stream().map(this::toModel).collect(Collectors.toList());
    }

    public List<T> listByWorkspace(HttpServletRequest request) {
        String workspaceId = WorkspaceContext.getWorkspaceId(request);
        Map<String, String> paramMap = new java.util.HashMap<>();
        paramMap.put("workspaceId", workspaceId);
        return specExecutor().findAll(JpaQuerySupport.specification(paramMap))
            .stream().map(this::toModel).collect(Collectors.toList());
    }

    public PageResultDto<T> listPage(HttpServletRequest request) {
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        paramMap.put("workspaceId", WorkspaceContext.getWorkspaceId(request));
        return this.listPage(paramMap);
    }

    public PageResultDto<T> listPage(Map<String, String> paramMap, boolean fill) {
        return this.listPage(paramMap);
    }

    public PageResultDto<T> listPage(Map<String, String> paramMap) {
        Page<E> page = specExecutor().findAll(
            JpaQuerySupport.specification(paramMap), JpaQuerySupport.pageable(paramMap));
        List<T> result = page.getContent().stream().map(this::toModel).collect(Collectors.toList());
        return JpaQuerySupport.toPageResult(page, result);
    }

    /**
     * 兼容承继存储框架的 {@code listPage(Entity, Page)} 分页契约（含排序）。
     */
    public PageResultDto<T> listPage(io.voyager1.core.db.Entity where, Pageable pageable) {
        Page<E> result = specExecutor().findAll(JpaQuerySupport.specificationFromEntity(where), pageable);
        List<T> models = result.getContent().stream().map(this::toModel).collect(Collectors.toList());
        return JpaQuerySupport.toPageResult(result, models);
    }

    @Transactional
    public int delByKey(List<String> ids) {
        return this.delByKey((Object) ids, null);
    }

    @Transactional
    public int delByKey(Object keyValue, Consumer<io.voyager1.core.db.Entity> consumer) {
        io.voyager1.core.db.Entity where = new io.voyager1.core.db.Entity();
        if (keyValue != null) {
            where.set("id", keyValue);
        }
        if (consumer != null) {
            consumer.accept(where);
        }
        if (where.isEmpty()) {
            return 0;
        }
        List<E> toDelete = specExecutor().findAll(JpaQuerySupport.specificationFromEntity(where));
        repository().deleteAll(toDelete);
        return toDelete.size();
    }

    /**
     * 按条件删除（复刻 BaseDbCommonService.del(Entity) 契约）。
     */
    @Transactional
    public int del(io.voyager1.core.db.Entity where) {
        if (where == null || where.isEmpty()) {
            return 0;
        }
        List<E> toDelete = specExecutor().findAll(JpaQuerySupport.specificationFromEntity(where));
        repository().deleteAll(toDelete);
        return toDelete.size();
    }

    /**
     * 按工作空间删除（复刻 BaseWorkspaceService.delByWorkspace 契约）。
     */
    @Transactional
    public int delByWorkspace(HttpServletRequest request, Consumer<io.voyager1.core.db.Entity> consumer) {
        String workspace = this.getCheckUserWorkspace(request);
        io.voyager1.core.db.Entity where = new io.voyager1.core.db.Entity();
        where.set("workspaceId", workspace);
        if (consumer != null) {
            consumer.accept(where);
        }
        if (where.isEmpty()) {
            return 0;
        }
        List<E> toDelete = specExecutor().findAll(JpaQuerySupport.specificationFromEntity(where));
        repository().deleteAll(toDelete);
        return toDelete.size();
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

    /**
     * 自动清理数据接口（复刻 BaseDbService.autoLoopClear：保留最近 maxCount 条，其余删除）。
     */
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
                List<String> ids = pageResult.getResult().stream().filter(predicate).map(BaseWorkspaceModel::getId).collect(Collectors.toList());
                this.delByKey(ids, null);
            }
        });
    }

    /**
     * 插入前填充钩子（复刻 BaseDbService.fillInsert：默认字段/工作空间绑定等）。
     */
    protected void fillInsert(T model) {
    }

    public T getByKey(String id, String workspaceId) {
        E entity = repository().findById(id).orElse(null);
        if (entity == null || !java.util.Objects.equals(entity.getWorkspaceId(), workspaceId)) {
            return null;
        }
        return toModel(entity);
    }

    /**
     * 查询结果回填钩子（复刻 BaseDbService.fillSelectResult：脱敏/填充临时字段等）。
     */
    protected void copyProperties(Object src, Object dst) {
        BeanWrapperImpl srcW = new BeanWrapperImpl(src);
        BeanWrapperImpl dstW = new BeanWrapperImpl(dst);
        java.util.List<String> nullNames = new java.util.ArrayList<>();
        for (PropertyDescriptor pd : srcW.getPropertyDescriptors()) {
            String name = pd.getName();
            if ("class".equals(name)) continue;
            if (srcW.getPropertyValue(name) == null) nullNames.add(name);
        }
        BeanUtils.copyProperties(src, dst, nullNames.toArray(new String[0]));
        for (PropertyDescriptor pd : dstW.getPropertyDescriptors()) {
            String name = pd.getName();
            if ("class".equals(name)) continue;
            Class<?> srcType = srcW.getPropertyType(name);
            Class<?> dstType = pd.getPropertyType();
            if (srcType == null || dstType == null) continue;
            boolean toInteger = srcType == Boolean.class && dstType == Integer.class;
            boolean toBoolean = srcType == Integer.class && dstType == Boolean.class;
            if (toInteger || toBoolean) {
                Object v = srcW.getPropertyValue(name);
                if (v != null) {
                    dstW.setPropertyValue(name, toInteger ? (((Boolean) v) ? 1 : 0) : (((Integer) v) != 0));
                }
            } else if (dstType == String.class && Number.class.isAssignableFrom(srcType)) {
                Object v = srcW.getPropertyValue(name);
                if (v != null) dstW.setPropertyValue(name, v.toString());
            } else if (srcType == String.class && Number.class.isAssignableFrom(dstType)) {
                Object v = srcW.getPropertyValue(name);
                if (v != null && !v.toString().isEmpty()) {
                    try {
                        if (dstType == Long.class) dstW.setPropertyValue(name, Long.parseLong(v.toString()));
                        else if (dstType == Integer.class) dstW.setPropertyValue(name, Integer.parseInt(v.toString()));
                        else if (dstType == Double.class) dstW.setPropertyValue(name, Double.parseDouble(v.toString()));
                        else if (dstType == Float.class) dstW.setPropertyValue(name, Float.parseFloat(v.toString()));
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }
    }

    protected String[] clearTimeColumns() {
        return new String[]{};
    }

    protected void executeClearImpl(int logStorageCount) {
    }

    protected void fillSelectResult(T model) {
    }

    @Transactional
    public void sortToTop(String id, HttpServletRequest request) {
        this.moveSort(id, null, request, true);
    }

    @Transactional
    public void sortMoveUp(String id, String beforeId, HttpServletRequest request) {
        this.moveSort(id, beforeId, request, true);
    }

    @Transactional
    public void sortMoveDown(String id, String afterId, HttpServletRequest request) {
        this.moveSort(id, afterId, request, false);
    }

    private void moveSort(String id, String compareId, HttpServletRequest request, boolean up) {
        String workspaceId = WorkspaceContext.getWorkspaceId(request);
        double base = 1d;
        if (compareId != null && !compareId.isEmpty()) {
            E compare = repository().findById(compareId).orElse(null);
            if (compare != null) {
                Object sv = new BeanWrapperImpl(compare).getPropertyValue("sortValue");
                if (sv instanceof Number) {
                    base = ((Number) sv).doubleValue();
                }
            }
        } else {
            for (E e : specExecutor().findAll(JpaQuerySupport.specification(java.util.Collections.singletonMap("workspaceId", workspaceId)))) {
                Object sv = new BeanWrapperImpl(e).getPropertyValue("sortValue");
                if (sv instanceof Number) {
                    base = Math.max(base, ((Number) sv).doubleValue());
                }
            }
        }
        E target = repository().findById(id).orElse(null);
        if (target != null) {
            double next = up ? base + 0.0001 : base - 0.0001;
            new BeanWrapperImpl(target).setPropertyValue("sortValue", (float) next);
            repository().save(target);
        }
    }

    public Pageable parsePage(Map<String, String> paramMap) {
        int page = ConvertUtil.toInt(paramMap.get("page"), 1);
        int limit = ConvertUtil.toInt(paramMap.get("limit"), 10);
        if (page <= 0) page = 1;
        if (limit <= 0 || limit >= 200) limit = 10;
        return PageRequest.of(page - 1, limit);
    }

    public T queryByBean(T model) {
        List<T> list = this.listByBean(model);
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    public void checkUserWorkspace(String workspaceId) {
        io.voyager1.model.user.UserModel userModel = io.voyager1.common.BaseServerController.getUserByThreadLocal();
        this.checkUserWorkspace(workspaceId, userModel);
    }

    protected void checkUserWorkspace(String workspaceId, io.voyager1.model.user.UserModel userModel) {
        if (userModel == null) {
            return;
        }
        if (java.util.Objects.equals(userModel.getId(), io.voyager1.model.user.UserModel.SYSTEM_ADMIN)) {
            return;
        }
        if (userModel.isSuperSystemUser()) {
            return;
        }
        if (java.util.Objects.equals(workspaceId, io.voyager1.common.ServerConst.WORKSPACE_GLOBAL)) {
            return;
        }
        io.voyager1.service.user.UserBindWorkspaceService ubw =
            io.voyager1.common.SpringContextHolder.getBean(io.voyager1.service.user.UserBindWorkspaceService.class);
        if (!ubw.exists(userModel, workspaceId)) {
            throw new io.voyager1.exception.PermissionException("没有对应的工作空间权限");
        }
    }

    public List<String> listGroup(HttpServletRequest request) {
        String workspaceId = WorkspaceContext.getWorkspaceId(request);
        Map<String, String> pm = new java.util.HashMap<>();
        pm.put("workspaceId", workspaceId);
        return specExecutor().findAll(JpaQuerySupport.specification(pm)).stream()
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

    /**
     * 表名（来自模型 {@code @TableName} 注解），供 ITriggerToken 等契约复用。
     */
    public String getTableName() {
        io.voyager1.core.db.TableName annotation = modelClass().getAnnotation(io.voyager1.core.db.TableName.class);
        return annotation == null ? null : annotation.value();
    }

    /**
     * 触发器 token 类型名（等价于旧框架 {@code getTableName()}）。
     */
    public String typeName() {
        return getTableName();
    }

    /**
     * 数据描述（来自模型 {@code @TableName} 注解的 nameKey 国际化）。
     */
    public String getDataDesc() {
        io.voyager1.core.db.TableName annotation = modelClass().getAnnotation(io.voyager1.core.db.TableName.class);
        org.springframework.util.Assert.notNull(annotation, "请配置 table Name");
        return io.voyager1.common.i18n.I18nMessageUtil.get(annotation.nameKey());
    }
}
