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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * URL 工具 {@code io.voyager1.util.URLUtil}。
 */
public class URLUtil {

    public static final String FILE = "file";
    public static final String FILE_URL_PREFIX = "file:";

    public static String encode(String url) {
        try {
            return URLEncoder.encode(url, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return url;
        }
    }

    public static String encode(String url, java.nio.charset.Charset charset) {
        try {
            return URLEncoder.encode(url, charset.name());
        } catch (Exception e) {
            return url;
        }
    }

    public static String encodeAll(String url) {
        return encode(url);
    }

    public static String decode(String url, java.nio.charset.Charset charset) {
        try {
            return URLDecoder.decode(url, charset.name());
        } catch (Exception e) {
            return url;
        }
    }

    public static String decode(String url) {
        try {
            return URLDecoder.decode(url, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return url;
        }
    }

    public static String normalize(String url) {
        return url == null ? null : url.replaceAll("/{2,}", "/");
    }

    public static String getDataUriBase(String mimeType) {
        return "data:" + mimeType + ";base64,";
    }

    public static String getDataUriBase64(String mimeType, String base64Data) {
        return getDataUriBase(mimeType) + base64Data;
    }

    public static InputStream getStream(URL url) {
        try {
            return url.openStream();
        } catch (Exception e) {
            throw new RuntimeException("打开 URL 失败: " + url, e);
        }
    }

    public static URL toUrl(String url) {
        try {
            return new URL(url);
        } catch (Exception e) {
            throw new RuntimeException("URL 格式错误: " + url, e);
        }
    }
}
