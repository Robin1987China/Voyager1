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

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 文件跟随器工具
 *
 * @since 2019/7/21
 */
@Slf4j
public abstract class BaseFileTailWatcher<T extends AutoCloseable> {

    private static int initReadLine = 10;

    public static void setInitReadLine(int initReadLine) {
        BaseFileTailWatcher.initReadLine = initReadLine;
    }

    protected File logFile;
    private final Charset charset;
    /**
     * 缓存近x条
     */
    private final LimitQueue<String> limitQueue = new LimitQueue<>(initReadLine);
    private Tailer tailer;

    /**
     * 所有会话
     */
    protected final Set<T> socketSessions = new HashSet<>();

    public BaseFileTailWatcher(File logFile, Charset charset) {
        this.logFile = logFile;
        this.charset = charset;
    }

    /**
     * 发生消息
     *
     * @param session 会话
     * @param msg     消息内容
     * @return 是否发送成功
     * @throws IOException io
     */
    protected abstract boolean send(T session, String msg) throws IOException;

    /**
     * 有新的日志
     *
     * @param msg 日志
     */
    private void sendAll(String msg) {
        Iterator<T> iterator = socketSessions.iterator();
        while (iterator.hasNext()) {
            T socketSession = iterator.next();
            try {
                boolean send = this.send(socketSession, msg);
                if (!send) {
                    //
                    this.errorAutoClose(socketSession);
                    iterator.remove();
                }
            } catch (Exception e) {
                log.error("发送消息失败", e);
                this.errorAutoClose(socketSession);
                iterator.remove();
            }
        }
        if (this.socketSessions.isEmpty()) {
            this.close();
        }
    }

    private void errorAutoClose(T socketSession) {
        log.warn("消息发送失败,自动移除此会话:{}", this.getId(socketSession));
        IoUtil.close(socketSession);
    }

    private String getId(T session) {
        Method byName = ReflectUtil.getMethodByName(session.getClass(), "getId");
        Assert.notNull(byName, "没有  getId 方法");
        return ReflectUtil.invoke(session, byName);
    }

    /**
     * 添加监听会话
     *
     * @param name    文件名
     * @param session 会话
     */
    protected boolean add(T session, String name) throws IOException {
        String id = getId(session);
        Method byName = ReflectUtil.getMethodByName(session.getClass(), "getId");
        boolean match = this.socketSessions.stream()
            .anyMatch(t -> {
                String itemId = ReflectUtil.invoke(t, byName);
                return java.util.Objects.equals(id, itemId);
            });
        if (match) {
            return false;
        }
        if (this.socketSessions.add(session)) {
            this.send(session, String.format("监听{}日志成功, 目前共有{}个会话正在查看", name, this.socketSessions.size()));
            // 开发发送头信息
            for (String s : limitQueue) {
                this.send(session, s);
            }
        }
        return true;
    }

    public void start() {
        //this.tailWatcherRun = new FileTailWatcherRun(logFile, this::sendAll);
        if (this.tailer != null) {
            return;
        }
        this.tailer = new Tailer(logFile, charset, line -> {
            limitQueue.offer(line);
            this.sendAll(line);
        }, initReadLine, DateUnit.SECOND.getMillis());
        this.tailer.start(true);
    }

    public void restart() {
        if (this.tailer != null) {
            this.close();
        }
        this.sendAll("Relisten to the file............");
        this.start();
    }

    /**
     * 关闭
     */
    protected void close() {
        if (this.tailer == null) {
            return;
        }
        this.tailer.stop();
        this.tailer = null;
    }
}
