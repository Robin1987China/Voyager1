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

package io.voyager1.service.dblog;

import io.voyager1.util.StrUtil;
import io.voyager1.util.Task;
import io.voyager1.core.db.Entity;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.cron.ICron;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.build.BuildExecuteService;
import io.voyager1.common.BaseServerController;
import io.voyager1.cron.CronUtils;
import io.voyager1.core.entity.BuildInfoEntity;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.BuildInfoRepository;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.enums.BuildReleaseMethod;
import io.voyager1.model.enums.BuildStatus;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.IStatusRecover;
import io.voyager1.service.ITriggerToken;
import io.voyager1.util.StringUtil;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 构建 service 新版本，数据从数据库里面加载
 *
 * @since 2021-08-10
 **/
@Service
@Slf4j
public class BuildInfoService extends JpaWorkspaceService<BuildInfoModel, BuildInfoEntity> implements ICron<BuildInfoModel>, IStatusRecover, ITriggerToken {

    private final BuildInfoRepository buildInfoRepository;
    private final EntityManager entityManager;

    public BuildInfoService(BuildInfoRepository buildInfoRepository, EntityManager entityManager) {
        this.buildInfoRepository = buildInfoRepository;
        this.entityManager = entityManager;
    }

    @Override
    protected JpaRepository<BuildInfoEntity, String> repository() {
        return buildInfoRepository;
    }

    @Override
    protected JpaSpecificationExecutor<BuildInfoEntity> specExecutor() {
        return buildInfoRepository;
    }

    @Override
    protected Class<BuildInfoEntity> entityClass() {
        return BuildInfoEntity.class;
    }

    @Override
    protected Class<BuildInfoModel> modelClass() {
        return BuildInfoModel.class;
    }

    /**
     * 更新状态
     *
     * @param id          ID
     * @param buildStatus to Status
     */
    public void updateStatus(String id, BuildStatus buildStatus, String desc) {
        BuildInfoModel buildInfoModel = new BuildInfoModel();
        buildInfoModel.setId(id);
        buildInfoModel.setStatusMsg(desc);
        buildInfoModel.setStatus(buildStatus.getCode());
        this.updateById(buildInfoModel);
    }

    /**
     * 更新状态
     *
     * @param id            ID
     * @param buildNumberId 构建编号id
     * @param buildStatus   to Status
     */
    public void updateStatus(String id, int buildNumberId, BuildStatus buildStatus, String msg) {

        BuildInfoModel buildInfoModel = new BuildInfoModel();
        buildInfoModel.setId(id);
        buildInfoModel.setBuildId(buildNumberId);
        Entity where = this.dataBeanToEntity(buildInfoModel);
        //
        BuildInfoModel dataModel = new BuildInfoModel();
        dataModel.setStatus(buildStatus.getCode());
        dataModel.setStatusMsg(msg);
        Entity data = this.dataBeanToEntity(dataModel);
        this.update(data, where);
    }

    @Override
    public void insert(BuildInfoModel buildInfoModel) {
        super.insert(buildInfoModel);
        this.checkCron(buildInfoModel);
    }

    @Override
    public void updateById(BuildInfoModel info, HttpServletRequest request) {
        super.updateById(info, request);
        this.checkCron(info);
    }

    @Override
    public int delByKey(String keyValue, HttpServletRequest request) {
        int delByKey = super.delByKey(keyValue, request);
        if (delByKey > 0) {
            String taskId = "build:" + keyValue;
            CronUtils.remove(taskId);
        }
        return delByKey;
    }

