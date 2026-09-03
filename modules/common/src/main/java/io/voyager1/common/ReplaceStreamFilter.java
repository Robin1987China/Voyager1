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

import com.alibaba.fastjson2.JSONObject;
import io.voyager1.util.BetweenFormatter;
import io.voyager1.util.Opt;
import io.voyager1.util.StrUtil;
import io.voyager1.util.StringUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * <a href="https://springboot.io/t/topic/3637">https://springboot.io/t/topic/3637</a>
 *
 * @since 2022/12/8
 */
@Configuration
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReplaceStreamFilter implements Filter {


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        long startTime = System.currentTimeMillis();
        ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper((HttpServletRequest) request);
        chain.doFilter(wrapper, response);
        long endTime = System.currentTimeMillis();
        long l = endTime - startTime;
        if (l > 1000 * 5) {
            byte[] contentAsByteArray = wrapper.getContentAsByteArray();
            String str = StrUtil.str(contentAsByteArray, StandardCharsets.UTF_8);
            String reqData = Opt.ofBlankAble(str)
                .map(s -> wrapper.getParameterMap())
                .map(JSONObject::toJSONString)
                .orElse("");
            log.warn("[timeout] {} {} {}", wrapper.getRequestURI(), reqData, StringUtil.formatBetween(l, BetweenFormatter.Level.MILLISECOND));
        }
    }
}
