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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AIOps 自愈服务（规则版：告警 → 根因 → 修复动作映射）
 *
 * <p>根因分析复用 LLM 网关（后续接入），当前为规则映射。</p>
 *
 * @since 2026/8/25
 */
@Service
@Slf4j
public class SelfHealService {

    /**
     * 告警诊断 + 修复动作建议
     *
     * @param alertType 告警类型（process_down/high_cpu/deploy_failed/...）
     * @param target    告警目标
     * @return 根因 + 修复动作
     */
    public JSONObject diagnose(String alertType, String target) {
        JSONObject result = new JSONObject();
        result.put("alertType", alertType);
        result.put("target", target);
        switch (alertType == null ? "" : alertType.toLowerCase()) {
            case "process_down":
                result.put("rootCause", "进程停止");
                result.put("action", "restart");
                result.put("tool", "ssh.execute");
                result.put("approval", true);
                break;
            case "high_cpu":
                result.put("rootCause", "CPU 使用率过高");
                result.put("action", "scale");
                result.put("tool", "k8s.resourceList");
                result.put("approval", false);
                break;
            case "deploy_failed":
                result.put("rootCause", "部署失败");
                result.put("action", "rollback");
                result.put("tool", "deploy.publish");
                result.put("approval", true);
                break;
            default:
                result.put("rootCause", "未知告警类型，转人工处理");
                result.put("action", "manual");
                result.put("tool", null);
                result.put("approval", true);
        }
        log.info("自愈诊断: alertType={} action={}", alertType, result.get("action"));
        return result;
    }
}
