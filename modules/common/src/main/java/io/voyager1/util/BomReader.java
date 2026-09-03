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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 读取带 BOM 头流内容的 Reader {@code io.voyager1.util.BomReader}。
 * <p>构造时会探测并跳过 UTF-8/UTF-16/UTF-32 的 BOM，非 BOM 流默认按 UTF-8 解码。</p>
 */
public class BomReader extends Reader {

    private final InputStreamReader reader;

    /**
     * 构造。
     *
     * @param in 输入流
     */
    public BomReader(InputStream in) {
        if (in == null) {
            throw new IllegalArgumentException("InputStream must be not null!");
        }
        PushbackInputStream pushback = new PushbackInputStream(in, 4);
        Charset charset = detectAndSkipBom(pushback, StandardCharsets.UTF_8);
        this.reader = new InputStreamReader(pushback, charset);
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return reader.read(cbuf, off, len);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    /**
     * 探测并跳过 BOM，返回解码字符集。
     */
    static Charset detectAndSkipBom(PushbackInputStream in, Charset defaultCharset) {
        try {
            byte[] bom = new byte[4];
            int n = in.read(bom, 0, 4);
            if (n <= 0) {
                return defaultCharset;
            }
            Charset charset;
            int unread;
            if (n >= 4 && bom[0] == 0x00 && bom[1] == 0x00 && bom[2] == (byte) 0xFE && bom[3] == (byte) 0xFF) {
                charset = Charset.forName("UTF-32BE");
                unread = n - 4;
            } else if (n >= 4 && bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE && bom[2] == 0x00 && bom[3] == 0x00) {
                charset = Charset.forName("UTF-32LE");
                unread = n - 4;
            } else if (n >= 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
                charset = StandardCharsets.UTF_8;
                unread = n - 3;
            } else if (n >= 2 && bom[0] == (byte) 0xFE && bom[1] == (byte) 0xFF) {
                charset = StandardCharsets.UTF_16BE;
                unread = n - 2;
            } else if (n >= 2 && bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE) {
                charset = StandardCharsets.UTF_16LE;
                unread = n - 2;
            } else {
                charset = defaultCharset;
                unread = n;
            }
            if (unread > 0) {
                in.unread(bom, n - unread, unread);
            }
            return charset;
        } catch (IOException e) {
            return defaultCharset;
        }
    }
}
