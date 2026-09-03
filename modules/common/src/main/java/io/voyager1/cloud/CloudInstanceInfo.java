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

import lombok.Data;

import java.util.Map;

/**
 * 云实例统一模型（SPI 返回，厂商无关）
 *
 * @since 2026/8/12
 */
@Data
public class CloudInstanceInfo {

    /**
     * 实例 ID（云厂商实例标识）
     */
    private String instanceId;

    /**
     * 实例名称
     */
    private String name;

    /**
     * 实例状态
     */
    private String status;

    /**
     * 区域
     */
    private String regionId;

    /**
     * 可用区
     */
    private String zoneId;

    /**
     * 公网 IP
     */
    private String publicIp;

    /**
     * 内网 IP
     */
    private String privateIp;

    /**
     * 实例规格（如 ecs.g7.large）
     */
    private String instanceType;

    /**
     * CPU 核数
     */
    private Integer cpu;

    /**
     * 内存（MB）
     */
    private Integer memory;

    /**
     * 操作系统名称
     */
    private String osName;

    /**
     * 到期时间
     */
    private String expireTime;

    /**
     * 计费类型（PostPaid/PrePaid，FinOps 使用）
     */
    private String chargeType;

    /**
     * 标签（FinOps 分摊使用）
     */
    private Map<String, String> tags;
}