    /**
     * 开启定时构建任务
     */
    @Override
    public List<BuildInfoModel> queryStartingList() {
        List<BuildInfoEntity> entities = buildInfoRepository.findByAutoBuildCronIsNotNullAndAutoBuildCronNot("");
        return entities.stream().map(this::toModel).collect(java.util.stream.Collectors.toList());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public int statusRecover() {
        // 恢复异常数据
        jakarta.persistence.Query query = entityManager.createNativeQuery(
            "update CI_BUILD set status=?1 where status=?2 or status=?3 or status=?4");
        query.setParameter(1, BuildStatus.AbnormalShutdown.getCode());
        query.setParameter(2, BuildStatus.Ing.getCode());
        query.setParameter(3, BuildStatus.PubIng.getCode());
        query.setParameter(4, BuildStatus.WaitExec.getCode());
        return query.executeUpdate();
    }

    /**
     * 检查定时任务 状态
     *
     * @param buildInfoModel 构建信息
     */
    @Override
    public boolean checkCron(BuildInfoModel buildInfoModel) {
        String id = buildInfoModel.getId();
        String taskId = "build:" + id;
        String autoBuildCron = buildInfoModel.getAutoBuildCron();
        autoBuildCron = StringUtil.parseCron(autoBuildCron);
        if ((autoBuildCron == null || autoBuildCron.isEmpty())) {
            CronUtils.remove(taskId);
            return false;
        }
        log.debug("start build cron {} {} {}", id, buildInfoModel.getName(), autoBuildCron);
        CronUtils.upsert(taskId, autoBuildCron, new CronTask(id, autoBuildCron));
        return true;
    }

    public List<BuildInfoModel> hasResultKeep() {
        List<BuildInfoEntity> entities = buildInfoRepository.findByResultKeepDayGreaterThan(0);
        return entities.stream().map(this::toModel).collect(java.util.stream.Collectors.toList());
    }

    private static class CronTask implements Task {

        private final String buildId;
        private final String autoBuildCron;

        public CronTask(String buildId, String autoBuildCron) {
            this.buildId = buildId;
            this.autoBuildCron = autoBuildCron;
        }

        @Override
        public void execute() {
            BuildExecuteService buildExecuteService = SpringContextHolder.getBean(BuildExecuteService.class);
            try {
                BaseServerController.resetInfo(UserModel.EMPTY);
                buildExecuteService.start(this.buildId, null, null, 2, "auto build:" + this.autoBuildCron);
            } finally {
                BaseServerController.removeEmpty();
            }
        }
    }


    /**
     * 判断是否存在 节点关联
     *
     * @param nodeId 节点ID
     * @return true 关联
     */
    public boolean checkNode(String nodeId, HttpServletRequest request) {
        Entity entity = new Entity();
        entity.set("releaseMethod", BuildReleaseMethod.Project.getCode());
        String workspaceId = this.getCheckUserWorkspace(request);
        entity.set("workspaceId", workspaceId);
        entity.set("releaseMethodDataId", String.format(" like '%s:%'", nodeId));
        return super.exists(entity);
    }

    /**
     * 判断是否存在 发布关联
     *
     * @param dataId        数据ID
     * @param releaseMethod 发布方法
     * @param request       请求对象
     * @return true 关联
     */
    public boolean checkReleaseMethodByLike(String dataId, HttpServletRequest request, BuildReleaseMethod releaseMethod) {
        Entity entity = new Entity();
        entity.set("releaseMethod", releaseMethod.getCode());
        String workspaceId = this.getCheckUserWorkspace(request);
        entity.set("workspaceId", workspaceId);
        entity.set("releaseMethodDataId", String.format(" like '%%s%'", dataId));
        return super.exists(entity);
    }

    /**
     * 判断是否存在 发布关联
     *
     * @param dataId        数据ID
     * @param releaseMethod 发布方法
     * @return true 关联
     */
    public boolean checkReleaseMethodByLike(String dataId, BuildReleaseMethod releaseMethod) {
        Entity entity = new Entity();
        entity.set("releaseMethod", releaseMethod.getCode());
        entity.set("releaseMethodDataId", String.format(" like '%%s%'", dataId));
        return super.exists(entity);
    }

    /**
     * 判断是否存在 发布关联
     *
     * @param dataId        数据ID
     * @param request       请求对象
     * @param releaseMethod 发布方法
     * @return true 关联
     */
    public boolean checkReleaseMethod(String dataId, HttpServletRequest request, BuildReleaseMethod releaseMethod) {
        BuildInfoModel buildInfoModel = new BuildInfoModel();
        String workspaceId = this.getCheckUserWorkspace(request);
        buildInfoModel.setWorkspaceId(workspaceId);
        buildInfoModel.setReleaseMethodDataId(dataId);
        buildInfoModel.setReleaseMethod(releaseMethod.getCode());
        return super.exists(buildInfoModel);
    }

    /**
     * 查询发布关联的构建
     *
     * @param dataId        数据ID
     * @param request       请求对象
     * @param releaseMethod 发布方法
     * @return list
     */
    public List<BuildInfoModel> listReleaseMethod(String dataId, HttpServletRequest request, BuildReleaseMethod releaseMethod) {
        BuildInfoModel buildInfoModel = new BuildInfoModel();
        String workspaceId = this.getCheckUserWorkspace(request);
        buildInfoModel.setWorkspaceId(workspaceId);
        buildInfoModel.setReleaseMethodDataId(dataId);
        buildInfoModel.setReleaseMethod(releaseMethod.getCode());
        return super.listByBean(buildInfoModel);
    }
}
