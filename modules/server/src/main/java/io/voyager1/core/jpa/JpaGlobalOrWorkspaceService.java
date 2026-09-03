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

import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerConst;
import io.voyager1.model.BaseWorkspaceModel;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.user.UserModel;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.JakartaServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 支持「全局共享 + 工作空间」的 JPA 服务基类（清洁室实现，取代 BaseGlobalOrWorkspaceService 持久层）。
 * <p>
 * 全局数据（workspaceId = WORKSPACE_GLOBAL）对所有工作空间可见；普通数据按当前工作空间隔离。
 */
public abstract class JpaGlobalOrWorkspaceService<T extends BaseWorkspaceModel, E extends WorkspaceEntity>
    extends JpaWorkspaceService<T, E> {

    @Override
    public T getByKey(String id, HttpServletRequest request) {
        String workspaceId = WorkspaceContext.getWorkspaceId(request);
        E entity = repository().findById(id).orElse(null);
        if (entity == null) {
            return null;
        }
        List<String> allowed = Arrays.asList(workspaceId, ServerConst.WORKSPACE_GLOBAL);
        if (!allowed.contains(entity.getWorkspaceId())) {
            return null;
        }
        return toModel(entity);
    }

    @Override
    public List<T> listByWorkspace(HttpServletRequest request) {
        String workspaceId = WorkspaceContext.getWorkspaceId(request);
        Map<String, String> pm = new HashMap<>();
        pm.put("workspaceId:in", workspaceId + "," + ServerConst.WORKSPACE_GLOBAL);
        return specExecutor().findAll(JpaQuerySupport.specification(pm))
            .stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public PageResultDto<T> listPage(HttpServletRequest request) {
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        String workspaceId = WorkspaceContext.getWorkspaceId(request);
        paramMap.put("workspaceId:in", workspaceId + "," + ServerConst.WORKSPACE_GLOBAL);
        return this.listPage(paramMap);
    }

    public T getByKeyAndGlobal(String keyValue, HttpServletRequest request) {
        return this.getByKeyAndGlobal(keyValue, request, "数据不存在");
    }

    public T getByKeyAndGlobal(String keyValue, HttpServletRequest request, String errorMsg) {
        T byKey = this.getByKey(keyValue, request);
        Assert.notNull(byKey, errorMsg);
        UserModel userModel = BaseServerController.getUserByThreadLocal();
        Assert.notNull(userModel, "当前未登录不能操作此数据");
        if (java.util.Objects.equals(byKey.getWorkspaceId(), ServerConst.WORKSPACE_GLOBAL) && !userModel.checkSystemUser()) {
            Assert.state(java.util.Objects.equals(userModel.getId(), byKey.getCreateUser()), "没有当前数据权限,需要管理员或者数据创建人才操作该数据");
        }
        return byKey;
    }

    public String covertGlobalWorkspace(HttpServletRequest request) {
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        boolean global = ConvertUtil.toBool(paramMap.get("global"), false);
        return global ? ServerConst.WORKSPACE_GLOBAL : WorkspaceContext.getWorkspaceId(request);
    }

    @Override
    @Transactional
    public void updateById(T model, HttpServletRequest request) {
        String workspaceId = WorkspaceContext.getWorkspaceId(request);
        E entity = repository().findById(model.getId()).orElse(null);
        if (entity == null) {
            return;
        }
        List<String> allowed = Arrays.asList(workspaceId, ServerConst.WORKSPACE_GLOBAL);
        if (!allowed.contains(entity.getWorkspaceId())) {
            return;
        }
        model.setModifyTimeMillis(System.currentTimeMillis());
        this.copyProperties(model, entity);
        repository().save(entity);
    }

    @Override
    @Transactional
    public int delByWorkspace(HttpServletRequest request, Consumer<io.voyager1.core.db.Entity> consumer) {
        String workspaceId = this.getCheckUserWorkspace(request);
        io.voyager1.core.db.Entity where = new io.voyager1.core.db.Entity();
        where.set("workspaceId", Arrays.asList(workspaceId, ServerConst.WORKSPACE_GLOBAL));
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
}
