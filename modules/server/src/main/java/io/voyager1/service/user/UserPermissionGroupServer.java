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

package io.voyager1.service.user;

import io.voyager1.core.entity.UserPermissionGroupEntity;
import io.voyager1.core.jpa.DataService;
import io.voyager1.core.jpa.JpaQuerySupport;
import io.voyager1.core.repository.UserPermissionGroupRepository;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.user.UserPermissionGroupBean;
import io.voyager1.util.JakartaServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户权限组服务。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA 仓库（UserPermissionGroupRepository），对外契约不变。
 *
 * @since 2022/8/3
 */
@Service
public class UserPermissionGroupServer implements DataService<UserPermissionGroupBean> {

    private final UserPermissionGroupRepository repository;

    public UserPermissionGroupServer(UserPermissionGroupRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserPermissionGroupBean getByKey(String id) {
        UserPermissionGroupEntity entity = repository.findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    @Transactional
    public void delByKey(String id) {
        repository.deleteById(id);
    }

    @Transactional
    public void insert(UserPermissionGroupBean bean) {
        long now = System.currentTimeMillis();
        UserPermissionGroupEntity entity = new UserPermissionGroupEntity();
        entity.setId(bean.getId() == null || bean.getId().isEmpty() ? UUID.randomUUID().toString() : bean.getId());
        entity.setCreateTimeMillis(now);
        entity.setModifyTimeMillis(now);
        copyFields(bean, entity);
        repository.save(entity);
        bean.setId(entity.getId());
    }

    @Transactional
    public void updateById(UserPermissionGroupBean bean) {
        UserPermissionGroupEntity entity = repository.findById(bean.getId()).orElse(null);
        if (entity == null) {
            this.insert(bean);
            return;
        }
        entity.setModifyTimeMillis(System.currentTimeMillis());
        copyFields(bean, entity);
        repository.save(entity);
    }

    public List<UserPermissionGroupBean> list() {
        return repository.findAll().stream().map(this::toModel).collect(Collectors.toList());
    }

    public List<UserPermissionGroupBean> listById(Collection<String> ids) {
        return repository.findAllById(ids).stream().map(this::toModel).collect(Collectors.toList());
    }

    public PageResultDto<UserPermissionGroupBean> listPage(HttpServletRequest request) {
        return this.listPage(JakartaServletUtil.getParamMap(request));
    }

    public PageResultDto<UserPermissionGroupBean> listPage(Map<String, String> paramMap) {
        Page<UserPermissionGroupEntity> page = repository.findAll(
            JpaQuerySupport.specification(paramMap), JpaQuerySupport.pageable(paramMap));
        List<UserPermissionGroupBean> result = page.getContent().stream().map(this::toModel).collect(Collectors.toList());
        return JpaQuerySupport.toPageResult(page, result);
    }

    private void copyFields(UserPermissionGroupBean bean, UserPermissionGroupEntity entity) {
        entity.setName(bean.getName());
        entity.setDescription(bean.getDescription());
        entity.setProhibitExecute(bean.getProhibitExecute());
        entity.setAllowExecute(bean.getAllowExecute());
    }

    private UserPermissionGroupBean toModel(UserPermissionGroupEntity entity) {
        UserPermissionGroupBean bean = new UserPermissionGroupBean();
        bean.setId(entity.getId());
        bean.setCreateTimeMillis(entity.getCreateTimeMillis());
        bean.setModifyTimeMillis(entity.getModifyTimeMillis());
        bean.setName(entity.getName());
        bean.setDescription(entity.getDescription());
        bean.setProhibitExecute(entity.getProhibitExecute());
        bean.setAllowExecute(entity.getAllowExecute());
        return bean;
    }
}
