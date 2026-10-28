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

import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.Const;
import io.voyager1.core.entity.AgentAuditEntity;
import io.voyager1.core.jpa.DataService;
import io.voyager1.core.jpa.JpaQuerySupport;
import io.voyager1.core.jpa.WorkspaceContext;
import io.voyager1.core.repository.AgentAuditRepository;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.AgentAuditModel;
import io.voyager1.util.JakartaServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agent 执行审计服务：每次 MCP 工具调用留痕（会话 / 意图 / 工具 / 入参 / 结果 / 耗时）。
 * <p>
 * 审计写入失败**不得**影响工具调用本身，故 {@link #record} 内部吞异常并只打日志。
 *
 * @since 2026/10/27
 */
@Service
@Slf4j
public class AgentAuditService implements DataService<AgentAuditModel> {

    private static final int MAX_TEXT_LENGTH = 8000;

    private final AgentAuditRepository repository;

    public AgentAuditService(AgentAuditRepository repository) {
        this.repository = repository;
    }

    /**
     * 记录一次工具调用。
     *
     * @param agentSessionId  Agent 会话 ID
     * @param intentText      意图原文（可为 null）
     * @param toolName        工具名
     * @param arguments       入参
     * @param result          结果
     * @param success         是否成功
     * @param durationMillis  耗时（毫秒）
     * @param operator        发起人
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String agentSessionId, String intentText, String toolName, JSONObject arguments,
                       JSONObject result, boolean success, long durationMillis, String operator) {
        try {
            long now = System.currentTimeMillis();
            AgentAuditEntity entity = new AgentAuditEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setWorkspaceId(Const.WORKSPACE_DEFAULT_ID);
            entity.setCreateTimeMillis(now);
            entity.setModifyTimeMillis(now);
            entity.setAgentSessionId(trim(agentSessionId, 50));
            entity.setIntentText(trim(intentText, MAX_TEXT_LENGTH));
            entity.setToolName(trim(toolName, 50));
            entity.setArguments(trim(arguments == null ? null : arguments.toJSONString(), MAX_TEXT_LENGTH));
            entity.setResult(trim(result == null ? null : result.toJSONString(), MAX_TEXT_LENGTH));
            entity.setSuccess(success ? 1 : 0);
            entity.setDurationMillis(durationMillis);
            entity.setOperator(trim(operator, 50));
            repository.save(entity);
            log.debug("Agent 审计: session={} tool={} success={} {}ms",
                agentSessionId, toolName, success, durationMillis);
        } catch (Exception e) {
            // 审计失败不影响工具调用
            log.warn("Agent 审计写入失败: tool={} {}", toolName, e.getMessage());
        }
    }

    @Override
    public AgentAuditModel getByKey(String id) {
        AgentAuditEntity entity = repository.findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    /**
     * 按会话查询审计轨迹（时间正序）。
     */
    public List<AgentAuditModel> listBySession(String agentSessionId) {
        return repository.findByAgentSessionIdOrderByCreateTimeMillisAsc(agentSessionId)
            .stream().map(this::toModel).collect(Collectors.toList());
    }

    public PageResultDto<AgentAuditModel> listPage(HttpServletRequest request) {
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        paramMap.put("workspaceId", WorkspaceContext.getWorkspaceId(request));
        return this.listPage(paramMap);
    }

    public PageResultDto<AgentAuditModel> listPage(Map<String, String> paramMap) {
        Page<AgentAuditEntity> page = repository.findAll(
            JpaQuerySupport.specification(paramMap), JpaQuerySupport.pageable(paramMap));
        List<AgentAuditModel> result = page.getContent().stream().map(this::toModel).collect(Collectors.toList());
        return JpaQuerySupport.toPageResult(page, result);
    }

    private AgentAuditModel toModel(AgentAuditEntity entity) {
        AgentAuditModel model = AgentAuditModel.builder()
            .agentSessionId(entity.getAgentSessionId())
            .intentText(entity.getIntentText())
            .toolName(entity.getToolName())
            .arguments(entity.getArguments())
            .result(entity.getResult())
            .success(entity.getSuccess())
            .durationMillis(entity.getDurationMillis())
            .operator(entity.getOperator())
            .build();
        model.setId(entity.getId());
        model.setWorkspaceId(entity.getWorkspaceId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        return model;
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
