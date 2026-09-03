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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * URL 构建工具，"" {@code io.voyager1.util.UrlBuilder} 的常用方法。
 */
public class UrlBuilder {

    private final StringBuilder url;
    private boolean hasQuery;

    private UrlBuilder(String url) {
        this.url = new StringBuilder(url);
        this.hasQuery = url != null && url.contains("?");
    }

    public static UrlBuilder of(String url) {
        return new UrlBuilder(url);
    }

    public static UrlBuilder ofHttp(String url) {
        if (url == null) {
            return new UrlBuilder(null);
        }
        return url.startsWith("http://") || url.startsWith("https://") ? of(url) : of("http://" + url);
    }

    public UrlBuilder addPath(String path) {
        if (path == null || path.isEmpty()) {
            return this;
        }
        String s = url.toString();
        boolean endSlash = s.endsWith("/");
        boolean startSlash = path.startsWith("/");
        if (endSlash && startSlash) {
            url.append(path.substring(1));
        } else if (!endSlash && !startSlash) {
            url.append("/").append(path);
        } else {
            url.append(path);
        }
        return this;
    }

    public UrlBuilder addQuery(String name, Object value) {
        if (name == null) {
            return this;
        }
        url.append(hasQuery ? "&" : "?").append(name).append("=").append(encode(value == null ? "" : value.toString()));
        hasQuery = true;
        return this;
    }

    public UrlBuilder addQuery(String name) {
        if (name == null) {
            return this;
        }
        url.append(hasQuery ? "&" : "?").append(name);
        hasQuery = true;
        return this;
    }

    /**
     * 获取当前 URL 的查询参数
     */
    public UrlQuery getQuery() {
        return UrlQuery.of(url.toString(), StandardCharsets.UTF_8);
    }

    public String build() {
        return url.toString();
    }

    @Override
    public String toString() {
        return build();
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
    }
}
