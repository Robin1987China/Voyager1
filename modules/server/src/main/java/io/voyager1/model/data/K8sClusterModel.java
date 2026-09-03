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

package io.voyager1.model.data;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseDbModel;

/**
 * K8s 集群（kubeconfig 接入）
 *
 * @since 2026/8/9
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "K8S_CLUSTER", nameKey = "K8s集群")
@Data
@Builder
public class K8sClusterModel extends BaseDbModel {

    /**
     * 集群名称
     */
    private String name;

    /**
     * kubeconfig 内容
     */
    private String kubeconfig;

    /**
     * 集群服务地址
     */
    private String serverUrl;

    /**
     * 默认命名空间
     */
    private String namespace;

    /**
     * 备注
     */
    private String remark;
}
