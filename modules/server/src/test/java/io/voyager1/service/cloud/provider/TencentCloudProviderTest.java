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

package io.voyager1.service.cloud.provider;

import com.tencentcloudapi.billing.v20180709.models.BillDetail;
import com.tencentcloudapi.cvm.v20170312.models.Instance;
import com.tencentcloudapi.cvm.v20170312.models.Placement;
import com.tencentcloudapi.cvm.v20170312.models.Tag;
import io.voyager1.cloud.CloudBill;
import io.voyager1.cloud.CloudInstanceInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 腾讯云 provider 响应映射测试（用 SDK setter 构造假响应，无真实云调用）
 *
 * @since 2026/8/31
 */
public class TencentCloudProviderTest {

    private final TencentCloudProvider provider = new TencentCloudProvider();

    @Test
    public void testToInfoMapping() {
        Instance instance = new Instance();
        instance.setInstanceId("ins-001");
        instance.setInstanceName("web");
        instance.setInstanceState("RUNNING");
        instance.setInstanceType("S5.MEDIUM4");
        instance.setCPU(2L);
        instance.setMemory(4L); // GB
        instance.setOsName("TencentOS");
        instance.setExpiredTime("2027-01-01");
        instance.setInstanceChargeType("POSTPAID_BY_HOUR");
        instance.setPublicIpAddresses(new String[]{"1.2.3.4"});
        instance.setPrivateIpAddresses(new String[]{"10.0.0.1"});
        Placement placement = new Placement();
        placement.setZone("ap-guangzhou-3");
        instance.setPlacement(placement);
        Tag tag = new Tag();
        tag.setKey("env");
        tag.setValue("prod");
        instance.setTags(new Tag[]{tag});

        CloudInstanceInfo info = provider.toInfo(instance, "ap-guangzhou");
        assertEquals("ins-001", info.getInstanceId());
        assertEquals("web", info.getName());
        assertEquals("Running", info.getStatus(), "状态应规范化为首字母大写");
        assertEquals("ap-guangzhou", info.getRegionId());
        assertEquals("ap-guangzhou-3", info.getZoneId());
        assertEquals("S5.MEDIUM4", info.getInstanceType());
        assertEquals(2, info.getCpu());
        assertEquals(4096, info.getMemory(), "腾讯云内存 GB 应归一为 MB（4GB=4096MB）");
        assertEquals("TencentOS", info.getOsName());
        assertEquals("1.2.3.4", info.getPublicIp());
        assertEquals("10.0.0.1", info.getPrivateIp());
        assertNotNull(info.getTags());
        assertEquals("prod", info.getTags().get("env"));
    }

    @Test
    public void testBillFromItem() {
        BillDetail detail = new BillDetail();
        detail.setBillDay("2026-08-01");
        detail.setBusinessCodeName("云服务器 CVM");
        detail.setResourceId("ins-001");
        detail.setRegionName("华南地区（广州）");
        detail.setPriceInfo(new String[]{"{\"realTotalCost\":\"100.5\"}"});

        CloudBill bill = provider.billFromItem(detail);
        assertEquals("2026-08-01", bill.getBillDate());
        assertEquals("云服务器 CVM", bill.getServiceName());
        assertEquals("ins-001", bill.getResourceId());
        assertEquals("华南地区（广州）", bill.getRegion());
        assertEquals(100.5, bill.getAmount(), 0.001, "PriceInfo JSON 应解析 realTotalCost");
        assertEquals("CNY", bill.getCurrency());
    }

    @Test
    public void testParseAmountNull() {
        assertEquals(null, provider.parseAmount(null));
        assertEquals(null, provider.parseAmount(new String[0]));
        assertEquals(null, provider.parseAmount(new String[]{"not-json"}));
    }
}
