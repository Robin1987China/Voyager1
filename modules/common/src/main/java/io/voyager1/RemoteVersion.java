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

package io.voyager1;

import com.alibaba.fastjson2.JSONObject;
import io.voyager1.core.AppType;
import io.voyager1.util.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 远程的版本信息
 *
 * <pre>
 * {
 * "tag_name": "v2.6.4",
 * "agentUrl": "",
 * "serverUrl": "",
 * "changelog": ""
 * }
 * </pre>
 */
@Data
@Slf4j
public class RemoteVersion {

    /**
     * 主 url 用于拉取远程版本信息
     * <p>
     * 默认不配置，请在 application.yml 中通过 remote-version-url 配置自己的版本服务地址
     */
    private static final String DEFAULT_URL = "";
    private static final String BETA_URL = "";
    private static String remoteVersionUrl;
    /**
     * 检查间隔时间
     */
    private static final int CHECK_INTERVAL = 24;

    /**
     * 版本信息
     */
    private String tagName;
    /**
     * 插件端下载地址
     */
    private String agentUrl;
    /**
     * 服务端下载地址
     */
    private String serverUrl;
    /**
     * 更新日志 (远程url)
     */
    private String changelogUrl;
    /**
     * 更新日志
     */
    private String changelog;
    /**
     * 上次获取时间
     */
    private Long lastTime;

    /**
     * 是否有新版本
     */
    private Boolean upgrade;
    /**
     * 是否为 beta 版本
     */
    private Boolean beta;
    /**
     * 下载源
     */
    private String downloadSource;
    /**
     * 认证信息
     */
    private RemoteVersionAuth auth;

    @Data
    public static class RemoteVersionAuth {
        /**
         * 下载源
         */
        private String downloadSource;
        /**
         * 插件端下载地址
         */
        private String agentUrl;
        /**
         * 服务端下载地址
         */
        private String serverUrl;
    }

    public String getDownloadSource() {
        String voyager1RemoteVersionAuth = (System.getenv("VOYAGER1_REMOTE_VERSION_AUTH") != null ? System.getenv("VOYAGER1_REMOTE_VERSION_AUTH") : (System.getProperty("VOYAGER1_REMOTE_VERSION_AUTH") != null ? System.getProperty("VOYAGER1_REMOTE_VERSION_AUTH") : ""));
        if ((voyager1RemoteVersionAuth != null && !voyager1RemoteVersionAuth.isEmpty())) {
            RemoteVersionAuth auth = this.getAuth();
            if ((auth.getDownloadSource() != null && !auth.getDownloadSource().isEmpty())) {
                return auth.getDownloadSource();
            }
        }
        return this.downloadSource;
    }

    public static void setRemoteVersionUrl(String remoteVersionUrl) {
        RemoteVersion.remoteVersionUrl = remoteVersionUrl;
    }

    /**
     * 获取 版本检查的 url
     *
     * @return 远程地址
     */
    private static String loadDefaultUrl() {
        boolean beta = betaRelease();
        return beta ? BETA_URL : DEFAULT_URL;
    }

    /**
     * 判断当前是否加入 beta 计划
     *
     * @return true 已经加入 false 未加入
     */
    public static boolean betaRelease() {
        String betaRelease = SystemPropsUtil.get("JOIN_VOYAGER1_BETA_RELEASE", "");
        return ConvertUtil.toBool(betaRelease, false);
    }

    public static void changeBetaRelease(String beta) {
        SystemPropsUtil.set("JOIN_VOYAGER1_BETA_RELEASE", beta);
    }

    /**
     * 获取远程最新版本
     *
     * @return 版本信息
     */
    public static RemoteVersion loadRemoteInfo() {
        String body = "";
        try {
            String remoteVersionUrl = (RemoteVersion.remoteVersionUrl == null || RemoteVersion.remoteVersionUrl.isEmpty() ? loadDefaultUrl() : RemoteVersion.remoteVersionUrl);
            remoteVersionUrl = Validator.isUrl(remoteVersionUrl) ? remoteVersionUrl : loadDefaultUrl();
            if (!Validator.isUrl(remoteVersionUrl)) {
                // 未配置版本检查地址，直接跳过
                return null;
            }
            // 获取缓存中到信息
            RemoteVersion remoteVersion = RemoteVersion.loadTransitUrl(remoteVersionUrl);
            if (remoteVersion == null || (remoteVersion.getTagName() == null || remoteVersion.getTagName().isEmpty())) {
                // 没有版本信息
                return null;
            }
            // 缓存信息
            RemoteVersion.cacheLoadTime(remoteVersion);
            return remoteVersion;
        } catch (Exception e) {
            log.warn("获取远程版本信息失败:{} {}", e.getMessage(), body);
            return null;
        }
    }

