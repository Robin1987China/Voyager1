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

package io.voyager1.common;

import com.alibaba.fastjson2.JSONObject;
import io.voyager1.Voyager1Application;
import io.voyager1.core.AppType;
import io.voyager1.core.api.ApiResult;
import io.voyager1.system.Voyager1RuntimeException;
import io.voyager1.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Voyager1 的运行信息
 *
 * @since 2019/4/7
 */
@Slf4j
public class Voyager1Manifest {

 private volatile static Voyager1Manifest VOYAGER1_MANIFEST;
 /**
 * 允许降级
 */
 private volatile static boolean allowedDowngrade;
 /**
 * 当前版本
 */
 private String version = "dev";
 /**
 * 打包时间
 */
 private String timeStamp;
 /**
 * 进程id
 */
 private long pid = SystemUtil.getCurrentPID();
 /**
 * 当前运行类型
 */
 private final AppType type = Voyager1Application.getAppType();
 /**
 * 端口号
 */
 private int port;
 /**
 * 随机ID
 */
 private String randomId;
 /**
 * Voyager1 的数据目录
 */
 private String dataPath;
 /**
 * jar 运行路径
 */
 private String jarFile;
 /**
 * 系统名称
 */
 private final String osName = SystemUtil.getOsInfo().getName();
 /**
 * 安装id
 */
 private String installId;

 private static Voyager1Manifest buildVoyager1Manifest() {
 Voyager1Manifest voyager1Manifest = new Voyager1Manifest();
 File jarFile = getRunPath();
 Tuple jarVersion = getJarVersion(jarFile);
 if (jarVersion != null) {
 voyager1Manifest.setVersion(jarVersion.get(0));
 voyager1Manifest.setTimeStamp(jarVersion.get(1));
 }
 voyager1Manifest.setJarFile(FileUtil.getAbsolutePath(jarFile));
 //
 voyager1Manifest.randomId = java.util.UUID.randomUUID().toString().replace("-", "");
 return voyager1Manifest;
 }

 private static String buildOsInfo() {
 // Windows NT 10.0; Win64; x64
 OsInfo osInfo = SystemUtil.getOsInfo();
 JavaInfo javaInfo = SystemUtil.getJavaInfo();
 boolean inDocker = (SystemUtil.get("VOYAGER1_PKG") != null && !SystemUtil.get("VOYAGER1_PKG").isEmpty());
 String osName = Opt.ofBlankAble(osInfo.getName()).orElseGet(() -> "Unknown");
 return String.format("%s %s; %s; %s", inDocker ? "docker" : osName, Opt.ofBlankAble(osInfo.getVersion()).orElse("0"), Opt.ofBlankAble(osInfo.getArch()).orElse("Unknown"), Opt.ofBlankAble(javaInfo.getVersion()).orElse("Unknown"));
 }

 /**
 * 根据 jar 文件解析 voyager1 版本信息
 *
 * @param jarFile 文件
 * @return 版本, 打包时间, mainClass
 */
 private static Tuple getJarVersion(File jarFile) {
 Manifest manifest = ManifestUtil.getManifest(jarFile);
 if (manifest != null) {
 Attributes attributes = manifest.getMainAttributes();
 String version = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
 if (version != null) {
 // @see VersionUtils#getVersion()
 String timeStamp = attributes.getValue("Voyager1-Timestamp");
 timeStamp = parseVoyager1Time(timeStamp);
 String mainClass = attributes.getValue(Attributes.Name.MAIN_CLASS);
 String voyager1MinVersion = attributes.getValue("Voyager1-Min-Version");
 return new Tuple(version, timeStamp, mainClass, jarFile, voyager1MinVersion);
 }
 }
 return null;
 }


 private Voyager1Manifest() {
 }

