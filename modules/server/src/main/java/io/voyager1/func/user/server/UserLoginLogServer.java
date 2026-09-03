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

package io.voyager1.func.user.server;

import io.voyager1.core.entity.UserLoginLogEntity;
import io.voyager1.core.jpa.DataService;
import io.voyager1.core.jpa.JpaQuerySupport;
import io.voyager1.core.repository.UserLoginLogRepository;
import io.voyager1.func.user.model.UserLoginLogModel;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.user.UserModel;
import io.voyager1.util.Header;
import io.voyager1.util.JakartaServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户登录日志服务。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA 仓库（UserLoginLogRepository），对外契约不变。
 *
 * @since 2023/3/9
 */
@Service
public class UserLoginLogServer implements DataService<UserLoginLogModel> {

    private final UserLoginLogRepository repository;

    public UserLoginLogServer(UserLoginLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserLoginLogModel getByKey(String id) {
        UserLoginLogEntity entity = repository.findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    public PageResultDto<UserLoginLogModel> listPageByUserId(HttpServletRequest request, String userId) {
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        paramMap.put("modifyUser", userId);
        return this.listPage(paramMap);
    }

    public PageResultDto<UserLoginLogModel> listPage(HttpServletRequest request) {
        return this.listPage(JakartaServletUtil.getParamMap(request));
    }

    public List<UserLoginLogModel> listByModifyUser(String modifyUser, int limit) {
        return repository.findByModifyUserOrderByCreateTimeMillisDesc(modifyUser, PageRequest.of(0, limit))
            .stream().map(this::toModel).collect(Collectors.toList());
    }

    public PageResultDto<UserLoginLogModel> listPage(Map<String, String> paramMap) {
        Page<UserLoginLogEntity> page = repository.findAll(
            JpaQuerySupport.specification(paramMap), JpaQuerySupport.pageable(paramMap));
        List<UserLoginLogModel> result = page.getContent().stream().map(this::toModel).collect(Collectors.toList());
        return JpaQuerySupport.toPageResult(page, result);
    }

    @Transactional
    public void log(UserModel userModel, boolean success, int operateCode, HttpServletRequest request) {
        long now = System.currentTimeMillis();
        UserLoginLogEntity entity = new UserLoginLogEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTimeMillis(now);
        entity.setModifyTimeMillis(now);
        entity.setModifyUser(userModel.getId());
        entity.setUsername(userModel.getName());
        entity.setSuccess(success ? 1 : 0);
        entity.setOperateCode(operateCode);
        entity.setIp(JakartaServletUtil.getClientIP(request));
        entity.setUserAgent(JakartaServletUtil.getHeader(request, Header.USER_AGENT.getValue(), StandardCharsets.UTF_8));
        repository.save(entity);
    }

    public void success(UserModel userModel, int code, HttpServletRequest request) {
        this.log(userModel, true, code, request);
    }

    public void fail(UserModel userModel, int code, HttpServletRequest request) {
        this.log(userModel, false, code, request);
    }

    private UserLoginLogModel toModel(UserLoginLogEntity entity) {
        UserLoginLogModel model = new UserLoginLogModel();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        model.setModifyUser(entity.getModifyUser());
        model.setIp(entity.getIp());
        model.setUserAgent(entity.getUserAgent());
        model.setSuccess(entity.getSuccess() != null && entity.getSuccess() == 1);
        model.setOperateCode(entity.getOperateCode());
        model.setUsername(entity.getUsername());
        return model;
    }
}
