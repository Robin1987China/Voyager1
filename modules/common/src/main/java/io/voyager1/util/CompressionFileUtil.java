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

import io.voyager1.util.CharsetUtil;
import io.voyager1.util.CompressUtil;
import io.voyager1.util.Extractor;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.CompressorInputStream;
import io.voyager1.common.i18n.I18nMessageUtil;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 压缩文件工具
 *
 */
@Slf4j
public class CompressionFileUtil {

    private static final Charset[] CHARSETS = new Charset[]{java.nio.charset.Charset.forName("GBK"), StandardCharsets.UTF_8};

    /**
     * 解压文件
     *
     * @param compressFile 压缩文件
     * @param destDir      解压到的文件夹
     */
    public static void unCompress(File compressFile, File destDir) {
        unCompress(compressFile, destDir, 0);
    }

    /**
     * 解压文件
     *
     * @param compressFile    压缩文件
     * @param destDir         解压到的文件夹
     * @param stripComponents 剔除文件夹
     */
    public static void unCompress(File compressFile, File destDir, int stripComponents) {
        try {
            unCompressTryCharset(compressFile, destDir, stripComponents);
        } catch (Exception e) {
            try {
                unCompressByInputStreamTryCharset(compressFile, destDir, stripComponents);
            } catch (Exception e2) {
                //
                e2.addSuppressed(e);
                //
                throw Lombok.sneakyThrow(e2);
            }
        }
    }

    private static void unCompressTryCharset(File compressFile, File destDir, int stripComponents) {
        for (int i = 0; i < CHARSETS.length; i++) {
            Charset charset = CHARSETS[i];
            try (Extractor extractor = CompressUtil.createExtractor(charset, compressFile)) {
                extractor.extract(destDir, stripComponents);
            } catch (Exception e) {
                log.warn("{} 解压异常 {} {}", compressFile.getName(), charset, e.getMessage());
                if (i == CHARSETS.length - 1) {
                    // 最后一个
                    throw Lombok.sneakyThrow(e);
                }
            }
        }
    }

    private static void unCompressByInputStreamTryCharset(File compressFile, File destDir, int stripComponents) {
        for (int i = 0; i < CHARSETS.length; i++) {
            Charset charset = CHARSETS[i];
            try (FileInputStream fileInputStream = new FileInputStream(compressFile);
                 CompressorInputStream compressUtilIn = CompressUtil.getIn(null, fileInputStream);) {
                try (Extractor extractor = CompressUtil.createExtractor(charset, compressUtilIn)) {
                    extractor.extract(destDir, stripComponents);
                }
            } catch (Exception e) {
                log.warn("解压异常 {} by InputStream {}", charset, e.getMessage());
                if (i == CHARSETS.length - 1) {
                    // 最后一个
                    throw Lombok.sneakyThrow(e);
                }
            }
        }
    }
}
