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

import io.voyager1.cron.ICron;
import io.voyager1.core.entity.MonitorEntity;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.MonitorRepository;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.cron.CronUtils;
import io.voyager1.model.data.MonitorModel;
import io.voyager1.monitor.MonitorItem;
import io.voyager1.util.StringUtil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 监控管理Service
 *
 */
@Service
@Slf4j
public class MonitorService extends JpaWorkspaceService<MonitorModel, MonitorEntity> implements ICron<MonitorModel> {

    private final MonitorRepository monitorRepository;

    public MonitorService(MonitorRepository monitorRepository) {
        this.monitorRepository = monitorRepository;
    }

    @Override
    protected JpaRepository<MonitorEntity, String> repository() {
        return monitorRepository;
    }

    @Override
    protected JpaSpecificationExecutor<MonitorEntity> specExecutor() {
        return monitorRepository;
    }

    @Override
    protected Class<MonitorEntity> entityClass() {
        return MonitorEntity.class;
    }

    @Override
    protected Class<MonitorModel> modelClass() {
        return MonitorModel.class;
    }

    @Override
    public void insert(MonitorModel monitorModel) {
        super.insert(monitorModel);
        this.checkCron(monitorModel);
    }

    @Override
    public int delByKey(String keyValue, HttpServletRequest request) {
        int i = super.delByKey(keyValue, request);
        if (i > 0) {
            String taskId = "monitor:" + keyValue;
            CronUtils.remove(taskId);
        }
        return i;
    }

    @Override
    public void updateById(MonitorModel info, HttpServletRequest request) {
        super.updateById(info, request);
        this.checkCron(info);
    }

    @Override
    public List<MonitorModel> queryStartingList() {
        // 关闭监听
        MonitorModel monitorModel = new MonitorModel();
        monitorModel.setStatus(true);
        return super.listByBean(monitorModel);
    }

    /**
     * 检查定时任务 状态
     *
     * @param monitorModel 监控信息
     */
    @Override
    public boolean checkCron(MonitorModel monitorModel) {
        String id = monitorModel.getId();
        String taskId = "monitor:" + id;
        String autoExecCron = monitorModel.getExecCron();
        autoExecCron = StringUtil.parseCron(autoExecCron);
        if (!monitorModel.status(autoExecCron)) {
            CronUtils.remove(taskId);
            return false;
        }
        log.debug("start monitor cron {} {} {}", id, monitorModel.getName(), autoExecCron);
        CronUtils.upsert(taskId, autoExecCron, new MonitorItem(id));
        return true;
    }

    /**
     * 设置报警状态
     *
     * @param id    监控id
     * @param alarm 状态
     */
    public void setAlarm(String id, boolean alarm) {
        MonitorModel monitorModel = new MonitorModel();
        monitorModel.setId(id);
        monitorModel.setAlarm(alarm);
        super.updateById(monitorModel);
    }

    /**
     * 判断是否存在对应节点数据
     *
     * @param nodeId 节点id
     * @return true 存在
     */
    public boolean checkNode(String nodeId) {
        List<MonitorModel> list = list();
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (MonitorModel monitorModel : list) {
            List<MonitorModel.NodeProject> projects = monitorModel.projects();
            if (projects != null) {
                for (MonitorModel.NodeProject project : projects) {
                    if (nodeId.equals(project.getNode())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /*public boolean checkProject(String nodeId, String projectId) {
        List<MonitorModel> list = list();
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (MonitorModel monitorModel : list) {
            List<MonitorModel.NodeProject> projects = monitorModel.projects();
            if (projects != null) {
                for (MonitorModel.NodeProject project : projects) {
                    if (project.getNode().equals(nodeId)) {
                        List<String> projects1 = project.getProjects();
                        if (projects1 != null && projects1.contains(projectId)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }*/
}
