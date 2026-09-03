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

package io.voyager1.system.db;

import io.voyager1.util.StrUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.event.ICacheTask;
import io.voyager1.model.BaseIdModel;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.ILoadEvent;
import io.voyager1.common.ServerConst;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseWorkspaceModel;
import io.voyager1.model.data.WorkspaceModel;
import io.voyager1.core.jpa.JpaNodeService;
import io.voyager1.service.IStatusRecover;
import io.voyager1.service.system.WorkspaceService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据库初始化完成后
 *
 * @since 2023/2/18
 */
@Configuration
@Slf4j
public class DataInitEvent implements ILoadEvent, ICacheTask {

    private final WorkspaceService workspaceService;
    private final Map<String, List<String>> errorWorkspaceTable = new HashMap<>();

    public DataInitEvent(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    public Map<String, List<String>> getErrorWorkspaceTable() {
        return errorWorkspaceTable;
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        // 预置环境
        try {
            SpringContextHolder.getBean(io.voyager1.service.environment.EnvironmentService.class).initDefaultEnvironments();
        } catch (Exception e) {
            log.warn("预置环境初始化失败: {}", e.getMessage());
        }
        // 恢复 Pipeline 定时触发
        try {
            SpringContextHolder.getBean(io.voyager1.service.pipeline.PipelineConfigService.class).restoreCronTriggers();
        } catch (Exception e) {
            log.warn("Pipeline 定时触发恢复失败: {}", e.getMessage());
        }
        // 状态恢复的数据
        Map<String, IStatusRecover> statusRecoverMap = SpringContextHolder.getApplicationContext().getBeansOfType(IStatusRecover.class);
        statusRecoverMap.forEach((name, iCron) -> {
            int count = iCron.statusRecover();
            if (count > 0) {
                log.info("{} 恢复 {} 条异常数据", name, count);
            }
        });
        //  同步项目
        Map<String, JpaNodeService> beansOfType = SpringContextHolder.getApplicationContext().getBeansOfType(JpaNodeService.class);
        for (JpaNodeService<?, ?> value : beansOfType.values()) {
            value.syncAllNode();
        }
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE + 2;
    }


    private void checkErrorWorkspace() {
        errorWorkspaceTable.clear();
        // 判断是否存在关联数据
        Set<String> workspaceIds = this.allowWorkspaceIds();
        Set<Class<?>> classes = BaseWorkspaceModel.allTableClass();
        for (Class<?> aClass : classes) {
            TableName tableName = aClass.getAnnotation(TableName.class);
            int workspaceBind = tableName.workspaceBind();
            if (workspaceBind == 3) {
                // 父级不存在自动删除
                Class<?> parents = tableName.parents();
                Assert.state(parents != Void.class, "表信息配置错误," + aClass);
                //
                TableName tableName1 = parents.getAnnotation(TableName.class);
                Assert.notNull(tableName1, "父级表信息配置错误," + aClass);
            }
            String sql = "select workspaceId,count(1) as allCount from " + tableName.value() + " group by workspaceId";
            List<Entity> query = workspaceService.query(sql);
            for (Entity entity : query) {
                String workspaceId = (String) entity.get("workspaceId");
                long allCount = (long) entity.get("allCount");
                if (workspaceIds.contains(workspaceId)) {
                    continue;
                }
                String format = String.format("表 %s[%s] 存在 %s 条错误工作空间数据 -> %s", I18nMessageUtil.get(tableName.nameKey()), tableName.value(), allCount, workspaceId);
                log.error(format);
                List<String> stringList = errorWorkspaceTable.computeIfAbsent(tableName.value(), s -> new ArrayList<>());
                stringList.add(format);
            }
        }
    }

    public Set<String> allowWorkspaceIds() {
        // 判断是否存在关联数据
        List<WorkspaceModel> list = workspaceService.list();
        Set<String> workspaceIds = Optional.ofNullable(list)
            .map(workspaceModels -> workspaceModels.stream()
                .map(BaseIdModel::getId)
                .collect(Collectors.toSet()))
            .orElse(new HashSet<>());
        // 添加默认的全局工作空间 id
        workspaceIds.add(ServerConst.WORKSPACE_GLOBAL);
        return workspaceIds;
    }

    public void clearErrorWorkspace(String tableName) {
        Assert.state(errorWorkspaceTable.containsKey(tableName), "当前表没有错误数据");
        Set<String> workspaceIds = this.allowWorkspaceIds();
        String sql = "select workspaceId,count(1) as allCount from " + tableName + " group by workspaceId";
        List<Entity> query = workspaceService.query(sql);
        for (Entity entity : query) {
            String workspaceId = (String) entity.get("workspaceId");
            if (workspaceIds.contains(workspaceId)) {
                continue;
            }
            String deleteSql = "delete from " + tableName + " where workspaceId=?";
            int execute = workspaceService.execute(deleteSql, workspaceId);
            log.info("删除表 {} 中 {} 条工作空间id为：{} 的数据", tableName, execute, workspaceId);
        }
        this.checkErrorWorkspace();
    }

    @Override
    public void refreshCache() {
        try {
            checkErrorWorkspace();
        } catch (Exception e) {
            log.error("查询错误的工作空间失败", e);
        }
    }
}
