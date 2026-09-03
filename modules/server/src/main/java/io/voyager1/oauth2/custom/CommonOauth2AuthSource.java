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

package io.voyager1.oauth2.custom;

import me.zhyd.oauth.config.AuthSource;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.oauth2.platform.CustomOauth2Config;
import org.springframework.util.Assert;

/**
 */
public abstract class CommonOauth2AuthSource implements AuthSource {

    private final CustomOauth2Config oauthConfig;

    public CommonOauth2AuthSource(CustomOauth2Config oauthConfig) {
        this.oauthConfig = oauthConfig;
    }

    @Override
    public String authorize() {
        Assert.notNull(oauthConfig, "未配置 oauth2");
        return oauthConfig.getAuthorizationUri();
    }

    @Override
    public String accessToken() {
        Assert.notNull(oauthConfig, "未配置 oauth2");
        return oauthConfig.getAccessTokenUri();
    }

    @Override
    public String userInfo() {
        Assert.notNull(oauthConfig, "未配置 oauth2");
        return oauthConfig.getUserInfoUri();
    }
}
