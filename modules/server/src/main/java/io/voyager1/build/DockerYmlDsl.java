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

package io.voyager1.build;
import io.voyager1.util.HttpStatus;
import io.voyager1.util.Method;
import io.voyager1.util.HttpRequest;
import io.voyager1.util.HttpUtil;

import io.voyager1.util.CollUtil;
import io.voyager1.util.ListUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.Validator;
import io.voyager1.util.ReUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.HttpResponse;
import io.voyager1.util.StrUtil;
import io.voyager1.util.YamlUtil;
import io.voyager1.model.BaseJsonModel;
import io.voyager1.plugin.IPlugin;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.IDockerConfigPlugin;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.func.assets.server.MachineDockerServer;
import io.voyager1.model.docker.DockerInfoModel;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.docker.DockerInfoService;
import io.voyager1.util.StringUtil;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * docker 构建 配置
 * <p>
 * <a href="https://www.jianshu.com/p/54cfa5721d5f">https://www.jianshu.com/p/54cfa5721d5f</a>
 *
 * @since 2022/1/25
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class DockerYmlDsl extends BaseJsonModel {

    /**
     * 基础镜像
     */
    private String runsOn;
    /**
     * 使用对应到 docker tag 构建
     */
    private String fromTag;
    /**
     * 构建步骤
     */
    private List<Map<String, Object>> steps;

    /**
     * 将本地文件复制到 容器
     * <p>
     * <host path>:<container path>:true
     * <p>
     * * If this flag is set to true, all children of the local directory will be copied to the remote without the root directory. For ex: if
     * * I have root/titi and root/tata and the remote path is /var/data. dirChildrenOnly = true will create /var/data/titi and /var/data/tata
     * * dirChildrenOnly = false will create /var/data/root/titi and /var/data/root/tata
     * *
     * * @param dirChildrenOnly
     * *            if root directory is ignored
     */
    private List<String> copy;
    /**
     * bind mounts 将宿主机上的任意位置的文件或者目录挂在到容器 （--mount type=bind,src=源目录,dst=目标目录）
     * /host:/container:ro
     */
    private List<String> binds;
    /**
     * 环境变量
     */
    private Map<String, String> env;
    /**
     * <a href="https://docs.docker.com/engine/api/v1.43/#tag/Container/operation/ContainerCreate">https://docs.docker.com/engine/api/v1.43/#tag/Container/operation/ContainerCreate</a>
     * <p>
     * cpuCount
     * <p>
     * cpuPercent
     * <p>
     * memoryReservation
     * <p>
     * cpusetCpus 允许执行的CPU（例如，0-3, 0,1）。
     * <p>
     * cpuShares
     */
    private Map<String, String> hostConfig;

    /**
     * 验证信息是否正确
     *
     * @param dockerInfoService   容器server
     * @param machineDockerServer 机器server
     * @param workspaceId         工作空间id
     * @param plugin              插件
     */
    public void check(DockerInfoService dockerInfoService,
                      MachineDockerServer machineDockerServer,
                      String workspaceId,
                      IDockerConfigPlugin plugin) {
        Assert.hasText(runsOn, "请填写runsOn。");
        Validator.validateMatchRegex(StringUtil.GENERAL_STR, runsOn, "runsOn 镜像名称不合法");
        Assert.state((steps != null && !steps.isEmpty()), "请填写 steps");
        this.stepsCheck(dockerInfoService, machineDockerServer, workspaceId, plugin);
    }

    /**
     * 检查 steps
     */
    private void stepsCheck(DockerInfoService dockerInfoService, MachineDockerServer machineDockerServer,
                            String workspaceId,
                            IDockerConfigPlugin plugin) {
        Set<String> usesSet = new HashSet<>();
        boolean containsRun = false;
        for (Map<String, Object> step : steps) {
            if (!containsRun && step.containsKey("run")) {
                containsRun = true;
            }
            if (step.containsKey("env")) {
                Object env1 = step.get("env");
                Assert.isInstanceOf(Map.class, env1, "env 必须是 map 类型");
            }
            if (step.containsKey("uses")) {
                Object uses1 = step.get("uses");
                Assert.isInstanceOf(String.class, uses1, "uses 只支持 String 类型");
                String uses = (String) step.get("uses");
                if ("node".equals(uses)) {
                    nodePluginCheck(step);
                } else if ("java".equals(uses)) {
                    javaPluginCheck(step);
                } else if ("gradle".equals(uses)) {
                    gradlePluginCheck(step);
                } else if ("maven".equals(uses)) {
                    mavenPluginCheck(step, dockerInfoService, machineDockerServer, workspaceId);
                } else if ("cache".equals(uses)) {
                    cachePluginCheck(step);
                } else if ("go".equals(uses)) {
                    goPluginCheck(step);
                } else if ("python3".equals(uses)) {
                    python3PluginCheck(step);
                } else {
                    // 其他自定义插件
                    File tmpDir = FileUtil.file(FileUtil.getTmpDir(), "check-users");
                    File pluginInstallResource = null;
                    try {
                        pluginInstallResource = plugin.getResourceToFile("uses/" + uses + "/install.sh", tmpDir);
                        Assert.notNull(pluginInstallResource, String.format("当前还不支持 %s 插件", uses));
                    } finally {
                        FileUtil.del(pluginInstallResource);
                    }
                }
                usesSet.add(uses);
            }
        }
        if (usesSet.contains("maven") && !usesSet.contains("java")) {
            throw new IllegalArgumentException("maven 插件依赖 java , 使用 maven 插件必须优先引入 java 插件");
        }
        if (usesSet.contains("gradle") && !usesSet.contains("java")) {
            throw new IllegalArgumentException("gradle 插件依赖 java , 使用 gradle 插件必须优先引入 java 插件");
        }
        Assert.isTrue(containsRun, "steps 中没有发现任何 run , run 用于执行命令");
    }

    private void cachePluginCheck(Map<String, Object> step) {
        Object path = step.get("path");
        Assert.notNull(path, "cache 插件 path 不能为空");
    }

    /**
     * 检查 maven 插件
     *
     * @param step 参数
     */
    private void mavenPluginCheck(Map<String, Object> step, DockerInfoService dockerInfoService, MachineDockerServer machineDockerServer, String workspaceId) {
        Object version1 = step.get("version");
        Assert.notNull(version1, "maven 插件 version 不能为空");
        String version = String.valueOf(version1);
        String link = String.format("https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/%s/binaries/apache-maven-%s-bin.tar.gz", version, version);
        HttpRequest request = HttpUtil.createRequest(Method.HEAD, link);
        try (HttpResponse httpResponse = request.execute()) {
            boolean success = httpResponse.isOk()
                || httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP
                || httpResponse.getStatus() == HttpStatus.HTTP_BAD_METHOD;
            if (success) {
                return;
            }
        }
        // 判断容器中是否存在
        try {
            // 根据 tag 查询
            List<DockerInfoModel> dockerInfoModels =
                dockerInfoService
                    .queryByTag(workspaceId, fromTag);
            Map<String, Object> map = machineDockerServer.dockerParameter(dockerInfoModels);
            if (map != null) {
                map.put("pluginName", "maven");
                map.put("version", version);
                IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
                boolean exists = ConvertUtil.toBool(plugin.execute("hasDependPlugin", map), false);
                if (exists) {
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("检查 docker 依赖错误:{}", e.getMessage());
        }
        // 提示远程版本
        Collection<String> pluginVersion = this.listMavenPluginVersion();
        throw new IllegalArgumentException("请填入正确的 maven 版本号,可用的版本如下：" + String.join(",", pluginVersion));
    }


    private Collection<String> listMavenPluginVersion() {
        String html = HttpUtil.get("https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/");
        //使用正则获取所有可用版本
        List<String> versions = new ArrayList<>();
        java.util.regex.Matcher versionMatcher = java.util.regex.Pattern.compile("<a\\s+href=\"3.*?/\">(.*?)</a>").matcher(html);
        while (versionMatcher.find()) {
            versions.add(versionMatcher.group(1));
        }
        Set<String> set = versions.stream()
            .map(s -> (s != null && s.endsWith("/") ? s.substring(0, s.length() - "/".length()) : s))
            .filter(StrUtil::isNotEmpty)
            .collect(Collectors.toSet());
        Assert.notEmpty(set, "maven 镜像库中没有找到任何可用的 maven 版本");
        return set;
    }

    /**
     * 检查 go 插件
     *
     * @param step 参数
     */
    private void javaPluginCheck(Map<String, Object> step) {
        Object version1 = step.get("version");
        Assert.notNull(version1, "java 插件 version 不能为空");
        Integer version = Integer.valueOf(String.valueOf(version1));
        List<Integer> supportedVersions = java.util.Arrays.asList(8, 11, 17);
        Assert.isTrue(supportedVersions.contains(version), String.format("目前java 插件支持的版本: %s", supportedVersions));
    }


    /**
     * 检查 gradle 插件
     *
     * @param step 参数
     */
    private void gradlePluginCheck(Map<String, Object> step) {
        Object version1 = step.get("version");
        Assert.notNull(version1, "gradle 插件 version 不能为空");
        String version = String.valueOf(version1);
        String link = String.format("https://downloads.gradle-dn.com/distributions/gradle-%s-bin.zip", version);
        HttpUtil.createRequest(Method.HEAD, link).thenFunction(httpResponse -> {
            Assert.isTrue(httpResponse.isOk() ||
                httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP ||
                httpResponse.getStatus() == HttpStatus.HTTP_SEE_OTHER, "请填入正确的 gradle 版本号");
            return null;
        });
    }

    /**
     * 检查 node 插件
     *
     * @param step 参数
     */
    private void nodePluginCheck(Map<String, Object> step) {
        Object version1 = step.get("version");
        Assert.notNull(version1, "node 插件 version 不能为空");
        String version = String.valueOf(version1);
        String link = String.format("https://registry.npmmirror.com/-/binary/node/v%s/node-v%s-linux-x64.tar.gz", version, version);
        HttpResponse httpResponse = HttpUtil.createRequest(Method.HEAD, link).execute();
        Assert.isTrue(httpResponse.isOk() || httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP, "请填入正确的 node 版本号");
    }

    /**
     * 检查 go 插件
     *
     * @param step 参数
     */
    private void goPluginCheck(Map<String, Object> step) {
        Object version1 = step.get("version");
        Assert.notNull(version1, "go 插件 version 不能为空");
        String version = String.valueOf(version1);
        String link = String.format("https://studygolang.com/dl/golang/go%s.linux-amd64.tar.gz", version);
        HttpUtil.createRequest(Method.HEAD, link).thenFunction(new Function<HttpResponse, Object>() {
            @Override
            public Object apply(HttpResponse httpResponse) {
                Assert.isTrue(httpResponse.isOk() ||
                    httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP ||
                    httpResponse.getStatus() == HttpStatus.HTTP_SEE_OTHER, "请填入正确的 go 版本号");
                return null;
            }
        });
    }

    /**
     * 检查 python3 插件
     *
     * @param step 参数
     */
    private void python3PluginCheck(Map<String, Object> step) {
        Object version1 = step.get("version");
        Assert.notNull(version1, "python3 插件 version 不能为空");
        String version = String.valueOf(version1);
        Assert.state((version != null && version.startsWith("3.")), () -> {
            //
            return "请填入正确的 python3 版本号";
        });
        String link = String.format("https://repo.huaweicloud.com/python/%s/Python-%s.tar.xz", version, version);
        HttpUtil.createRequest(Method.HEAD, link).thenFunction(new Function<HttpResponse, Object>() {
            @Override
            public Object apply(HttpResponse httpResponse) {
                Assert.isTrue(httpResponse.isOk() ||
                    httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP, "请填入正确的 python3 版本号");
                return null;
            }
        });

    }

    /**
     * 构建对象
     *
     * @param yml yml 内容
     * @return DockerYmlDsl
     */
    public static DockerYmlDsl build(String yml) {
        yml = yml.replace("\t", " " + " ");
        InputStream inputStream = new ByteArrayInputStream(yml.getBytes());
        return YamlUtil.load(inputStream, DockerYmlDsl.class);
    }
}
