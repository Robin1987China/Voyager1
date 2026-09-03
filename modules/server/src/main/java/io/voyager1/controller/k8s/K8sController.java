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

package io.voyager1.controller.k8s;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.model.data.K8sClusterModel;
import io.voyager1.model.dto.K8sResourceItem;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.k8s.K8sService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * K8s API（集群管理 + 资源查看/详情/操作/日志/事件 + 部署）
 *
 * @since 2026/8/9
 */
@RestController
@RequestMapping(value = "/k8s")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE)
public class K8sController extends BaseServerController {

    private final K8sService k8sService;

    public K8sController(K8sService k8sService) {
        this.k8sService = k8sService;
    }

    /**
     * 保存集群
     */
    @PostMapping(value = "cluster/save", produces = "application/json")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> save(String id, String name, String kubeconfig, String serverUrl, String namespace, String remark) {
        return ApiResult.success("保存成功", k8sService.save(id, name, kubeconfig, serverUrl, namespace, remark));
    }

    /**
     * 集群列表
     */
    @PostMapping(value = "cluster/list", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<K8sClusterModel>> list() {
        return ApiResult.success("", k8sService.listAll());
    }

    /**
     * 删除集群
     */
    @PostMapping(value = "cluster/delete", produces = "application/json")
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> delete(String id) {
        k8sService.deleteCluster(id);
        return ApiResult.success("删除成功");
    }

    /**
     * 命名空间列表
     */
    @PostMapping(value = "namespace/list", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<String>> namespaces(String id) {
        return ApiResult.success("", k8sService.listNamespaces(id));
    }

    /**
     * 结构化资源列表
     */
    @PostMapping(value = "resource/list", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<K8sResourceItem>> resources(String id, String namespace, String type) {
        return ApiResult.success("", k8sService.listResources(id, namespace, type));
    }

    /**
     * 资源详情（YAML）
     */
    @PostMapping(value = "resource/detail", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<String> detail(String id, String namespace, String type, String name) {
        return ApiResult.success("", k8sService.getResourceDetail(id, namespace, type, name));
    }

    /**
     * 删除资源
     */
    @PostMapping(value = "resource/delete", produces = "application/json")
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> deleteResource(String id, String namespace, String type, String name) {
        k8sService.deleteResource(id, namespace, type, name);
        return ApiResult.success("删除成功");
    }

    /**
     * Deployment 扩缩容
     */
    @PostMapping(value = "deployment/scale", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> scale(String id, String namespace, String name, Integer replicas) {
        k8sService.scaleDeployment(id, namespace, name, ConvertUtil.toInt(replicas, 1));
        return ApiResult.success("扩缩容成功");
    }

    /**
     * Deployment 滚动重启
     */
    @PostMapping(value = "deployment/restart", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> restart(String id, String namespace, String name) {
        k8sService.restartDeployment(id, namespace, name);
        return ApiResult.success("重启成功");
    }

    /**
     * Pod 日志
     */
    @PostMapping(value = "pod/log", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<String> podLog(String id, String namespace, String name, Integer tailLines) {
        return ApiResult.success("", k8sService.getPodLog(id, namespace, name, tailLines));
    }

    /**
     * 事件列表
     */
    @PostMapping(value = "event/list", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<Map<String, Object>>> events(String id, String namespace) {
        return ApiResult.success("", k8sService.listEvents(id, namespace));
    }

    /**
     * 部署：apply manifest（createOrReplace）
     */
    @PostMapping(value = "deploy/apply", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> apply(String id, String namespace, String manifest) {
        if ((manifest == null || manifest.isEmpty())) {
            return ApiResult.fail("manifest 不能为空");
        }
        k8sService.applyManifest(id, manifest);
        return ApiResult.success("部署成功");
    }
}
