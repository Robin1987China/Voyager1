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

import io.voyager1.util.RegexPool;
import io.voyager1.util.Validator;
import io.voyager1.util.UrlBuilder;
import io.voyager1.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.zhyd.oauth.config.AuthDefaultSource;
import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.request.AuthDefaultRequest;
import me.zhyd.oauth.request.AuthRequest;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.oauth2.BaseOauth2Config;
import io.voyager1.oauth2.MyAuthGitlabRequest;
import org.springframework.util.Assert;

/**
 * 自建 Gitlab 配置
 *
 * @see AuthDefaultSource#GITLAB
 * @since 2024/04/07
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MyGitlabOauth2Config extends BaseOauth2Config implements AuthSource {

    public static final String KEY = "OAUTH_CONFIG_MYGITLAB_OAUTH2";

    private String host;

    @Override
    public String provide() {
        return "mygitlab";
    }

    @Override
    public AuthRequest authRequest() {
        Assert.state(this.enabled(), String.format("没有开启此 %s oauth2", this.provide()));
        return new MyAuthGitlabRequest(this.authConfig(), this);
    }

    @Override
    public void check() {
        super.check();
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.host, "请配置正确的自建 gitlab 地址");
    }

    /**
     * @return str
     * @see AuthDefaultSource#GITLAB#authorize()
     */
    @Override
    public String authorize() {
        return UrlBuilder.of(this.host).addPath("/oauth/authorize").build();
    }

    @Override
    public String accessToken() {
        // return "https://gitlab.com/oauth/token";
        return UrlBuilder.of(this.host).addPath("/oauth/token").build();
    }

    @Override
    public String userInfo() {
        // "https://gitlab.com/api/v4/user";
        return UrlBuilder.of(this.host).addPath("/api/v4/user").build();
    }

    @Override
    public Class<? extends AuthDefaultRequest> getTargetClass() {
        return MyAuthGitlabRequest.class;
    }
}
