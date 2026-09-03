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

package io.voyager1.webhook;

import io.voyager1.util.StrUtil;
import io.voyager1.util.HttpRequest;
import io.voyager1.util.HttpResponse;
import io.voyager1.util.HttpUtil;
import io.voyager1.plugin.PluginConfig;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.plugin.IDefaultPlugin;

import java.util.Map;

/**
 * 默认到 webhook 实现
 *
 * @since 2021/12/22
 */
@PluginConfig(name = "webhook")
@Slf4j
public class DefaultWebhookPluginImpl implements IDefaultPlugin {

    public enum WebhookEvent {
        /**
         * 构建
         */
        BUILD,
        /**
         * 项目
         */
        PROJECT,
        /**
         * 监控
         */
        MONITOR,
        /**
         * 分发
         */
        DISTRIBUTE,
    }

    @Override
    public Object execute(Object main, Map<String, Object> parameter) {
        String webhook = (main == null ? null : main.toString());
        if ((webhook == null || webhook.isEmpty())) {
            return null;
        }
        Object voyager1WebhookEvent = parameter.remove("VOYAGER1_WEBHOOK_EVENT");
        if (voyager1WebhookEvent instanceof WebhookEvent) {
            WebhookEvent webhookEvent = (WebhookEvent) voyager1WebhookEvent;
            log.debug("webhook event: [{}]{}", webhookEvent, webhook);
        }
        try {
            HttpRequest httpRequest = HttpUtil.createGet(webhook, true);
            httpRequest.form(parameter);
            try (HttpResponse execute = httpRequest.execute()) {
                String body = execute.body();
                log.info(webhook + ":" + body);
                return body;
            }
        } catch (Exception e) {
            log.error("WebHooks 调用错误", e);
            return "WebHooks error:" + e.getMessage();
        }
    }
}
