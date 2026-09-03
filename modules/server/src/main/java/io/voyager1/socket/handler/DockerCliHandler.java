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

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.IoUtil;

import io.voyager1.util.ThreadUtil;
import io.voyager1.util.CharsetUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.plugin.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONValidator;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.i18n.I18nThreadUtil;
import io.voyager1.func.assets.model.MachineDockerModel;
import io.voyager1.func.assets.server.MachineDockerServer;
import io.voyager1.model.docker.DockerInfoModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.docker.DockerInfoService;
import io.voyager1.util.SocketSessionUtil;
import io.voyager1.util.StringUtil;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.nio.charset.StandardCharsets;

/**
 * docker cli
 *
 * @since 2022/02/10
 */
@Feature(cls = ClassFeature.DOCKER, method = MethodFeature.EXECUTE)
@Slf4j
public class DockerCliHandler extends BaseTerminalHandler {

 private static final ConcurrentHashMap<String, HandlerItem> HANDLER_ITEM_CONCURRENT_HASH_MAP = new java.util.concurrent.ConcurrentHashMap<>();


 @Override
 public void afterConnectionEstablishedImpl(WebSocketSession session) throws Exception {
 super.afterConnectionEstablishedImpl(session);
 MachineDockerServer machineDockerServer = SpringContextHolder.getBean(MachineDockerServer.class);
 Map<String, Object> attributes = session.getAttributes();
 MachineDockerModel dockerInfoModel = (MachineDockerModel) attributes.get("machineDocker");
 DockerInfoService dockerInfoService = SpringContextHolder.getBean(DockerInfoService.class);
 String containerId = (String) attributes.get("containerId");
 //
 HandlerItem handlerItem;
 try {
 DockerInfoModel model = new DockerInfoModel();
 model.setMachineDockerId(dockerInfoModel.getId());
 model = dockerInfoService.queryByBean(model);
 Map<String, Object> parameter = machineDockerServer.toParameter(dockerInfoModel);
 handlerItem = new HandlerItem(session, dockerInfoModel, parameter, containerId);
 handlerItem.startRead();
 } catch (Exception e) {
 // 输出超时日志 
 log.error("docker 控制台连接超时", e);
 sendBinary(session, "docker 控制台连接超时");
 this.destroy(session);
 return;
 }
 HANDLER_ITEM_CONCURRENT_HASH_MAP.put(session.getId(), handlerItem);
 //
 try {
 Thread.sleep(1000);
 } catch (InterruptedException ie) {
 Thread.currentThread().interrupt();
 }
 }

