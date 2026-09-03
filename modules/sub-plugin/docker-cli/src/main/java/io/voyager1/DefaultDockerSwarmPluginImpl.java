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

import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import org.springframework.util.unit.DataSize;
import io.voyager1.util.Opt;
import io.voyager1.util.MapUtil;
import io.voyager1.util.EnumUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.ReflectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.plugin.PluginConfig;
import com.alibaba.fastjson2.JSONObject;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.RemoveSwarmNodeCmdImpl;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.plugin.IDefaultPlugin;
import org.springframework.util.Assert;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * docker swarm
 *
 * @since 2022/2/13
 */
@PluginConfig(name = "docker-cli:swarm")
@Slf4j
public class DefaultDockerSwarmPluginImpl implements IDefaultPlugin {

    @Override
    public Object execute(Object main, Map<String, Object> parameter) {
        String type = main.toString();
        switch (type) {
            case "inSpectSwarm":
                return this.inSpectSwarmCmd(parameter);
            case "tryInitializeSwarm":
                return this.tryInitializeSwarmCmd(parameter);
            case "joinSwarm":
                this.joinSwarmCmd(parameter);
                return null;
            case "listSwarmNodes":
                return this.listSwarmNodesCmd(parameter);
            case "leaveSwarm":
                this.leaveSwarmCmd(parameter);
                return null;
            case "updateSwarmNode":
                this.updateSwarmNodeCmd(parameter);
                return null;
            case "removeSwarmNode":
                this.removeSwarmNodeCmd(parameter);
                return null;
            case "listServices":
                return this.listServicesCmd(parameter);
            case "listTasks":
                return this.listTasksCmd(parameter);
            case "removeService":
                this.removeServiceCmd(parameter);
                return null;
            case "updateService":
                this.updateServiceCmd(parameter);
                return null;
            case "updateServiceImage":
                this.updateServiceImage(parameter);
                return null;
            case "logService":
                this.logServiceCmd(parameter);
                return null;
            case "logTask":
                this.logTaskCmd(parameter);
                return null;
            default:
                break;
        }
        return null;
    }

    private void logTaskCmd(Map<String, Object> parameter) {
        this.logServiceCmd(parameter, (String) parameter.get("taskId"), "task");
    }

    private void logServiceCmd(Map<String, Object> parameter) {
        this.logServiceCmd(parameter, (String) parameter.get("serviceId"), "service");
    }

