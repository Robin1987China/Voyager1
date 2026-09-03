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

import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * IO 工具{@code io.voyager1.util.IoUtil} 的常用方法。
 */
public class IoUtil {

    public static final String DEFAULT = StandardCharsets.UTF_8.name();
    /**
     * 默认缓冲区大小
     */
    public static final int DEFAULT_MIDDLE_BUFFER_SIZE = 8192;
    /**
     * 默认大缓冲区大小
     */
    public static final int DEFAULT_LARGE_BUFFER_SIZE = 16384;
    /**
     * 默认缓冲区大小
     */
    public static final int DEFAULT_BUFFER_SIZE = DEFAULT_MIDDLE_BUFFER_SIZE;

    public static String readUtf(InputStream in) {
        try {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取流失败", e);
        }
    }

    public static String readUtf8(InputStream in) {
        return readUtf(in);
    }

    public static String read(InputStream in, Charset charset) {
        try {
            return IOUtils.toString(in, charset);
        } catch (IOException e) {
            throw new RuntimeException("读取流失败", e);
        }
    }

    public static byte[] readBytes(InputStream in) {
        try {
            return IOUtils.toByteArray(in);
        } catch (IOException e) {
            throw new RuntimeException("读取流失败", e);
        }
    }

    public static List<String> readLines(Reader reader) {
        try {
            return IOUtils.readLines(reader);
        } catch (Exception e) {
            throw new RuntimeException("读取行失败", e);
        }
    }

    public static List<String> readLines(InputStream in, Charset charset) {
        try {
            return IOUtils.readLines(in, charset);
        } catch (Exception e) {
            throw new RuntimeException("读取行失败", e);
        }
    }

    public static void readLines(InputStream in, Charset charset, LineHandler handler) {
        for (String line : readLines(in, charset)) {
            handler.handle(line);
        }
    }

    public static void readLines(InputStream in, LineHandler handler) {
        readLines(in, StandardCharsets.UTF_8, handler);
    }

    public static void readLines(Reader reader, LineHandler handler) {
        for (String line : readLines(reader)) {
            handler.handle(line);
        }
    }

    public static java.io.BufferedReader getUtf(InputStream in) {
        return new java.io.BufferedReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
    }

    public static java.io.BufferedReader getUtf8Reader(InputStream in) {
        return getUtf(in);
    }

    public static java.io.BufferedReader getReader(InputStream in, Charset charset) {
        return new java.io.BufferedReader(new java.io.InputStreamReader(in, charset));
    }

    public static Reader getBomReader(InputStream in) {
        return new java.io.InputStreamReader(in, StandardCharsets.UTF_8);
    }

    public static InputStream toStream(String content) {
        return IOUtils.toInputStream(content, StandardCharsets.UTF_8);
    }

    public static InputStream toStream(String content, Charset charset) {
        return IOUtils.toInputStream(content, charset);
    }

    public static Reader toReader(String content) {
        return new StringReader(content);
    }

    public static void close(Object closeable) {
        if (closeable instanceof AutoCloseable) {
            try {
                ((AutoCloseable) closeable).close();
            } catch (Exception ignore) {
                // 忽略关闭异常
            }
        } else if (closeable instanceof Closeable) {
            try {
                ((Closeable) closeable).close();
            } catch (IOException ignore) {
                // 忽略关闭异常
            }
        }
    }

    public static void flush(java.io.Flushable flushable) {
        if (flushable != null) {
            try {
                flushable.flush();
            } catch (IOException ignore) {
                // 忽略
            }
        }
    }

    public static void write(java.io.OutputStream out, String content, Charset charset) {
        try {
            out.write(content.getBytes(charset));
        } catch (IOException e) {
            throw new RuntimeException("写入流失败", e);
        }
    }

    public static void copy(InputStream in, java.io.OutputStream out) {
        try {
            IOUtils.copy(in, out);
        } catch (IOException e) {
            throw new RuntimeException("复制流失败", e);
        }
    }
}
