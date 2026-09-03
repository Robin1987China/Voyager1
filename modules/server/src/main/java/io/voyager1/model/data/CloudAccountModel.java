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
 * 云账号（AK 加密存储）
 *
 * @since 2026/8/9
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "CLOUD_ACCOUNT", nameKey = "云账号")
@Data
@Builder
public class CloudAccountModel extends BaseDbModel {

    /**
     * 账号名称
     */
    private String name;

    /**
     * 云厂商（aliyun/tencent/huawei/aws/azure/gcp/volcengine）
     */
    private String vendor;

    /**
     * AccessKey（加密存储）
     */
    private String accessKey;

    /**
     * SecretKey（加密存储）
     */
    private String secretKey;

    /**
     * 额外凭证（加密存储，如 Azure tenantId、GCP projectId）
     */
    private String extraKey;

    /**
     * 默认区域
     */
    private String region;

    /**
     * 备注
     */
    private String remark;
}
