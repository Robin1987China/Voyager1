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

package io.voyager1.transport;

import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * HTTP 请求封装，{@code io.voyager1.util.HttpRequest} 的核心能力。
 * 基于 {@link java.net.http.HttpClient} 实现。
 */
public class HttpRequest {

    /**
     * HTTP 方法
     */
    public enum Method {
        GET, POST, PUT, DELETE, PATCH
    }

    private String url;
    private Method method = Method.POST;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private String jsonBody;
    private Map<String, Object> formData;
    private int timeout = -1;
    private boolean followRedirects = false;
    private ProxySelector proxySelector;

    private HttpRequest(String url) {
        this.url = url;
    }

    public static HttpRequest of(String url) {
        return new HttpRequest(url);
    }

    public static HttpRequest of(io.voyager1.util.UrlBuilder urlBuilder) {
        return new HttpRequest(urlBuilder.build());
    }

    public HttpRequest setMethod(Method method) {
        this.method = method;
        return this;
    }

    public HttpRequest header(String name, String value) {
        if (name != null && value != null) {
            headers.put(name, value);
        }
        return this;
    }

    public HttpRequest headerMap(Map<String, String> header, boolean override) {
        if (header != null) {
            if (override) {
                headers.putAll(header);
            } else {
                header.forEach(headers::putIfAbsent);
            }
        }
        return this;
    }

    public HttpRequest form(Map<String, Object> formData) {
        this.formData = formData;
        return this;
    }

    public HttpRequest body(String body, String contentType) {
        this.jsonBody = body;
        if (contentType != null) {
            header("Content-Type", contentType);
        }
        return this;
    }

    public HttpRequest timeout(int milliseconds) {
        this.timeout = milliseconds;
        return this;
    }

