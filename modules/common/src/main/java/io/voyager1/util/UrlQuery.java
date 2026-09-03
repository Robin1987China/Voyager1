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

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * URL 查询参数解析，"" {@code io.voyager1.util.UrlQuery} 的常用方法。
 */
public class UrlQuery {

    private final Map<String, String> queryMap = new LinkedHashMap<>();

    private UrlQuery(String query, Charset charset) {
        if (query == null || query.isEmpty()) {
            return;
        }
        String q = query;
        int idx = q.indexOf('?');
        if (idx >= 0) {
            q = q.substring(idx + 1);
        }
        for (String pair : q.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key;
            String value;
            if (eq >= 0) {
                key = decode(pair.substring(0, eq), charset);
                value = decode(pair.substring(eq + 1), charset);
            } else {
                key = decode(pair, charset);
                value = "";
            }
            queryMap.put(key, value);
        }
    }

    public static UrlQuery of(String query, Charset charset) {
        return new UrlQuery(query, charset);
    }

    public Map<String, String> getQueryMap() {
        return queryMap;
    }

    public String get(String key) {
        return queryMap.get(key);
    }

    private static String decode(String value, Charset charset) {
        try {
            return URLDecoder.decode(value, charset.name());
        } catch (Exception e) {
            return value;
        }
    }
}
