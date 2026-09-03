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

package io.voyager1.func.openapi.controller;
import io.voyager1.util.CharsetUtil;
import io.voyager1.util.URLUtil;
import io.voyager1.util.StrUtil;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.DateTime;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.NioUtil;
import io.voyager1.util.JakartaServletUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import io.voyager1.common.BaseVoyager1Controller;
import io.voyager1.common.i18n.I18nMessageUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.nio.charset.StandardCharsets;

/**
 * @since 23/12/28 028
 */
@Slf4j
public abstract class BaseDownloadApiController extends BaseVoyager1Controller {

    protected long[] resolveRange(HttpServletRequest request, long fileSize, String id, String name, HttpServletResponse response) {
        String range = JakartaServletUtil.getHeader(request, HttpHeaders.RANGE, StandardCharsets.UTF_8);
        log.debug("下载文件 {} {} {}", id, name, range);
        long fromPos = 0, toPos, downloadSize;
        if ((range == null || range.isEmpty())) {
            downloadSize = fileSize;
        } else {
            // 设置状态码 206
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            List<String> list = io.voyager1.util.ConvertUtil.splitTrim(range, "=");
            String rangeByte = (list == null || list.isEmpty() ? null : list.get(list.size() - 1));
            //  Range: bytes=0-499 表示第 0-499 字节范围的内容
            //  Range: bytes=500-999 表示第 500-999 字节范围的内容
            //  Range: bytes=-500 表示最后 500 字节的内容
            //  Range: bytes=500- 表示从第 500 字节开始到文件结束部分的内容
            //  Range: bytes=0-0,-1 表示第一个和最后一个字节
            //  Range: bytes=500-600,601-999 同时指定几个范围
            Assert.state(!(rangeByte != null && rangeByte.contains(",")), "不支持分片多端下载");
            // TODO 解析更多格式的 RANGE 请求头
            long[] split = StrUtil.splitToLong(rangeByte, "-");
            Assert.state(split != null, "range 传入的信息不正确");
            if (split.length == 2) {
                // Range: bytes=0-499 表示第 0-499 字节范围的内容
                toPos = split[1];
                fromPos = split[0];
            } else if (split.length == 1) {
                if ((rangeByte != null && rangeByte.startsWith("-"))) {
                    // Range: bytes=-500 表示最后 500 字节的内容
                    fromPos = Math.max(fileSize - split[0], 0);
                    toPos = fileSize;
                } else if ((rangeByte != null && rangeByte.endsWith("-"))) {
                    // Range: bytes=500- 表示从第 500 字节开始到文件结束部分的内容
                    fromPos = split[0];
                    toPos = fileSize;
                } else {
                    throw new IllegalArgumentException("不支持的 range 格式 " + rangeByte);
                }
            } else {
                throw new IllegalArgumentException("不支持的 range 格式 " + rangeByte);
            }
            downloadSize = toPos > fromPos ? (toPos - fromPos) : (fileSize - fromPos);
        }
        return new long[]{fromPos, downloadSize};
    }

    public void download(File file, long fileSize, String name, long[] resolveRange, HttpServletResponse response) throws IOException {
        Assert.state(FileUtil.isFile(file), "文件已经不存在啦");
        String contentType = (FileUtil.getMimeType(name) != null ? FileUtil.getMimeType(name) : "application/octet-stream");
        String charset = response.getCharacterEncoding() != null ? response.getCharacterEncoding() : StandardCharsets.UTF_8.name();
        response.setHeader("Content-Disposition", String.format("attachment;filename=\"%s\"", URLUtil.encode(name, CharsetUtil.charset(charset))));
        response.setContentType(contentType);
        //    解析断点续传相关信息
        long fromPos = resolveRange[0];
        long downloadSize = resolveRange[1];
        //
        response.setHeader(HttpHeaders.LAST_MODIFIED, DateTime.of(file.lastModified()).toString("EEE, dd MMM yyyy HH:mm:ss zzz"));
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        //  Content-Range: bytes (unit first byte pos) - [last byte pos]/[entity legth]
        response.setHeader(HttpHeaders.CONTENT_RANGE, String.format("bytes %s-%s/%s", fromPos, downloadSize, fileSize));
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(downloadSize));
        // Copy the stream to the response's output stream.
        ServletOutputStream out = null;
        try (RandomAccessFile in = new RandomAccessFile(file, "r"); FileChannel channel = in.getChannel()) {
            out = response.getOutputStream();
            // 设置下载起始位置
            if (fromPos > 0) {
                channel.position(fromPos);
            }
            // 缓冲区大小
            int bufLen = (int) Math.min(downloadSize, IoUtil.DEFAULT_BUFFER_SIZE);
            ByteBuffer buffer = ByteBuffer.allocate(bufLen);
            int num;
            long count = 0;
            // 当前写到客户端的大小
            while ((num = channel.read(buffer)) != NioUtil.EOF) {
                buffer.flip();
                out.write(buffer.array(), 0, num);
                buffer.clear();
                count += num;
                //处理最后一段，计算不满缓冲区的大小
                long last = (downloadSize - count);
                if (last == 0) {
                    break;
                }
                if (last < bufLen) {
                    bufLen = (int) last;
                    buffer = ByteBuffer.allocate(bufLen);
                }
            }
            response.flushBuffer();
        } catch (ClientAbortException clientAbortException) {
            log.warn("客户端终止连接：{}", clientAbortException.getMessage());
        } catch (Exception e) {
            log.error("数据下载失败", e);
            if (out != null) {
                out.write(StrUtil.bytes("error:" + e.getMessage()));
            }
        } finally {
            IoUtil.close(out);
        }
    }
}