 /**
 * 单利模式获取Voyager1 信息
 *
 * @return this
 */
 public static Voyager1Manifest getInstance() {
 if (VOYAGER1_MANIFEST == null) {
 synchronized (Voyager1Manifest.class) {
 if (VOYAGER1_MANIFEST == null) {
 VOYAGER1_MANIFEST = buildVoyager1Manifest();
 }
 String voyager1Tag = String.format("Voyager1 %s/%s", VOYAGER1_MANIFEST.getType(), VOYAGER1_MANIFEST.getVersion());
 String osInfo = buildOsInfo();
 // Mozilla/5.0 (${os-name} ${os-version}; ${os-arch}; ${jdk-version}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.0000.000 Safari/537.36 ${voyager1-type}/${voyager1-version}
 GlobalHeaders.INSTANCE.header(Header.USER_AGENT, String.format("Mozilla/5.0 (%s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.0000.000 Safari/537.36 %s", osInfo, voyager1Tag), true);
 }
 }
 return VOYAGER1_MANIFEST;
 }

 public AppType getType() {
 return type;
 }

 public long getPid() {
 return pid;
 }

 public void setPid(int pid) {
 this.pid = pid;
 }

 public String randomIdSign() {
 String tempToken = SystemUtil.get("VOYAGER1_SERVER_TEMP_TOKEN");
 return Opt.ofBlankAble(tempToken).orElseGet(() -> DigestUtil.sha1(Voyager1Manifest.this.getPid() + Voyager1Manifest.this.randomId));

 }

 /**
 * 获取当前运行的版本号
 *
 * @return 返回当前版本号
 */
 public String getVersion() {
 return version;
 }

 /**
 * 判断当前是否为调试模式
 *
 * @return jar 为非调试模式
 */
 public boolean isDebug() {
 return "dev".equals(getVersion());
 }

 public void setVersion(String version) {
 if ((version != null && !version.isEmpty())) {
 this.version = version;
 }
 }

 public String getJarFile() {
 return jarFile;
 }

 public void setJarFile(String jarFile) {
 this.jarFile = jarFile;
 }

 public String getTimeStamp() {
 if (timeStamp == null) {
 long uptime = SystemUtil.getRuntimeMXBean().getUptime();
 long statTime = System.currentTimeMillis() - uptime;
 return new DateTime(statTime).toString();
 }
 return timeStamp;
 }

 /**
 * 装换打包时间
 *
 * @param timeStamp utc时间
 */
 public void setTimeStamp(String timeStamp) {
 this.timeStamp = timeStamp;
 }

 public void setPort(int port) {
 this.port = port;
 }

 /**
 * 程序运行的端口
 *
 * @return 端口
 */
 public int getPort() {
 if (port == 0) {
 port = Voyager1Application.getInstance().getPort();
 }
 return port;
 }

 public String getDataPath() {
 if ((dataPath == null || dataPath.isEmpty())) {
 dataPath = Voyager1Application.getInstance().getDataPath();
 }
 return dataPath;
 }

 public void setDataPath(String dataPath) {
 this.dataPath = dataPath;
 }

 public long getUpTime() {
 return SystemUtil.getRuntimeMXBean().getUptime();
 }

 public String getOsName() {
 return osName;
 }

 public String getInstallId() {
 return installId;
 }

 public void setInstallId(String installId) {
 this.installId = installId;
 }

 @Override
 public String toString() {
 return JSONObject.toJSONString(this);
 }

 /**
 * 获取当前运行的路径
 *
 * @return jar 或者classPath
 */
 public static File getRunPath() {
 URL location = ClassUtil.getLocation(Voyager1Application.getAppClass());
 String file = location.getFile();
 String before = StrUtil.subBefore(file, "!", false);
 // Spring Boot fat jar 的 codeSource location.getFile() 形如：
 //   nested:/path/app.jar/!BOOT-INF/classes!/  （Spring Boot 3.2+ nested jar）
 //   /path/app.jar!/BOOT-INF/classes!/         （普通 fat jar）
 //   file:/path/app.jar!/BOOT-INF/classes!/    （旧版）
 // 需去掉 nested:/file:/jar:file: 前缀与结尾的 /，否则 getManifest(File) 因 exists()==false 读不到版本号
 if (before.startsWith("nested:")) {
  before = before.substring("nested:".length());
 } else if (before.startsWith("jar:nested:")) {
  before = before.substring("jar:nested:".length());
 } else if (before.startsWith("jar:file:")) {
  before = before.substring("jar:file:".length());
 } else if (before.startsWith("file:")) {
  before = before.substring("file:".length());
 }
 while (before.endsWith("/")) {
  before = before.substring(0, before.length() - 1);
 }
 return FileUtil.file(before);
 }

