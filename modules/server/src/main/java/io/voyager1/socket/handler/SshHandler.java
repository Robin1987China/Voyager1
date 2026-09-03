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

import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.NioUtil;

import io.voyager1.util.StrUtil;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.util.ChannelType;
import io.voyager1.util.JschUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONValidator;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.i18n.I18nThreadUtil;
import io.voyager1.func.assets.model.MachineSshModel;
import io.voyager1.model.data.SshModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.dblog.SshTerminalExecuteLogService;
import io.voyager1.service.node.ssh.SshService;
import io.voyager1.service.user.UserBindWorkspaceService;
import io.voyager1.util.SocketSessionUtil;
import io.voyager1.util.StringUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * ssh 处理2
 *
 * @since 2019/8/9
 */
@Feature(cls = ClassFeature.SSH_TERMINAL, method = MethodFeature.EXECUTE)
@Slf4j
public class SshHandler extends BaseTerminalHandler {

 private static final ConcurrentHashMap<String, HandlerItem> HANDLER_ITEM_CONCURRENT_HASH_MAP = new java.util.concurrent.ConcurrentHashMap<>();
 private static SshTerminalExecuteLogService sshTerminalExecuteLogService;
 private static UserBindWorkspaceService userBindWorkspaceService;
 private static SshService sshService;

 private static void init() {
 if (sshTerminalExecuteLogService == null) {
 sshTerminalExecuteLogService = SpringContextHolder.getBean(SshTerminalExecuteLogService.class);
 }
 if (userBindWorkspaceService == null) {
 userBindWorkspaceService = SpringContextHolder.getBean(UserBindWorkspaceService.class);
 }
 if (sshService == null) {
 sshService = SpringContextHolder.getBean(SshService.class);
 }
 }

 @Override
 public void afterConnectionEstablishedImpl(WebSocketSession session) throws Exception {
 super.afterConnectionEstablishedImpl(session);
 init();
 Map<String, Object> attributes = session.getAttributes();
 MachineSshModel machineSshModel = (MachineSshModel) attributes.get("machineSsh");
 SshModel sshModel = (SshModel) attributes.get("dataItem");
 //
 UserModel userInfo = (UserModel) attributes.get("userInfo");
 if (sshModel != null) {
 // 判断是没有任何限制
 String workspaceId = sshModel.getWorkspaceId();
 boolean sshCommandNotLimited = userBindWorkspaceService.exists(userInfo, workspaceId + UserBindWorkspaceService.SSH_COMMAND_NOT_LIMITED);
 attributes.put("sshCommandNotLimited", sshCommandNotLimited);
 } else {
 // 通过资产管理方式进入
 attributes.put("sshCommandNotLimited", true);
 }
 //
 HandlerItem handlerItem;
 try {
 //
 handlerItem = new HandlerItem(session, machineSshModel, sshModel);
 handlerItem.startRead();
 } catch (Exception e) {
 // 输出超时日志 
 log.error("ssh 控制台连接超时", e);
 sendBinary(session, "ssh 控制台连接超时");
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
 //
 Map<String, Object> attributes = session.getAttributes();
 UserModel userInfo = (UserModel) attributes.get("userInfo");
 boolean sshCommandNotLimited = (boolean) attributes.get("sshCommandNotLimited");
 try {
 this.sendCommand(handlerItem, payload, userInfo, sshCommandNotLimited);
 } catch (Exception e) {
 sendBinary(session, "Failure:" + e.getMessage());
 log.error("执行命令异常", e);
 }
 } finally {
 clearLanguage();
 }
 }

 private void sendCommand(HandlerItem handlerItem, String data, UserModel userInfo, boolean sshCommandNotLimited) throws Exception {
 if (handlerItem.checkInput(data, userInfo, sshCommandNotLimited)) {
 handlerItem.outputStream.write(data.getBytes());
 } else {
 handlerItem.outputStream.write("没有执行相关命令权限".getBytes());
 handlerItem.outputStream.flush();
 handlerItem.outputStream.write(new byte[]{3});
 }
 handlerItem.outputStream.flush();
 }

