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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * 基于 byte[] 的资源对象 {@code io.voyager1.util.BytesResource}。
 */
public class BytesResource implements Serializable {

    private static final long serialVersionUID = 1L;

    private final byte[] bytes;
    private final String name;

    /**
     * 构造。
     *
     * @param bytes 字节数组
     */
    public BytesResource(byte[] bytes) {
        this(bytes, null);
    }

    /**
     * 构造。
     *
     * @param bytes 字节数组
     * @param name  资源名称
     */
    public BytesResource(byte[] bytes, String name) {
        this.bytes = bytes;
        this.name = name;
    }

    /**
     * 获取资源名称。
     *
     * @return 名称
     */
    public String getName() {
        return this.name;
    }

    /**
     * 获取 URL，始终返回 {@code null}。
     *
     * @return null
     */
    public URL getUrl() {
        return null;
    }

    /**
     * 获取输入流。
     *
     * @return 输入流
     */
    public InputStream getStream() {
        return new ByteArrayInputStream(this.bytes);
    }

    /**
     * 读取为字符串。
     *
     * @param charset 编码
     * @return 字符串
     */
    public String readStr(Charset charset) {
        return new String(this.bytes, charset);
    }

    /**
     * 读取为字节数组。
     *
     * @return 字节数组
     */
    public byte[] readBytes() {
        return this.bytes;
    }
}
