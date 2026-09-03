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

package io.voyager1.service.k8s;

import io.voyager1.core.entity.K8sClusterEntity;
import io.voyager1.core.repository.K8sClusterRepository;
import io.voyager1.model.data.K8sClusterModel;
import io.voyager1.model.dto.K8sResourceItem;
import org.springframework.transaction.annotation.Transactional;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.NodeCondition;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimList;
import io.fabric8.kubernetes.api.model.PersistentVolumeList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.DaemonSetList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetList;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobList;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobList;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.EventList;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * K8s 集群服务：基于 fabric8 kubernetes-client 的完整管理（集群/命名空间/资源/详情/操作/日志/事件/部署）
 *
 * @since 2026/8/9
 */
@Slf4j
@org.springframework.stereotype.Service
public class K8sService {

    private final K8sClusterRepository repository;

    public K8sService(K8sClusterRepository repository) {
        this.repository = repository;
    }

    /**
     * 允许查询/操作的资源类型白名单
     */
    private static final Set<String> ALLOWED_RESOURCE_TYPES = Set.of(
        "pods", "deployments", "services", "configmaps", "secrets",
        "statefulsets", "daemonsets", "jobs", "cronjobs", "ingresses",
        "nodes", "persistentvolumes", "persistentvolumeclaims", "namespaces"
    );

    /**
     * Kubernetes 命名空间 DNS label 格式
     */
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("^[a-z0-9]([-a-z0-9]*[a-z0-9])?$");

    /**
     * 集群 id -> 客户端缓存
     */
    private final Map<String, KubernetesClient> clientCache = new ConcurrentHashMap<>();

    /**
     * 校验资源类型，防止非法输入
     */
    public void validateResourceType(String type) {
        Assert.hasText(type, "资源类型不能为空");
        Assert.state(ALLOWED_RESOURCE_TYPES.contains(type), "不支持的资源类型: " + type);
    }

    /**
     * 校验命名空间（空返回 null 表示所有命名空间）
     */
    String validateNamespace(String namespace, String defaultNamespace) {
        if ((namespace == null || namespace.isEmpty()) || "all".equalsIgnoreCase(namespace)) {
            return null;
        }
        String ns = (namespace == null || namespace.isEmpty() ? (defaultNamespace == null || defaultNamespace.isEmpty() ? "default" : defaultNamespace) : namespace);
        Assert.state(NAMESPACE_PATTERN.matcher(ns).matches(), "命名空间格式非法: " + ns);
        return ns;
    }

    /**
     * 保存集群（新增/更新）
     */
    @Transactional
    public String save(String id, String name, String kubeconfig, String serverUrl, String namespace, String remark) {
        Assert.hasText(name, "集群名称不能为空");
        Assert.hasText(kubeconfig, "kubeconfig 内容不能为空");
        long now = System.currentTimeMillis();
        K8sClusterEntity entity;
        boolean created = (id == null || id.isEmpty());
        if (created) {
            entity = new K8sClusterEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setCreateTimeMillis(now);
        } else {
            entity = repository.findById(id).orElse(null);
            Assert.notNull(entity, "集群不存在: " + id);
            // 更新后使旧客户端失效
            this.closeClient(id);
        }
        entity.setModifyTimeMillis(now);
        entity.setName(name);
        entity.setKubeconfig(kubeconfig);
        entity.setServerUrl(serverUrl);
        entity.setNamespace((namespace == null || namespace.isEmpty() ? "default" : namespace));
        entity.setRemark(remark);
        repository.save(entity);
        return entity.getId();
    }

    /**
     * 集群列表
     */
    public List<K8sClusterModel> listAll() {
        return repository.findByOrderByCreateTimeMillisDesc().stream().map(this::toModel).collect(Collectors.toList());
    }

