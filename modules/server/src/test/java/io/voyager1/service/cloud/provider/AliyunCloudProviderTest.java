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

import com.aliyun.ecs20140526.models.DescribeInstancesResponseBody.DescribeInstancesResponseBodyInstancesInstance;
import com.aliyun.ecs20140526.models.DescribeInstancesResponseBody.DescribeInstancesResponseBodyInstancesInstancePublicIpAddress;
import com.aliyun.ecs20140526.models.DescribeInstancesResponseBody.DescribeInstancesResponseBodyInstancesInstanceTags;
import com.aliyun.ecs20140526.models.DescribeInstancesResponseBody.DescribeInstancesResponseBodyInstancesInstanceTagsTag;
import com.aliyun.bssopenapi20171214.models.QueryInstanceBillResponseBody.QueryInstanceBillResponseBodyDataItemsItem;
import io.voyager1.cloud.CloudBill;
import io.voyager1.cloud.CloudInstanceInfo;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 阿里云 provider 响应映射测试（用 SDK setter 构造假响应，无真实云调用）
 *
 * @since 2026/8/31
 */
public class AliyunCloudProviderTest {

    private final AliyunCloudProvider provider = new AliyunCloudProvider();

    @Test
    public void testToInfoMapping() {
        DescribeInstancesResponseBodyInstancesInstance instance = new DescribeInstancesResponseBodyInstancesInstance();
        instance.setInstanceId("i-001");
        instance.setInstanceName("web");
        instance.setStatus("Running");
        instance.setRegionId("cn-hangzhou");
        instance.setZoneId("cn-hangzhou-a");
        instance.setInstanceType("ecs.g7.large");
        instance.setCpu(2);
        instance.setMemory(8192);
        instance.setOSName("CentOS");
        instance.setExpiredTime("2027-01-01");
        instance.setInstanceChargeType("PostPaid");

        DescribeInstancesResponseBodyInstancesInstancePublicIpAddress publicIp =
            new DescribeInstancesResponseBodyInstancesInstancePublicIpAddress();
        publicIp.setIpAddress(Collections.singletonList("1.2.3.4"));
        instance.setPublicIpAddress(publicIp);

        DescribeInstancesResponseBodyInstancesInstanceTags tags =
            new DescribeInstancesResponseBodyInstancesInstanceTags();
        DescribeInstancesResponseBodyInstancesInstanceTagsTag tag = new DescribeInstancesResponseBodyInstancesInstanceTagsTag();
        tag.setTagKey("env");
        tag.setTagValue("prod");
        tags.setTag(Collections.singletonList(tag));
        instance.setTags(tags);

        CloudInstanceInfo info = provider.toInfo(instance);
        assertEquals("i-001", info.getInstanceId());
        assertEquals("web", info.getName());
        assertEquals("Running", info.getStatus());
        assertEquals("cn-hangzhou", info.getRegionId());
        assertEquals("cn-hangzhou-a", info.getZoneId());
        assertEquals("ecs.g7.large", info.getInstanceType());
        assertEquals(2, info.getCpu());
        assertEquals(8192, info.getMemory());
        assertEquals("CentOS", info.getOsName());
        assertEquals("2027-01-01", info.getExpireTime());
        assertEquals("PostPaid", info.getChargeType());
        assertEquals("1.2.3.4", info.getPublicIp());
        assertNotNull(info.getTags());
        assertEquals("prod", info.getTags().get("env"));
    }

    @Test
    public void testToInfoNoTags() {
        DescribeInstancesResponseBodyInstancesInstance instance = new DescribeInstancesResponseBodyInstancesInstance();
        instance.setInstanceId("i-002");
        CloudInstanceInfo info = provider.toInfo(instance);
        assertEquals("i-002", info.getInstanceId());
        assertEquals(null, info.getTags());
    }

    @Test
    public void testBillFromItem() {
        QueryInstanceBillResponseBodyDataItemsItem item = new QueryInstanceBillResponseBodyDataItemsItem();
        item.setBillingDate("2026-08-01");
        item.setProductName("云服务器 ECS");
        item.setInstanceID("i-001");
        item.setRegion("cn-hangzhou");
        item.setPretaxGrossAmount(100.5F);
        item.setCurrency("CNY");

        CloudBill bill = provider.billFromItem(item);
        assertEquals("2026-08-01", bill.getBillDate());
        assertEquals("云服务器 ECS", bill.getServiceName());
        assertEquals("i-001", bill.getResourceId());
        assertEquals("cn-hangzhou", bill.getRegion());
        assertEquals(100.5, bill.getAmount(), 0.001);
        assertEquals("CNY", bill.getCurrency());
    }
}
