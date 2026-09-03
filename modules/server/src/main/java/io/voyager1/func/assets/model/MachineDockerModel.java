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

package io.voyager1.func.assets.model;

import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.DigestUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import io.voyager1.Voyager1Application;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseGroupNameModel;
import io.voyager1.model.docker.DockerInfoModel;
import org.springframework.util.Assert;

import java.io.File;

/**
 * @see DockerInfoModel
 * @since 2023/3/3
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "INFRA_MACHINE_DOCKER",
    nameKey = "机器DOCKER信息")
@Data
@NoArgsConstructor
public class MachineDockerModel extends BaseGroupNameModel {
    /**
     * 地址
     */
    private String host;
    /**
     * 开启 tls 验证
     */
    private Boolean tlsVerify;
    /**
     * 证书信息
     */
    private String certInfo;

    private Boolean certExist;
    /**
     * 状态 0 , 异常离线 1 正常
     */
    private Integer status;
    /**
     * 错误消息
     */
    private String failureMsg;
    /**
     * docker 版本
     */
    private String dockerVersion;
    /**
     * 最后心跳时间
     */
    private Long lastHeartbeatTime;
    /**
     * 超时时间，单位 秒
     */
    private Integer heartbeatTimeout;
    /**
     * 仓库账号
     */
    private String registryUsername;

    /**
     * 仓库密码
     */
    private String registryPassword;

    /**
     * 仓库邮箱
     */
    private String registryEmail;

    /**
     * 仓库地址
     */
    private String registryUrl;

    /**
     * 集群ID
     */
    private String swarmId;
    /**
     * 集群节点ID
     */
    private String swarmNodeId;
    /**
     * 集群的创建时间
     */
    private Long swarmCreatedAt;
    /**
     * 集群的更新时间
     */
    private Long swarmUpdatedAt;
    /**
     * 节点 地址
     */
    private String swarmNodeAddr;
    /**
     * 集群管理员
     */
    private Boolean swarmControlAvailable;

    /**
     * 开启 SSH 访问
     */
    private Boolean enableSsh;

    /**
     * SSH Id
     */
    private String machineSshId;
    /**
     * 是否使用 sudo 执行命令
     */
    private Boolean sshUseSudo;


    public void setFailureMsg(String failureMsg) {
        this.failureMsg = (failureMsg == null ? null : (failureMsg.length() <= 240 ? failureMsg : failureMsg.substring(0, 240)));
    }

    public boolean isControlAvailable() {
        return swarmControlAvailable != null && swarmControlAvailable;
    }

    /**
     * 生成证书路径
     *
     * @return path
     */
    @Deprecated
    public String generateCertPath() {
        String dataPath = Voyager1Application.getInstance().getDataPath();
        String host = this.getHost();
        Assert.hasText(host, "host empty");
        host = DigestUtil.sha1(host);
        File docker = FileUtil.file(dataPath, "docker", "tls-cert", host);
        return FileUtil.getAbsolutePath(docker);
    }


    public void restSwarm() {
        this.setSwarmId("");
        this.setSwarmNodeId("");
        this.setSwarmCreatedAt(0L);
        this.setSwarmUpdatedAt(0L);
        this.setSwarmNodeAddr("");
        this.setSwarmControlAvailable(false);
    }

}
