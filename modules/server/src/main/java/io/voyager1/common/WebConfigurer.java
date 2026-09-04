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

import io.voyager1.common.interceptor.IpInterceptor;
import io.voyager1.common.interceptor.LoginInterceptor;
import io.voyager1.common.interceptor.PermissionInterceptor;
import io.voyager1.common.validator.ParameterInterceptor;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.CacheControl;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.Resource;
import java.io.IOException;

/**
 * @since 2022/12/8
 */
@Configuration
public class WebConfigurer implements WebMvcConfigurer, WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    @Resource
    private ParameterInterceptor parameterInterceptor;
    @Resource
    private IpInterceptor ipInterceptor;
    @Resource
    private LoginInterceptor loginInterceptor;
    @Resource
    private PermissionInterceptor permissionInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ipInterceptor).excludePathPatterns(ServerOpenApi.API + "**");
        registry.addInterceptor(loginInterceptor).excludePathPatterns(ServerOpenApi.API + "**");
        registry.addInterceptor(parameterInterceptor).addPathPatterns("/**");
        registry.addInterceptor(permissionInterceptor).excludePathPatterns(ServerOpenApi.API + "**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // SPA 静态资源：带内容 hash 的 JS/CSS 允许浏览器缓存（性能），
        // 但设置合理时长并开启协商缓存，避免升级后旧 chunk 长期驻留。
        registry.addResourceHandler("/assets/**")
            .addResourceLocations("classpath:/dist/assets/")
            .setCacheControl(CacheControl.maxAge(7, java.util.concurrent.TimeUnit.DAYS).cachePublic())
            .setCachePeriod(7 * 24 * 3600);
    }

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
        mappings.add("js", "application/javascript;charset=utf-8");
        factory.setMimeMappings(mappings);
    }

    /**
     * SPA 入口（index.html）禁止缓存：JS/CSS 带内容 hash 可长缓存，
     * 但入口必须每次拉新，避免升级后浏览器继续加载旧 bundle 导致页面崩溃/空白。
     */
    @Bean
    public FilterRegistrationBean<Filter> indexHtmlNoCacheFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/", "/index.html");
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
                filterChain.doFilter(request, response);
            }
        });
        return registration;
    }
}
