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

package io.voyager1.func.system.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseUserModifyDbModel;

/**
 * @since 2023/8/19
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "INFRA_CLUSTER",
    nameKey = "集群信息")
@Data
public class ClusterInfoModel extends BaseUserModifyDbModel {
    /**
     * 集群Id
     */
    private String clusterId;
    /**
     * 集群名称
     */
    private String name;
    /**
     * 集群地址
     */
    private String url;
    /**
     * 集群关联的分组
     */
    private String linkGroup;
    /**
     * 最后心跳时间
     */
    private Long lastHeartbeat;
    /**
     * 主机名
     */
    private String localHostName;
    /**
     * voyager1 版本
     */
    private String voyager1Version;
    /**
     * 集群地址状态消息
     */
    private String statusMsg;
}