    private void logServiceCmd(Map<String, Object> parameter, String id, String type) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        Consumer<String> consumer = (Consumer<String>) parameter.get("consumer");
        String uuid = (String) parameter.get("uuid");
        try {

            LogSwarmObjectCmd logSwarmObjectCmd = (type != null && type.equalsIgnoreCase("Service")) ? dockerClient.logServiceCmd(id) : dockerClient.logTaskCmd(id);

            Charset charset = (Charset) parameter.get("charset");
            Integer tail = (Integer) parameter.get("tail");
            // 获取日志
            if (tail != null && tail > 0) {
                logSwarmObjectCmd.withTail(tail);
            }
            //String since = (String) parameter.get("since");
            //            Opt.ofBlankAble(since).ifPresent(s -> logSwarmObjectCmd.withSince(s));
            Boolean timestamps = ConvertUtil.toBool(parameter.get("timestamps"));
            logSwarmObjectCmd.withTimestamps(timestamps);
            ResultCallback.Adapter<Frame> exec = logSwarmObjectCmd
                .withDetails(true)
                .withStderr(true)
                .withFollow(true)
                .withStdout(true)
                .exec(new ResultCallback.Adapter<Frame>() {
                    @Override
                    public void onNext(Frame object) {
                        byte[] payload = object.getPayload();
                        if (payload == null) {
                            return;
                        }
                        String s = new String(payload, charset);
                        consumer.accept(s);
                    }
                });
            // 添加到缓存中
            DockerUtil.putClose(uuid, exec);
            exec.awaitCompletion();
        } catch (InterruptedException e) {
            consumer.accept("获取容器日志被中断:" + e);
        } finally {
            DockerUtil.close(uuid);
        }
    }

    private ServiceSpec intServiceSpec(DockerClient dockerClient, String serviceId) {
        if ((serviceId == null || serviceId.isEmpty())) {
            return new ServiceSpec();
        }
        // 读取之前的信息-保留之前的信息-否则会全部替换
        InspectServiceCmd inspectServiceCmd = dockerClient.inspectServiceCmd(serviceId);
        Service service = inspectServiceCmd.exec();
        ServiceSpec spec = service.getSpec();
        return (spec != null ? spec : new ServiceSpec());
    }

    public void updateServiceImage(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        String serviceId = (String) parameter.get("serviceId");
        String image = (String) parameter.get("image");
        //
        InspectServiceCmd inspectServiceCmd = dockerClient.inspectServiceCmd(serviceId);
        Service service = inspectServiceCmd.exec();
        ServiceSpec spec = service.getSpec();
        Assert.notNull(spec, "服务信息不完整不能操作");
        TaskSpec taskTemplate = spec.getTaskTemplate();
        Assert.notNull(taskTemplate, "服务信息不完整不能操作：-1");
        ContainerSpec templateContainerSpec = taskTemplate.getContainerSpec();
        Assert.notNull(templateContainerSpec, "服务信息不完整不能操作：-2");
        templateContainerSpec.withImage(image);
        //
        UpdateServiceCmd updateServiceCmd = dockerClient.updateServiceCmd(serviceId, spec);
        ResourceVersion version = service.getVersion();
        Assert.notNull(version, "服务信息不完整不能操作：-3");
        updateServiceCmd.withVersion(version.getIndex());
        updateServiceCmd.exec();
    }

    /**
     * 更新 服务，如果不存在 id 则创建。需要传人版本号
     *
     * @param parameter 测试
     */
    public void updateServiceCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        String serviceId = (String) parameter.get("serviceId");
        ServiceSpec serviceSpec = this.intServiceSpec(dockerClient, serviceId);
        String name = (String) parameter.get("name");
        serviceSpec.withName(name);
        {
            String mode = (String) parameter.get("mode");
            ServiceMode serviceMode = EnumUtil.fromString(ServiceMode.class, mode);
            ServiceModeConfig serviceModeConfig = new ServiceModeConfig();
            if (serviceMode == ServiceMode.GLOBAL) {
                serviceModeConfig.withGlobal(new ServiceGlobalModeOptions());
            } else if (serviceMode == ServiceMode.REPLICATED) {
                Object replicas = parameter.get("replicas");
                ServiceReplicatedModeOptions serviceReplicatedModeOptions = new ServiceReplicatedModeOptions();
                serviceReplicatedModeOptions.withReplicas(ConvertUtil.toInt(replicas, 1));
                serviceModeConfig.withReplicated(serviceReplicatedModeOptions);
            }
            serviceSpec.withMode(serviceModeConfig);
        }
        {
            TaskSpec taskSpec = (serviceSpec.getTaskTemplate() != null ? serviceSpec.getTaskTemplate() : new TaskSpec());
            //
            ContainerSpec containerSpec = this.buildContainerSpec(parameter, taskSpec.getContainerSpec());
            taskSpec.withContainerSpec(containerSpec);
            //
            Map<String, Map<String, Object>> resources = (Map<String, Map<String, Object>>) parameter.get("resources");

            if (MapUtil.isNotEmpty(resources)) {
                ResourceRequirements resourceRequirements = new ResourceRequirements();
                ResourceSpecs limitsResourceSpecs = this.buildResourceSpecs(resources.get("limits"));
                if (limitsResourceSpecs != null) {
                    resourceRequirements.withLimits(limitsResourceSpecs);
                }
                ResourceSpecs reservationsResourceSpecs = this.buildResourceSpecs(resources.get("reservations"));
                if (reservationsResourceSpecs != null) {
                    resourceRequirements.withReservations(reservationsResourceSpecs);
                }
                if (ObjectUtil.isAllEmpty(resourceRequirements.getLimits(), resourceRequirements.getReservations())) {
                    taskSpec.withResources(null);
                } else {
                    taskSpec.withResources(resourceRequirements);
                }
            }
            serviceSpec.withTaskTemplate(taskSpec);
        }
        {
            EndpointSpec endpointSpec = this.buildEndpointSpec(parameter);
            serviceSpec.withEndpointSpec(endpointSpec);
        }
        {
            Map<String, Object> update = (Map<String, Object>) parameter.get("update");
            UpdateConfig updateConfig = this.buildUpdateConfig(update);
            serviceSpec.withUpdateConfig(updateConfig);
            Map<String, Object> rollback = (Map<String, Object>) parameter.get("rollback");
            UpdateConfig rollbackConfig = this.buildUpdateConfig(rollback);
            serviceSpec.withRollbackConfig(rollbackConfig);
        }

        if ((serviceId != null && !serviceId.isEmpty())) {
            Object version = parameter.get("version");
            UpdateServiceCmd updateServiceCmd = dockerClient.updateServiceCmd(serviceId, serviceSpec);
            updateServiceCmd.withVersion(ConvertUtil.toLong(version, 0L));
            updateServiceCmd.exec();
        } else {
            CreateServiceCmd createServiceCmd = dockerClient.createServiceCmd(serviceSpec).withAuthConfig(dockerClient.authConfig());
            createServiceCmd.exec();
        }
    }

    private ResourceSpecs buildResourceSpecs(Map<String, Object> map) {
        if (MapUtil.isNotEmpty(map)) {
            ResourceSpecs resourceSpecs = new ResourceSpecs();
            Object nanoCpus = map.get("nanoCPUs");
            if (nanoCpus != null) {
                String text = nanoCpus.toString();
                if ((text != null && !text.isEmpty())) {
                    resourceSpecs.withNanoCPUs(ConvertUtil.toLong(nanoCpus, 1L));
                }
            }
            Object memoryBytes = map.get("memoryBytes");
            if (memoryBytes != null) {
                String text = memoryBytes.toString();
                if ((text != null && !text.isEmpty())) {
                    DataSize dataSize = DataSize.parse(text);
                    resourceSpecs.withMemoryBytes(dataSize.toBytes());
                }
            }
            if (ObjectUtil.isAllEmpty(resourceSpecs.getNanoCPUs(), resourceSpecs.getMemoryBytes())) {
                return null;
            }
            return resourceSpecs;
        }
        return null;
    }

    private EndpointSpec buildEndpointSpec(Map<String, Object> parameter) {
        String endpointResolutionModeStr = (String) parameter.get("endpointResolutionMode");
        EndpointResolutionMode endpointResolutionMode = EnumUtil.fromString(EndpointResolutionMode.class, endpointResolutionModeStr);
        EndpointSpec endpointSpec = new EndpointSpec();
        endpointSpec.withMode(endpointResolutionMode);
        Collection<Map<String, Object>> exposedPorts = (Collection) parameter.get("exposedPorts");
        if ((exposedPorts != null && !exposedPorts.isEmpty())) {
            List<PortConfig> portConfigs = exposedPorts.stream()
                .map(stringStringMap -> {
                    Object port = stringStringMap.get("targetPort");
                    Object publicPort = stringStringMap.get("publishedPort");
                    if (ObjectUtil.hasEmpty(port, publicPort)) {
                        return null;
                    }
                    PortConfig portConfig = new PortConfig();
                    String mode = (String) stringStringMap.get("publishMode");
                    PortConfig.PublishMode publishMode = EnumUtil.fromString(PortConfig.PublishMode.class, mode);
                    portConfig.withPublishMode(publishMode);
                    String scheme = (String) stringStringMap.get("protocol");
                    PortConfigProtocol protocol = EnumUtil.fromString(PortConfigProtocol.class, scheme);
                    portConfig.withProtocol(protocol);
                    portConfig.withTargetPort(ConvertUtil.toInt(port, 0));
                    portConfig.withPublishedPort(ConvertUtil.toInt(publicPort, 0));
                    return portConfig;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            endpointSpec.withPorts(portConfigs);
        }
        return endpointSpec;
    }

    private ContainerSpec buildContainerSpec(Map<String, Object> parameter, ContainerSpec oldContainerSpec) {
        String image = (String) parameter.get("image");
        ContainerSpec containerSpec = (oldContainerSpec != null ? oldContainerSpec : new ContainerSpec());
        String hostname = (String) parameter.get("hostname");
        if (hostname != null && !hostname.isEmpty()) containerSpec.withHostname(hostname);
        containerSpec.withImage(image);
        //
        Collection<Map<String, String>> args = (Collection) parameter.get("args");
        if ((args != null && !args.isEmpty())) {
            List<String> value = args.stream()
                .map(stringStringMap -> stringStringMap.get("value"))
                .filter(StrUtil::isNotEmpty)
                .collect(Collectors.toList());
            containerSpec.withArgs(value);
        }
        Collection<Map<String, String>> envs = (Collection) parameter.get("envs");
        if ((envs != null && !envs.isEmpty())) {
            List<String> value = envs.stream()
                .map(stringStringMap -> {
                    String name1 = stringStringMap.get("name");
                    String value1 = stringStringMap.get("value");
                    if ((name1 == null || name1.isEmpty())) {
                        return null;
                    }
                    return String.format("%s=%s", name1, value1);
                })
                .filter(StrUtil::isNotEmpty)
                .collect(Collectors.toList());
            containerSpec.withEnv(value);
        }
        Collection<Map<String, String>> commands = (Collection) parameter.get("commands");
        if ((commands != null && !commands.isEmpty())) {
            List<String> value = commands.stream()
                .map(stringStringMap -> stringStringMap.get("value"))
                .filter(StrUtil::isNotEmpty)
                .collect(Collectors.toList());
            containerSpec.withCommand(value);
        }
        //
        Collection<Map<String, String>> volumes = (Collection<Map<String, String>>) parameter.get("volumes");
        if ((volumes != null && !volumes.isEmpty())) {
            List<Mount> value = volumes.stream()
                .map(stringStringMap -> {
                    String source = stringStringMap.get("source");
                    String target = stringStringMap.get("target");
                    if (StrUtil.hasBlank(source, target)) {
                        return null;
                    }
                    String type = stringStringMap.get("type");
                    MountType mountType = EnumUtil.fromString(MountType.class, type);
                    Mount mount = new Mount();
                    mount.withSource(source);
                    mount.withTarget(target);
                    mount.withType(mountType);
                    return mount;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            containerSpec.withMounts(value);
        }
        return containerSpec;
    }

    private UpdateConfig buildUpdateConfig(Map<String, Object> update) {
        if (MapUtil.isNotEmpty(update)) {
            UpdateConfig updateConfig = new UpdateConfig();
            String failureAction = (String) update.get("failureAction");
            if ((failureAction != null && !failureAction.isEmpty())) {
                UpdateFailureAction updateFailureAction = EnumUtil.fromString(UpdateFailureAction.class, failureAction);
                updateConfig.withFailureAction(updateFailureAction);
            }
            String order = (String) update.get("order");
            if ((order != null && !order.isEmpty())) {
                UpdateOrder updateOrder = EnumUtil.fromString(UpdateOrder.class, order);
                updateConfig.withOrder(updateOrder);
            }
            Object parallelism = update.get("parallelism");
            if (parallelism != null) {
                updateConfig.withParallelism(ConvertUtil.toLong(parallelism));
            }
            Object delay = update.get("delay");
            if (delay != null) {
                updateConfig.withDelay(ConvertUtil.toLong(delay));
            }
            Object maxFailureRatio = update.get("maxFailureRatio");
            if (maxFailureRatio != null) {
                updateConfig.withMaxFailureRatio(ConvertUtil.toFloat(maxFailureRatio));
            }
            Object monitor = update.get("monitor");
            if (monitor != null) {
                updateConfig.withMonitor(ConvertUtil.toLong(monitor));
            }
            return updateConfig;
        }
        return null;
    }


    public void removeServiceCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        String serviceId = (String) parameter.get("serviceId");
        RemoveServiceCmd removeServiceCmd = dockerClient.removeServiceCmd(serviceId);
        removeServiceCmd.exec();
    }

    private List<JSONObject> listTasksCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        ListTasksCmd listTasksCmd = dockerClient.listTasksCmd();
        String serviceId = (String) parameter.get("serviceId");
        String id = (String) parameter.get("id");
        if ((serviceId != null && !serviceId.isEmpty())) {
            listTasksCmd.withServiceFilter(serviceId);
        }
        if ((id != null && !id.isEmpty())) {
            listTasksCmd.withIdFilter(id);
        }
        String name = (String) parameter.get("name");
        if ((name != null && !name.isEmpty())) {
            listTasksCmd.withNameFilter(name);
        }
        String node = (String) parameter.get("node");
        if ((node != null && !node.isEmpty())) {
            listTasksCmd.withNodeFilter(node);
        }
        String state = (String) parameter.get("state");
        if ((state != null && !state.isEmpty())) {
            TaskState taskState = EnumUtil.fromString(TaskState.class, state);
            listTasksCmd.withStateFilter(taskState);
        }
        List<Task> exec = listTasksCmd.exec();
        return exec.stream().map(DockerUtil::toJSON).collect(Collectors.toList());
    }

    public List<JSONObject> listServicesCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        ListServicesCmd listServicesCmd = dockerClient.listServicesCmd();
        String id = (String) parameter.get("id");
        String name = (String) parameter.get("name");
        if ((id != null && !id.isEmpty())) {
            listServicesCmd.withIdFilter(new java.util.ArrayList<>(java.util.Arrays.asList(id)));
        }
        if ((name != null && !name.isEmpty())) {
            listServicesCmd.withNameFilter(new java.util.ArrayList<>(java.util.Arrays.asList(name)));
        }
        List<Service> exec = listServicesCmd.exec();
        return exec.stream().map(DockerUtil::toJSON).collect(Collectors.toList());
    }

    private void removeSwarmNodeCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        DockerCmdExecFactory dockerCmdExecFactory = (DockerCmdExecFactory) ReflectUtil.getFieldValue(dockerClient, "dockerCmdExecFactory");
        Assert.notNull(dockerCmdExecFactory, "当前方法不被支持，暂时不能使用");
        String nodeId = (String) parameter.get("nodeId");
        RemoveSwarmNodeCmdImpl removeSwarmNodeCmd = new RemoveSwarmNodeCmdImpl(
            dockerCmdExecFactory.removeSwarmNodeCmdExec(), nodeId);
        removeSwarmNodeCmd.withForce(true);
        removeSwarmNodeCmd.exec();
    }

    private void updateSwarmNodeCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        String nodeId = (String) parameter.get("nodeId");
        List<SwarmNode> nodes = dockerClient.listSwarmNodesCmd()
            .withIdFilter(new java.util.ArrayList<>(java.util.Arrays.asList(nodeId))).exec();
        SwarmNode swarmNode = (nodes == null || nodes.isEmpty() ? null : nodes.get(0));
        Assert.notNull(swarmNode, "没有对应的节点");
        ObjectVersion version = swarmNode.getVersion();
        Assert.notNull(version, "对应的节点信息不完整不能继续");
        //
        String availabilityStr = (String) parameter.get("availability");
        String roleStr = (String) parameter.get("role");
        //
        SwarmNodeAvailability availability = EnumUtil.fromString(SwarmNodeAvailability.class, availabilityStr);
        SwarmNodeRole role = EnumUtil.fromString(SwarmNodeRole.class, roleStr);
        UpdateSwarmNodeCmd swarmNodeCmd = dockerClient.updateSwarmNodeCmd();
        swarmNodeCmd.withSwarmNodeId(nodeId);
        SwarmNodeSpec swarmNodeSpec = new SwarmNodeSpec();
        swarmNodeSpec.withAvailability(availability);
        swarmNodeSpec.withRole(role);
        swarmNodeCmd.withSwarmNodeSpec(swarmNodeSpec);
        swarmNodeCmd.withVersion(version.getIndex());
        swarmNodeCmd.exec();
    }


    private void leaveSwarmCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        Object forceStr = parameter.get("force");
        boolean force = ConvertUtil.toBool(forceStr, false);
        LeaveSwarmCmd leaveSwarmCmd = dockerClient.leaveSwarmCmd();
        if (force) {
            leaveSwarmCmd.withForceEnabled(true);
        }
        leaveSwarmCmd.exec();

    }


