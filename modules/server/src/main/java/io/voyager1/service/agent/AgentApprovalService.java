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

package io.voyager1.service.agent;

import io.voyager1.core.jpa.WorkspaceContext;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.Const;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.core.entity.AgentApprovalEntity;
import io.voyager1.core.jpa.DataService;
import io.voyager1.core.jpa.JpaQuerySupport;
import io.voyager1.core.repository.AgentApprovalRepository;
import io.voyager1.mcp.McpToolRegistry;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.AgentApprovalModel;
import io.voyager1.util.JakartaServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agent 审批服务（危险工具的人工审批闸门 + 批准后执行）。
 * <p>
 * 已从承继存储框架（BaseWorkspaceService）搬家到 JPA 仓库（AgentApprovalRepository），对外契约不变。
 *
 * @since 2026/8/25
 */
@Service
@Slf4j
public class AgentApprovalService implements DataService<AgentApprovalModel> {

    private static final long DEFAULT_EXPIRE_MILLIS = 30 * 60 * 1000L;

    private final AgentApprovalRepository repository;

    public AgentApprovalService(AgentApprovalRepository repository) {
        this.repository = repository;
    }

    @Override
    public AgentApprovalModel getByKey(String id) {
        AgentApprovalEntity entity = repository.findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    @Transactional
    public String createApproval(String agentSessionId, String toolName, JSONObject args, String operator) {
        long now = System.currentTimeMillis();
        AgentApprovalEntity entity = new AgentApprovalEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setWorkspaceId(Const.WORKSPACE_DEFAULT_ID);
        entity.setCreateTimeMillis(now);
        entity.setModifyTimeMillis(now);
        entity.setAgentSessionId(agentSessionId);
        entity.setToolName(toolName);
        entity.setArguments(args == null ? null : args.toJSONString());
        entity.setStatus(AgentApprovalModel.STATUS_PENDING);
        entity.setOperator(operator);
        entity.setExpireTimeMillis(now + DEFAULT_EXPIRE_MILLIS);
        repository.save(entity);
        log.info("创建 Agent 审批: tool={} id={} operator={}", toolName, entity.getId(), operator);
        return entity.getId();
    }

    @Transactional
    public JSONObject approve(String id, String approver) {
        AgentApprovalModel model = this.getPending(id);
        model.setStatus(AgentApprovalModel.STATUS_APPROVED);
        model.setApprover(approver);
        this.updateById(model);
        String result;
        try {
            Object data = SpringContextHolder.getBean(McpToolRegistry.class).executeTool(
                model.getToolName(), JSON.parseObject(model.getArguments()));
            result = data instanceof String ? (String) data : JSON.toJSONString(data);
        } catch (Exception e) {
            log.error("审批后执行工具失败: {} {}", model.getToolName(), e.getMessage(), e);
            result = "执行失败: " + e.getMessage();
        }
        model.setResult(result);
        model.setRemark("已批准并执行");
        this.updateById(model);
        JSONObject resp = new JSONObject();
        resp.put("approvalId", model.getId());
        resp.put("status", "approved");
        resp.put("result", result);
        return resp;
    }

    @Transactional
    public void reject(String id, String approver, String remark) {
        AgentApprovalModel model = this.getPending(id);
        model.setStatus(AgentApprovalModel.STATUS_REJECTED);
        model.setApprover(approver);
        model.setRemark((remark == null || remark.isEmpty() ? "已拒绝" : remark));
        this.updateById(model);
    }

    private AgentApprovalModel getPending(String id) {
        AgentApprovalModel model = this.getByKey(id);
        Assert.notNull(model, "审批记录不存在: " + id);
        if (model.getStatus() == AgentApprovalModel.STATUS_PENDING
            && model.getExpireTimeMillis() != null
            && model.getExpireTimeMillis() < System.currentTimeMillis()) {
            model.setStatus(AgentApprovalModel.STATUS_EXPIRED);
            model.setRemark("审批超时自动拒绝");
            this.updateById(model);
        }
        Assert.state(model.getStatus() == AgentApprovalModel.STATUS_PENDING,
            "审批已处理（当前状态: " + model.getStatus() + "）");
        return model;
    }

    @Transactional
    public void updateById(AgentApprovalModel model) {
        AgentApprovalEntity entity = repository.findById(model.getId()).orElse(null);
        if (entity == null) {
            return;
        }
        entity.setModifyTimeMillis(System.currentTimeMillis());
        entity.setStatus(model.getStatus());
        entity.setApprover(model.getApprover());
        entity.setResult(model.getResult());
        entity.setRemark(model.getRemark());
        entity.setExpireTimeMillis(model.getExpireTimeMillis());
        repository.save(entity);
    }

    public List<AgentApprovalModel> listPending() {
        List<AgentApprovalModel> list = repository.findByStatusOrderByCreateTimeMillisDesc(AgentApprovalModel.STATUS_PENDING)
            .stream().map(this::toModel).collect(Collectors.toList());
        long now = System.currentTimeMillis();
        for (AgentApprovalModel model : list) {
            if (model.getExpireTimeMillis() != null && model.getExpireTimeMillis() < now) {
                model.setStatus(AgentApprovalModel.STATUS_EXPIRED);
                model.setRemark("审批超时自动拒绝");
                this.updateById(model);
            }
        }
        return list.stream()
            .filter(m -> m.getStatus() == AgentApprovalModel.STATUS_PENDING)
            .collect(Collectors.toList());
    }

    public PageResultDto<AgentApprovalModel> listPage(HttpServletRequest request) {
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        paramMap.put("workspaceId", WorkspaceContext.getWorkspaceId(request));
        return this.listPage(paramMap);
    }

    public PageResultDto<AgentApprovalModel> listPage(Map<String, String> paramMap) {
        Page<AgentApprovalEntity> page = repository.findAll(
            JpaQuerySupport.specification(paramMap), JpaQuerySupport.pageable(paramMap));
        List<AgentApprovalModel> result = page.getContent().stream().map(this::toModel).collect(Collectors.toList());
        return JpaQuerySupport.toPageResult(page, result);
    }

    private AgentApprovalModel toModel(AgentApprovalEntity entity) {
        AgentApprovalModel model = AgentApprovalModel.builder()
            .agentSessionId(entity.getAgentSessionId())
            .toolName(entity.getToolName())
            .arguments(entity.getArguments())
            .status(entity.getStatus())
            .operator(entity.getOperator())
            .approver(entity.getApprover())
            .result(entity.getResult())
            .remark(entity.getRemark())
            .expireTimeMillis(entity.getExpireTimeMillis())
            .build();
        model.setId(entity.getId());
        model.setWorkspaceId(entity.getWorkspaceId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        return model;
    }
}
