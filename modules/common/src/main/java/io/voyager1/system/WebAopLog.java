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

package io.voyager1.system;

import io.voyager1.common.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.Optional;

/**
 * 自动记录日志
 */
@Aspect
@Component
@Slf4j
public class WebAopLog {

    private final Collection<AopLogInterface> aopLogInterface;

    public WebAopLog() {
        this.aopLogInterface = SpringContextHolder.getBeansOfType(AopLogInterface.class).values();
    }

    @Pointcut("execution(public * io.voyager1..*.*.controller..*.*(..)) || execution(public * io.voyager1.controller..*.*(..))")
    public void webLog() {
        //
    }

    @Around(value = "webLog()", argNames = "joinPoint")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 接收到请求，记录请求内容
        Object proceed;
        Object logResult = null;
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            // 可能其他方式执行
            return joinPoint.proceed();
        }
        String requestUri = requestAttributes.getRequest().getRequestURI();
        try {
            aopLogInterface.forEach(aopLogInterface -> aopLogInterface.before(joinPoint));
            proceed = joinPoint.proceed();
            logResult = proceed;
            log.debug("{} {}", requestUri, Optional.ofNullable(proceed).orElse(""));
        } catch (Throwable e) {
            // 不用记录异常日志，全局异常拦截里面会记录，此处不用重复记录
            // log.debug("发生异常 {}", requestUri, e);
            logResult = e;
            throw e;
        } finally {
            Object finalLogResult = logResult;
            aopLogInterface.forEach(aopLogInterface -> aopLogInterface.afterReturning(finalLogResult));
        }
        return proceed;
    }
}
