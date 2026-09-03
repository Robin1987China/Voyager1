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

package io.voyager1.model.docker;

import io.voyager1.util.PropIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.func.assets.model.MachineDockerModel;
import io.voyager1.model.BaseWorkspaceModel;
import io.voyager1.model.data.WorkspaceModel;

/**
 * @since 2022/1/26
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "INFRA_DOCKER",
    nameKey = "docker 信息")
public class DockerInfoModel extends BaseWorkspaceModel {
    /**
     * 名称
     */
    private String name;
    /**
     * 地址
     */
    @Deprecated
    private String host;
    /**
     * 开启 tls 验证
     */
    @Deprecated
    private Boolean tlsVerify;
    /**
     * 证书路径
     */
    @PropIgnore
    private Boolean certExist;
    /**
     * 集群节点ID
     */
    @Deprecated
    private String swarmNodeId;
    /**
     * 最后心跳时间
     */
    @Deprecated
    private Long lastHeartbeatTime;
    /**
     * 超时时间，单位 秒
     */
    @Deprecated
    private Integer heartbeatTimeout;
    /**
     * 标签
     */
    private String tags;
    /**
     * 集群ID
     */
    @Deprecated
    private String swarmId;

    /**
     * 仓库账号
     */
    @Deprecated
    private String registryUsername;

    /**
     * 仓库密码
     */
    @Deprecated
    private String registryPassword;

    /**
     * 仓库邮箱
     */
    @Deprecated
    private String registryEmail;

    /**
     * 仓库地址
     */
    @Deprecated
    private String registryUrl;

    /**
     * 机器 docker id
     */
    private String machineDockerId;

    @PropIgnore
    private MachineDockerModel machineDocker;

    @PropIgnore
    private WorkspaceModel workspace;

}
