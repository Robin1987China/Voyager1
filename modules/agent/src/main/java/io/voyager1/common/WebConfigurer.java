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

import io.voyager1.common.interceptor.AuthorizeInterceptor;
import io.voyager1.common.validator.ParameterInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @since 2022/12/8
 */
@Configuration
public class WebConfigurer implements WebMvcConfigurer {

    private final ParameterInterceptor parameterInterceptor;
    private final AuthorizeInterceptor authorizeInterceptor;

    public WebConfigurer(ParameterInterceptor parameterInterceptor,
                         AuthorizeInterceptor authorizeInterceptor) {
        this.parameterInterceptor = parameterInterceptor;
        this.authorizeInterceptor = authorizeInterceptor;
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(parameterInterceptor).addPathPatterns("/**");
        registry.addInterceptor(authorizeInterceptor).addPathPatterns("/**");
    }


}
