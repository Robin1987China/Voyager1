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

package io.voyager1.socket.handler;

import io.voyager1.util.BeanUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.MapUtil;
import io.voyager1.util.CharsetUtil;
import io.voyager1.util.IdUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.core.api.ApiResult;
import io.voyager1.plugin.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.i18n.I18nThreadUtil;
import io.voyager1.func.assets.model.MachineDockerModel;
import io.voyager1.func.assets.server.MachineDockerServer;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.docker.DockerInfoService;
import io.voyager1.socket.BaseProxyHandler;
import io.voyager1.socket.ConsoleCommandOp;
import io.voyager1.system.ServerConfig;
import io.voyager1.util.LogRecorder;
import io.voyager1.util.SocketSessionUtil;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.nio.charset.StandardCharsets;

/**
 * 容器
 *
 * @since 2022/02/10
 */
@Feature(cls = ClassFeature.DOCKER, method = MethodFeature.EXECUTE)
@Slf4j
public class DockerLogHandler extends BaseProxyHandler {


    @Override
    protected void init(WebSocketSession session, Map<String, Object> attributes) throws Exception {
        super.init(session, attributes);
        //
        Object data = attributes.get("dataItem");
        Object machineData = attributes.get("machineDocker");
        String dataName = BeanUtil.getProperty(data, "name");
        String machineDataName = BeanUtil.getProperty(machineData, "name");
        this.sendMsg(session, "连接成功：" + (dataName == null || dataName.isEmpty() ? machineDataName : dataName) + "\n");
    }

    public DockerLogHandler() {
        super(null);
    }

    @Override
    protected Object[] getParameters(Map<String, Object> attributes) {
        return new Object[0];
    }

    @Override
    protected String handleTextMessage(Map<String, Object> attributes, WebSocketSession session, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {
        MachineDockerModel dockerInfoModel = (MachineDockerModel) attributes.get("machineDocker");
        if (consoleCommandOp == ConsoleCommandOp.heart) {
            return null;
        }
        if (consoleCommandOp == ConsoleCommandOp.showlog) {
            MachineDockerServer machineDockerServer = SpringContextHolder.getBean(MachineDockerServer.class);
            ServerConfig serverConfig = SpringContextHolder.getBean(ServerConfig.class);
            super.logOpt(this.getClass(), attributes, json);
            String containerId = json.getString("containerId");
            Map<String, Object> map = machineDockerServer.toParameter(dockerInfoModel);
            map.put("containerId", containerId);
            int tail = json.getIntValue("tail");
            UserModel userModel = (UserModel) attributes.get("userInfo");
            if (userModel == null) {
                return "用户不存在";
            }
            if (tail > 0) {
                map.put("tail", tail);
            }
            String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
            File file = FileUtil.file(serverConfig.getUserTempPath(userModel.getId()), "docker-log", uuid + ".log");
            LogRecorder logRecorder = LogRecorder.builder().file(file).build();
            Consumer<String> consumer = s -> {
                try {
                    logRecorder.append(s);
                    SocketSessionUtil.send(session, s);
                } catch (IOException e) {
                    log.error("发消息异常", e);
                }
            };
            attributes.put("uuid", uuid);
            attributes.put("logRecorder", logRecorder);
            map.put("uuid", uuid);
            map.put("charset", StandardCharsets.UTF_8);
            map.put("consumer", consumer);
            map.put("timestamps", json.getBoolean("timestamps"));
            I18nThreadUtil.execute(() -> {
                attributes.put("thread", Thread.currentThread());
                IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
                try {
                    plugin.execute("logContainer", map);
                } catch (Exception e) {
                    log.error("拉取 容器日志异常", e);
                    try {
                        SocketSessionUtil.send(session, "执行异常:" + e.getMessage());
                    } catch (IOException ex) {
                        log.error("发消息异常", e);
                    }
                }
                log.debug("docker log 线程结束：{} {}", dockerInfoModel.getName(), uuid);
            });
            SocketSessionUtil.send(session, ApiResult.getString(200, "VOYAGER1_MSG_UUID", uuid));
        } else {
            return null;
        }
        return null;
    }


    @Override
    public void destroy(WebSocketSession session) {
        //
        super.destroy(session);
        Map<String, Object> attributes = session.getAttributes();
        String uuid = (String) attributes.get("uuid");
        try {
            IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
            Map<String, Object> map = java.util.Map.of("uuid", uuid);
            plugin.execute("closeAsyncResource", map);
        } catch (Exception e) {
            log.error("关闭资源失败", e);
        }
        LogRecorder logRecorder = (LogRecorder) attributes.get("logRecorder");
        IoUtil.close(logRecorder);
        // 删除日志缓存
        UserModel userModel = (UserModel) attributes.get("userInfo");
        Optional.ofNullable(userModel).ifPresent(userModel1 -> {
            ServerConfig serverConfig = SpringContextHolder.getBean(ServerConfig.class);
            File file = FileUtil.file(serverConfig.getUserTempPath(userModel1.getId()), "docker-log", uuid + ".log");
            FileUtil.del(file);
        });
        Thread thread = (Thread) attributes.get("thread");
        Optional.ofNullable(thread).ifPresent(Thread::interrupt);
        SocketSessionUtil.close(session);
    }
}
