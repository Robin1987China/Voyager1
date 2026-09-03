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
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.oauth2.BaseOauth2Config;

/**
 * @since 2024/12/3
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class CustomOauth2Config extends BaseOauth2Config {

    private String authorizationUri;
    private String accessTokenUri;
    private String userInfoUri;


    /**
     * 验证数据
     */
    public void check() {
        super.check();
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.authorizationUri, "请配置正确的授权 url");
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.accessTokenUri, "请配置正确的令牌 url");
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.userInfoUri, "请配置正确的用户信息 url");
    }
}
