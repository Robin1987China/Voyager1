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

import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.EnumUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.DigestUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseGroupNameModel;
import io.voyager1.transport.INodeInfo;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

/**
 * @since 2023/2/18
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "INFRA_MACHINE",
    nameKey = "机器节点信息")
@Data
public class MachineNodeModel extends BaseGroupNameModel implements INodeInfo {
    /**
     * 机器主机名
     */
    private String hostName;

    public void setHostName(String hostName) {
        this.hostName = (hostName == null ? null : (hostName.length() <= 240 ? hostName : hostName.substring(0, 240)));
    }

    /**
     * 机器的 IP （多个）
     */
    private String hostIpv4s;
    /**
     * 负载
     */
    private String osLoadAverage;
    /**
     * 系统运行时间（自启动以来的时间）。
     * 自启动以来的秒数。
     */
    private Long osSystemUptime;
    /**
     * 系统名称
     */
    private String osName;

    public void setOsName(String osName) {
        this.osName = (osName == null ? null : (osName.length() <= 40 ? osName : osName.substring(0, 40)));
    }

    /**
     * 系统版本
     */
    private String osVersion;
    /**
     * 硬件版本
     */
    private String osHardwareVersion;
    /**
     * CPU数
     */
    private Integer osCpuCores;
    /**
     * 总内存
     */
    private Long osMoneyTotal;
    /**
     * 交互总内存
     */
    private Long osSwapTotal;
    /**
     * 虚拟总内存
     */
    private Long osVirtualMax;
    /**
     * 硬盘总大小
     */
    private Long osFileStoreTotal;
    /**
     * CPU 型号
     */
    private String osCpuIdentifierName;
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
    private Double osOccupyDisk;
    /**
     * 节点连接状态
     * <p>
     * 状态{0，无法连接，1 正常, 2 授权信息错误, 3 状态码错误，4 资源监控异常}
     */
    private Integer status;
    /**
     * 状态消息
     */
    private String statusMsg;
    /**
     * 传输方式。0 服务器拉取，1 节点机器推送
     */
    private Integer transportMode;
    /**
     * voyager1 通讯地址
     */
    private String voyager1Url;
    /**
     * 节点协议
     */
    private String voyager1Protocol;
    /**
     * 通讯登录账号
     */
    private String voyager1Username;
    /**
     * 通讯登录密码
     */
    private String voyager1Password;
    /**
     * 超时时间
     */
    private Integer voyager1Timeout;
    /**
     * http 代理
     */
    private String voyager1HttpProxy;
    /**
     * http 代理 类型
     */
    private String voyager1HttpProxyType;
    /**
     * voyager1 版本号
     */
    private String voyager1Version;
    /**
     * voyager1 启动时间
     */
    private Long voyager1Uptime;
    /**
     * Voyager1 打包时间
     */
    private String voyager1BuildTime;
    /**
     * 网络耗时（延迟）
     */
    private Integer networkDelay;
    /**
     * voyager1 项目数
     */
    private Integer voyager1ProjectCount;
    /**
     * voyager1 脚本数据
     */
    private Integer voyager1ScriptCount;
    /**
     * java 版本
     */
    private String javaVersion;
    /**
     * jvm 总内存
     */
    private Long jvmTotalMemory;
    /**
     * jvm 剩余内存
     */
    private Long jvmFreeMemory;
    /**
     * 模板节点 ，1 模板节点 0 非模板节点
     */
    private Boolean templateNode;
    /**
     * 安装 id
     */
    private String installId;
    /**
     * 扩展信息
     */
    private String extendInfo;
    /**
     * 传输加密方式 0 不加密 1 BASE64 2 AES
     */
    private Integer transportEncryption;

    @Override
    public String name() {
        return this.getName();
    }

    @Override
    public String loginName() {
        return this.voyager1Username;
    }

    @Override
    public String url() {
        return this.getVoyager1Url();
    }

    @Override
    public String scheme() {
        return getVoyager1Protocol();
    }

    /**
     * 获取 授权的信息
     *
     * @return sha1
     */
    @Override
    public String authorize() {
        return DigestUtil.sha1(this.voyager1Username + "@" + this.voyager1Password);
    }

    /**
     * 获取节点的代理
     *
     * @return proxy
     */
    @Override
    public Proxy proxy() {
        String httpProxy = this.getVoyager1HttpProxy();
        if ((httpProxy != null && !httpProxy.isEmpty())) {
            List<String> split = io.voyager1.util.ConvertUtil.splitTrim(httpProxy, ":");
            String host = (split == null || split.isEmpty() ? null : split.get(0));
            int port = ConvertUtil.toInt((split == null || split.isEmpty() ? null : split.get(split.size() - 1)), 0);
            String type = this.getVoyager1HttpProxyType();
            Proxy.Type type1 = EnumUtil.fromString(Proxy.Type.class, type, Proxy.Type.HTTP);
            return new Proxy(type1, new InetSocketAddress(host, port));
        }
        return null;
    }

    @Override
    public Integer timeout() {
        return this.getVoyager1Timeout();
    }

    @Override
    public Integer transportEncryption() {
        // 需要兼容旧数据
        return (this.getTransportEncryption() != null ? this.getTransportEncryption() : 0);
    }
}