 /**
 * 记录终端执行记录
 *
 * @param session 回话
 * @param command 命令行
 * @param refuse 是否拒绝
 */
 private void logCommands(WebSocketSession session, String command, boolean refuse) {
 List<String> split = io.voyager1.util.ConvertUtil.splitTrim(command, StrUtil.CR);
 // 最后一个是否为回车, 最后一个不是回车表示还未提交，还在缓存去待确认
 boolean all = (command != null && command.endsWith(StrUtil.CR));
 int size = split.size();
 split = split.subList(0, all ? size : size - 1);
 if ((split == null || split.isEmpty())) {
 return;
 }
 // 获取基础信息
 Map<String, Object> attributes = session.getAttributes();
 UserModel userInfo = (UserModel) attributes.get("userInfo");
 String ip = (String) attributes.get("ip");
 String userAgent = (String) attributes.get(HttpHeaders.USER_AGENT);
 MachineSshModel machineSshModel = (MachineSshModel) attributes.get("machineSsh");
 SshModel sshItem = (SshModel) attributes.get("dataItem");
 //
 sshTerminalExecuteLogService.batch(userInfo, machineSshModel, sshItem, ip, userAgent, refuse, split);
 }

 private class HandlerItem implements Runnable, AutoCloseable {
 private final WebSocketSession session;
 private final InputStream inputStream;
 private final OutputStream outputStream;
 private final Session openSession;
 private final ChannelShell channel;
 private final SshModel sshItem;
 private final MachineSshModel machineSshModel;
 private final StringBuilder nowLineInput = new StringBuilder();
 private final KeyEventCycle keyEventCycle = new KeyEventCycle();

 HandlerItem(WebSocketSession session, MachineSshModel machineSshModel, SshModel sshModel) throws IOException {
 this.session = session;
 this.sshItem = sshModel;
 this.machineSshModel = machineSshModel;
 this.openSession = sshService.getSessionByModel(machineSshModel);
 this.channel = (ChannelShell) JschUtil.createChannel(openSession, ChannelType.SHELL);
 this.inputStream = channel.getInputStream();
 this.outputStream = channel.getOutputStream();
 keyEventCycle.setCharset(machineSshModel.charset());
 }

 void startRead() throws JSchException {
 this.channel.connect(machineSshModel.timeout());
 I18nThreadUtil.execute(this);
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
 this.channel.setPtySize(cols, rows, wp, hp);
 }

 /**
 * 添加到命令队列
 *
 * @param msg 输入
 * @return 当前待确认待所有命令
 */
 private String append(String msg) {
 char[] x = msg.toCharArray();
 if (x.length == 1 && x[0] == 127) {
 // 退格键
 int length = nowLineInput.length();
 if (length > 0) {
 nowLineInput.delete(length - 1, length);
 }
 } else {
 nowLineInput.append(msg);
 }
 return nowLineInput.toString();
 }

 /**
 * 检查输入是否包含禁止命令，记录执行记录
 *
 * @param msg 输入
 * @param userInfo 用户
 * @param sshCommandNotLimited 是否解除限制
 * @return true 没有任何限制
 */
 public boolean checkInput(String msg, UserModel userInfo, boolean sshCommandNotLimited) {
 String allCommand = this.append(msg);
 boolean refuse;
 // 超级管理员不限制,有权限都不限制
 boolean systemUser = userInfo.isSuperSystemUser() || sshCommandNotLimited;
 if (StrUtil.equalsAny(msg, StrUtil.CR, "\t")) {
 String join = nowLineInput.toString();
 if (java.util.Objects.equals(msg, StrUtil.CR)) {
 nowLineInput.setLength(0);
 }
 // sshItem 可能为空
 refuse = sshItem == null || SshModel.checkInputItem(sshItem, join);
 } else {
 // 复制输出
 refuse = sshItem == null || SshModel.checkInputItem(sshItem, msg);
 }
 // 执行命令行记录
 keyEventCycle.read(text -> {
 // 获取基础信息
 Map<String, Object> attributes = session.getAttributes();
 String ip = (String) attributes.get("ip");
 String userAgent = (String) attributes.get(HttpHeaders.USER_AGENT);
 MachineSshModel machineSshModel = (MachineSshModel) attributes.get("machineSsh");
 SshModel sshItem = (SshModel) attributes.get("dataItem");
 sshTerminalExecuteLogService.batch(userInfo, machineSshModel, sshItem, ip, userAgent, refuse, Collections.singletonList(text));
 }, msg.getBytes(Charset.forName(machineSshModel.getCharset())));
 // 执行命令行记录
 // logCommands(session, allCommand, refuse);
 return systemUser || refuse;
 }


