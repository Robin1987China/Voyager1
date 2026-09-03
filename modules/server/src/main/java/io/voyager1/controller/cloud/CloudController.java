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

package io.voyager1.controller.cloud;

import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.cloud.CloudScalingGroup;
import io.voyager1.cloud.CloudSecurityGroup;
import io.voyager1.cloud.CloudSnapshot;
import io.voyager1.model.data.CloudAccountModel;
import io.voyager1.model.data.CloudInstanceModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.cloud.CloudInstanceService;
import io.voyager1.service.cloud.CloudService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 云资产 API（云账号 + 云实例）
 *
 * @since 2026/8/9
 */
@RestController
@RequestMapping(value = "/cloud")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE)
public class CloudController extends BaseServerController {

    private final CloudService cloudService;
    private final CloudInstanceService cloudInstanceService;

    public CloudController(CloudService cloudService, CloudInstanceService cloudInstanceService) {
        this.cloudService = cloudService;
        this.cloudInstanceService = cloudInstanceService;
    }

    /**
     * 保存云账号
     */
    @PostMapping(value = "account/save", produces = "application/json")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> saveAccount(String id, String name, String vendor, String accessKey, String secretKey, String extraKey, String region, String remark) {
        return ApiResult.success("保存成功", cloudService.saveAccount(id, name, vendor, accessKey, secretKey, extraKey, region, remark));
    }

    /**
     * 云账号列表
     */
    @PostMapping(value = "account/list", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<CloudAccountModel>> listAccounts() {
        return ApiResult.success("", cloudService.listAccounts());
    }

    /**
     * 实例列表（按账号）
     */
    @PostMapping(value = "instance/list", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<CloudInstanceModel>> listInstances(String accountId) {
        return ApiResult.success("", cloudInstanceService.listByAccount(accountId));
    }

    /**
     * 新增实例（手动录入）
     */
    @PostMapping(value = "instance/save", produces = "application/json")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> saveInstance(String accountId, String instanceId, String name, String publicIp, String privateIp, String status, String groupName) {
        CloudInstanceModel instance = CloudInstanceModel.builder()
            .accountId(accountId)
            .instanceId(instanceId)
            .name(name)
            .publicIp(publicIp)
            .privateIp(privateIp)
            .status(status)
            .groupName(groupName)
            .build();
        return ApiResult.success("保存成功", cloudInstanceService.saveInstance(instance));
    }

    /**
     * 导入为机器（SSH 部署目标）
     */
    @PostMapping(value = "instance/import-machine", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> importMachine(String id, String sshUser, Integer sshPort, String password) {
        String machineId = cloudInstanceService.importAsMachine(id, sshUser, sshPort, password);
        if ((machineId == null || machineId.isEmpty())) {
            return ApiResult.success("导入失败：实例不存在", machineId);
        }
        return ApiResult.success("已导入为机器", machineId);
    }

    /**
     * 云账号连通性校验
     */
    @PostMapping(value = "account/connectivity-test", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<Boolean> testConnectivity(String id) {
        boolean connected = cloudService.testConnectivity(id);
        return ApiResult.success(connected ? "连通成功" : "连通失败", connected);
    }

    /**
     * 同步云实例（从云厂商 API 拉取）
     */
    @PostMapping(value = "instance/sync", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<Integer> syncInstances(String accountId) {
        return ApiResult.success("同步完成", cloudService.syncInstances(accountId));
    }

    /**
     * 启动实例
     */
    @PostMapping(value = "instance/start", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> startInstance(String accountId, String instanceId) {
        cloudService.operateInstance(accountId, instanceId, "start");
        return ApiResult.success("启动指令已下发", instanceId);
    }

    /**
     * 停止实例
     */
    @PostMapping(value = "instance/stop", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> stopInstance(String accountId, String instanceId) {
        cloudService.operateInstance(accountId, instanceId, "stop");
        return ApiResult.success("停止指令已下发", instanceId);
    }

    /**
     * 重启实例
     */
    @PostMapping(value = "instance/reboot", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> rebootInstance(String accountId, String instanceId) {
        cloudService.operateInstance(accountId, instanceId, "reboot");
        return ApiResult.success("重启指令已下发", instanceId);
    }

    /**
     * 规格变配
     */
    @PostMapping(value = "instance/resize", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> resizeInstance(String accountId, String instanceId, String newInstanceType) {
        cloudService.resizeInstance(accountId, instanceId, newInstanceType);
        return ApiResult.success("变配指令已下发", instanceId);
    }

    /**
     * 创建磁盘快照
     */
    @PostMapping(value = "snapshot/create", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> createSnapshot(String accountId, String diskId, String snapshotName) {
        return ApiResult.success("快照创建中", cloudService.createSnapshot(accountId, diskId, snapshotName));
    }

    /**
     * 快照列表
     */
    @PostMapping(value = "snapshot/list", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<CloudSnapshot>> listSnapshots(String accountId) {
        return ApiResult.success("", cloudService.listSnapshots(accountId));
    }

    /**
     * 删除快照
     */
    @PostMapping(value = "snapshot/delete", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> deleteSnapshot(String accountId, String snapshotId) {
        cloudService.deleteSnapshot(accountId, snapshotId);
        return ApiResult.success("删除成功", snapshotId);
    }

    /**
     * 安全组列表
     */
    @PostMapping(value = "security-group/list", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<CloudSecurityGroup>> listSecurityGroups(String accountId) {
        return ApiResult.success("", cloudService.listSecurityGroups(accountId));
    }

    /**
     * 从实例创建自定义镜像
     */
    @PostMapping(value = "instance/create-image", produces = "application/json")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> createImage(String accountId, String instanceId, String imageName) {
        return ApiResult.success("镜像创建中", cloudService.createImage(accountId, instanceId, imageName));
    }

    /**
     * 弹性伸缩组列表
     */
    @PostMapping(value = "scaling-group/list", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<CloudScalingGroup>> listScalingGroups(String accountId) {
        return ApiResult.success("", cloudService.listScalingGroups(accountId));
    }
}
