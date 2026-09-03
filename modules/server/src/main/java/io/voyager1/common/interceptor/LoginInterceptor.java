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
import io.voyager1.common.SpringContextHolder;
import io.voyager1.util.JWT;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerConst;
import io.voyager1.common.ServerOpenApi;
import io.voyager1.configuration.UserConfig;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.user.UserService;
import io.voyager1.system.ServerConfig;
import io.voyager1.util.JwtUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 登录拦截器
 *
 * @since 2017/2/4.
 */
@Configuration
public class LoginInterceptor implements HandlerMethodInterceptor {
    /**
     * session
     */
    public static final String SESSION_NAME = "user";

    private static final Map<Integer, Supplier<String>> MSG_CACHE = new HashMap<>(3);

    private final UserConfig userConfig;

    static {
        MSG_CACHE.put(ServerConst.AUTHORIZE_TIME_OUT_CODE, ServerConst.LOGIN_TIP);
        MSG_CACHE.put(ServerConst.RENEWAL_AUTHORIZE_CODE, ServerConst.LOGIN_TIP);
        MSG_CACHE.put(ServerConst.ACCOUNT_LOCKED, ServerConst.ACCOUNT_LOCKED_TIP);
    }

    public LoginInterceptor(ServerConfig serverConfig) {
        this.userConfig = serverConfig.getUser();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
        HttpSession session = request.getSession();
        //
        NotLogin notLogin = handlerMethod.getMethodAnnotation(NotLogin.class);
        if (notLogin == null) {
            notLogin = handlerMethod.getBeanType().getAnnotation(NotLogin.class);
        }
        if (notLogin == null) {
            // 这里需要判断请求头里是否有 Authorization 属性
            String authorization = request.getHeader(ServerOpenApi.HTTP_HEAD_AUTHORIZATION);
            if ((authorization != null && !authorization.isEmpty())) {
                // jwt token 检测机制
                int code = this.checkHeaderUser(request, session);
                if (code > 0) {
                    this.responseLogin(request, session, response, code);
                    return false;
                }
            } else {
                // 老版本登录拦截
                int code = this.tryGetHeaderUser(request, session);
                if (code > 0) {
                    this.responseLogin(request, session, response, ServerConst.AUTHORIZE_TIME_OUT_CODE);
                    return false;
                }
            }
        }
        //
        return true;
    }

    /**
     * 尝试获取 header 中的信息
     *
     * @param session ses
     * @param request req
     * @return true 获取成功
     */
    private int checkHeaderUser(HttpServletRequest request, HttpSession session) {
        String token = request.getHeader(ServerOpenApi.HTTP_HEAD_AUTHORIZATION);
        if ((token == null || token.isEmpty())) {
            return ServerConst.AUTHORIZE_TIME_OUT_CODE;
        }
        JWT jwt = JwtUtil.readBody(token);
        if (JwtUtil.expired(jwt, 0)) {
            int renewal = userConfig.getTokenRenewal();
            if (jwt == null || renewal <= 0 || JwtUtil.expired(jwt, TimeUnit.MINUTES.toSeconds(renewal))) {
                return ServerConst.AUTHORIZE_TIME_OUT_CODE;
            }
            return ServerConst.RENEWAL_AUTHORIZE_CODE;
        }
        UserModel user = (UserModel) session.getAttribute(SESSION_NAME);
        UserService userService = SpringContextHolder.getBean(UserService.class);
        String id = JwtUtil.getId(jwt);
        UserModel newUser = userService.checkUser(id);
        if (newUser == null) {
            return ServerConst.AUTHORIZE_TIME_OUT_CODE;
        }
        if (null != user) {
            String tokenUserId = JwtUtil.readUserId(jwt);
            boolean b = user.getId().equals(tokenUserId);
            if (!b) {
                return ServerConst.AUTHORIZE_TIME_OUT_CODE;
            }
        }
        if (newUser.getStatus() != null && newUser.getStatus() == 0) {
            // 账号禁用
            return ServerConst.ACCOUNT_LOCKED;
        }
        session.setAttribute(LoginInterceptor.SESSION_NAME, newUser);
        return 0;
    }


    /**
     * 尝试获取 header 中的信息
     *
     * @param session ses
     * @param request req
     * @return 状态码
     */
    private int tryGetHeaderUser(HttpServletRequest request, HttpSession session) {
        String header = request.getHeader(ServerOpenApi.USER_TOKEN_HEAD);
        if ((header == null || header.isEmpty())) {
            // 兼容就版本 登录状态 （下载功能需要使用到 session 的登录状态）
            UserModel user = (UserModel) session.getAttribute(SESSION_NAME);
            return user != null ? 0 : ServerConst.AUTHORIZE_TIME_OUT_CODE;
        }
        UserService userService = SpringContextHolder.getBean(UserService.class);
        UserModel userModel = userService.checkUser(header);
        if (userModel == null) {
            return ServerConst.AUTHORIZE_TIME_OUT_CODE;
        }
        if (userModel.getStatus() != null && userModel.getStatus() == 0) {
            // 账号禁用
            return ServerConst.ACCOUNT_LOCKED;
        }
        session.setAttribute(LoginInterceptor.SESSION_NAME, userModel);
        return 0;
    }

    /**
     * 提示登录
     *
     * @param request  req
     * @param session  回话
     * @param response res
     * @throws IOException 异常
     */
    private void responseLogin(HttpServletRequest request, HttpSession session, HttpServletResponse response, int code) throws IOException {
        session.removeAttribute(LoginInterceptor.SESSION_NAME);
        Supplier<String> msg = MSG_CACHE.getOrDefault(code, ServerConst.LOGIN_TIP);
        JakartaServletUtil.write(response, ApiResult.getString(code, msg.get()), MediaType.APPLICATION_JSON_VALUE);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BaseServerController.removeAll();
    }
}
