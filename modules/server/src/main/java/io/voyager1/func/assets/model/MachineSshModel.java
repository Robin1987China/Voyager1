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

import io.voyager1.util.CharsetUtil;
import io.voyager1.util.EnumUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseGroupNameModel;
import io.voyager1.model.data.SshModel;
import io.voyager1.plugins.ISshInfo;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;

/**
 * @see SshModel
 * @since 2023/2/25
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "INFRA_MACHINE_SSH",
    nameKey = "机器SSH信息")
@Data
@NoArgsConstructor
public class MachineSshModel extends BaseGroupNameModel implements ISshInfo {

    /**
     * 主机地址
     */
    private String host;
    /**
     * 端口
     */
    private Integer port;
    /**
     * 登录账号
     */
    private String user;
    /**
     * 账号密码，证书密码
     */
    private String password;
    /**
     * 编码格式
     */
    private String charset;
    /**
     * ssh 私钥
     */
    private String privateKey;

    private String connectType;
    /**
     * 节点超时时间
     */
    private Integer timeout;

    /**
     * ssh连接状态
     * <p>
     * 状态{0，无法连接，1 正常，2 禁用监控}
     */
    private Integer status;
    /**
     * 状态消息
     */
    private String statusMsg;

    private String allowEditSuffix;
    /**
     * 系统名称
     */
    private String osName;

    public void setOsName(String osName) {
        this.osName = (osName == null ? null : (osName.length() <= 45 ? osName : osName.substring(0, 45)));
    }

    /**
     * 机器主机名
     */
    private String hostName;

    public void setHostName(String hostName) {
        this.hostName = (hostName == null ? null : (hostName.length() <= 240 ? hostName : hostName.substring(0, 240)));
    }

    /**
     * 系统版本
     */
    private String osVersion;
    /**
     * 总内存
     */
    private Long osMoneyTotal;
    /**
     * 硬盘总大小
     */
    private Long osFileStoreTotal;
    /**
     * CPU 型号
     */
    private String osCpuIdentifierName;
    /**
     * CPU数
     */
    private Integer osCpuCores;
    /**
     * 占用cpu
     */
    private Double osOccupyCpu;
    /**
     * 占用内存 （总共）
     */
    private Double osOccupyMemory;
    /**
     * 占用磁盘
     */
    private Double osMaxOccupyDisk;
    /**
     * 占用磁盘 分区名
     */
    private String osMaxOccupyDiskName;
    /**
     * 负载
     */
    private String osLoadAverage;
    /**
     * 系统运行时间（自启动以来的时间）。
     * 自启动以来的毫秒数。
     */
    private Long osSystemUptime;
    /**
     * voyager1 查询进程号
     */
    private Integer voyager1AgentPid;
    /**
     * java 版本
     */
    private String javaVersion;
    /**
     * 服务器中的 docker 信息
     */
    private String dockerInfo;

    public MachineSshModel(String id) {
        setId(id);
    }

    public void allowEditSuffix(List<String> allowEditSuffix) {
        if (allowEditSuffix == null) {
            this.allowEditSuffix = null;
        } else {
            this.allowEditSuffix = JSONArray.toJSONString(allowEditSuffix);
        }
    }

    @Override
    public String id() {
        return getId();
    }

    @Override
    public String host() {
        return getHost();
    }

    @Override
    public String user() {
        return getUser();
    }

    @Override
    public String password() {
        return getPassword();
    }

    @Override
    public String privateKey() {
        return getPrivateKey();
    }

    @Override
    public int port() {
        return (getPort() != null ? getPort() : 0);
    }

    @Override
    public ISshInfo.ConnectType connectType() {
        return EnumUtil.fromString(MachineSshModel.ConnectType.class, this.connectType, MachineSshModel.ConnectType.PASS);
    }

    /**
     * 超时时间
     *
     * @return 最小值 1 秒钟
     */
    @Override
    public int timeout() {
        if (this.timeout == null) {
            return (int) TimeUnit.SECONDS.toMillis(5);
        }
        return (int) TimeUnit.SECONDS.toMillis(Math.max(1, this.timeout));
    }

    @Override
    public Charset charset() {
        return CharsetUtil.parse(this.getCharset(), StandardCharsets.UTF_8);
    }
}
