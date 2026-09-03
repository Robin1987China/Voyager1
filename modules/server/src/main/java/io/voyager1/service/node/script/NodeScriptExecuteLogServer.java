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

import io.voyager1.util.CollUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerConst;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.exception.AgentException;
import io.voyager1.core.entity.NodeScriptExecuteLogEntity;
import io.voyager1.core.jpa.JpaNodeService;
import io.voyager1.core.repository.NodeScriptExecuteLogRepository;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.model.BaseDbModel;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.data.WorkspaceModel;
import io.voyager1.model.node.NodeScriptExecuteLogCacheModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.node.NodeService;
import io.voyager1.service.system.WorkspaceService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 脚本默认执行记录
 *
 * @since 2021/12/12
 */
@Service
@Slf4j
public class NodeScriptExecuteLogServer extends JpaNodeService<NodeScriptExecuteLogCacheModel, NodeScriptExecuteLogEntity> {

    private final NodeScriptExecuteLogRepository nodeScriptExecuteLogRepository;

    public NodeScriptExecuteLogServer(NodeService nodeService,
                                      WorkspaceService workspaceService,
                                      NodeScriptExecuteLogRepository nodeScriptExecuteLogRepository) {
        super(nodeService, workspaceService, "脚本模版日志");
        this.nodeScriptExecuteLogRepository = nodeScriptExecuteLogRepository;
    }

    @Override
    protected JpaRepository<NodeScriptExecuteLogEntity, String> repository() {
        return nodeScriptExecuteLogRepository;
    }

    @Override
    protected JpaSpecificationExecutor<NodeScriptExecuteLogEntity> specExecutor() {
        return nodeScriptExecuteLogRepository;
    }

    @Override
    protected Class<NodeScriptExecuteLogEntity> entityClass() {
        return NodeScriptExecuteLogEntity.class;
    }

    @Override
    protected Class<NodeScriptExecuteLogCacheModel> modelClass() {
        return NodeScriptExecuteLogCacheModel.class;
    }

    @Override
    protected String[] clearTimeColumns() {
        return new String[]{"createTimeMillis"};
    }

    @Override
    public JSONObject getItem(NodeModel nodeModel, String id) {
        return null;
    }

    @Override
    public JSONArray getLitDataArray(NodeModel nodeModel) {
        ApiResult<?> jsonMessage = NodeForward.request(nodeModel, NodeUrl.SCRIPT_PULL_EXEC_LOG, "pullCount", 100);
        if (!jsonMessage.success()) {
            throw new AgentException(jsonMessage.toString());
        }
        Object data = jsonMessage.getData();
        //
        JSONArray jsonArray = (JSONArray) JSON.toJSON(data);
        for (Object o : jsonArray) {
            JSONObject jsonObject = (JSONObject) o;
            jsonObject.put("nodeId", nodeModel.getId());
            // 自动
            if (!jsonObject.containsKey("triggerExecType")) {
                jsonObject.put("triggerExecType", 1);
            }
        }
        return jsonArray;
    }

    @Override
    public List<NodeScriptExecuteLogCacheModel> lonelyDataArray(MachineNodeModel machineNodeModel) {
        throw new IllegalStateException("不支持的模式，script log");
    }

    @Override
    public void syncAllNode() {
        //
    }

    /**
     * 同步执行 同步节点信息(增量)
     *
     * @param nodeModel 节点信息
     * @return json
     */
    public Collection<String> syncExecuteNodeInc(NodeModel nodeModel) {
        String nodeModelName = nodeModel.getName();
        if (!nodeModel.isOpenStatus()) {
            log.debug("{} 节点未启用", nodeModelName);
            return null;
        }
        try {
            JSONArray jsonArray = this.getLitDataArray(nodeModel);
            if ((jsonArray == null || jsonArray.isEmpty())) {
                //
                return null;
            }
            //
            List<NodeScriptExecuteLogCacheModel> models = jsonArray.toJavaList(this.tClass)
                .stream()
                .filter(item -> {
                    if (java.util.Objects.equals(item.getWorkspaceId(), ServerConst.WORKSPACE_GLOBAL)) {
                        return true;
                    }
                    // 检查对应的工作空间 是否存在
                    return workspaceService.exists(new WorkspaceModel(item.getWorkspaceId()));
                })
                .filter(item -> {
                    if (java.util.Objects.equals(item.getWorkspaceId(), ServerConst.WORKSPACE_GLOBAL)) {
                        return true;
                    }
                    // 避免重复同步
                    return java.util.Objects.equals(nodeModel.getWorkspaceId(), item.getWorkspaceId());
                })
                .collect(Collectors.toList());
            // 设置 临时缓存，便于放行检查
            BaseServerController.resetInfo(UserModel.EMPTY);
            //
            models.forEach(this::upsert);
            String template = "{} 物理节点拉取到 {} 个执行记录,更新 {} 个执行记录";
            String format = String.format(template, nodeModelName, (jsonArray == null ? 0 : jsonArray.size()), (models == null ? 0 : models.size()));
            log.debug(format);
            return models.stream().map(BaseDbModel::getId).collect(Collectors.toList());
        } catch (Exception e) {
            this.checkException(e, nodeModelName);
            return null;
        } finally {
            BaseServerController.removeEmpty();
        }
    }

    @Override
    protected void executeClearImpl(int h2DbLogStorageCount) {
        super.autoLoopClear("createTimeMillis", h2DbLogStorageCount,
            null,
            executeLogModel -> {
                try {
                    NodeModel nodeModel = nodeService.getByKey(executeLogModel.getNodeId());
                    ApiResult<Object> jsonMessage = NodeForward.request(nodeModel, NodeUrl.SCRIPT_DEL_LOG,
                        "id", executeLogModel.getScriptId(), "executeId", executeLogModel.getId());
                    if (!jsonMessage.success()) {
                        log.warn("{} {} {}", executeLogModel.getNodeId(), executeLogModel.getScriptName(), jsonMessage);
                        return false;
                    }
                    return true;
                } catch (Exception e) {
                    log.error("自动清除数据错误 {} {}", executeLogModel.getNodeId(), executeLogModel.getScriptName(), e);
                    return false;
                }
            });
    }
}
