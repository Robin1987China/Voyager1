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

package io.voyager1.common;

import io.voyager1.core.api.ApiResult;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.exception.AgentAuthorizeException;
import io.voyager1.exception.AgentException;
import io.voyager1.exception.BaseExceptionHandler;
import io.voyager1.exception.PermissionException;
import io.voyager1.transport.TransportAgentException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 全局异常处理
 *
 * @since 2019/04/17
 */
@RestControllerAdvice
@Slf4j
public class ServerExceptionHandler extends BaseExceptionHandler {

    /**
     * 声明要捕获的异常
     *
     * @param e 异常
     */
    @ExceptionHandler({AgentAuthorizeException.class})
    public ApiResult<String> delExceptionHandler(AgentAuthorizeException e) {
        return e.getApiResult();
    }

    /**
     * 插件端异常
     * <p>
     * 避免重复记录堆栈
     *
     * @param request 请求
     * @param e       异常
     * @since 2021-08-01
     */
    @ExceptionHandler({AgentException.class, TransportAgentException.class})
    public ApiResult<String> agentExceptionHandler(HttpServletRequest request, AgentException e) {
        Throwable cause = e.getCause();
        if (cause != null) {
            log.error("controller {}", request.getRequestURI(), cause);
        }
        return new ApiResult<>(405, e.getMessage());
    }

    /**
     * 权限异常 需要退出登录
     *
     * @param e 异常
     * @return json
     */
    @ExceptionHandler({PermissionException.class})
    public ApiResult<String> doPermissionException(PermissionException e) {
        return new ApiResult<>(ServerConst.AUTHORIZE_TIME_OUT_CODE, e.getMessage());
    }
}