    /**
     * 获取第一层信息，用于中转
     *
     * @param remoteVersionUrl 请url
     * @return 中转URL
     */
    private static RemoteVersion loadTransitUrl(String remoteVersionUrl) {
        String body = "";
        try {
            log.debug("use remote version url: {}", remoteVersionUrl);
            HttpRequest request = HttpUtil.createGet(remoteVersionUrl, true);
            request.timeout(10 * 1000);
            try (HttpResponse execute = request.execute()) {
                body = execute.body();
            }
            //
            JSONObject jsonObject = JSONObject.parseObject(body);
            RemoteVersion remoteVersion = jsonObject.to(RemoteVersion.class);
            if (StrUtil.isAllNotEmpty(remoteVersion.getTagName(), remoteVersion.getAgentUrl(), remoteVersion.getServerUrl(), remoteVersion.getServerUrl())) {
                return remoteVersion;
            }
            String jumpUrl = jsonObject.getString("url");
            if ((jumpUrl == null || jumpUrl.isEmpty())) {
                return null;
            }
            return loadTransitUrl(jumpUrl);
        } catch (Exception e) {
            log.warn("获取远程版本信息失败:{} {}", e.getMessage(), body);
            return null;
        }
    }

    /**
     * 缓存信息
     *
     * @param remoteVersion 远程版本信息
     */
    private static void cacheLoadTime(RemoteVersion remoteVersion) {
        remoteVersion = (remoteVersion != null ? remoteVersion : new RemoteVersion());
        remoteVersion.setLastTime(System.currentTimeMillis());
        // 判断是否可以升级
        boolean isDebug = Boolean.parseBoolean((System.getenv("VOYAGER1_IS_DEBUG") != null ? System.getenv("VOYAGER1_IS_DEBUG") : (System.getProperty("VOYAGER1_IS_DEBUG") != null ? System.getProperty("VOYAGER1_IS_DEBUG") : "")));
        String voyager1Type = (System.getenv("VOYAGER1_TYPE") != null ? System.getenv("VOYAGER1_TYPE") : (System.getProperty("VOYAGER1_TYPE") != null ? System.getProperty("VOYAGER1_TYPE") : ""));
        AppType type = EnumUtil.fromStringQuietly(AppType.class, voyager1Type);
        Assert.notNull(type, "没有配置正确的环境变量");
        if (!isDebug) {
            String version = (System.getenv("VOYAGER1_VERSION") != null ? System.getenv("VOYAGER1_VERSION") : (System.getProperty("VOYAGER1_VERSION") != null ? System.getProperty("VOYAGER1_VERSION") : ""));
            String tagName = remoteVersion.getTagName();
            tagName = StrUtil.removePrefixIgnoreCase(tagName, "v");
            remoteVersion.setUpgrade(StrUtil.compareVersion(version, tagName) < 0);
        } else {
            remoteVersion.setUpgrade(false);
        }
        // 检查是否存在下载地址

        String remoteUrl = type.getRemoteUrl(remoteVersion);
        if ((remoteUrl == null || remoteUrl.isEmpty())) {
            remoteVersion.setUpgrade(false);
        }
        // 获取 changelog
        String changelogUrl = remoteVersion.getChangelogUrl();
        if ((changelogUrl != null && !changelogUrl.isEmpty())) {
            try (HttpResponse execute = HttpUtil.createGet(changelogUrl, true).execute()) {
                String body = execute.body();
                remoteVersion.setChangelog(body);
            }
        }
        //
        File file = getFile();
        FileUtil.writeUtf8String(remoteVersion.toString(), file);
    }

    /**
     * 当前缓存中的 远程版本信息
     *
     * @return RemoteVersion
     */
    public static RemoteVersion cacheInfo() {
        File file = getFile();
        if (!FileUtil.isFile(file)) {
            return null;
        }
        RemoteVersion remoteVersion = null;
        String fileStr = "";
        try {
            fileStr = FileUtil.readUtf8String(file);
            if ((fileStr == null || fileStr.isEmpty())) {
                return null;
            }
            remoteVersion = JSONObject.parseObject(fileStr).to(RemoteVersion.class);
        } catch (Exception e) {
            log.warn("解析远程版本信息失败:{} {}", e.getMessage(), fileStr);
        }
        // 判断上次获取时间
        Long lastTime = remoteVersion == null ? 0 : remoteVersion.getLastTime();
        lastTime = (lastTime != null ? lastTime : 0L);
        long interval = System.currentTimeMillis() - lastTime;
        return interval >= TimeUnit.HOURS.toMillis(CHECK_INTERVAL) ? null : remoteVersion;
    }


    /**
     * 保存的文件
     *
     * @return file
     */
    private static File getFile() {
        String cacheFile = (System.getenv("VOYAGER1_REMOTE_VERSION_CACHE_FILE") != null ? System.getenv("VOYAGER1_REMOTE_VERSION_CACHE_FILE") : (System.getProperty("VOYAGER1_REMOTE_VERSION_CACHE_FILE") != null ? System.getProperty("VOYAGER1_REMOTE_VERSION_CACHE_FILE") : ""));
        Assert.state((cacheFile != null && !cacheFile.isEmpty()), "没有配置正确的环境变量");
        return FileUtil.file(cacheFile);
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }
}
