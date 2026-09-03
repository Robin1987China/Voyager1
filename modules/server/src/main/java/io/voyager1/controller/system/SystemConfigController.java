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

package io.voyager1.controller.system;

import io.voyager1.util.BeanUtil;
import io.voyager1.util.CopyOptions;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.Validator;
import io.voyager1.util.Ipv4Util;
import io.voyager1.util.MaskBit;
import io.voyager1.util.CharsetUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Const;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.db.DbExtConfig;
import io.voyager1.model.data.SystemIpConfigModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.system.SystemParametersServer;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.system.init.ProxySelectorConfig;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 系统配置
 *
 * @since 2019/08/08
 */
@RestController
@RequestMapping(value = "system")
@Feature(cls = ClassFeature.SYSTEM_CONFIG)
@SystemPermission
@Slf4j
public class SystemConfigController extends BaseServerController {

 private final SystemParametersServer systemParametersServer;
 private final ProxySelectorConfig proxySelectorConfig;
 private final DbExtConfig dbExtConfig;
 private final javax.sql.DataSource dataSource;

 public SystemConfigController(SystemParametersServer systemParametersServer,
 ProxySelectorConfig proxySelectorConfig,
 DbExtConfig dbExtConfig, javax.sql.DataSource dataSource) {
 this.systemParametersServer = systemParametersServer;
 this.proxySelectorConfig = proxySelectorConfig;
 this.dbExtConfig = dbExtConfig;
 this.dataSource = dataSource;
 }

