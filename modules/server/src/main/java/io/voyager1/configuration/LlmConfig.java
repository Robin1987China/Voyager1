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
 * LLM 网关配置（OpenAI 兼容协议）。
 * <p>
 * 默认关闭：未配置时意图解析/根因分析走规则版，行为与改造前完全一致。
 *
 * @since 2026/10/27
 */
@Data
@ConfigurationProperties("voyager1.llm")
public class LlmConfig {

    /**
     * 是否启用 LLM（关闭时全部走规则版）。
     */
    private boolean enabled = false;

    /**
     * OpenAI 兼容接口的 base url，例如 https://api.deepseek.com/v1
     */
    private String baseUrl;

    /**
     * API Key（不回显、不落日志）。
     */
    private String apiKey;

    /**
     * 模型名，例如 deepseek-chat / gpt-4o-mini
     */
    private String model = "deepseek-chat";

    /**
     * 请求超时（秒）。
     */
    private int timeoutSeconds = 30;

    /**
     * 是否已具备调用条件。
     */
    public boolean callable() {
        return enabled
            && baseUrl != null && !baseUrl.trim().isEmpty()
            && model != null && !model.trim().isEmpty();
    }
}