 @Override
 protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
 try {
 setLanguage(session);
 HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
 if (handlerItem == null) {
 sendBinary(session, "已经离线啦");
 IoUtil.close(session);
 return;
 }
 String payload = message.getPayload();
 JSONValidator.Type type = StringUtil.validatorJson(payload);
 if (type == JSONValidator.Type.Object) {
 JSONObject jsonObject = JSONObject.parseObject(payload);
 String data = jsonObject.getString("data");
 if (java.util.Objects.equals(data, "voyager1-heart")) {
 // 心跳消息不转发
 return;
 }
 if (java.util.Objects.equals(data, "resize")) {
 // 缓存区大小
 handlerItem.resize(jsonObject);
 return;
 }
 }
 try {
 handlerItem.sendCommand(payload);
 } catch (Exception e) {
 sendBinary(session, "Failure:" + e.getMessage());
 log.error("执行命令异常", e);
 }
 } finally {
 clearLanguage();
 }
 }

 private class HandlerItem implements Runnable, AutoCloseable {
 private final WebSocketSession session;
 private final MachineDockerModel dockerInfoModel;
 private final Map<String, Object> map;
 private PipedInputStream inputStream = new PipedInputStream();
 private PipedOutputStream outputStream = new PipedOutputStream(inputStream);
 private String containerId;
 private Thread thread;

 HandlerItem(WebSocketSession session, MachineDockerModel dockerInfoModel, Map<String, Object> map, String containerId) throws IOException {
 this.session = session;
 this.dockerInfoModel = dockerInfoModel;
 this.containerId = containerId;
 this.map = map;
 }

 void startRead() {
 I18nThreadUtil.execute(this);
 }

 private void sendCommand(String data) throws Exception {
 if (this.outputStream == null) {
 return;
 }
 this.outputStream.write(data.getBytes());
 this.outputStream.flush();
 }

 /**
 * 调整 缓存区大小
 *
 * @param jsonObject 参数
 */
 private void resize(JSONObject jsonObject) {
 Integer rows = ConvertUtil.toInt(jsonObject.getString("rows"), 10);
 Integer cols = ConvertUtil.toInt(jsonObject.getString("cols"), 10);
 Integer wp = ConvertUtil.toInt(jsonObject.getString("wp"), 10);
 Integer hp = ConvertUtil.toInt(jsonObject.getString("hp"), 10);
 map.put("sizeHeight", rows);
 map.put("sizeWidth", cols);
 IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
 try {
 plugin.execute("resizeExec", map);
 } catch (Exception e) {
 log.error("执行容器命令异常", e);
 sendBinary(session, "执行异常:" + e.getMessage());
 }
 }

 @Override
 public void run() {
 map.put("containerId", containerId);
 thread = Thread.currentThread();
 Consumer<String> logConsumer = s -> {
 if ((s != null && s.startsWith("CALLBACK_EXECID:"))) {
 // 终端id
 String execId = (s != null && s.startsWith("CALLBACK_EXECID:") ? s.substring("CALLBACK_EXECID:".length()) : s);
 session.getAttributes().put("execId", execId);
 map.put("execId", execId);
 return;
 }
 sendBinary(session, s);
 };
 map.put("charset", StandardCharsets.UTF_8);
 map.put("stdin", inputStream);
 map.put("logConsumer", logConsumer);
 Consumer<String> errorConsumer = s -> {
 sendBinary(session, s);
 if (java.util.Objects.equals(s, "exit")) {
 // 退出
 destroy(session);
 }
 };
 map.put("errorConsumer", errorConsumer);
 IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
 try {
 plugin.execute("exec", map);
 } catch (Exception e) {
 log.error("执行容器命令异常", e);
 sendBinary(session, "执行异常:" + e.getMessage());
 }
 log.debug("[{}] docker exec 终端进程结束", dockerInfoModel.getName());
 // 标记自动结束
 this.containerId = null;
 }

 private void tryExit() throws Exception {
 if (this.containerId == null) {
 // 如果线程已经结束，不再尝试发送消息
 return;
 }
 // ctrl + c
 this.sendCommand(String.valueOf((char) 3));
 try {
 Thread.sleep(100);
 } catch (InterruptedException ie) {
 Thread.currentThread().interrupt();
 }
 // ctrl + c
 this.sendCommand(String.valueOf((char) 3));
 try {
 Thread.sleep(100);
 } catch (InterruptedException ie) {
 Thread.currentThread().interrupt();
 }
 // quit
 this.sendCommand("quit");
 this.sendCommand(String.valueOf((char) 13));
 try {
 Thread.sleep(100);
 } catch (InterruptedException ie) {
 Thread.currentThread().interrupt();
 }
 // exit
 this.sendCommand("exit");
 this.sendCommand(String.valueOf((char) 13));
 try {
 Thread.sleep(100);
 } catch (InterruptedException ie) {
 Thread.currentThread().interrupt();
 }
 }

 @Override
 public void close() {
 if (this.inputStream == null) {
 // 避免多次调用
 return;
 }
 Object execId = session.getAttributes().get("execId");
 try {
 // 多次尝试退出，可能终端内部进入交互命令行
 for (int i = 0; i < 3; i++) {
 this.tryExit();
 }
 //
 Optional.ofNullable(this.thread).ifPresent(Thread::interrupt);
 } catch (Exception e) {
 log.error("执行容器命令异常", e);
 }
 log.debug("关闭[{}] docker exec 终端：{}", dockerInfoModel.getName(), execId);
 IoUtil.close(this.inputStream);
 IoUtil.close(this.outputStream);
 this.inputStream = null;
 this.outputStream = null;
 }
 }

 @Override
 public void destroy(WebSocketSession session) {
 HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.remove(session.getId());
 IoUtil.close(handlerItem);
 IoUtil.close(session);
 SocketSessionUtil.close(session);
 }
}