    public HttpRequest setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
        return this;
    }

    public HttpRequest setProxy(ProxySelector proxySelector) {
        this.proxySelector = proxySelector;
        return this;
    }

    public HttpRequest setProxy(java.net.Proxy proxy) {
        if (proxy != null && proxy.address() instanceof java.net.InetSocketAddress) {
            this.proxySelector = ProxySelector.of((java.net.InetSocketAddress) proxy.address());
        }
        return this;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, Object> form() {
        return formData;
    }

    private HttpClient buildClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
            .followRedirects(followRedirects ? HttpClient.Redirect.ALWAYS : HttpClient.Redirect.NEVER);
        if (timeout > 0) {
            builder.connectTimeout(Duration.ofMillis(timeout));
        }
        if (proxySelector != null) {
            builder.proxy(proxySelector);
        }
        return builder.build();
    }

    private java.net.http.HttpRequest buildRequest() throws java.io.IOException {
        java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder().uri(URI.create(url));
        headers.forEach(builder::header);
        if (timeout > 0) {
            builder.timeout(Duration.ofMillis(timeout));
        }
        if (jsonBody != null) {
            builder.method(method.name(), java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody));
            return builder.build();
        }
        if (formData != null && !formData.isEmpty()) {
            if (hasFileContent(formData)) {
                // 含文件内容 → multipart/form-data（分片上传/文件分发）
                String boundary = "----Voyager1FormBoundary" + Long.toHexString(System.nanoTime());
                builder.setHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
                builder.method(method.name(), java.net.http.HttpRequest.BodyPublishers.ofByteArray(buildMultipartBody(formData, boundary)));
            } else {
                // 普通表单 → application/x-www-form-urlencoded（接收端依赖该头解析参数）
                builder.setHeader("Content-Type", "application/x-www-form-urlencoded");
                builder.method(method.name(), java.net.http.HttpRequest.BodyPublishers.ofString(buildFormBody(formData)));
            }
            return builder.build();
        }
        builder.method(method.name(), java.net.http.HttpRequest.BodyPublishers.noBody());
        return builder.build();
    }

    private static boolean hasFileContent(Map<String, Object> form) {
        for (Object value : form.values()) {
            if (isFileContent(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFileContent(Object value) {
        return value instanceof byte[] || value instanceof java.io.File
            || value instanceof java.io.InputStream || value instanceof org.springframework.core.io.Resource
            || value instanceof io.voyager1.util.BytesResource;
    }

    private static String buildFormBody(Map<String, Object> form) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : form.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(encode(entry.getKey())).append("=").append(encode(String.valueOf(entry.getValue())));
        }
        return sb.toString();
    }

    private static byte[] buildMultipartBody(Map<String, Object> form, String boundary) throws java.io.IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] crlf = "\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (Map.Entry<String, Object> entry : form.entrySet()) {
            out.write(("--" + boundary).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.write(crlf);
            Object value = entry.getValue();
            if (isFileContent(value)) {
                byte[] bytes = readFileBytes(value);
                String filename = resolveFilename(value, entry.getKey());
                out.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"; filename=\"" + filename + "\"").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.write(crlf);
                out.write("Content-Type: application/octet-stream".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.write(crlf);
                out.write(crlf);
                out.write(bytes);
                out.write(crlf);
            } else {
                out.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.write(crlf);
                out.write(crlf);
                out.write(String.valueOf(value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.write(crlf);
            }
        }
        out.write(("--" + boundary + "--").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.write(crlf);
        return out.toByteArray();
    }

    private static byte[] readFileBytes(Object value) throws java.io.IOException {
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        if (value instanceof java.io.File) {
            return java.nio.file.Files.readAllBytes(((java.io.File) value).toPath());
        }
        if (value instanceof java.io.InputStream) {
            try (java.io.InputStream is = (java.io.InputStream) value) {
                return is.readAllBytes();
            }
        }
        if (value instanceof org.springframework.core.io.Resource) {
            try (java.io.InputStream is = ((org.springframework.core.io.Resource) value).getInputStream()) {
                return is.readAllBytes();
            }
        }
        if (value instanceof io.voyager1.util.BytesResource) {
            return ((io.voyager1.util.BytesResource) value).readBytes();
        }
        return new byte[0];
    }

    private static String resolveFilename(Object value, String fieldName) {
        if (value instanceof java.io.File) {
            return ((java.io.File) value).getName();
        }
        if (value instanceof io.voyager1.util.BytesResource) {
            String name = ((io.voyager1.util.BytesResource) value).getName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        if (value instanceof org.springframework.core.io.Resource) {
            String filename = ((org.springframework.core.io.Resource) value).getFilename();
            if (filename != null && !filename.isEmpty()) {
                return filename;
            }
        }
        return fieldName;
    }

    public Response execute() {
        try {
            HttpResponse<byte[]> response = buildClient().send(buildRequest(), HttpResponse.BodyHandlers.ofByteArray());
            return new Response(response);
        } catch (Exception e) {
            throw new RuntimeException("HTTP 请求失败: " + url, e);
        }
    }

    public <T> T thenFunction(Function<Response, T> function) {
        return function.apply(execute());
    }

    private static String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * HTTP 响应封装
     */
    public static class Response implements AutoCloseable {
        private final HttpResponse<byte[]> response;

        Response(HttpResponse<byte[]> response) {
            this.response = response;
        }

        public int getStatus() {
            return response.statusCode();
        }

        public String body() {
            return new String(response.body(), java.nio.charset.StandardCharsets.UTF_8);
        }

        public byte[] bodyBytes() {
            return response.body();
        }

        public String header(String name) {
            return response.headers().firstValue(name).orElse(null);
        }

        public java.io.InputStream bodyStream() {
            return new java.io.ByteArrayInputStream(response.body());
        }

        @Override
        public void close() {
            // HttpClient 响应无需显式关闭
        }
    }

    /**
     * HTTP 状态码常量
     */
    public static class HttpStatus {
        public static final int HTTP_OK = 200;
    }

    /**
     * 常见请求头常量
     */
    public static class Header {
        public static final String CONTENT_DISPOSITION = "Content-Disposition";
        public static final String CONTENT_TYPE = "Content-Type";
    }
}