 /**
 * 升级之后的旧包
 *
 * @return oldJars
 */
 public static File getOldJarsPath() {
 File runFile = getRunPath().getParentFile();
 return FileUtil.file(runFile, "oldJars");
 }

 /**
 * 转化时间
 *
 * @param timeStamp time
 * @return 默认使用utc
 */
 private static String parseVoyager1Time(String timeStamp) {
 if ((timeStamp != null && !timeStamp.isEmpty())) {
 try {
 DateTime dateTime = DateUtil.parseUTC(timeStamp);
 return dateTime.toStringDefaultTimeZone();
 } catch (Exception e) {
 return timeStamp;
 }
 } else {
 return "dev";
 }
 }

 /**
 * 检查是否为voyager1包
 *
 * @param path 路径
 * @param type 类型
 * @return 结果消息
 * @see AppType#getApplicationClass()
 */
 public static ApiResult<Tuple> checkVoyager1Jar(String path, AppType type) {
 return checkVoyager1Jar(path, type, true);
 }

 public static void setAllowedDowngrade(boolean allowedDowngrade) {
 Voyager1Manifest.allowedDowngrade = allowedDowngrade;
 }

 /**
 * 检查是否为voyager1包
 *
 * @param path 路径
 * @param type 类型
 * @param checkRepeat 是否检查版本重复
 * @return 结果消息
 * @see AppType#getApplicationClass()
 */
 public static ApiResult<Tuple> checkVoyager1Jar(String path, AppType type, boolean checkRepeat) {
 File jarFile = new File(path);
 Tuple jarVersion = getJarVersion(jarFile);
 if (jarVersion == null) {
 return new ApiResult<>(405, "jar 包文件不合法");
 }
 try (JarFile jarFile1 = new JarFile(jarFile)) {
 //Manifest manifest = jarFile1.getManifest();
 //Attributes attributes = manifest.getMainAttributes();
 String mainClass = jarVersion.get(2);
 if (mainClass == null) {
 return new ApiResult<>(405, "清单文件中没有找到对应的MainClass属性");
 }
 try (JarClassLoader jarClassLoader = JarClassLoader.load(jarFile)) {
 jarClassLoader.loadClass(mainClass);
 } catch (ClassNotFoundException notFound) {
 return new ApiResult<>(405, "中没有找到对应的MainClass:" + mainClass);
 }
 String applicationClass = type.getApplicationClass();
 ZipEntry entry = jarFile1.getEntry(String.format("BOOT-INF/classes/%s.class", applicationClass.replace(".", "/")));
 if (entry == null) {
 return new ApiResult<>(405, String.format("此包不是Voyager1【%s】包", type.name()));
 }
 String version = jarVersion.get(0);
 String timeStamp = jarVersion.get(1);
 String minVersion = jarVersion.get(4);
 if (((version == null || version.isEmpty()) || (timeStamp == null || timeStamp.isEmpty()) || (minVersion == null || minVersion.isEmpty()))) {
 return new ApiResult<>(405, "此包没有版本号、打包时间、最小兼容版本");
 }
 if (checkRepeat) {
 //
 Voyager1Manifest voyager1Manifest = Voyager1Manifest.getInstance();
 if (java.util.Objects.equals(version, voyager1Manifest.getVersion()) &&
 java.util.Objects.equals(timeStamp, voyager1Manifest.getTimeStamp())) {
 return new ApiResult<>(405, "新包和正在运行的包一致");
 }
 if (StrUtil.compareVersion(voyager1Manifest.getVersion(), minVersion) < 0) {
 return new ApiResult<>(405, String.format("当前程序版本 %s 新版程序最低兼容 %s 不能直接升级", voyager1Manifest.getVersion(), minVersion));
 }
 // 判断降级
 if (!allowedDowngrade && StrUtil.compareVersion(version, voyager1Manifest.getVersion()) < 0) {
 return new ApiResult<>(405, "在线升级不能降级操作");
 }
 }
 } catch (Exception e) {
 log.error("解析jar", e);
 return new ApiResult<>(500, " 解析错误:" + e.getMessage());
 }
 return new ApiResult<>(200, "", jarVersion);
 }

