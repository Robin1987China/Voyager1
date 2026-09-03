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

package io.voyager1.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanWrapperImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servlet 工具，"" {@code io.voyager1.util.JakartaServletUtil}。
 */
public class JakartaServletUtil {

    public static Map<String, String> getParamMap(HttpServletRequest request) {
        Map<String, String> map = new LinkedHashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            map.put(name, request.getParameter(name));
        }
        return map;
    }

    public static String getBody(HttpServletRequest request) {
        try {
            return new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取请求体失败", e);
        }
    }

    public static String getHeader(HttpServletRequest request, String name) {
        return request.getHeader(name);
    }

    public static String getHeader(HttpServletRequest request, String name, java.nio.charset.Charset charset) {
        return request.getHeader(name);
    }

    public static String getHeaderIgnoreCase(HttpServletRequest request, String name) {
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String headerName = names.nextElement();
            if (headerName.equalsIgnoreCase(name)) {
                return request.getHeader(headerName);
            }
        }
        return null;
    }

    public static String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            int idx = ip.indexOf(',');
            return idx < 0 ? ip.trim() : ip.substring(0, idx).trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    public static void write(HttpServletResponse response, String content, String contentType) {
        response.setContentType(contentType);
        try {
            response.getWriter().write(content);
        } catch (IOException e) {
            throw new RuntimeException("写入响应失败", e);
        }
    }

    public static void write(HttpServletResponse response, java.io.File file) {
        try {
            response.getOutputStream().write(java.nio.file.Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new RuntimeException("写入响应失败", e);
        }
    }

    public static void write(HttpServletResponse response, String content) {
        write(response, content, "application/json;charset=UTF-8");
    }

    public static void write(HttpServletResponse response, InputStream in) {
        try {
            OutputStream out = response.getOutputStream();
            in.transferTo(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("写入响应失败", e);
        }
    }

    public static void write(HttpServletResponse response, InputStream in, String contentType) {
        response.setContentType(contentType);
        write(response, in);
    }

    public static void write(HttpServletResponse response, InputStream in, String contentType, String filename) {
        response.setContentType(contentType);
        if (filename != null) {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        }
        write(response, in);
    }

    /**
     * 将请求参数绑定到指定类型的 Bean，"" 的 {@code JakartaServletUtil.toBean}。
     */
    public static <T> T toBean(HttpServletRequest request, Class<T> beanClass, boolean ignoreError) {
        T bean = ReflectUtil.newInstance(beanClass);
        BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (!wrapper.isWritableProperty(name)) {
                continue;
            }
            try {
                String[] values = request.getParameterValues(name);
                if (values == null) {
                    continue;
                }
                Object value;
                if (values.length > 1) {
                    value = values;
                } else {
                    value = values.length > 0 ? values[0] : null;
                }
                wrapper.setPropertyValue(name, value);
            } catch (Exception e) {
                if (!ignoreError) {
                    throw new RuntimeException("绑定请求参数失败: " + name, e);
                }
            }
        }
        return bean;
    }
}
