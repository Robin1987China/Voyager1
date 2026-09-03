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

package io.voyager1.cloud;

import java.util.List;

/**
 * 云厂商 SPI：统一抽象多云对接能力。
 * <p>
 * 每个云厂商提供一个实现，按 {@link #vendor()} 路由。M1 仅实现阿里云样板，
 * 其余厂商按同一接口在 M2 接入。
 *
 * @since 2026/8/12
 */
public interface ICloudProvider {

    /**
     * 云厂商标识（与 CLOUD_ACCOUNT.vendor 一致）
     *
     * @return vendor
     */
    String vendor();

    /**
     * 连通性校验（AK/SK 是否可用）
     *
     * @param credential 凭证
     * @return true 连通
     * @throws Exception 调用异常
     */
    boolean testConnectivity(CloudCredential credential) throws Exception;

    /**
     * 拉取区域下的云主机实例列表
     *
     * @param credential 凭证
     * @param region     区域（为空时使用凭证默认区域）
     * @return 实例列表
     * @throws Exception 调用异常
     */
    List<CloudInstanceInfo> listInstances(CloudCredential credential, String region) throws Exception;

    /**
     * 启动实例
     *
     * @param credential 凭证
     * @param region     区域
     * @param instanceId 实例 ID
     * @throws Exception 调用异常
     */
    void startInstance(CloudCredential credential, String region, String instanceId) throws Exception;

    /**
     * 停止实例
     *
     * @param credential 凭证
     * @param region     区域
     * @param instanceId 实例 ID
     * @throws Exception 调用异常
     */
    void stopInstance(CloudCredential credential, String region, String instanceId) throws Exception;

    /**
     * 重启实例
     *
     * @param credential 凭证
     * @param region     区域
     * @param instanceId 实例 ID
     * @throws Exception 调用异常
     */
    void rebootInstance(CloudCredential credential, String region, String instanceId) throws Exception;

    /**
     * 状态规范化（统一为首字母大写，如 Running/Stopped/Starting/Stopping/Pending）
     *
     * @param rawStatus 原始状态
     * @return 规范化状态
     */
    default String normalizeStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isEmpty()) {
            return rawStatus;
        }
        String lower = rawStatus.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    /**
     * 规格变配（可选操作，未实现的厂商抛 UnsupportedOperationException）
     *
     * @param credential      凭证
     * @param region          区域
     * @param instanceId      实例 ID
     * @param newInstanceType 新规格（如 ecs.g7.2xlarge / S5.MEDIUM4 / t3.large）
     * @throws Exception 调用异常
     */
    default void resizeInstance(CloudCredential credential, String region, String instanceId, String newInstanceType) throws Exception {
        throw new UnsupportedOperationException("该厂商不支持规格变配");
    }

    /**
     * 创建磁盘快照（可选操作，未实现的厂商抛 UnsupportedOperationException）
     *
     * @param credential   凭证
     * @param region       区域
     * @param diskId       磁盘/卷 ID
     * @param snapshotName 快照名称
     * @return 快照 ID
     * @throws Exception 调用异常
     */
    default String createSnapshot(CloudCredential credential, String region, String diskId, String snapshotName) throws Exception {
        throw new UnsupportedOperationException("该厂商不支持快照");
    }

    /**
     * 列出快照（可选操作）
     *
     * @param credential 凭证
     * @param region     区域
     * @return 快照列表
     * @throws Exception 调用异常
     */
    default List<CloudSnapshot> listSnapshots(CloudCredential credential, String region) throws Exception {
        throw new UnsupportedOperationException("该厂商不支持快照");
    }

    /**
     * 删除快照（可选操作）
     *
     * @param credential 凭证
     * @param region     区域
     * @param snapshotId 快照 ID
     * @throws Exception 调用异常
     */
    default void deleteSnapshot(CloudCredential credential, String region, String snapshotId) throws Exception {
        throw new UnsupportedOperationException("该厂商不支持快照");
    }

    /**
     * 列出安全组（可选操作，未实现的厂商抛 UnsupportedOperationException）
     *
     * @param credential 凭证
     * @param region     区域
     * @return 安全组列表
     * @throws Exception 调用异常
     */
    default List<CloudSecurityGroup> listSecurityGroups(CloudCredential credential, String region) throws Exception {
        throw new UnsupportedOperationException("该厂商不支持安全组");
    }

    /**
     * 从实例创建自定义镜像（可选操作，未实现的厂商抛 UnsupportedOperationException）
     *
     * @param credential 凭证
     * @param region     区域
     * @param instanceId 实例 ID
     * @param imageName  镜像名称
     * @return 镜像 ID
     * @throws Exception 调用异常
     */
    default String createImage(CloudCredential credential, String region, String instanceId, String imageName) throws Exception {
        throw new UnsupportedOperationException("该厂商不支持镜像构建");
    }

    /**
     * 列出弹性伸缩组（可选操作，未实现的厂商抛 UnsupportedOperationException）
     *
     * @param credential 凭证
     * @param region     区域
     * @return 伸缩组列表
     * @throws Exception 调用异常
     */
    default List<CloudScalingGroup> listScalingGroups(CloudCredential credential, String region) throws Exception {
        throw new UnsupportedOperationException("该厂商不支持弹性伸缩");
    }

    /**
     * 拉取账单明细（FinOps，可选操作，未实现的厂商抛 UnsupportedOperationException）
     *
     * @param credential   凭证
     * @param region       区域
     * @param billingCycle 账期（yyyy-MM）
     * @return 账单明细列表
     * @throws Exception 调用异常
     */
    default List<CloudBill> listBills(CloudCredential credential, String region, String billingCycle) throws Exception {
        throw new UnsupportedOperationException("该厂商不支持账单采集");
    }
}
