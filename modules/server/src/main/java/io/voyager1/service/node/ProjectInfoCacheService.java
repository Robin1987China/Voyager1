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

package io.voyager1.service.node;

import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.core.entity.ProjectInfoCacheEntity;
import io.voyager1.core.jpa.JpaNodeService;
import io.voyager1.core.repository.ProjectInfoCacheRepository;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.node.ProjectInfoCacheModel;
import io.voyager1.service.ITriggerToken;
import io.voyager1.service.system.WorkspaceService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @since 2021/12/5
 */
@Service
public class ProjectInfoCacheService extends JpaNodeService<ProjectInfoCacheModel, ProjectInfoCacheEntity> implements ITriggerToken {

    private final ProjectInfoCacheRepository projectInfoCacheRepository;

    public ProjectInfoCacheService(NodeService nodeService,
                                   WorkspaceService workspaceService,
                                   ProjectInfoCacheRepository projectInfoCacheRepository) {
        super(nodeService, workspaceService, "项目");
        this.projectInfoCacheRepository = projectInfoCacheRepository;
    }

    @Override
    protected JpaRepository<ProjectInfoCacheEntity, String> repository() {
        return projectInfoCacheRepository;
    }

    @Override
    protected JpaSpecificationExecutor<ProjectInfoCacheEntity> specExecutor() {
        return projectInfoCacheRepository;
    }

    @Override
    protected Class<ProjectInfoCacheEntity> entityClass() {
        return ProjectInfoCacheEntity.class;
    }

    @Override
    protected Class<ProjectInfoCacheModel> modelClass() {
        return ProjectInfoCacheModel.class;
    }

    /**
     * 查询远端项目
     *
     * @param nodeModel 节点ID
     * @param id        项目ID
     * @return json
     */
    @Override
    public JSONObject getItem(NodeModel nodeModel, String id) {
        ApiResult<JSONObject> request = NodeForward.request(nodeModel, NodeUrl.Manage_GetProjectItem, "id", id);
        return request.getData();
    }

    /**
     * 查询项目是否存在
     *
     * @param workspaceId 工作空间ID
     * @param nodeId      节点id
     * @param id          项目id
     * @return true 存在
     */
    public boolean exists(String workspaceId, String nodeId, String id) {
        ProjectInfoCacheModel projectInfoCacheModel = new ProjectInfoCacheModel();
        projectInfoCacheModel.setWorkspaceId(workspaceId);
        projectInfoCacheModel.setNodeId(nodeId);
        projectInfoCacheModel.setProjectId(id);
        return super.exists(projectInfoCacheModel);
    }

    /**
     * 查询项目是否存在
     *
     * @param nodeId 节点id
     * @param id     项目id
     * @return true 存在
     */
    public boolean exists(String nodeId, String id) {
        NodeModel nodeModel = nodeService.getByKey(nodeId);
        if (nodeModel == null) {
            return false;
        }
        return this.exists(nodeModel.getWorkspaceId(), nodeId, id);
    }

    /**
     * 将响应的数据转为请求的数据
     *
     * @param item 数据
     * @return data
     */
    public JSONObject convertToRequestData(JSONObject item) {

        return item;
    }


    @Override
    public JSONArray getLitDataArray(NodeModel nodeModel) {
        ApiResult<JSONArray> tJsonMessage = NodeForward.request(nodeModel, NodeUrl.Manage_GetProjectInfo, "notStatus", "true");
        return tJsonMessage.getData();
    }

    @Override
    public List<ProjectInfoCacheModel> lonelyDataArray(MachineNodeModel machineNodeModel) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("notStatus", true);
        ApiResult<JSONArray> tJsonMessage = NodeForward.request(machineNodeModel, NodeUrl.Manage_GetProjectInfo, jsonObject);
        return this.checkLonelyDataArray(tJsonMessage.getData(), machineNodeModel.getId());
    }

    @Override
    protected void refreshCacheStat(String nodeId, int dataCount) {
        NodeModel nodeModel = new NodeModel();
        nodeModel.setId(nodeId);
        nodeModel.setVoyager1ProjectCount(dataCount);
        nodeService.updateById(nodeModel);
    }
}
