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

package io.voyager1.service.monitor;

import io.voyager1.core.entity.MonitorUserOptEntity;
import io.voyager1.core.jpa.JpaQuerySupport;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.MonitorUserOptRepository;
import io.voyager1.model.data.MonitorUserOptModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.MethodFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 监控用户操作服务。
 */
@Service
public class MonitorUserOptService extends JpaWorkspaceService<MonitorUserOptModel, MonitorUserOptEntity> {

    private final MonitorUserOptRepository repository;

    public MonitorUserOptService(MonitorUserOptRepository repository) {
        this.repository = repository;
    }

    @Override
    protected JpaRepository<MonitorUserOptEntity, String> repository() { return repository; }

    @Override
    protected JpaSpecificationExecutor<MonitorUserOptEntity> specExecutor() { return repository; }

    @Override
    protected Class<MonitorUserOptEntity> entityClass() { return MonitorUserOptEntity.class; }

    @Override
    protected Class<MonitorUserOptModel> modelClass() { return MonitorUserOptModel.class; }

    public List<MonitorUserOptModel> listByType(String workspaceId, ClassFeature classFeature, MethodFeature methodFeature, String userId) {
        List<MonitorUserOptModel> list;
        if ((workspaceId != null && !workspaceId.isEmpty())) {
            Map<String, String> pm = new HashMap<>();
            pm.put("workspaceId", workspaceId);
            list = specExecutor().findAll(JpaQuerySupport.specification(pm))
                .stream().map(this::toModel).collect(Collectors.toList());
        } else {
            list = this.list();
        }
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.stream().filter(monitorUserOptModel -> {
            if (!Boolean.TRUE.equals(monitorUserOptModel.getStatus())) {
                return false;
            }
            List<ClassFeature> classFeatures = monitorUserOptModel.monitorFeature();
            List<MethodFeature> methodFeatures = monitorUserOptModel.monitorOpt();
            boolean b = (classFeatures != null && classFeatures.contains(classFeature))
                && (methodFeatures != null && methodFeatures.contains(methodFeature));
            if (b) {
                List<String> monitorUser = monitorUserOptModel.monitorUser();
                return (monitorUser != null && monitorUser.contains(userId));
            }
            return false;
        }).collect(Collectors.toList());
    }
}
