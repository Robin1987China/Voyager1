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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 文件内容跟随器（类似 tail -f），"" {@code io.voyager1.util.Tailer}。
 */
public class Tailer {

    private final Charset charset;
    private final LineHandler lineHandler;
    private final int initReadLine;
    private final long period;
    private final RandomAccessFile randomAccessFile;
    private final ScheduledExecutorService executorService;
    private final StringBuilder lineBuffer = new StringBuilder();

    private volatile boolean running;
    private ScheduledFuture<?> future;
    private long lastOffset;

    /**
     * 构造。
     *
     * @param file         文件
     * @param charset      编码
     * @param lineHandler  行处理器
     * @param initReadLine 启动时预读取的行数
     * @param period       检查间隔（毫秒）
     */
    public Tailer(File file, Charset charset, LineHandler lineHandler, int initReadLine, long period) {
        checkFile(file);
        this.charset = (charset != null ? charset : StandardCharsets.UTF_8);
        this.lineHandler = lineHandler;
        this.initReadLine = initReadLine;
        this.period = period;
        try {
            this.randomAccessFile = new RandomAccessFile(file, "r");
        } catch (IOException e) {
            throw new RuntimeException("打开文件失败: " + file.getAbsolutePath(), e);
        }
        this.executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "tailer-" + file.getName());
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 开始监听（同步阻塞）。
     */
    public void start() {
        start(false);
    }

    /**
     * 开始监听。
     *
     * @param async 是否异步执行
     */
    public void start(boolean async) {
        try {
            readTail();
        } catch (IOException e) {
            throw new RuntimeException("预读取文件失败", e);
        }
        running = true;
        future = executorService.scheduleAtFixedRate(this::pollNewLines, 0, period, TimeUnit.MILLISECONDS);
        if (!async) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ignore) {
                // 忽略
            }
        }
    }

    /**
     * 停止监听。
     */
    public void stop() {
        running = false;
        if (future != null) {
            future.cancel(false);
        }
        executorService.shutdownNow();
        try {
            randomAccessFile.close();
        } catch (IOException ignore) {
            // 忽略
        }
    }

    /**
     * 预读取末尾若干行。
     */
    private void readTail() throws IOException {
        long len = randomAccessFile.length();
        if (initReadLine > 0 && len > 0) {
            List<String> lines = readLastLines(len, initReadLine);
            for (String line : lines) {
                if (lineHandler != null) {
                    lineHandler.handle(line);
                }
            }
        }
        randomAccessFile.seek(len);
        lastOffset = len;
    }

    private List<String> readLastLines(long len, int maxLines) throws IOException {
        List<String> lines = new ArrayList<>();
        long start = Math.max(0, len - (long) maxLines * 1024 * 4);
        randomAccessFile.seek(start);
        byte[] buf = new byte[(int) (len - start)];
        randomAccessFile.readFully(buf);
        String content = new String(buf, charset);
        String[] split = content.split("\r\n|\n|\r", -1);
        int from = Math.max(0, split.length - maxLines);
        for (int i = from; i < split.length; i++) {
            if (i == split.length - 1 && split[i].isEmpty()) {
                continue;
            }
            lines.add(split[i]);
        }
        return lines;
    }

    private synchronized void pollNewLines() {
        if (!running) {
            return;
        }
        try {
            long len = randomAccessFile.length();
            if (len < lastOffset) {
                // 文件被截断
                lastOffset = 0;
                randomAccessFile.seek(0);
                lineBuffer.setLength(0);
            }
            if (len == lastOffset) {
                return;
            }
            randomAccessFile.seek(lastOffset);
            int available = (int) Math.min(len - lastOffset, 8192);
            byte[] buf = new byte[available];
            int n = randomAccessFile.read(buf);
            if (n > 0) {
                lastOffset += n;
                lineBuffer.append(new String(buf, 0, n, charset));
                drainLines(false);
            }
        } catch (IOException ignore) {
            // 忽略轮询读取异常
        }
    }

    private void drainLines(boolean flushAll) {
        String s = lineBuffer.toString();
        int idx;
        while ((idx = s.indexOf('\n')) >= 0) {
            String line = s.substring(0, idx);
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            s = s.substring(idx + 1);
            if (lineHandler != null) {
                lineHandler.handle(line);
            }
        }
        lineBuffer.setLength(0);
        lineBuffer.append(s);
        if (flushAll && s.length() > 0 && lineHandler != null) {
            lineHandler.handle(s);
            lineBuffer.setLength(0);
        }
    }

    private static void checkFile(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File [" + (file == null ? "null" : file.getAbsolutePath()) + "] not exist !");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("Path [" + file.getAbsolutePath() + "] is not a file !");
        }
    }
}
