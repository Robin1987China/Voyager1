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
 * 云账单明细统一模型（SPI 返回，厂商无关）
 *
 * @since 2026/8/31
 */
@Data
public class CloudBill {

    /**
     * 账单日期（yyyy-MM-dd）
     */
    private String billDate;

    /**
     * 服务/产品名称
     */
    private String serviceName;

    /**
     * 资源/实例 ID
     */
    private String resourceId;

    /**
     * 区域
     */
    private String region;

    /**
     * 应付金额（元）
     */
    private Double amount;

    /**
     * 币种
     */
    private String currency;
}
