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

package io.voyager1.common.interceptor;

import io.voyager1.util.StrUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.Const;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.AgentAuthorize;
import io.voyager1.configuration.AgentConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 授权拦截
 *
 * @since 2019/4/17
 */
@Configuration
public class AuthorizeInterceptor implements HandlerMethodInterceptor {

    private final AgentAuthorize agentAuthorize;

    public AuthorizeInterceptor(AgentConfig agentConfig) {
        this.agentAuthorize = agentConfig.getAuthorize();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
        NotAuthorize notAuthorize = handlerMethod.getMethodAnnotation(NotAuthorize.class);
        if (notAuthorize == null) {
            String authorize = JakartaServletUtil.getHeaderIgnoreCase(request, Const.VOYAGER1_AGENT_AUTHORIZE);
            if ((authorize == null || authorize.isEmpty())) {
                this.error(response);
                return false;
            }
            // 新签名令牌优先，旧 sha1 回退（过渡期）
            if (!agentAuthorize.checkSignedToken(authorize) && !agentAuthorize.checkAuthorize(authorize)) {
                this.error(response);
                return false;
            }
        }
        return true;
    }

    private void error(HttpServletResponse response) {
        JakartaServletUtil.write(response, ApiResult.getString(Const.AUTHORIZE_ERROR, "授权信息错误"), MediaType.APPLICATION_JSON_VALUE);
    }
}
