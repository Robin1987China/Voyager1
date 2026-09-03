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

package io.voyager1.oauth2.platform;

import io.voyager1.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.config.AuthDefaultSource;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.request.AuthWeChatEnterpriseQrcodeRequest;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.oauth2.BaseOauth2Config;
import org.springframework.util.Assert;

/**
 * @see AuthDefaultSource#WECHAT_ENTERPRISE
 * @since 2024/04/07
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class WechatEnterpriseOauth2Config extends BaseOauth2Config {

    public static final String KEY = "OAUTH_CONFIG_WECHAT_ENTERPRISE_OAUTH2";
    /**
     * 企业微信，授权方的网页应用ID
     *
     * @since 1.10.0
     */
    private String agentId;

    @Override
    public String provide() {
        return "wechat_enterprise";
    }

    @Override
    public AuthRequest authRequest() {
        Assert.state(this.enabled(), String.format("没有开启此 %s oauth2", this.provide()));
        return new AuthWeChatEnterpriseQrcodeRequest(this.authConfig());
    }

    @Override
    public AuthConfig authConfig() {
        AuthConfig authConfig = super.authConfig();
        authConfig.setAgentId(this.agentId);
        return authConfig;
    }
}
