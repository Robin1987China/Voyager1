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

package io.voyager1.system.init;

import io.voyager1.common.ILoadEvent;
import io.voyager1.cron.CronUtils;
import io.voyager1.service.agent.AgentApprovalService;
import io.voyager1.util.CronUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * 审批超时扫描任务：每分钟把过期未处理的 Agent 待审批单置为「已超时/自动拒绝」。
 * <p>
 * 之前只在有人打开审批列表（{@code listPending}）时才惰性过期，没人看就永远挂着；
 * 该任务让超时拒绝成为系统行为。
 * </p>
 *
 * @since 2026/10/27
 */
@Configuration
@Slf4j
public class AgentApprovalTimeoutTask implements ILoadEvent {

    private static final String TASK_ID = "agent-approval-timeout";

    /**
     * 每分钟第 0 秒执行
     */
    private static final String CRON = "0 * * * * ?";

    private final AgentApprovalService agentApprovalService;

    public AgentApprovalTimeoutTask(AgentApprovalService agentApprovalService) {
        this.agentApprovalService = agentApprovalService;
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) {
        Runnable scan = () -> {
            try {
                agentApprovalService.rejectExpired();
            } catch (Exception e) {
                log.warn("审批超时扫描失败: {}", e.getMessage());
            }
        };
        CronUtil.schedule(TASK_ID, CRON, scan);
        CronUtils.start();
        log.info("审批超时扫描任务已启动: id={} cron={}", TASK_ID, CRON);
    }
}
