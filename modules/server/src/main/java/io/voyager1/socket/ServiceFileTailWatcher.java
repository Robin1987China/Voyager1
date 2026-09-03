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

package io.voyager1.socket;

import io.voyager1.util.FileUtil;

import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.util.BaseFileTailWatcher;
import io.voyager1.util.SocketSessionUtil;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件跟随器
 *
 * @since 2019/07/21
 */
@Slf4j
public class ServiceFileTailWatcher<T extends AutoCloseable> extends BaseFileTailWatcher<T> {
    private static final ConcurrentHashMap<File, ServiceFileTailWatcher<WebSocketSession>> CONCURRENT_HASH_MAP = new java.util.concurrent.ConcurrentHashMap<>();

    private static Charset charset;

    public static void setCharset(Charset charset) {
        ServiceFileTailWatcher.charset = charset;
    }

    private ServiceFileTailWatcher(File logFile) {
        super(logFile, charset);
    }

    public static int getOneLineCount() {
        return CONCURRENT_HASH_MAP.size();
    }

    /**
     * 添加文件监听
     *
     * @param file    文件
     * @param session 会话
     * @throws IOException 异常
     */
    public static void addWatcher(File file, WebSocketSession session) throws IOException {
        if (!file.exists() || file.isDirectory()) {
            throw new IOException("文件不存在或者是目录:" + file.getPath());
        }
        ServiceFileTailWatcher<WebSocketSession> agentFileTailWatcher = CONCURRENT_HASH_MAP.computeIfAbsent(file, s -> {
            try {
                return new ServiceFileTailWatcher<>(file);
            } catch (Exception e) {
                log.error("创建文件监听失败", e);
                return null;
            }
        });
        if (agentFileTailWatcher == null) {
            throw new IOException("加载文件失败:" + file.getPath());
        }
        if (agentFileTailWatcher.add(session, FileUtil.getName(file))) {
            agentFileTailWatcher.start();
        }
    }

    /**
     * 有客户端离线
     *
     * @param session 会话
     */
    public static void offline(WebSocketSession session) {
        Collection<ServiceFileTailWatcher<WebSocketSession>> collection = CONCURRENT_HASH_MAP.values();
        for (ServiceFileTailWatcher<WebSocketSession> agentFileTailWatcher : collection) {
            agentFileTailWatcher.socketSessions.removeIf(session::equals);
            if (agentFileTailWatcher.socketSessions.isEmpty()) {
                agentFileTailWatcher.close();
            }
        }
    }

    /**
     * 关闭文件
     *
     * @param fileName 文件
     */
    public static void offlineFile(File fileName) {
        ServiceFileTailWatcher<WebSocketSession> agentFileTailWatcher = CONCURRENT_HASH_MAP.get(fileName);
        if (null == agentFileTailWatcher) {
            return;
        }
        Set<WebSocketSession> socketSessions = agentFileTailWatcher.socketSessions;
        for (WebSocketSession socketSession : socketSessions) {
            offline(socketSession);
        }
        agentFileTailWatcher.close();
    }

    /**
     * 关闭文件读取流
     *
     * @param fileName 文件名
     * @param session  回话
     */
    public static void offlineFile(File fileName, WebSocketSession session) {
        ServiceFileTailWatcher<WebSocketSession> serviceFileTailWatcher = CONCURRENT_HASH_MAP.get(fileName);
        if (null == serviceFileTailWatcher) {
            return;
        }
        Set<WebSocketSession> socketSessions = serviceFileTailWatcher.socketSessions;
        for (WebSocketSession socketSession : socketSessions) {
            if (socketSession.equals(session)) {
                offline(socketSession);
                break;
            }
        }
        if (serviceFileTailWatcher.socketSessions.isEmpty()) {
            serviceFileTailWatcher.close();
        }
    }

    @Override
    protected boolean send(T session, String msg) throws IOException {
        return SocketSessionUtil.send((WebSocketSession) session, msg);
    }

    /**
     * 关闭
     */
    @Override
    protected void close() {
        super.close();
        // 清理线程记录
        CONCURRENT_HASH_MAP.remove(this.logFile);
    }
}
