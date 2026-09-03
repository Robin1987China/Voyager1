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
import me.zhyd.oauth.request.AuthRequest;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.oauth2.custom.TopiamAuthOauth2Request;
import io.voyager1.oauth2.custom.TopiamOauth2AuthSource;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 2024/12/03
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TopiamOauth2Config extends CustomOauth2Config {
    public static final String KEY = "OAUTH_CONFIG_TOPIAM_OAUTH2";


    @Override
    public String provide() {
        return "topiam";
    }

    public AuthRequest authRequest() {
        Assert.state(this.enabled(), String.format("没有开启此 %s oauth2", this.provide()));
        TopiamOauth2AuthSource oauth2AuthSource = new TopiamOauth2AuthSource(this);
        AuthConfig config = this.authConfig();
        List<String> scopes = new ArrayList<>();
        scopes.add("openid");
        scopes.add("email");
        scopes.add("profile");
        config.setScopes(scopes);
        return new TopiamAuthOauth2Request(config, oauth2AuthSource);
    }
}
