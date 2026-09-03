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

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.MapUtil;

import io.voyager1.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.AuthResponse;
import com.github.dockerjava.api.model.ResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.ssh.JschDockerHttpClient;

import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @since 2022/1/26
 */
@Slf4j
public class DockerUtil {

    static {
        // 禁用 jackson
        JSONFactory.setUseJacksonAnnotation(false);
        // 枚举对象使用 枚举名称
        JSON.config(JSONWriter.Feature.WriteEnumsUsingName);
    }

    private static final Map<String, DockerClient> DOCKER_CLIENT_MAP = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, AutoCloseable> CACHE_CLOSET = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * dockerfile 文件名称
     */
    public static final String DOCKER_FILE = "Dockerfile";

    /**
     * 获取 docker client ，会使用缓存
     * <p>
     * 如果参数包含 closeBefore 则重新创建
     *
     * @param parameter 参数
     * @return DockerClient
     */
    public static DockerClient get(Map<String, Object> parameter) {
        String host = (String) parameter.get("dockerHost");
        String dockerCertPath = (String) parameter.get("dockerCertPath");
        String key = String.format("%s-%s", host, (dockerCertPath == null || dockerCertPath.isEmpty() ? "" : dockerCertPath));
        if (parameter.containsKey("closeBefore")) {
            //  关闭之前的连接
            DockerClient dockerClient = DOCKER_CLIENT_MAP.remove(key);
            IoUtil.close(dockerClient);
        }
        return DOCKER_CLIENT_MAP.computeIfAbsent(key, s -> create(parameter));
    }

    /**
     * 构建 docker client 对象
     *
     * @param parameter 参数
     * @return DockerClient
     */
    private static DockerClient create(Map<String, Object> parameter) {
        String host = (String) parameter.get("dockerHost");
        String apiVersion = (String) parameter.get("apiVersion");
        String dockerCertPath = (String) parameter.get("dockerCertPath");
        String registryUsername = (String) parameter.get("registryUsername");
        String registryPassword = (String) parameter.get("registryPassword");
        String registryEmail = (String) parameter.get("registryEmail");
        String registryUrl = (String) parameter.get("registryUrl");
        Boolean sshUseSudo = (Boolean) parameter.get("sshUseSudo");
        Supplier<Session> sessionSupplier = (Supplier<Session>) parameter.get("session");
        //
        DefaultDockerClientConfig.Builder defaultConfigBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
        defaultConfigBuilder
            .withDockerTlsVerify((dockerCertPath != null && !dockerCertPath.isEmpty()))
            .withApiVersion(apiVersion)
            .withDockerCertPath(dockerCertPath)
            .withDockerHost(host);
        //
        Opt.ofBlankAble(registryUrl).ifPresent(s -> defaultConfigBuilder.withRegistryUrl(registryUrl));
        Opt.ofBlankAble(registryEmail).ifPresent(s -> defaultConfigBuilder.withRegistryEmail(registryEmail));
        Opt.ofBlankAble(registryUsername).ifPresent(s -> defaultConfigBuilder.withRegistryUsername(registryUsername));
        Opt.ofBlankAble(registryPassword).ifPresent(s -> defaultConfigBuilder.withRegistryPassword(registryPassword));

        DockerClient dockerClient;
        DockerClientConfig config = defaultConfigBuilder.build();
        if (sessionSupplier != null) {
            // 通过SSH连接Docker
            JschDockerHttpClient httpClient = new JschDockerHttpClient(config.getDockerHost(), sshUseSudo != null && sshUseSudo, sessionSupplier);
            dockerClient = DockerClientImpl.getInstance(config, httpClient);
        } else {
            ApacheDockerHttpClient.Builder builder = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100);
            int timeout = ConvertUtil.toInt(parameter.get("timeout"), 0);
            if (timeout > 0) {
                builder.connectionTimeout(Duration.ofSeconds(timeout));
                builder.responseTimeout(Duration.ofSeconds(timeout));
            }
            ApacheDockerHttpClient httpClient = builder.build();
            dockerClient = DockerClientImpl.getInstance(config, httpClient);
        }
        if ((registryUrl != null && !registryUrl.isEmpty())) {
            AuthConfig authConfig = dockerClient.authConfig();
            AuthResponse authResponse = dockerClient.authCmd().withAuthConfig(authConfig).exec();
            log.debug("auth cmd:{}", JSONObject.toJSONString(authResponse));
        }
        return dockerClient;
    }

    /**
     * 临时文件目录
     *
     * @param name    文件名
     * @param tempDir 临时文件目录
     * @return temp
     */
    public static File createTemp(String name, File tempDir) {
        return FileUtil.file(tempDir, name);
    }

    /**
     * 获取进度信息
     *
     * @param responseItem 响应结果
     * @return 转化为 字符串
     */
    public static String parseResponseItem(ResponseItem responseItem) {
        String stream = responseItem.getStream();
        if (stream == null) {
            String status = responseItem.getStatus();
            if (status == null) {
                Map<String, Object> rawValues = responseItem.getRawValues();
                return MapUtil.join(rawValues, ",", "=") + "\n";
            }
            String progress = responseItem.getProgress();
            progress = (progress == null || progress.isEmpty() ? "" : progress);
            String id = responseItem.getId();
            id = (id == null || id.isEmpty() ? "" : id);
            return String.format("%s %s %s", status, id, progress);
        }
        return stream;
    }

    /**
     * 深度转换为 json object
     *
     * @param object 对象
     * @return 转换后的
     */
    public static JSONObject toJSON(Object object) {
        String jsonString = JSONObject.toJSONString(object);
        return (JSONObject) JSON.parse(jsonString);
    }

    public static void putClose(String id, AutoCloseable autoCloseable) {
        AutoCloseable closeable = CACHE_CLOSET.put(id, autoCloseable);
        // 关闭上一次资源
        IoUtil.close(closeable);
    }

    public static void close(String id) {
        IoUtil.close(CACHE_CLOSET.remove(id));
    }
}
