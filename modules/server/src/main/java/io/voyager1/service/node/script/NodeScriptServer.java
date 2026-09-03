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

package io.voyager1.service.node.script;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.core.entity.NodeScriptCacheEntity;
import io.voyager1.core.jpa.JpaNodeService;
import io.voyager1.core.repository.NodeScriptCacheRepository;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.node.NodeScriptCacheModel;
import io.voyager1.service.ITriggerToken;
import io.voyager1.service.node.NodeService;
import io.voyager1.service.system.WorkspaceService;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 2019/8/16
 */
@Service
public class NodeScriptServer extends JpaNodeService<NodeScriptCacheModel, NodeScriptCacheEntity> implements ITriggerToken {

    private final NodeScriptCacheRepository nodeScriptCacheRepository;
    private final EntityManager entityManager;

    public NodeScriptServer(NodeService nodeService,
                            WorkspaceService workspaceService,
                            NodeScriptCacheRepository nodeScriptCacheRepository,
                            EntityManager entityManager) {
        super(nodeService, workspaceService, "脚本模版");
        this.nodeScriptCacheRepository = nodeScriptCacheRepository;
        this.entityManager = entityManager;
    }

    @Override
    protected JpaRepository<NodeScriptCacheEntity, String> repository() {
        return nodeScriptCacheRepository;
    }

    @Override
    protected JpaSpecificationExecutor<NodeScriptCacheEntity> specExecutor() {
        return nodeScriptCacheRepository;
    }

    @Override
    protected Class<NodeScriptCacheEntity> entityClass() {
        return NodeScriptCacheEntity.class;
    }

    @Override
    protected Class<NodeScriptCacheModel> modelClass() {
        return NodeScriptCacheModel.class;
    }

    /**
     * 查询操作脚本 模版的节点
     *
     * @return nodeId list
     */
    public List<String> hasScriptNode() {
        jakarta.persistence.Query query = entityManager.createNativeQuery("select nodeId from OPS_SCRIPT group by nodeId");
        List<?> result = query.getResultList();
        if (result == null) {
            return null;
        }
        return result.stream().map(Object::toString).collect(Collectors.toList());
    }

    @Override
    public JSONObject getItem(NodeModel nodeModel, String id) {
        return null;
    }

    @Override
    public JSONArray getLitDataArray(NodeModel nodeModel) {
        return NodeForward.requestData(nodeModel, NodeUrl.Script_List, null, JSONArray.class);
    }

    @Override
    public List<NodeScriptCacheModel> lonelyDataArray(MachineNodeModel machineNodeModel) {
        JSONArray jsonArray = NodeForward.requestData(machineNodeModel, NodeUrl.Script_List, null, JSONArray.class);
        return this.checkLonelyDataArray(jsonArray, machineNodeModel.getId());
    }

    @Override
    protected void refreshCacheStat(String nodeId, int dataCount) {
        NodeModel nodeModel = new NodeModel();
        nodeModel.setId(nodeId);
        nodeModel.setVoyager1ScriptCount(dataCount);
        nodeService.updateById(nodeModel);
    }
}
