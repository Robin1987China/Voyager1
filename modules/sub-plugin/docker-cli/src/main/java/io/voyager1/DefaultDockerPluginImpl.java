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
import io.voyager1.util.EnumUtil;
import io.voyager1.util.ReflectUtil;

import io.voyager1.util.DataSizeUtil;

import io.voyager1.util.CollStreamUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.InvocationTargetRuntimeException;
import io.voyager1.util.IoUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.Tuple;
import io.voyager1.util.MapUtil;
import io.voyager1.util.UrlQuery;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.InvocationTargetRuntimeException;
import io.voyager1.util.StrUtil;
import io.voyager1.util.Tuple;
import io.voyager1.util.UrlQuery;
import io.voyager1.plugin.PluginConfig;
import com.alibaba.fastjson2.JSONObject;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.InvocationBuilder;
import com.github.dockerjava.core.NameParser;
import lombok.Lombok;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.util.StringUtil;
import org.springframework.util.Assert;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

/**
 * docker 插件
 *
 * @since 2022/1/26
 */
@PluginConfig(name = "docker-cli")
@Slf4j
public class DefaultDockerPluginImpl implements IDockerConfigPlugin {


    @Override
    public Object execute(Object main, Map<String, Object> parameter) throws Exception {
        String type = main.toString();
        if ("build".equals(type)) {
            try (DockerBuild dockerBuild = new DockerBuild(parameter, this)) {
                return dockerBuild.build();
            }
        }
        Method method = ReflectUtil.getMethodByName(this.getClass(), type + "Cmd");
        Assert.notNull(method, "不支持的类型:" + type);
        try {
            return ReflectUtil.invoke(this, method, parameter);
        } catch (InvocationTargetRuntimeException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof InvocationTargetException) {
                InvocationTargetException invocationTargetException = (InvocationTargetException) cause;
                throw Lombok.sneakyThrow(invocationTargetException.getTargetException());
            }
            throw Lombok.sneakyThrow(cause);
        }
    }

    /**
     * 裁剪
     * <a href="https://blog.csdn.net/zhanremo3062/article/details/120860327">https://blog.csdn.net/zhanremo3062/article/details/120860327</a>
     *
     * @param parameter 参数
     * @return 回收空间
     */
    private Long pruneCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        String pruneTypeStr = (String) parameter.get("pruneType");

        PruneType pruneType = EnumUtil.fromString(PruneType.class, pruneTypeStr, null);
        Assert.notNull(pruneType, "pruneType 未知");
        String until = (String) parameter.get("until");
        String labels = (String) parameter.get("labels");
        String dangling = (String) parameter.get("dangling");
        PruneCmd pruneCmd = dockerClient.pruneCmd(pruneType);
        Opt.ofBlankAble(dangling).map(s -> ConvertUtil.toBool(s, true)).ifPresent(pruneCmd::withDangling);
        if (until != null && !until.isEmpty()) pruneCmd.withUntilFilter(until);
        Opt.ofBlankAble(labels).ifPresent(s -> pruneCmd.withLabelFilter(s.split(",")));
        PruneResponse pruneResponse = pruneCmd.exec();
        return pruneResponse.getSpaceReclaimed();
    }

    private Map<String, JSONObject> statsCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        String containerId = (String) parameter.get("containerId");
        List<String> split = java.util.Arrays.asList(containerId.split(","));
        return split.stream().map(s -> {
            Statistics statistics = dockerClient.statsCmd(s).exec(new InvocationBuilder.AsyncResultCallback<Statistics>() {
                @SneakyThrows
                @Override
                public void onNext(Statistics object) {
                    super.onNext(object);
                    super.close();
                }
            }).awaitResult();
            return new Tuple(s, DockerUtil.toJSON(statistics));
        }).collect(Collectors.toMap(tuple -> tuple.get(0), tuple -> tuple.get(1)));
    }

    private JSONObject updateContainerCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        String containerId = (String) parameter.get("containerId");
        UpdateContainerCmd updateContainerCmd = dockerClient.updateContainerCmd(containerId);
        //
        Optional.ofNullable(parameter.get("cpusetCpus"))
            .map(StrUtil::toStringOrNull)
            .ifPresent(updateContainerCmd::withCpusetCpus);

        Optional.ofNullable(parameter.get("cpusetMems"))
            .map(StrUtil::toStringOrNull)
            .ifPresent(updateContainerCmd::withCpusetMems);

        Optional.ofNullable(parameter.get("cpuPeriod"))
            .map(ConvertUtil::toInt)
            .ifPresent(updateContainerCmd::withCpuPeriod);

        Optional.ofNullable(parameter.get("cpuQuota"))
            .map(ConvertUtil::toInt)
            .ifPresent(updateContainerCmd::withCpuQuota);

        Optional.ofNullable(parameter.get("cpuShares"))
            .map(ConvertUtil::toInt)
            .ifPresent(updateContainerCmd::withCpuShares);

        Optional.ofNullable(parameter.get("blkioWeight"))
            .map(ConvertUtil::toInt)
            .ifPresent(updateContainerCmd::withBlkioWeight);

        Optional.ofNullable(parameter.get("memoryReservation"))
            .map(StrUtil::toStringOrNull)
            .map(s -> {
                if ((s == null || s.isEmpty())) {
                    return null;
                }
                return DataSizeUtil.parse(s);
            })
            .ifPresent(updateContainerCmd::withMemoryReservation);

        Optional.ofNullable(parameter.get("memory"))
            .map(StrUtil::toStringOrNull)
            .map(s -> {
                if ((s == null || s.isEmpty())) {
                    return null;
                }
                return DataSizeUtil.parse(s);
            })
            .ifPresent(updateContainerCmd::withMemory);

        //            updateContainerCmd.withKernelMemory(DataSizeUtil.parse("10M"));

        Optional.ofNullable(parameter.get("memorySwap"))
            .map(StrUtil::toStringOrNull)
            .map(s -> {
                if ((s == null || s.isEmpty())) {
                    return null;
                }
                return DataSizeUtil.parse(s);
            })
            .ifPresent(updateContainerCmd::withMemorySwap);

        UpdateContainerResponse updateContainerResponse = updateContainerCmd.exec();
        return DockerUtil.toJSON(updateContainerResponse);
    }

    private JSONObject inspectContainerCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        String containerId = (String) parameter.get("containerId");
        InspectContainerResponse containerResponse = dockerClient.inspectContainerCmd(containerId).withSize(true).exec();
        return DockerUtil.toJSON(containerResponse);
    }

    private List<JSONObject> listNetworksCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        ListNetworksCmd listNetworksCmd = dockerClient.listNetworksCmd();

        String name = (String) parameter.get("name");
        if ((name != null && !name.isEmpty())) {
            listNetworksCmd.withNameFilter(name);
        }
        String id = (String) parameter.get("id");
        if ((id != null && !id.isEmpty())) {
            listNetworksCmd.withIdFilter(id);
        }
        List<Network> networks = listNetworksCmd.exec();
        networks = (networks != null ? networks : new ArrayList<>());
        return networks.stream().map(DockerUtil::toJSON).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public void pullImageCmd(Map<String, Object> parameter) throws InterruptedException {
        DockerClient dockerClient = DockerUtil.get(parameter);

        Consumer<String> logConsumer = (Consumer<String>) parameter.get("logConsumer");
        String repositoryStr = (String) parameter.get("repository");
        Assert.hasText(repositoryStr, "请填写镜名称");
        NameParser.ReposTag reposTag = NameParser.parseRepositoryTag(repositoryStr);
        // 解析 tag
        String tag = reposTag.tag;
        tag = (tag == null || tag.isEmpty() ? "latest" : tag);
        logConsumer.accept(String.format("start pull %s:%s", reposTag.repos, tag));
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(reposTag.repos)
            .withTag(tag)
            .withAuthConfig(dockerClient.authConfig());
        pullImageCmd.exec(new InvocationBuilder.AsyncResultCallback<PullResponseItem>() {
            @Override
            public void onNext(PullResponseItem object) {
                String responseItem = DockerUtil.parseResponseItem(object);
                logConsumer.accept(responseItem);
            }

        }).awaitCompletion();
    }


    @SuppressWarnings(value = {"unchecked"})
    private void createContainerCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        String imageId = (String) parameter.get("imageId");
        String name = (String) parameter.get("name");
        String exposedPorts = (String) parameter.get("exposedPorts");
        String volumes = (String) parameter.get("volumes");
        String networkMode = (String) parameter.get("networkMode");
        Object autorunStr = parameter.get("autorun");
        Object privileged = parameter.get("privileged");
        String restartPolicy = (String) parameter.get("restartPolicy");
        Map<String, String> env = (Map<String, String>) parameter.get("env");
        Map<String, String> storageOpt = (Map<String, String>) parameter.get("storageOpt");
        String labels = (String) parameter.get("labels");
        String runtime = (String) parameter.get("runtime");
        List<String> extraHosts = (List<String>) parameter.get("extraHosts");

        //
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(imageId);
        containerCmd.withName(name);
        if (labels != null && !labels.isEmpty()) {
            Map<String, String> queryMap = UrlQuery.of(labels, StandardCharsets.UTF_8).getQueryMap();
            HashMap<String, String> labelMap = new HashMap<>(queryMap);
            containerCmd.withLabels(labelMap);
        }
        String hostname = (String) parameter.get("hostname");
        if (hostname != null && !hostname.isEmpty()) containerCmd.withHostName(hostname);
        HostConfig hostConfig = HostConfig.newHostConfig();
        if (runtime != null && !runtime.isEmpty()) hostConfig.withRuntime(runtime);
        //
        Opt.ofBlankAble(extraHosts).ifPresent(list -> {
            String[] array = list.stream().filter(StrUtil::isNotEmpty).toArray(String[]::new);
            hostConfig.withExtraHosts(array);
        });
        List<ExposedPort> exposedPortList = new ArrayList<>();
        if ((exposedPorts != null && !exposedPorts.isEmpty())) {
            List<PortBinding> portBindings = io.voyager1.util.ConvertUtil.splitTrim(exposedPorts, ",")
                .stream()
                .map(PortBinding::parse)
                .peek(portBinding -> exposedPortList.add(portBinding.getExposedPort()))
                .collect(Collectors.toList());
            hostConfig.withPortBindings(portBindings);
        }
        if ((volumes != null && !volumes.isEmpty())) {
            List<Bind> binds = io.voyager1.util.ConvertUtil.splitTrim(volumes, ",")
                .stream()
                .map(Bind::parse)
                .collect(Collectors.toList());
            hostConfig.withBinds(binds);
        }
        if (networkMode != null && !networkMode.isEmpty()) hostConfig.withNetworkMode(networkMode);
        Optional.ofNullable(privileged).map(o -> ConvertUtil.toBool(o, false)).ifPresent(hostConfig::withPrivileged);
        Opt.ofBlankAble(restartPolicy).map(RestartPolicy::parse).ifPresent(hostConfig::withRestartPolicy);
        // 环境变量
        if (env != null) {
            List<String> envList = env.entrySet()
                .stream()
                .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
            containerCmd.withEnv(envList);
        }
        Optional.ofNullable(storageOpt).map(map -> {
            if (MapUtil.isEmpty(map)) {
                // 空参数不能传入，避免低版本不支持
                return null;
            }
            return map;
        }).ifPresent(hostConfig::withStorageOpt);

        // 命令
        List<String> commands = (List<String>) parameter.get("commands");
        Optional.ofNullable(commands).ifPresent(strings -> {
            List<String> list = strings.stream()
                .filter(StrUtil::isNotEmpty)
                .collect(Collectors.toList());
            if ((list != null && !list.isEmpty())) {
                containerCmd.withCmd(list);
            }
        });

        containerCmd.withHostConfig(hostConfig).withExposedPorts(exposedPortList);
        CreateContainerResponse containerResponse = containerCmd.exec();
        //
        boolean autorun = ConvertUtil.toBool(autorunStr, false);
        if (autorun) {
            //
            dockerClient.startContainerCmd(containerResponse.getId()).exec();
        }

    }

    private JSONObject inspectImageCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        String imageId = (String) parameter.get("imageId");
        InspectImageCmd inspectImageCmd = dockerClient.inspectImageCmd(imageId);
        InspectImageResponse inspectImageResponse = inspectImageCmd.exec();
        return DockerUtil.toJSON(inspectImageResponse);
    }

    @SuppressWarnings("unchecked")
    private void pushImageCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        Consumer<String> logConsumer = (Consumer<String>) parameter.get("logConsumer");
        String repository = (String) parameter.get("repository");
        try {
            dockerClient.pushImageCmd(repository).exec(new InvocationBuilder.AsyncResultCallback<PushResponseItem>() {
                @Override
                public void onNext(PushResponseItem object) {
                    String responseItem = DockerUtil.parseResponseItem(object);
                    logConsumer.accept(responseItem);
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            logConsumer.accept("push image 被中断:" + e);
        }
    }

    /**
     * <a href="http://edu.jb51.net/docker/docker-command-manual-build.html">http://edu.jb51.net/docker/docker-command-manual-build.html</a>
     * 构建镜像
     *
     * @param parameter 参数
     * @return 构建是否成功
     */
    @SuppressWarnings("unchecked")
    private boolean buildImageCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        Consumer<String> logConsumer = (Consumer<String>) parameter.get("logConsumer");
        File dockerfile = (File) parameter.get("Dockerfile");
        File baseDirectory = (File) parameter.get("baseDirectory");
        String tags = (String) parameter.get("tags");
        String buildArgs = (String) parameter.get("buildArgs");
        Object pull = parameter.get("pull");
        Object noCache = parameter.get("noCache");
        String labels = (String) parameter.get("labels");
        Map<String, String> env = (Map<String, String>) parameter.get("env");
        InvocationBuilder.AsyncResultCallback<BuildResponseItem> callback = null;
        try {
            AuthConfigurations authConfigurations = new AuthConfigurations();
            authConfigurations.addConfig(dockerClient.authConfig());

            BuildImageCmd buildImageCmd = dockerClient.buildImageCmd();
            buildImageCmd
                .withBaseDirectory(baseDirectory)
                .withDockerfile(dockerfile)
                .withBuildAuthConfigs(authConfigurations)
                .withTags(new java.util.HashSet<>(io.voyager1.util.ConvertUtil.splitTrim(tags, ",")));
            // 添加构建参数
            UrlQuery query = UrlQuery.of(buildArgs, StandardCharsets.UTF_8);
            query.getQueryMap()
                .forEach((key, value) -> {
                    String valueStr = String.valueOf(value);
                    valueStr = StringUtil.formatStrByMap(valueStr, env);
                    buildImageCmd.withBuildArg(String.valueOf(key), valueStr);
                });
            // 标签
            UrlQuery labelsQuery = UrlQuery.of(labels, StandardCharsets.UTF_8);
            HashMap<String, String> labelMap = new java.util.HashMap<>();
            labelsQuery.getQueryMap().forEach((key, value) -> {
                String valueStr = String.valueOf(value);
                valueStr = StringUtil.formatStrByMap(valueStr, env);
                labelMap.put(String.valueOf(key), valueStr);
            });
            buildImageCmd.withLabels(labelMap);
            //
            Optional.ofNullable(pull).map(ConvertUtil::toBool).ifPresent(buildImageCmd::withPull);
            Optional.ofNullable(noCache).map(ConvertUtil::toBool).ifPresent(buildImageCmd::withNoCache);
            //
            final boolean[] hasError = {false};
            callback = buildImageCmd.exec(new InvocationBuilder.AsyncResultCallback<BuildResponseItem>() {
                @Override
                public void onNext(BuildResponseItem object) {
                    String responseItem = DockerUtil.parseResponseItem(object);
                    logConsumer.accept(responseItem);
                    hasError[0] = hasError[0] || object.isErrorIndicated();
                }
            }).awaitCompletion();
            return !hasError[0];
        } catch (InterruptedException e) {
            logConsumer.accept("容器 build 被中断:" + e);
            return false;
        } finally {
            IoUtil.close(callback);
        }
    }

    @SuppressWarnings("unchecked")
    private void execCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        Consumer<String> logConsumer = (Consumer<String>) parameter.get("logConsumer");
        Consumer<String> errorConsumer = (Consumer<String>) parameter.get("errorConsumer");
        InvocationBuilder.AsyncResultCallback<Frame> callback = null;
        try {
            String containerId = (String) parameter.get("containerId");
            Charset charset = (Charset) parameter.get("charset");
            InputStream stdin1 = (InputStream) parameter.get("stdin");
            //
            ExecCreateCmd execCreateCmd = dockerClient.execCreateCmd(containerId);
            execCreateCmd.withAttachStdout(true)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withTty(true)
                .withCmd("/bin/bash");
            ExecCreateCmdResponse exec = execCreateCmd.exec();
            //
            String execId = exec.getId();
            ExecStartCmd execStartCmd = dockerClient.execStartCmd(execId);
            execStartCmd.withDetach(false).withTty(true).withStdIn(stdin1);
            logConsumer.accept(String.format("CALLBACK_EXECID:%s", execId));
            callback = execStartCmd.exec(new InvocationBuilder.AsyncResultCallback<Frame>() {
                @Override
                public void onNext(Frame frame) {
                    String s = new String(frame.getPayload(), charset);
                    logConsumer.accept(s);
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            errorConsumer.accept("容器cli被中断:" + e);
        } finally {
            errorConsumer.accept("exit");
            IoUtil.close(callback);
        }
    }

    /**
     * 检查 终端
     *
     * @param parameter 参数
     */
    private void inspectExecCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        String execId = (String) parameter.get("execId");
        //
        InspectExecCmd inspectExecCmd = dockerClient.inspectExecCmd(execId);
        inspectExecCmd.exec().isRunning();
    }

    /**
     * 中断 终端
     *
     * @param parameter 参数
     */
    private void resizeExecCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        String execId = (String) parameter.get("execId");
        //
        ResizeExecCmd resizeExecCmd = dockerClient.resizeExecCmd(execId);
        Integer sizeHeight = (Integer) parameter.get("sizeHeight");
        Integer sizeWidth = (Integer) parameter.get("sizeWidth");
        resizeExecCmd.withSize(sizeHeight, sizeWidth).exec();
    }

    @SuppressWarnings("unchecked")
    private void logContainerCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        String uuid = (String) parameter.get("uuid");
        Consumer<String> consumer = (Consumer<String>) parameter.get("consumer");
        try {
            String containerId = (String) parameter.get("containerId");
            Charset charset = (Charset) parameter.get("charset");
            Integer tail = (Integer) parameter.get("tail");
            Boolean timestamps = ConvertUtil.toBool(parameter.get("timestamps"));
            DockerClientUtil.pullLog(dockerClient, containerId, timestamps, tail, charset, consumer, autoCloseable -> DockerUtil.putClose(uuid, autoCloseable));
        } catch (InterruptedException e) {
            consumer.accept("获取容器日志被中断:" + e);
        } finally {
            DockerUtil.close(uuid);
        }
    }

    private List<JSONObject> listVolumesCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        ListVolumesCmd listVolumesCmd = dockerClient.listVolumesCmd();
        Boolean dangling = ConvertUtil.toBool(parameter.get("dangling"), false);
        if (dangling) {
            listVolumesCmd.withDanglingFilter(true);
        }
        String name = (String) parameter.get("name");
        if ((name != null && !name.isEmpty())) {
            listVolumesCmd.withFilter("name", new java.util.ArrayList<>(java.util.Arrays.asList(name)));
        }

        ListVolumesResponse exec = listVolumesCmd.exec();
        List<InspectVolumeResponse> volumes = exec.getVolumes();
        volumes = (volumes != null ? volumes : new ArrayList<>());
        return volumes.stream().map((Function<InspectVolumeResponse, Object>) inspectVolumeResponse -> {
            InspectVolumeCmd inspectVolumeCmd = dockerClient.inspectVolumeCmd(inspectVolumeResponse.getName());
            return inspectVolumeCmd.exec();
        }).map(DockerUtil::toJSON).collect(Collectors.toList());

    }

    private void removeVolumeCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        String volumeName = (String) parameter.get("volumeName");
        dockerClient.removeVolumeCmd(volumeName).exec();

    }


    private List<JSONObject> listImagesCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        ListImagesCmd listImagesCmd = dockerClient.listImagesCmd();
        listImagesCmd.withShowAll(ConvertUtil.toBool(parameter.get("showAll"), true));
        listImagesCmd.withDanglingFilter(ConvertUtil.toBool(parameter.get("dangling"), false));

        String name = (String) parameter.get("name");
        if ((name != null && !name.isEmpty())) {
            listImagesCmd.withImageNameFilter(name);
        }
        List<Image> exec = listImagesCmd.exec();
        exec = (exec != null ? exec : new ArrayList<>());
        return exec.stream().map(DockerUtil::toJSON).collect(Collectors.toList());
    }

    private void removeImageCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        String imageId = (String) parameter.get("imageId");
        dockerClient.removeImageCmd(imageId).withForce(true).exec();
    }

    private void batchRemoveCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        String[] imagesIds = (String[]) parameter.get("imagesIds");
        int successCount = 0, failCount = 0;
        // 已经使用的镜像禁止删除
        for (String imageId : imagesIds) {
            try {
                dockerClient.removeImageCmd(imageId).withForce(false).exec();
                successCount++;
            } catch (Exception e) {
                log.warn("删除容器异常", e);
            }
        }
        failCount = imagesIds.length - successCount;
    }

    /**
     * 不包含 docker compose
     *
     * @param parameter 参数
     * @return list
     */
    private List<JSONObject> listContainerCmd(Map<String, Object> parameter) {
        List<JSONObject> list = this.listContainerByLabelCmd(parameter, null);
        String composeLabel = "com.docker.compose.project";
        return list.stream()
            .filter(jsonObject -> {
                JSONObject labels = jsonObject.getJSONObject("labels");
                String project = MapUtil.get(labels, composeLabel, String.class);
                return project == null;
            })
            .collect(Collectors.toList());
    }

    private List<JSONObject> listContainerByLabelCmd(Map<String, Object> parameter, String label) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
        listContainersCmd.withShowAll(ConvertUtil.toBool(parameter.get("showAll"), true));
        String name = (String) parameter.get("name");
        if ((name != null && !name.isEmpty())) {
            listContainersCmd.withNameFilter(new java.util.ArrayList<>(java.util.Arrays.asList(name)));
        }
        String containerId = (String) parameter.get("containerId");
        if ((containerId != null && !containerId.isEmpty())) {
            listContainersCmd.withIdFilter(new java.util.ArrayList<>(java.util.Arrays.asList(containerId)));
        }

        Opt.ofBlankAble(label).ifPresent(s -> {
            // 只筛选 docker compose
            listContainersCmd.withLabelFilter(new java.util.ArrayList<>(java.util.Arrays.asList(s)));
        });
        String imageId = (String) parameter.get("imageId");
        List<Container> exec = listContainersCmd.exec();
        exec = (exec != null ? exec : new ArrayList<>());
        return exec.stream()
            .map(DockerUtil::toJSON)
            .filter(jsonObject -> {
                if ((imageId == null || imageId.isEmpty())) {
                    return true;
                }
                String imageId1 = jsonObject.getString("imageId");
                return (imageId1 != null && imageId1.contains(imageId));
            })
            .collect(Collectors.toList());
    }

    /**
     * 不包含 docker compose
     *
     * @param parameter 参数
     * @return list
     */
    private List<JSONObject> listComposeContainerCmd(Map<String, Object> parameter) {
        String composeLabel = "com.docker.compose.project";
        List<JSONObject> list = this.listContainerByLabelCmd(parameter, composeLabel);
        //
        Map<String, List<JSONObject>> map = CollStreamUtil.groupKeyValue(list, jsonObject -> {
            JSONObject labels = jsonObject.getJSONObject("labels");
            return Optional.ofNullable(labels)
                .map(jsonObject1 -> jsonObject1.getString(composeLabel))
                .orElse("null");
        }, jsonObject -> jsonObject);
        //
        return map.entrySet().stream()
            .map(stringListEntry -> {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", stringListEntry.getKey());
                jsonObject.put("child", stringListEntry.getValue());
                return jsonObject;
            })
            .collect(Collectors.toList());
    }

    private void restartContainerCmd(Map<String, Object> parameter) {
        String containerId = (String) parameter.get("containerId");
        DockerClient dockerClient = DockerUtil.get(parameter);

        dockerClient.restartContainerCmd(containerId).exec();
    }

    private void startContainerCmd(Map<String, Object> parameter) {
        String containerId = (String) parameter.get("containerId");
        DockerClient dockerClient = DockerUtil.get(parameter);

        dockerClient.startContainerCmd(containerId).exec();
    }

    private void stopContainerCmd(Map<String, Object> parameter) {
        String containerId = (String) parameter.get("containerId");
        DockerClient dockerClient = DockerUtil.get(parameter);

        dockerClient.stopContainerCmd(containerId).exec();
    }

    /**
     * 删除容器
     *
     * @param parameter 参数
     */
    private void removeContainerCmd(Map<String, Object> parameter) {
        String containerId = (String) parameter.get("containerId");
        DockerClient dockerClient = DockerUtil.get(parameter);

        DockerClientUtil.removeContainerCmd(dockerClient, containerId);

    }

    /**
     * 关闭异步资源
     *
     * @param parameter 参数
     */
    private void closeAsyncResourceCmd(Map<String, Object> parameter) {
        String uuid = (String) parameter.get("uuid");
        DockerUtil.close(uuid);
    }

    /**
     * 导出镜像
     *
     * @param parameter 参数
     * @return 镜像流
     */
    private Tuple saveImageCmd(Map<String, Object> parameter) {
        try {
            String imageId = (String) parameter.get("imageId");
            DockerClient dockerClient = DockerUtil.get(parameter);
            //
            InspectImageResponse imageResponse = dockerClient.inspectImageCmd(imageId).exec();
            List<String> repoTags = imageResponse.getRepoTags();
            String arch = imageResponse.getArch();
            String nameTag = (repoTags == null || repoTags.isEmpty() ? null : repoTags.get(0));
            // xxx/xxx 只保留最后的名称
            String name = CollUtil.getLast(io.voyager1.util.ConvertUtil.splitTrim(nameTag, "/"));
            // els-app:1.0.106 冒号替换
            name = name.replace(":", "-");
            InputStream inputStream = dockerClient.saveImageCmd(nameTag).exec();
            return new Tuple(inputStream, name + "-" + arch + ".tar");
        } catch (com.github.dockerjava.api.exception.NotFoundException e) {
            log.debug("{}", e.getMessage());
            return null;
        }
    }

    /**
     * 导入镜像
     *
     * @param parameter 参数
     */
    private void loadImageCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        InputStream inputStream = (InputStream) parameter.get("stream");
        //
        dockerClient.loadImageCmd(inputStream).exec();
    }
}
