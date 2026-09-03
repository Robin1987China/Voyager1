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

/**
 * 云厂商凭证（SPI 调用参数，非持久化）
 *
 * @since 2026/8/12
 */
@Data
public class CloudCredential {

    /**
     * 云厂商标识（aliyun/tencent/aws/...）
     */
    private String vendor;

    /**
     * AccessKey（明文，由上层解密后传入）
     */
    private String accessKey;

    /**
     * SecretKey（明文，由上层解密后传入）
     */
    private String secretKey;

    /**
     * 额外凭证（厂商特有，如 Azure tenantId、GCP projectId）
     */
    private String extraKey;

    /**
     * 默认区域
     */
    private String region;
}