 /**
 * 发布包到对应运行路径
 *
 * @param path 文件路径
 * @param version 新版本号
 */
 public static void releaseJar(String path, String version) {
 File runFile = getRunPath();
 File runPath = runFile.getParentFile();
 if (!runPath.isDirectory()) {
 throw new Voyager1RuntimeException(runPath.getAbsolutePath() + " error");
 }
 String upgrade = FileUtil.file(runPath, Const.UPGRADE).getAbsolutePath();
 JSONObject jsonObject = null;
 try {
 jsonObject = JsonFileUtil.readJson(upgrade);
 } catch (FileNotFoundException ignored) {
 }
 if (jsonObject == null) {
 jsonObject = new JSONObject();
 }
 jsonObject.put("beforeJar", runFile.getName());
 // 如果升级的版本号一致
 if (java.util.Objects.equals(version, Voyager1Manifest.getInstance().getVersion())) {
 version = String.format("%s_%s", version, System.currentTimeMillis());
 }
 String newFile;
 File to;
 while (true) {
 newFile = Voyager1Application.getAppType().name() + "-" + version + FileUtil.JAR_FILE_EXT;
 to = FileUtil.file(runPath, newFile);
 if (FileUtil.equals(to, runFile)) {
 version = String.format("%s_%s", version, RandomUtil.randomInt(1, 100));
 continue;
 }
 break;
 }
 //
 FileUtil.move(new File(path), to, true);
 jsonObject.put("newJar", newFile);
 jsonObject.put("updateTime", new DateTime().toString());
 // 新增升级次数
 jsonObject.put("upgradeCount", jsonObject.getIntValue("upgradeCount"));
 //
 JsonFileUtil.saveJson(upgrade, jsonObject);
 FileUtil.writeString(newFile, FileUtil.file(runPath, Const.RUN_JAR), StandardCharsets.UTF_8);
 }

 /**
 * 获取当前的管理名文件
 *
 * @return file
 */
 public static File getScriptFile() {
 File runPath = getRunPath().getParentFile().getParentFile();
 String type = Voyager1Application.getAppType().name();
 File scriptFile = FileUtil.file(runPath, "bin", String.format("%s.%s", type, CommandUtil.SUFFIX));
 Assert.state(FileUtil.isFile(scriptFile), String.format("当前服务中没有命令脚本：%s.%s", type, CommandUtil.SUFFIX));
 return scriptFile;
 }

 /**
 * 解析 voyager1 安装包
 *
 * @param path 文件路径
 * @param type 查找类型
 * @param savePath 保存对文件夹
 * @return 结果文件
 */
 public static File zipFileFind(String path, AppType type, String savePath) throws IOException {
 String extName = FileUtil.extName(path);
 if (StrUtil.endWithIgnoreCase(extName, "zip")) {
 try (ZipFile zipFile = ZipUtil.toZipFile(FileUtil.file(path), StandardCharsets.UTF_8)) {
 Optional<? extends ZipEntry> first = zipFile.stream().filter((Predicate<ZipEntry>) zipEntry -> {
 String name = zipEntry.getName().toLowerCase();
 String typeName = type.name().toLowerCase();
 return (name != null && name.startsWith("lib/" + typeName)) && (name != null && name.endsWith(".jar"));
 }).findFirst();
 Assert.state(first.isPresent(), String.format("上传的压缩包不是 Voyager1 [%s] 包", type));
 //
 ZipEntry zipEntry = first.get();
 try (InputStream stream = ZipUtil.getStream(zipFile, zipEntry)) {
 String name = FileUtil.getName(zipEntry.getName());
 return FileUtil.writeFromStream(stream, FileUtil.file(savePath, name));
 }
 }
 } else if (StrUtil.endWithIgnoreCase(extName, "jar")) {
 return FileUtil.file(path);
 }
 throw new IllegalArgumentException("此文件不是 voyager1 安装包");
 }
}
