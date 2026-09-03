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

import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.util.ContentType;
import io.voyager1.util.Method;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUploadBase;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.transport.BodyRewritingRequestWrapper;
import io.voyager1.common.transport.MultipartRequestWrapper;
import io.voyager1.common.transport.ParameterRequestWrapper;
import io.voyager1.configuration.WebConfig;
import io.voyager1.encrypt.EncryptFactory;
import io.voyager1.encrypt.Encryptor;
import io.voyager1.system.ServerConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @since 2024/2/22
 */
@Configuration
@Slf4j
@Order(1)
public class ServerDecryptionFilter implements Filter {

    private final Encryptor encryptor;

    public ServerDecryptionFilter(ServerConfig serverConfig) {
        Encryptor encryptor1;
        WebConfig config = serverConfig.getWeb();
        String transportEncryption = config.getTransportEncryption();
        transportEncryption = (transportEncryption != null ? transportEncryption : "").toUpperCase();
        switch (transportEncryption) {
            case "NONE":
                encryptor1 = null;
                break;
            case "BASE64":
                try {
                    encryptor1 = EncryptFactory.createEncryptor(1);
                } catch (Exception e) {
                    log.error("获取解密实现失败", e);
                    encryptor1 = null;
                }
                break;
            default:
                log.warn("不支持的编码方式：{}", transportEncryption);
                encryptor1 = null;
                break;
        }
        encryptor = encryptor1;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        if (encryptor == null) {
            chain.doFilter(servletRequest, response);
            return;
        }
        log.debug("当前请求需要解码：{}", encryptor.name());
        String contentType = request.getContentType();
        String method = request.getMethod();
        if (ContentType.isDefault(contentType)) {
            // 普通表单
            HttpServletRequestWrapper wrapper = new ParameterRequestWrapper(request, encryptor);
            chain.doFilter(wrapper, response);
        } else if ((contentType != null && contentType.toLowerCase().startsWith(MediaType.APPLICATION_JSON_VALUE.toLowerCase()))) {
            String body = JakartaServletUtil.getBody(request);
            String temp;
            try {
                temp = encryptor.decrypt(body);
            } catch (Exception e) {
                log.error("解码失败", e);
                temp = body;
            }
            BodyRewritingRequestWrapper requestWrapper = new BodyRewritingRequestWrapper(request, temp.getBytes(StandardCharsets.UTF_8));
            chain.doFilter(requestWrapper, response);
        } else if ((Method.POST.name() != null && Method.POST.name().equalsIgnoreCase(method)) && (contentType != null && contentType.startsWith(FileUploadBase.MULTIPART))) {
            // 文件上传
            HttpServletRequestWrapper wrapper = new MultipartRequestWrapper(request, encryptor);
            chain.doFilter(wrapper, response);
        } else {
            log.warn("当前请求类型不支持解码：{}", contentType);
            chain.doFilter(servletRequest, response);
        }
    }
}
