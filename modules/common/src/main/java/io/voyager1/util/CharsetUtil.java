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

import java.nio.charset.Charset;

/**
 * 字符集工具 {@code io.voyager1.util.CharsetUtil} 的常用方法。
 */
public class CharsetUtil {

    public static Charset charset(String name) {
        return Charset.forName(name);
    }

    public static String parse(Charset charset) {
        return charset == null ? null : charset.name();
    }

    public static String convert(String src, Charset srcCharset, Charset destCharset) {
        if (src == null) {
            return null;
        }
        return new String(src.getBytes(srcCharset), destCharset);
    }

    public static Charset defaultCharset() {
        return Charset.defaultCharset();
    }

    public static Charset parse(CharSequence charsetName, Charset defaultCharset) {
        if (charsetName == null || charsetName.length() == 0) {
            return defaultCharset;
        }
        try {
            return Charset.forName(charsetName.toString());
        } catch (Exception e) {
            return defaultCharset;
        }
    }
}