 /**
 * get server's config or node's config
 * 加载服务端或者节点端配置
 *
 * @param machineId 机器ID
 * @return json
 */
 @RequestMapping(value = "config-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
 @Feature(method = MethodFeature.LIST)
 public ApiResult<JSONObject> configData(String machineId, HttpServletRequest request) {
 ApiResult<JSONObject> message = this.tryRequestMachine(machineId, request, NodeUrl.SystemGetConfig);
 return Optional.ofNullable(message).orElseGet(() -> {
 JSONObject jsonObject = new JSONObject();
 Resource resource = ExtConfigBean.getResource();
 try {
 String content = IoUtil.read(resource.getInputStream(), StandardCharsets.UTF_8);
 jsonObject.put("content", content);
 jsonObject.put("file", FileUtil.getAbsolutePath(resource.getFile()));
 return ApiResult.success("", jsonObject);
 } catch (IOException e) {
 throw Lombok.sneakyThrow(e);
 }
 });
 }

 @PostMapping(value = "save_config.json", produces = MediaType.APPLICATION_JSON_VALUE)
 @Feature(method = MethodFeature.EDIT)
 @SystemPermission(superUser = true)
 public ApiResult<String> saveConfig(String machineId, String content, String restart, HttpServletRequest request) throws SQLException, IOException {
 ApiResult<String> jsonMessage = this.tryRequestMachine(machineId, request, NodeUrl.SystemSaveConfig);
 if (jsonMessage != null) {
 return jsonMessage;
 }
 Assert.hasText(content, "内容不能为空");

 ByteArrayResource byteArrayResource;
 try {
 YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
 // 
 byteArrayResource = new ByteArrayResource(content.replace("\t", " ").getBytes(StandardCharsets.UTF_8));
 yamlPropertySourceLoader.load("test", byteArrayResource);
 } catch (Exception e) {
 log.warn("内容格式错误，请检查修正", e);
 return new ApiResult<>(500, "内容格式错误，请检查修正:" + e.getMessage());
 }
 boolean restartBool = ConvertUtil.toBool(restart, false);
 // 修改数据库密码
 YamlMapFactoryBean yamlMapFactoryBean = new YamlMapFactoryBean();
 yamlMapFactoryBean.setResources(byteArrayResource);

 Map<String, Object> yamlMap = yamlMapFactoryBean.getObject();
 ConfigurationProperties configurationProperties = DbExtConfig.class.getAnnotation(ConfigurationProperties.class);
 Assert.notNull(configurationProperties, "没有找到数据库配置标识头");
 Map<String, Object> dbYamlMap = BeanUtil.getProperty(yamlMap, configurationProperties.prefix());
 Assert.notNull(dbYamlMap, "未解析出配置文件中的数据库配置信息");
 // 解析字段密码
 DbExtConfig dbExtConfig2 = BeanUtil.toBean(dbYamlMap, DbExtConfig.class, CopyOptions.create()
 .setIgnoreError(true)
 .setFieldNameEditor(SystemConfigController::toCamelCase));
 Assert.hasText(dbExtConfig2.getUserName(), "未配置(未解析到)数据库用户名");
 if (dbExtConfig2.getMode() == DbExtConfig.Mode.H2) {
 String newDbExtConfigUserName = dbExtConfig2.userName();
 String newDbExtConfigUserPwd = dbExtConfig2.userPwd();
 String oldDbExtConfigUserName = dbExtConfig.userName();
 String oldDbExtConfigUserPwd = dbExtConfig.userPwd();
 if (!java.util.Objects.equals(oldDbExtConfigUserName, newDbExtConfigUserName) || !java.util.Objects.equals(oldDbExtConfigUserPwd, newDbExtConfigUserPwd)) {
 // 执行修改数据库账号密码
 Assert.state(restartBool, "修改数据库密码必须重启");
 try (java.sql.Connection conn = dataSource.getConnection(); java.sql.Statement st = conn.createStatement()) {
    if (java.util.Objects.equals(oldDbExtConfigUserName, newDbExtConfigUserName)) {
        st.execute(String.format("ALTER USER %s SET PASSWORD '%s'", newDbExtConfigUserName, newDbExtConfigUserPwd));
    } else {
        st.execute(String.format("create user %s password '%s'; DROP USER %s", newDbExtConfigUserName, newDbExtConfigUserPwd, oldDbExtConfigUserName));
    }
}
 }
 }
 Resource resource = ExtConfigBean.getResource();
 Assert.state(resource.isFile(), "当前环境下不支持在线修改配置文件");
 FileUtil.writeString(content, resource.getFile(), StandardCharsets.UTF_8);

 if (restartBool) {
 // 重启
 Voyager1Application.restart();
 return ApiResult.success(Const.UPGRADE_MSG.get());
 }
 return ApiResult.success("修改成功");
 }


 /**
 * 加载服务端的 ip 授权配置
 *
 * @return json
 */
 @RequestMapping(value = "ip-config-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
 @Feature(cls = ClassFeature.SYSTEM_CONFIG_IP, method = MethodFeature.LIST)
 public ApiResult<JSONObject> ipConfigData() {
 SystemIpConfigModel config = systemParametersServer.getConfig(SystemIpConfigModel.ID, SystemIpConfigModel.class);
 JSONObject jsonObject = new JSONObject();
 if (config != null) {
 jsonObject.put("allowed", config.getAllowed());
 jsonObject.put("prohibited", config.getProhibited());
 }
 //jsonObject.put("path", FileUtil.getAbsolutePath(systemIpConfigService.filePath()));
 jsonObject.put("ip", getIp());
 return ApiResult.success("加载成功", jsonObject);
 }

 @RequestMapping(value = "save_ip_config.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
 @Feature(cls = ClassFeature.SYSTEM_CONFIG_IP, method = MethodFeature.EDIT)
 public ApiResult<Object> saveIpConfig(String allowed, String prohibited) {
 SystemIpConfigModel systemIpConfigModel = new SystemIpConfigModel();
 String allowed1 = (allowed == null || allowed.isEmpty() ? "" : allowed);
 this.checkIpV4(allowed1);
 systemIpConfigModel.setAllowed(allowed1);
 //
 String prohibited1 = (prohibited == null || prohibited.isEmpty() ? "" : prohibited);
 systemIpConfigModel.setProhibited(prohibited1);
 this.checkIpV4(prohibited1);
 systemParametersServer.upsert(SystemIpConfigModel.ID, systemIpConfigModel, SystemIpConfigModel.ID);
 //
 return ApiResult.success("修改成功");
 }

 /**
 * 检查是否为 ipv4
 *
 * @param ips ip
 */
 private void checkIpV4(String ips) {
 if ((ips == null || ips.isEmpty())) {
 return;
 }
 String[] split = ips.split(java.util.regex.Pattern.quote("\n"));
 for (String itemIp : split) {
 itemIp = itemIp.trim();
 if (itemIp.startsWith("#")) {
 continue;
 }
 if (java.util.Objects.equals(itemIp, "0.0.0.0")) {
 // 开放所有
 continue;
 }
 if ((itemIp != null && itemIp.contains(Ipv4Util.IP_MASK_SPLIT_MARK))) {
 String[] param = itemIp.split(java.util.regex.Pattern.quote(Ipv4Util.IP_MASK_SPLIT_MARK));
 Assert.state(Validator.isIpv4(param[0]), "请填写 ipv4 地址：" + itemIp);
 int count1 = StrUtil.count(param[0], ".");
 int count2 = StrUtil.count(param[1], ".");
 if (count1 == 3 && count2 == 3) {
 //192.168.1.0/192.168.1.200
 Assert.state(Validator.isIpv4(param[1]), "请填写 ipv4 地址：" + itemIp);
 continue;
 }
 if (count1 == 3 && count2 == 0) {
 //192.168.1.0/24
 int maskBit = ConvertUtil.toInt(param[1], 0);
 String s = MaskBit.get(maskBit);
 Assert.hasText(s, "子掩码不正确：" + itemIp);
 continue;
 }
 }
 boolean ipv4 = Validator.isIpv4(itemIp);
 Assert.state(ipv4, "请填写 ipv4 地址：" + itemIp);
 }
 }


 /**
 * 加载代理配置
 *
 * @return json
 */
 @GetMapping(value = "get_proxy_config", produces = MediaType.APPLICATION_JSON_VALUE)
 @Feature(method = MethodFeature.LIST)
 public ApiResult<JSONArray> getPoxyConfig() {
 JSONArray array = systemParametersServer.getConfigDefNewInstance(ProxySelectorConfig.KEY, JSONArray.class);
 return ApiResult.success("", array);
 }

 /**
 * 保存代理
 *
 * @param proxys 参数
 * @return json
 */
 @PostMapping(value = "save_proxy_config", produces = MediaType.APPLICATION_JSON_VALUE)
 public ApiResult<Object> saveProxyConfig(@RequestBody List<ProxySelectorConfig.ProxyConfigItem> proxys) {
 proxys = (proxys != null ? proxys : Collections.emptyList());
 for (ProxySelectorConfig.ProxyConfigItem proxy : proxys) {
 if ((proxy.getProxyAddress() != null && !proxy.getProxyAddress().isEmpty())) {
 machineNodeServer.testHttpProxy(proxy.getProxyAddress());
 }
 }
 systemParametersServer.upsert(ProxySelectorConfig.KEY, proxys, ProxySelectorConfig.KEY);
 proxySelectorConfig.refreshCache();
 return ApiResult.success("修改成功");
 }


 private static String toCamelCase(String s) {
 if (s == null || s.isEmpty()) {
 return s;
 }
 String[] parts = s.split("[-_]");
 StringBuilder sb = new StringBuilder(parts[0]);
 for (int i = 1; i < parts.length; i++) {
 if (!parts[i].isEmpty()) {
 sb.append(Character.toUpperCase(parts[i].charAt(0)));
 sb.append(parts[i].substring(1));
 }
 }
 return sb.toString();
 }
}
