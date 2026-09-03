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

package io.voyager1.controller.node;

import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.ResourceUtil;
import io.voyager1.util.Tuple;
import io.voyager1.util.CharsetUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.core.AppType;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.*;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.controller.system.SystemUpdateController;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.func.openapi.controller.NodeInfoController;
import io.voyager1.model.AgentFileModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.system.SystemParametersServer;
import io.voyager1.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;


/**
 * @since 2021/11/29
 */
@RestController
@RequestMapping(value = "/node")
@SystemPermission(superUser = true)
@Feature(cls = ClassFeature.UPGRADE_NODE_LIST)
@Slf4j
public class NodeUpdateController extends BaseServerController {

 private final SystemParametersServer systemParametersServer;
 private final ServerConfig serverConfig;

 public NodeUpdateController(SystemParametersServer systemParametersServer,
 ServerConfig serverConfig) {
 this.systemParametersServer = systemParametersServer;
 this.serverConfig = serverConfig;
 }

 /**
 * 远程下载
 *
 * @return json
 * @see RemoteVersion
 */
 @GetMapping(value = "download_remote.json", produces = MediaType.APPLICATION_JSON_VALUE)
 @Feature(method = MethodFeature.REMOTE_DOWNLOAD)
 public ApiResult<String> downloadRemote() throws IOException {
 String saveDir = serverConfig.getAgentPath().getAbsolutePath();
 Tuple download = RemoteVersion.download(saveDir, AppType.Agent, false);
 // 保存文件
 this.saveAgentFile(download);
 return ApiResult.success("下载成功");
 }

 /**
 * 检查版本更新
 *
 * @return json
 * @see RemoteVersion
 * @see AgentFileModel
 */
 @GetMapping(value = "check_version.json", produces = MediaType.APPLICATION_JSON_VALUE)
 public ApiResult<JSONObject> checkVersion() {
 io.voyager1.RemoteVersion remoteVersion = RemoteVersion.cacheInfo();
 AgentFileModel agentFileModel = systemParametersServer.getConfig(AgentFileModel.ID, AgentFileModel.class, agentFileModel1 -> {
 if (agentFileModel1 == null || !FileUtil.exist(agentFileModel1.getSavePath())) {
 return null;
 }
 return agentFileModel1;
 });
 JSONObject jsonObject = new JSONObject();
 if (remoteVersion == null) {
 jsonObject.put("upgrade", false);
 } else {
 String tagName = StrUtil.removePrefixIgnoreCase(remoteVersion.getTagName(), "v");
 jsonObject.put("tagName", tagName);
 if (agentFileModel == null) {
 jsonObject.put("upgrade", true);
 } else {
 String version = StrUtil.removePrefixIgnoreCase(agentFileModel.getVersion(), "v");
 jsonObject.put("upgrade", StrUtil.compareVersion(version, tagName) < 0);
 jsonObject.put("path", agentFileModel.getSavePath());
 }
 jsonObject.put("downloadSource", remoteVersion.getDownloadSource());
 }
 return ApiResult.success("", jsonObject);
 }

