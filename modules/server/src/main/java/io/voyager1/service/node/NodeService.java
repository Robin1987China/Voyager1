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

import io.voyager1.common.Const;
import io.voyager1.core.entity.NodeEntity;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.NodeRepository;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.data.SshModel;
import io.voyager1.service.node.ssh.SshService;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.Validator;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 节点信息服务。
 */
@Service
@Slf4j
public class NodeService extends JpaWorkspaceService<NodeModel, NodeEntity> {

    private final SshService sshService;
    private final NodeRepository repository;

    @Resource
    @Lazy
    private ProjectInfoCacheService projectInfoCacheService;

    public NodeService(NodeRepository repository, SshService sshService) {
        this.repository = repository;
        this.sshService = sshService;
    }

    @Override
    protected JpaRepository<NodeEntity, String> repository() { return repository; }

    @Override
    protected JpaSpecificationExecutor<NodeEntity> specExecutor() { return repository; }

    @Override
    protected Class<NodeEntity> entityClass() { return NodeEntity.class; }

    @Override
    protected Class<NodeModel> modelClass() { return NodeModel.class; }

    @Override
    protected void fillSelectResult(NodeModel data) {
        if (data != null) {
            data.setLoginPwd(null);
        }
    }

    @Override
    protected void fillInsert(NodeModel nodeModel) {
        nodeModel.setLoginName("");
        nodeModel.setLoginPwd("");
        nodeModel.setProtocol("");
        nodeModel.setUrl("");
    }

    public boolean existsBySshId(String sshId, String workspaceId, String excludeId) {
        for (NodeEntity e : repository.findBySshIdAndWorkspaceId(sshId, workspaceId)) {
            if (excludeId == null || excludeId.isEmpty() || !excludeId.equals(e.getId())) {
                return true;
            }
        }
        return false;
    }

    private NodeModel resolveNode(HttpServletRequest request) {
        NodeModel nodeModel = JakartaServletUtil.toBean(request, NodeModel.class, true);
        String id = nodeModel.getId();
        Assert.hasText(id, "没有节点id");
        Assert.hasText(nodeModel.getName(), "请填写节点名称");
        String checkId = id.replace("-", "_");
        Validator.validateGeneral(checkId, 2, Const.ID_MAX_LEN, "节点id不能为空并且2-50（英文字母 、数字和下划线）");
        Assert.hasText(nodeModel.getName(), "节点名称 不能为空");
        String workspaceId = this.getCheckUserWorkspace(request);
        nodeModel.setWorkspaceId(workspaceId);
        String sshId = nodeModel.getSshId();
        if ((sshId != null && !sshId.isEmpty())) {
            SshModel byKey = sshService.getByKey(sshId, request);
            Assert.notNull(byKey, "对应的 SSH 不存在");
            Assert.state(!this.existsBySshId(sshId, workspaceId, id), "对应的SSH已经被其他节点绑定啦");
        }
        NodeModel update = new NodeModel();
        update.setId(id);
        update.setName(nodeModel.getName());
        update.setGroup(nodeModel.getGroup());
        update.setSshId(nodeModel.getSshId());
        update.setOpenStatus(nodeModel.getOpenStatus());
        // 节点连接信息（来自编辑表单）
        update.setUrl(nodeModel.getUrl());
        update.setLoginName(nodeModel.getLoginName());
        update.setLoginPwd(nodeModel.getLoginPwd());
        update.setProtocol(nodeModel.getProtocol());
        update.setTimeOut(nodeModel.getTimeOut());
        update.setHttpProxy(nodeModel.getHttpProxy());
        update.setHttpProxyType(nodeModel.getHttpProxyType());
        update.setSortValue(nodeModel.getSortValue());
        return update;
    }

    public void update(HttpServletRequest request) {
        NodeModel nodeModel = this.resolveNode(request);
        this.updateById(nodeModel);
        projectInfoCacheService.syncNode(nodeModel);
    }

    public void existsNode(String workspaceId, String machineId) {
        NodeModel where = new NodeModel();
        where.setWorkspaceId(workspaceId);
        where.setMachineId(machineId);
        Assert.isNull(this.queryByBean(where), () -> "对应工作空间已经存在该节点啦");
    }

    public boolean existsNode2(String workspaceId, String machineId) {
        NodeModel where = new NodeModel();
        where.setWorkspaceId(workspaceId);
        where.setMachineId(machineId);
        return this.exists(where);
    }

    public void syncToWorkspace(String ids, String nowWorkspaceId, String workspaceId) {
        io.voyager1.util.ConvertUtil.splitTrim(ids, ",").forEach(id -> {
            NodeModel data = this.getByKey(id, nowWorkspaceId);
            Assert.notNull(data, "没有对应到节点信息");
            this.existsNode(workspaceId, data.getMachineId());
            data.setId(null);
            data.setWorkspaceId(workspaceId);
            data.setCreateTimeMillis(null);
            data.setModifyTimeMillis(null);
            data.setModifyUser(null);
            data.setLoginName(null);
            data.setUrl(null);
            data.setLoginPwd(null);
            data.setProtocol(null);
            data.setHttpProxy(null);
            data.setHttpProxyType(null);
            data.setSshId(null);
            this.insert(data);
        });
    }

    public List<NodeModel> getNodeBySshId(String sshId) {
        NodeModel nodeModel = new NodeModel();
        nodeModel.setSshId(sshId);
        return this.listByBean(nodeModel);
    }

    @Override
    public NodeModel getData(String nodeId, String dataId) {
        return Opt.ofBlankAble(nodeId)
            .map(this::getByKey)
            .orElseGet(() -> Opt.ofBlankAble(dataId)
                .map(this::getByKey)
                .orElse(null)
            );
    }

    @org.springframework.transaction.annotation.Transactional
    public int updateMachineIdByUrl(String machineId, String url) {
        java.util.List<NodeEntity> list = repository.findByUrl(url);
        for (NodeEntity e : list) {
            e.setMachineId(machineId);
            repository.save(e);
        }
        return list.size();
    }

    @org.springframework.transaction.annotation.Transactional
    public int updateProjectScriptCount(String machineId, String workspaceId, int projectCount, int scriptCount) {
        java.util.List<NodeEntity> list = repository.findByMachineIdAndWorkspaceId(machineId, workspaceId);
        for (NodeEntity e : list) {
            e.setVoyager1ProjectCount(projectCount);
            e.setVoyager1ScriptCount(scriptCount);
            repository.save(e);
        }
        return list.size();
    }

    public long countByMachine(String machineId) {
        NodeModel nodeModel = new NodeModel();
        nodeModel.setMachineId(machineId);
        return this.count(nodeModel);
    }
}