//	private List<JSONObject> listSwarmNodesCmd(Map<String, Object> parameter) {
//		DockerClient dockerClient = DockerUtil.build(parameter);
//		try {
//			LeaveSwarmCmd leaveSwarmCmd = dockerClient.leaveSwarmCmd();
//			leaveSwarmCmd.withForceEnabled(true)
//		} finally {
//
//		}
//	}

    private List<JSONObject> listSwarmNodesCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        ListSwarmNodesCmd listSwarmNodesCmd = dockerClient.listSwarmNodesCmd();
        String id = (String) parameter.get("id");
        if ((id != null && !id.isEmpty())) {
            listSwarmNodesCmd.withIdFilter(io.voyager1.util.ConvertUtil.splitTrim(id, ","));
        }
        String role = (String) parameter.get("role");
        if ((role != null && !role.isEmpty())) {
            listSwarmNodesCmd.withRoleFilter(io.voyager1.util.ConvertUtil.splitTrim(role, ","));
        }
        String name = (String) parameter.get("name");
        if ((name != null && !name.isEmpty())) {
            listSwarmNodesCmd.withNameFilter(io.voyager1.util.ConvertUtil.splitTrim(name, ","));
        }
        List<SwarmNode> exec = listSwarmNodesCmd.exec();
        return exec.stream().map(swarmNode -> {
            JSONObject jsonObject = DockerUtil.toJSON(swarmNode);
            jsonObject.remove("rawValues");
            return jsonObject;
        }).collect(Collectors.toList());
    }


    private void joinSwarmCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        String token = (String) parameter.get("token");
        String remoteAddrs = (String) parameter.get("remoteAddrs");
        JoinSwarmCmd joinSwarmCmd = dockerClient.joinSwarmCmd()
            .withRemoteAddrs(io.voyager1.util.ConvertUtil.splitTrim(remoteAddrs, ","))
            .withJoinToken(token);
        joinSwarmCmd.exec();

    }

    private JSONObject tryInitializeSwarmCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        // 先尝试获取
        try {
            Swarm exec = dockerClient.inspectSwarmCmd().exec();
            JSONObject jsonObject = DockerUtil.toJSON(exec);
            if (jsonObject != null) {
                return jsonObject;
            }
        } catch (Exception ignored) {
            //
        }
        // 尝试初始化
        SwarmSpec swarmSpec = new SwarmSpec();
        swarmSpec.withName("default");
        dockerClient.initializeSwarmCmd(swarmSpec).exec();
        // 获取信息
        Swarm exec = dockerClient.inspectSwarmCmd().exec();
        return DockerUtil.toJSON(exec);
    }

    private JSONObject inSpectSwarmCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        Swarm exec = dockerClient.inspectSwarmCmd().exec();
        return DockerUtil.toJSON(exec);
    }
}