 @RequestMapping(value = "upload-agent-sharding", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
 @SystemPermission
 @Feature(method = MethodFeature.UPLOAD, log = false)
 public ApiResult<String> uploadAgentSharding(MultipartFile file,
 String sliceId,
 Integer totalSlice,
 Integer nowSlice,
 String fileSumMd5) throws IOException {
 File userTempPath = serverConfig.getUserTempPath();
 this.uploadSharding(file, userTempPath.getAbsolutePath(), sliceId, totalSlice, nowSlice, fileSumMd5, "jar", "zip");
 return ApiResult.success("上传成功");
 }

 @RequestMapping(value = "upload-agent-sharding-merge", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
 @SystemPermission
 @Feature(method = MethodFeature.UPLOAD)
 public ApiResult<String> uploadAgent(String sliceId,
 Integer totalSlice,
 String fileSumMd5) throws IOException {
 File agentPath = serverConfig.getAgentPath();

 File userTempPath = serverConfig.getUserTempPath();
 File successFile = this.shardingTryMerge(userTempPath.getAbsolutePath(), sliceId, totalSlice, fileSumMd5);
 FileUtil.move(successFile, agentPath, true);
 //
 String path = FileUtil.file(agentPath, successFile.getName()).getAbsolutePath();
 // 解析压缩包
 File file = Voyager1Manifest.zipFileFind(path, AppType.Agent, agentPath.getAbsolutePath());
 path = FileUtil.getAbsolutePath(file);
 // 基础检查
 ApiResult<Tuple> error = Voyager1Manifest.checkVoyager1Jar(path, AppType.Agent, false);
 if (!error.success()) {
 FileUtil.del(path);
 return new ApiResult<>(error.getCode(), error.getMsg());
 }
 // 保存文件
 this.saveAgentFile(error.getData());
 return ApiResult.success("上传成功");
 }

 private void saveAgentFile(Tuple data) {
 File file = data.get(3);
 AgentFileModel agentFileModel = new AgentFileModel();
 agentFileModel.setName(file.getName());
 agentFileModel.setSize(file.length());
 agentFileModel.setSavePath(FileUtil.getAbsolutePath(file));
 //
 agentFileModel.setVersion(data.get(0));
 agentFileModel.setTimeStamp(data.get(1));
 systemParametersServer.upsert(AgentFileModel.ID, agentFileModel, AgentFileModel.ID);
 // 删除历史包 
 String saveDir = serverConfig.getAgentPath().getAbsolutePath();
 List<File> files = FileUtil.loopFiles(saveDir, pathname -> !FileUtil.equals(pathname, file));
 for (File file1 : files) {
 FileUtil.del(file1);
 }
 }

 @GetMapping(value = "fast_install.json", produces = MediaType.APPLICATION_JSON_VALUE)
 public ApiResult<JSONObject> fastInstall(HttpServletRequest request) {
 boolean beta = RemoteVersion.betaRelease();
 String language = I18nMessageUtil.tryGetNormalLanguage();
 InputStream inputStream = ResourceUtil.getStream("classpath:/fast-install/" + language + (beta ? "/beta.json" : "/release.json"));
 String json = IoUtil.read(inputStream, StandardCharsets.UTF_8);
 JSONObject jsonObject = new JSONObject();
 Voyager1Manifest instance = Voyager1Manifest.getInstance();
 jsonObject.put("token", instance.randomIdSign());
 jsonObject.put("key", ServerOpenApi.PUSH_NODE_KEY);
 //
 JSONArray jsonArray = JSONArray.parseArray(json);
 jsonObject.put("shUrls", jsonArray);
 //
 String contextPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
 String url = String.format("/%s/%s", contextPath, ServerOpenApi.RECEIVE_PUSH);
 jsonObject.put("url", FileUtil.normalize(url));
 // 下载授权码
 String auth = systemParametersServer.getConfig(SystemUpdateController.VOYAGER1_REMOTE_VERSION_AUTH, String.class);
 jsonObject.put("auth", auth);
 return ApiResult.success("", jsonObject);
 }

 @GetMapping(value = "pull_fast_install_result.json", produces = MediaType.APPLICATION_JSON_VALUE)
 public ApiResult<Collection<JSONObject>> pullFastInstallResult(String removeId) {
 Collection<JSONObject> jsonObjects = NodeInfoController.listReceiveCache(removeId);
 jsonObjects = jsonObjects.stream()
 .map(jsonObject -> {
 JSONObject clone = jsonObject.clone();
 clone.remove("canUseNode");
 return clone;
 })
 .collect(Collectors.toList());
 return ApiResult.success("", jsonObjects);
 }

 @GetMapping(value = "confirm_fast_install.json", produces = MediaType.APPLICATION_JSON_VALUE)
 public ApiResult<Collection<JSONObject>> confirmFastInstall(HttpServletRequest request,
 @ValidatorItem String id,
 @ValidatorItem String ip,
 int port) {
 JSONObject receiveCache = NodeInfoController.getReceiveCache(id);
 Assert.notNull(receiveCache, "没有对应的缓存信息");
 JSONArray jsonArray = receiveCache.getJSONArray("canUseNode");
 Assert.notEmpty(jsonArray, "没有对应的缓存信息：-1");
 Optional<MachineNodeModel> any = jsonArray.stream().map(o -> {
 if (o instanceof MachineNodeModel) {
 return (MachineNodeModel) o;
 }
 JSONObject jsonObject = (JSONObject) o;
 return jsonObject.toJavaObject(MachineNodeModel.class);
 }).filter(nodeModel -> java.util.Objects.equals(nodeModel.getVoyager1Url(), String.format("%s:%s", ip, port))).findAny();
 Assert.state(any.isPresent(), "ip 地址信息不正确");
 MachineNodeModel machineNodeModel = any.get();
 try {
 machineNodeServer.testNode(machineNodeModel);
 } catch (Exception e) {
 log.warn("测试结果：{} {}", machineNodeModel.getVoyager1Url(), e.getMessage());
 return new ApiResult<>(500, "节点连接失败：" + e.getMessage());
 }
 String workspaceId = nodeService.getCheckUserWorkspace(request);

 MachineNodeModel existsMachine = machineNodeServer.getByUrl(machineNodeModel.getVoyager1Url());
 if (existsMachine == null) {
 // 插入
 machineNodeServer.insertAndNode(machineNodeModel, workspaceId);
 } else {
 boolean exists = nodeService.existsNode2(workspaceId, existsMachine.getId());
 Assert.state(!exists, "对应的节点已经存在拉");
 machineNodeServer.insertNode(machineNodeModel, workspaceId);
 }
 // 更新结果
 receiveCache.put("type", "success");
 return ApiResult.success("安装成功", NodeInfoController.listReceiveCache(null));
 }


}
