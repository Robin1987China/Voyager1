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

package io.voyager1.service.system;

import io.voyager1.core.jpa.WorkspaceContext;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollStreamUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.common.ServerConst;
import io.voyager1.core.entity.WorkspaceEnvVarEntity;
import io.voyager1.core.jpa.JpaQuerySupport;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.WorkspaceEnvVarRepository;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.WorkspaceEnvVarModel;
import io.voyager1.service.ITriggerToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作空间环境变量。
 * <p>
 * 已从承继存储框架（BaseWorkspaceService）搬家到 JPA（JpaWorkspaceService + WorkspaceEnvVarRepository），对外契约不变。
 *
 * @since 2021/12/10
 */
@Service
public class WorkspaceEnvVarService extends JpaWorkspaceService<WorkspaceEnvVarModel, WorkspaceEnvVarEntity> implements ITriggerToken {

    private final WorkspaceEnvVarRepository workspaceEnvVarRepository;

    public WorkspaceEnvVarService(WorkspaceEnvVarRepository workspaceEnvVarRepository) {
        this.workspaceEnvVarRepository = workspaceEnvVarRepository;
    }

    @Override
    protected JpaRepository<WorkspaceEnvVarEntity, String> repository() {
        return workspaceEnvVarRepository;
    }

    @Override
    protected JpaSpecificationExecutor<WorkspaceEnvVarEntity> specExecutor() {
        return workspaceEnvVarRepository;
    }

    @Override
    protected Class<WorkspaceEnvVarEntity> entityClass() {
        return WorkspaceEnvVarEntity.class;
    }

    @Override
    protected Class<WorkspaceEnvVarModel> modelClass() {
        return WorkspaceEnvVarModel.class;
    }

    /**
     * 获取我所有的空间
     *
     * @param request 请求对象
     * @return page
     */
    @Override
    public PageResultDto<WorkspaceEnvVarModel> listPage(HttpServletRequest request) {
        // 验证工作空间权限
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        String workspaceIds = WorkspaceContext.getWorkspaceId(request);
        List<String> split = Arrays.asList(workspaceIds.split(","));
        for (String workspaceId : split) {
            checkUserWorkspace(workspaceId);
        }
        paramMap.remove("workspaceId");
        paramMap.put("workspaceId:in", workspaceIds);
        return super.listPage(paramMap);
    }

    /**
     * 获取工作空间下面的所有环境变量
     *
     * @param workspaceId 工作空间ID
     * @return map
     */
    public EnvironmentMapBuilder getEnv(String workspaceId) {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("workspaceId:in", workspaceId + "," + ServerConst.WORKSPACE_GLOBAL);
        List<WorkspaceEnvVarModel> list = this.listByParamMap(paramMap);
        Map<String, EnvironmentMapBuilder.Item> map = CollStreamUtil.toMap(list, WorkspaceEnvVarModel::getName, workspaceEnvVarModel -> {
            Integer privacy = workspaceEnvVarModel.getPrivacy();
            return new EnvironmentMapBuilder.Item(workspaceEnvVarModel.getValue(), privacy != null && privacy == 1, false);
        });
        // java.lang.UnsupportedOperationException
        return EnvironmentMapBuilder.builder(map);
    }

    /**
     * 转化 工作空间环境变量
     *
     * @param workspaceId 工作空间
     * @param value       值
     * @return 如果存在值，则返回环境变量值。不存在则返回原始值
     */
    public String convertRefEnvValue(String workspaceId, String value) {
        //  "$ref.wEnv."
        if ((value == null || value.isEmpty()) || !(value != null && value.toLowerCase().startsWith(ServerConst.REF_WORKSPACE_ENV.toLowerCase()))) {
            return value;
        }
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("name", StrUtil.removePrefixIgnoreCase(value, ServerConst.REF_WORKSPACE_ENV));
        paramMap.put("workspaceId:in", workspaceId + "," + ServerConst.WORKSPACE_GLOBAL);
        List<WorkspaceEnvVarModel> modelList = this.listByParamMap(paramMap);
        WorkspaceEnvVarModel workspaceEnvVarModel = (modelList == null || modelList.isEmpty() ? null : modelList.get(0));
        if (workspaceEnvVarModel == null) {
            return value;
        }
        return workspaceEnvVarModel.getValue();
    }

    @Override
    public List<WorkspaceEnvVarModel> listByWorkspace(HttpServletRequest request) {
        String workspaceIds = WorkspaceContext.getWorkspaceId(request);
        List<String> split = ConvertUtil.splitTrim(workspaceIds, ",");
        for (String workspaceId : split) {
            checkUserWorkspace(workspaceId);
        }
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("workspaceId:in", workspaceIds);
        return this.listByParamMap(paramMap);
    }

    private List<WorkspaceEnvVarModel> listByParamMap(Map<String, String> paramMap) {
        return specExecutor().findAll(JpaQuerySupport.specification(paramMap))
            .stream().map(this::toModel).collect(Collectors.toList());
    }
}