 @Override
 public void run() {
 try {
 byte[] buffer = new byte[1024];
 int i;
 //如果没有数据来，线程会一直阻塞在这个地方等待数据。
 while ((i = inputStream.read(buffer)) != NioUtil.EOF) {
 byte[] tempBytes = Arrays.copyOfRange(buffer, 0, i);
 keyEventCycle.receive(tempBytes);
 sendBinary(session, new String(tempBytes, machineSshModel.charset()));
 }
 } catch (Exception e) {
 if (!this.openSession.isConnected()) {
 log.error("ssh 错误：{}", e.getMessage());
 return;
 }
 log.error("读取错误", e);
 SshHandler.this.destroy(this.session);
 }
 }

 @Override
 public void close() throws Exception {
 IoUtil.close(this.inputStream);
 IoUtil.close(this.outputStream);
 JschUtil.close(this.channel);
 JschUtil.close(this.openSession);
 }
 }

 @Override
 public void destroy(WebSocketSession session) {
 HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
 IoUtil.close(handlerItem);
 IoUtil.close(session);
 HANDLER_ITEM_CONCURRENT_HASH_MAP.remove(session.getId());
 SocketSessionUtil.close(session);
 }

 /**
 * 控制台案件事件处理
 */
 public static class KeyEventCycle {

 // 输入缓存
 private StringBuffer buffer = new StringBuffer();
 // 输入后是否接收返回字符串
 private boolean inputReceive = false;
 // TAB 输入暂停（处理Y/N确认）
 private boolean tabInputPause = false;
 // 光标位置
 private int inputSelection = 0;
 // 搜索状态，0未开始，1开始搜索，2搜索结束
 private int searchState = 0;
 @Setter
 private Charset charset;
 private KeyControl keyControl = KeyControl.KEY_END;
 private Consumer<String> consumer;

 /**
 * 从控制台读取输入按键进行处理
 *
 * @param consumer 完整命令后输入回调
 * @param bytes 输入按键
 */
 public void read(Consumer<String> consumer, byte... bytes) {
 this.consumer = consumer;
 String str = new String(bytes, charset);
 if (keyControl == KeyControl.KEY_TAB && tabInputPause) {
 if (str.equalsIgnoreCase("y") || str.equalsIgnoreCase("n")) {
 tabInputPause = false;
 return;
 }
 }
 keyControl = KeyControl.getKeyControl(bytes);
 if ((keyControl == KeyControl.KEY_INPUT || keyControl == KeyControl.KEY_FUNCTION) && !tabInputPause) {
 buffer.insert(inputSelection, str);
 inputSelection += str.length();
 } else if (keyControl == KeyControl.KEY_ENTER) {
 // 回车，结束当前输入周期
 if (buffer.length() > 0 && searchState != 1) {
 consumer.accept(buffer.toString());
 } else if (searchState == 1) {
 // Control + R结束
 searchState = 2;
 }
 // 重置周期
 buffer = new StringBuffer();
 inputReceive = false;
 inputSelection = 0;
 } else if (keyControl == KeyControl.KEY_BACK) {
 buffer.delete(Math.max(inputSelection - 1, 0), inputSelection);
 inputSelection = Math.max(inputSelection - 1, 0);
 } else if (keyControl == KeyControl.KEY_DELETE) {
 buffer.delete(inputSelection, Math.min(inputSelection + 1, buffer.length()));
 } else if (keyControl == KeyControl.KEY_LEFT) {
 inputSelection = Math.max(inputSelection - 1, 0);
 } else if (keyControl == KeyControl.KEY_RIGHT) {
 inputSelection = Math.min(inputSelection + 1, buffer.length());
 } else if (keyControl == KeyControl.KEY_HOME) {
 inputSelection = 0;
 } else if (keyControl == KeyControl.KEY_END) {
 inputSelection = buffer.length();
 } else if (keyControl == KeyControl.KEY_TAB) {
 inputReceive = true;
 } else if (keyControl == KeyControl.KEY_UP || keyControl == KeyControl.KEY_DOWN) {
 // 清空命令缓冲
 inputSelection = 0;
 inputReceive = true;
 } else if (keyControl == KeyControl.KEY_ETX) {
 buffer = new StringBuffer();
 inputSelection = 0;
 } else if (keyControl == KeyControl.KEY_SEARCH) {
 buffer = new StringBuffer();
 searchState = 1;
 }
 }