    /**
     * 按主键查询集群。
     */
    public K8sClusterModel getByKey(String id) {
        K8sClusterEntity entity = repository.findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    /**
     * 删除集群（关闭客户端缓存）
     */
    @Transactional
    public void deleteCluster(String id) {
        this.closeClient(id);
        repository.deleteById(id);
    }

    /**
     * 获取（或创建）集群客户端
     */
    public KubernetesClient getClient(String clusterId) {
        KubernetesClient client = clientCache.get(clusterId);
        if (client != null) {
            return client;
        }
        K8sClusterModel cluster = this.getByKey(clusterId);
        Assert.notNull(cluster, "集群不存在: " + clusterId);
        Config config = Config.fromKubeconfig(cluster.getKubeconfig());
        client = new KubernetesClientBuilder().withConfig(config).build();
        clientCache.put(clusterId, client);
        return client;
    }

    private void closeClient(String clusterId) {
        KubernetesClient client = clientCache.remove(clusterId);
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("关闭 K8s 客户端异常: {}", e.getMessage());
            }
        }
    }

    /**
     * 命名空间列表
     */
    public List<String> listNamespaces(String clusterId) {
        NamespaceList list = this.getClient(clusterId).namespaces().list();
        List<String> result = new ArrayList<>();
        for (Namespace ns : list.getItems()) {
            result.add(ns.getMetadata().getName());
        }
        return result;
    }

    /**
     * 结构化资源列表
     */
    public List<K8sResourceItem> listResources(String clusterId, String namespace, String type) {
        this.validateResourceType(type);
        KubernetesClient client = this.getClient(clusterId);
        String ns = this.validateNamespace(namespace, null);
        switch (type) {
            case "pods":
                PodList pods = ns == null ? client.pods().inAnyNamespace().list() : client.pods().inNamespace(ns).list();
                return podItems(pods);
            case "deployments":
                DeploymentList deployments = ns == null ? client.apps().deployments().inAnyNamespace().list() : client.apps().deployments().inNamespace(ns).list();
                return deploymentItems(deployments);
            case "services":
                ServiceList services = ns == null ? client.services().inAnyNamespace().list() : client.services().inNamespace(ns).list();
                return serviceItems(services);
            case "configmaps":
                ConfigMapList configMaps = ns == null ? client.configMaps().inAnyNamespace().list() : client.configMaps().inNamespace(ns).list();
                return configMapItems(configMaps);
            case "secrets":
                SecretList secrets = ns == null ? client.secrets().inAnyNamespace().list() : client.secrets().inNamespace(ns).list();
                return secretItems(secrets);
            case "statefulsets":
                StatefulSetList sts = ns == null ? client.apps().statefulSets().inAnyNamespace().list() : client.apps().statefulSets().inNamespace(ns).list();
                return statefulSetItems(sts);
            case "daemonsets":
                DaemonSetList ds = ns == null ? client.apps().daemonSets().inAnyNamespace().list() : client.apps().daemonSets().inNamespace(ns).list();
                return daemonSetItems(ds);
            case "jobs":
                JobList jobs = ns == null ? client.batch().v1().jobs().inAnyNamespace().list() : client.batch().v1().jobs().inNamespace(ns).list();
                return jobItems(jobs);
            case "cronjobs":
                CronJobList cronJobs = ns == null ? client.batch().v1().cronjobs().inAnyNamespace().list() : client.batch().v1().cronjobs().inNamespace(ns).list();
                return cronJobItems(cronJobs);
            case "ingresses":
                IngressList ingresses = ns == null ? client.network().v1().ingresses().inAnyNamespace().list() : client.network().v1().ingresses().inNamespace(ns).list();
                return ingressItems(ingresses);
            case "nodes":
                return nodeItems(client.nodes().list());
            case "persistentvolumes":
                return pvItems(client.persistentVolumes().list());
            case "persistentvolumeclaims":
                PersistentVolumeClaimList pvcs = ns == null ? client.persistentVolumeClaims().inAnyNamespace().list() : client.persistentVolumeClaims().inNamespace(ns).list();
                return pvcItems(pvcs);
            case "namespaces":
                return namespaceItems(client.namespaces().list());
            default:
                throw new IllegalArgumentException("不支持的资源类型: " + type);
        }
    }

    /**
     * 资源详情（YAML）
     */
    public String getResourceDetail(String clusterId, String namespace, String type, String name) {
        this.validateResourceType(type);
        Assert.hasText(name, "资源名称不能为空");
        KubernetesClient client = this.getClient(clusterId);
        String ns = this.validateNamespace(namespace, null);
        HasMetadata resource = getResource(client, ns, type, name);
        Assert.notNull(resource, "资源不存在: " + name);
        return Serialization.asYaml(resource);
    }

    /**
     * 删除资源
     */
    public void deleteResource(String clusterId, String namespace, String type, String name) {
        this.validateResourceType(type);
        Assert.hasText(name, "资源名称不能为空");
        KubernetesClient client = this.getClient(clusterId);
        String ns = this.validateNamespace(namespace, null);
        HasMetadata resource = getResource(client, ns, type, name);
        Assert.notNull(resource, "资源不存在: " + name);
        client.resource(resource).delete();
    }

    /**
     * Deployment 扩缩容
     */
    public void scaleDeployment(String clusterId, String namespace, String name, int replicas) {
        Assert.hasText(name, "Deployment 名称不能为空");
        Assert.state(replicas >= 0, "副本数不能小于 0");
        KubernetesClient client = this.getClient(clusterId);
        String ns = this.requireNamespace(namespace);
        client.apps().deployments().inNamespace(ns).withName(name).scale(replicas);
    }

    /**
     * Deployment 滚动重启
     */
    public void restartDeployment(String clusterId, String namespace, String name) {
        Assert.hasText(name, "Deployment 名称不能为空");
        KubernetesClient client = this.getClient(clusterId);
        String ns = this.requireNamespace(namespace);
        client.apps().deployments().inNamespace(ns).withName(name).rolling().restart();
    }

    /**
     * Pod 日志（截取末尾若干行）
     */
    public String getPodLog(String clusterId, String namespace, String name, Integer tailLines) {
        Assert.hasText(name, "Pod 名称不能为空");
        KubernetesClient client = this.getClient(clusterId);
        String ns = this.requireNamespace(namespace);
        String log = client.pods().inNamespace(ns).withName(name).getLog();
        if (log == null) {
            return "";
        }
        int lines = tailLines == null || tailLines <= 0 ? 500 : tailLines;
        String[] arr = log.split("\n");
        if (arr.length <= lines) {
            return log;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = arr.length - lines; i < arr.length; i++) {
            sb.append(arr[i]).append('\n');
        }
        return sb.toString();
    }

    /**
     * 事件列表
     */
    public List<Map<String, Object>> listEvents(String clusterId, String namespace) {
        KubernetesClient client = this.getClient(clusterId);
        String ns = this.validateNamespace(namespace, null);
        EventList events = ns == null ? client.v1().events().inAnyNamespace().list() : client.v1().events().inNamespace(ns).list();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Event event : events.getItems()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", event.getType());
            m.put("reason", event.getReason());
            m.put("message", event.getMessage());
            m.put("object", event.getInvolvedObject() != null ? event.getInvolvedObject().getName() : "");
            m.put("namespace", event.getMetadata().getNamespace());
            m.put("count", event.getCount());
            m.put("time", event.getMetadata().getCreationTimestamp());
            result.add(m);
        }
        return result;
    }

    /**
     * 部署：manifest createOrReplace
     */
    public void applyManifest(String clusterId, String manifest) {
        Assert.hasText(manifest, "manifest 不能为空");
        KubernetesClient client = this.getClient(clusterId);
        List<HasMetadata> items = client.load(new ByteArrayInputStream(manifest.getBytes(StandardCharsets.UTF_8))).items();
        Assert.state(!items.isEmpty(), "manifest 没有可部署的资源");
        for (HasMetadata item : items) {
            client.resource(item).createOrReplace();
        }
    }

    private String requireNamespace(String namespace) {
        Assert.hasText(namespace, "命名空间不能为空");
        Assert.state(NAMESPACE_PATTERN.matcher(namespace).matches(), "命名空间格式非法: " + namespace);
        return namespace;
    }

    private HasMetadata getResource(KubernetesClient client, String ns, String type, String name) {
        switch (type) {
            case "pods":
                return ns == null ? client.pods().withName(name).get() : client.pods().inNamespace(ns).withName(name).get();
            case "deployments":
                return ns == null ? client.apps().deployments().withName(name).get() : client.apps().deployments().inNamespace(ns).withName(name).get();
            case "services":
                return ns == null ? client.services().withName(name).get() : client.services().inNamespace(ns).withName(name).get();
            case "configmaps":
                return ns == null ? client.configMaps().withName(name).get() : client.configMaps().inNamespace(ns).withName(name).get();
            case "secrets":
                return ns == null ? client.secrets().withName(name).get() : client.secrets().inNamespace(ns).withName(name).get();
            case "statefulsets":
                return ns == null ? client.apps().statefulSets().withName(name).get() : client.apps().statefulSets().inNamespace(ns).withName(name).get();
            case "daemonsets":
                return ns == null ? client.apps().daemonSets().withName(name).get() : client.apps().daemonSets().inNamespace(ns).withName(name).get();
            case "jobs":
                return ns == null ? client.batch().v1().jobs().withName(name).get() : client.batch().v1().jobs().inNamespace(ns).withName(name).get();
            case "cronjobs":
                return ns == null ? client.batch().v1().cronjobs().withName(name).get() : client.batch().v1().cronjobs().inNamespace(ns).withName(name).get();
            case "ingresses":
                return ns == null ? client.network().v1().ingresses().withName(name).get() : client.network().v1().ingresses().inNamespace(ns).withName(name).get();
            case "nodes":
                return client.nodes().withName(name).get();
            case "persistentvolumes":
                return client.persistentVolumes().withName(name).get();
            case "persistentvolumeclaims":
                return ns == null ? client.persistentVolumeClaims().withName(name).get() : client.persistentVolumeClaims().inNamespace(ns).withName(name).get();
            case "namespaces":
                return client.namespaces().withName(name).get();
            default:
                throw new IllegalArgumentException("不支持的资源类型: " + type);
        }
    }

    // ---------- 各资源列表转换 ----------

    private List<K8sResourceItem> podItems(PodList list) {
        List<K8sResourceItem> result = new ArrayList<>();
        for (Pod pod : list.getItems()) {
            int ready = 0, total = 0;
            if (pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null) {
                total = pod.getStatus().getContainerStatuses().size();
                for (var cs : pod.getStatus().getContainerStatuses()) {
                    if (Boolean.TRUE.equals(cs.getReady())) {
                        ready++;
                    }
                }
            }
            result.add(toItem("pods", "Pod", pod.getMetadata(), pod.getStatus() != null ? pod.getStatus().getPhase() : "-", ready + "/" + total));
        }
        return result;
    }

    private List<K8sResourceItem> deploymentItems(DeploymentList list) {
        List<K8sResourceItem> result = new ArrayList<>();
        for (Deployment d : list.getItems()) {
            Integer ready = d.getStatus() != null ? d.getStatus().getReadyReplicas() : null;
            Integer replicas = d.getSpec() != null ? d.getSpec().getReplicas() : null;
            String readyStr = (ready == null ? 0 : ready) + "/" + (replicas == null ? 0 : replicas);
            result.add(toItem("deployments", "Deployment", d.getMetadata(), readyStr, readyStr));
        }
        return result;
    }

    private List<K8sResourceItem> serviceItems(ServiceList list) {
        List<K8sResourceItem> result = new ArrayList<>();
        for (Service s : list.getItems()) {
            result.add(toItem("services", "Service", s.getMetadata(), s.getSpec() != null ? s.getSpec().getType() : "-", s.getSpec() != null ? s.getSpec().getClusterIP() : ""));
        }
        return result;
    }

    private List<K8sResourceItem> configMapItems(ConfigMapList list) {
        List<K8sResourceItem> result = new ArrayList<>();
        for (ConfigMap c : list.getItems()) {
            result.add(toItem("configmaps", "ConfigMap", c.getMetadata(), "-", ""));
        }
        return result;
    }

    private List<K8sResourceItem> secretItems(SecretList list) {
        List<K8sResourceItem> result = new ArrayList<>();
        for (Secret s : list.getItems()) {
            result.add(toItem("secrets", "Secret", s.getMetadata(), s.getType() != null ? s.getType() : "-", ""));
        }
        return result;
    }

    private List<K8sResourceItem> statefulSetItems(StatefulSetList list) {
        List<K8sResourceItem> result = new ArrayList<>();
        for (StatefulSet s : list.getItems()) {
            Integer ready = s.getStatus() != null ? s.getStatus().getReadyReplicas() : null;
            Integer replicas = s.getSpec() != null ? s.getSpec().getReplicas() : null;
            String readyStr = (ready == null ? 0 : ready) + "/" + (replicas == null ? 0 : replicas);
            result.add(toItem("statefulsets", "StatefulSet", s.getMetadata(), readyStr, readyStr));
        }
        return result;
    }

    private List<K8sResourceItem> daemonSetItems(DaemonSetList list) {
        List<K8sResourceItem> result = new ArrayList<>();
        for (DaemonSet d : list.getItems()) {
            Integer ready = d.getStatus() != null ? d.getStatus().getNumberReady() : null;
            Integer desired = d.getStatus() != null ? d.getStatus().getDesiredNumberScheduled() : null;
            String readyStr = (ready == null ? 0 : ready) + "/" + (desired == null ? 0 : desired);
            result.add(toItem("daemonsets", "DaemonSet", d.getMetadata(), readyStr, readyStr));
        }
        return result;
    }

    private List<K8sResourceItem> jobItems(JobList list) {
        List<K8sResourceItem> result = new ArrayList<>();
        for (Job j : list.getItems()) {
            String status = j.getStatus() != null && j.getStatus().getSucceeded() != null ? "完成" : "运行中";
            result.add(toItem("jobs", "Job", j.getMetadata(), status, ""));
        }
        return result;
    }

    private List<K8sResourceItem> cronJobItems(CronJobList list) {
        List<K8sResourceItem> result = new ArrayList<>();
        for (CronJob c : list.getItems()) {
            String status = c.getSpec() != null && Boolean.TRUE.equals(c.getSpec().getSuspend()) ? "已暂停" : "运行中";
            result.add(toItem("cronjobs", "CronJob", c.getMetadata(), status, c.getSpec() != null ? c.getSpec().getSchedule() : ""));
        }
        return result;
    }

    private List<K8sResourceItem> ingressItems(IngressList list) {
        List<K8sResourceItem> result = new ArrayList<>();
        for (Ingress i : list.getItems()) {
            result.add(toItem("ingresses", "Ingress", i.getMetadata(), "-", ""));
        }
        return result;
    }

    private List<K8sResourceItem> nodeItems(NodeList list) {
        List<K8sResourceItem> result = new ArrayList<>();
        for (Node n : list.getItems()) {
            String status = "Unknown";
            if (n.getStatus() != null && n.getStatus().getConditions() != null) {
                for (NodeCondition cond : n.getStatus().getConditions()) {
                    if ("Ready".equals(cond.getType())) {
                        status = "True".equals(cond.getStatus()) ? "Ready" : "NotReady";
                        break;
                    }
                }
            }
            result.add(toItem("nodes", "Node", n.getMetadata(), status, ""));
        }
        return result;
    }

    private List<K8sResourceItem> pvItems(PersistentVolumeList list) {
        List<K8sResourceItem> result = new ArrayList<>();
        for (PersistentVolume p : list.getItems()) {
            result.add(toItem("persistentvolumes", "PersistentVolume", p.getMetadata(), p.getStatus() != null ? p.getStatus().getPhase() : "-", ""));
        }
        return result;
    }

    private List<K8sResourceItem> pvcItems(PersistentVolumeClaimList list) {
        List<K8sResourceItem> result = new ArrayList<>();
        for (PersistentVolumeClaim p : list.getItems()) {
            result.add(toItem("persistentvolumeclaims", "PersistentVolumeClaim", p.getMetadata(), p.getStatus() != null ? p.getStatus().getPhase() : "-", ""));
        }
        return result;
    }

    private List<K8sResourceItem> namespaceItems(NamespaceList list) {
        List<K8sResourceItem> result = new ArrayList<>();
        for (Namespace ns : list.getItems()) {
            result.add(toItem("namespaces", "Namespace", ns.getMetadata(), ns.getStatus() != null ? ns.getStatus().getPhase() : "-", ""));
        }
        return result;
    }

    private K8sResourceItem toItem(String type, String kind, ObjectMeta meta, String status, String ready) {
        return K8sResourceItem.builder()
            .name(meta != null ? meta.getName() : "")
            .namespace(meta != null ? meta.getNamespace() : "")
            .type(type)
            .kind(kind)
            .status(status == null ? "" : status)
            .ready(ready == null ? "" : ready)
            .createdAt(meta != null ? meta.getCreationTimestamp() : "")
            .build();
    }

    private K8sClusterModel toModel(K8sClusterEntity entity) {
        K8sClusterModel model = K8sClusterModel.builder()
            .name(entity.getName())
            .kubeconfig(entity.getKubeconfig())
            .serverUrl(entity.getServerUrl())
            .namespace(entity.getNamespace())
            .remark(entity.getRemark())
            .build();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        return model;
    }
}
