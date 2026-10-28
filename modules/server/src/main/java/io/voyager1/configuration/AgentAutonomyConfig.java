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

package io.voyager1.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Agent 自治行为配置。
 * <p>
 * 说明：{@code modules/agent} 里的同名概念是「插件端进程配置」，与此无关；
 * 这里是 Server 侧对 AI Agent 自治行为的开关与阈值。
 *
 * @since 2026/10/27
 */
@Data
@ConfigurationProperties("voyager1.agent")
public class AgentAutonomyConfig {

    /**
     * 部署失败是否自动回滚（把已提测版本打回，解锁 CI）。默认开启。
     */
    private boolean autoRollbackEnabled = true;

    /**
     * 自愈熔断阈值：同一告警目标连续失败达到该次数即熔断，转人工处理。
     */
    private int selfHealFailureThreshold = 3;

    /**
     * 自愈告警类型白名单（逗号分隔）；不在白名单内的告警只诊断不执行。
     */
    private String selfHealAllowedAlertTypes = "process_down,high_cpu,deploy_failed";

    public int getSelfHealFailureThreshold() {
        return Math.max(this.selfHealFailureThreshold, 1);
    }
}