 /**
 * 从SSH服务端接收字节
 *
 * @param bytes 字节
 */
 public void receive(byte... bytes) {
 if (searchState == 2) {
 // 处理搜索命令结束后，接收到ssh服务器返回的完整命令
 int index = indexOf(bytes, new byte[]{27, 91, 75});
 if (index > -1) {
 bytes = Arrays.copyOf(bytes, index);
 }
 String str = new String(bytes, charset).split("# ")[1];
 consumer.accept(str.trim());
 searchState = 0;
 return;
 }
 if (inputReceive) {
 String str = new String(bytes, charset);
 if (keyControl == KeyControl.KEY_UP || keyControl == KeyControl.KEY_DOWN) {
 // 上下键只有第一条是正常的，后面的都是根据第一条进行退格删除再补充的。
 // 8,8,8,99,100,32,47,112,114,50,111,99,47,
 try {
 try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
 for (byte aByte : bytes) {
 if (aByte == 8) {
 // 首位是退格键，就执行删除末尾值
 buffer.deleteCharAt(Math.max(buffer.length() - 1, 0));
 } else if (aByte == 27) {
 // 遇到【逃离/取消】就跳出循环
 break;
 } else if (aByte != 0) {
 outputStream.write(aByte);
 }
 }
 buffer.append(new String(outputStream.toByteArray(), charset));
 }
 inputSelection = buffer.length();
 } catch (Exception e) {
 log.error("", e);
 }
 return;
 } else {
 if (keyControl == KeyControl.KEY_TAB) {
 if (bytes[0] == 7) {
 // 接收到终端响铃，就删除响铃
 bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
 }
 if (Arrays.equals(new byte[]{13, 10}, bytes)) {
 inputReceive = false;
 return;
 }
 // tab下文件很多
 if (str.contains("y or n")) {
 tabInputPause = true;
 inputReceive = false;
 return;
 }
 // cat 'hello word.txt'
 // cat hello\ word.txt
 if (str.split(" ").length > 1 && (!str.contains("'") && !str.contains("\\"))) {
 inputReceive = false;
 return;
 }
 }
 // 非上下键输入输入中，如果接受到数据就执行插入数据，根据当前光标位置执行插入
 // 存在退格，就从光标位置开始删除
 int backCount = 0;
 for (byte aByte : bytes) {
 if (aByte == 8) {
 buffer.deleteCharAt(inputSelection - 1);
 backCount++;
 }
 }
 str = new String(Arrays.copyOfRange(bytes, 0, bytes.length - backCount), charset);
 buffer.insert(inputSelection, str);
 inputSelection += str.length();
 }
 }
 inputReceive = false;
 }

 /**
 * 查找指定字节数组在原始字节数组中的位置
 *
 * @param originalArray 原始字节数组
 * @param byteArrayToFind 要查找的字节数组
 * @return 找到的位置索引，如果找不到返回 -1
 */
 private static int indexOf(byte[] originalArray, byte[] byteArrayToFind) {
 // 遍历原始字节数组，查找匹配的起始位置
 for (int i = 0; i <= originalArray.length - byteArrayToFind.length; i++) {
 boolean match = true;
 for (int j = 0; j < byteArrayToFind.length; j++) {
 if (originalArray[i + j] != byteArrayToFind[j]) {
 match = false;
 break;
 }
 }
 if (match) {
 return i;
 }
 }
 return -1;
 }

 }

 /**
 * 功能键枚举
 */
 public enum KeyControl {
 KEY_TAB((byte) 9), // TAB
 KEY_ETX((byte) 3), // Control + C
 KEY_ENTER((byte) 13), // Enter
 KEY_SEARCH((byte) 18), // Control + R
 KEY_BACK((byte) 127), // 退格键
 KEY_DELETE(new byte[]{27, 91, 51, 126}), // DELETE键
 KEY_LEFT(new byte[]{27, 91, 68}), // 左
 KEY_RIGHT(new byte[]{27, 91, 67}), // 右
 KEY_UP(new byte[]{27, 91, 65}), // 上
 KEY_DOWN(new byte[]{27, 91, 66}), // 下
 KEY_HOME(new byte[]{27, 91, 72}),
 KEY_END(new byte[]{27, 91, 70}),
 KEY_FUNCTION(new byte[]{27, 91}), //其他功能键
 KEY_INPUT(new byte[]{-1}); // 正常输入

 private final byte[] control;

 KeyControl(byte... control) {
 this.control = control;
 }

 public static KeyControl getKeyControl(byte[] bytes) {
 for (KeyControl value : KeyControl.values()) {
 if (Arrays.equals(value.control, bytes)) {
 return value;
 }
 }
 // 其他功能键
 if (Arrays.equals(KEY_FUNCTION.control, Arrays.copyOf(bytes, 2))) {
 return KEY_FUNCTION;
 }
 // 正常输入
 return KEY_INPUT;
 }
 }
}
