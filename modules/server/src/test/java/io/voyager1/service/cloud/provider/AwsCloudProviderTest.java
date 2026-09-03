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

import io.voyager1.cloud.CloudBill;
import io.voyager1.cloud.CloudInstanceInfo;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.costexplorer.model.Group;
import software.amazon.awssdk.services.costexplorer.model.MetricValue;
import software.amazon.awssdk.services.ec2.model.CpuOptions;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Placement;
import software.amazon.awssdk.services.ec2.model.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * AWS provider 响应映射测试（用 SDK builder 构造响应对象，无真实云调用）
 *
 * @since 2026/8/31
 */
public class AwsCloudProviderTest {

    private final AwsCloudProvider provider = new AwsCloudProvider();

    private Instance buildInstance() {
        return Instance.builder()
            .instanceId("i-001")
            .instanceType(InstanceType.T3_MICRO)
            .state(InstanceState.builder().name(InstanceStateName.RUNNING).build())
            .publicIpAddress("1.2.3.4")
            .privateIpAddress("10.0.0.1")
            .placement(Placement.builder().availabilityZone("us-east-1a").build())
            .cpuOptions(CpuOptions.builder().coreCount(2).threadsPerCore(2).build())
            .tags(Tag.builder().key("Name").value("web").build(),
                Tag.builder().key("env").value("prod").build())
            .build();
    }

    @Test
    public void testToInfoMapping() {
        CloudInstanceInfo info = provider.toInfo(buildInstance(), "us-east-1");
        assertEquals("i-001", info.getInstanceId());
        assertEquals("web", info.getName());
        assertEquals("Running", info.getStatus(), "状态应规范化为首字母大写");
        assertEquals("us-east-1", info.getRegionId());
        assertEquals("us-east-1a", info.getZoneId());
        assertEquals("t3.micro", info.getInstanceType());
        assertEquals(2, info.getCpu());
        assertEquals("1.2.3.4", info.getPublicIp());
        assertEquals("10.0.0.1", info.getPrivateIp());
        assertEquals("PostPaid", info.getChargeType());
        assertNotNull(info.getTags());
        assertEquals("prod", info.getTags().get("env"));
    }

    @Test
    public void testToInfoNullState() {
        Instance instance = Instance.builder().instanceId("i-002").build();
        CloudInstanceInfo info = provider.toInfo(instance, "us-east-1");
        assertEquals("i-002", info.getInstanceId());
        assertNull(info.getStatus());
        assertNull(info.getPublicIp());
    }

    @Test
    public void testNameFromTags() {
        Instance withName = Instance.builder().tags(Tag.builder().key("Name").value("web").build()).build();
        assertEquals("web", provider.nameFromTags(withName));
        Instance withoutName = Instance.builder().build();
        assertNull(provider.nameFromTags(withoutName));
    }

    @Test
    public void testBillFromGroup() {
        Group group = Group.builder()
            .keys("Amazon EC2")
            .metrics(java.util.Collections.singletonMap("UnblendedCost",
                MetricValue.builder().amount("100.5").unit("USD").build()))
            .build();

        CloudBill bill = provider.billFromGroup(group, "us-east-1", "2026-08-01");
        assertEquals("2026-08-01", bill.getBillDate());
        assertEquals("Amazon EC2", bill.getServiceName());
        assertEquals(null, bill.getResourceId());
        assertEquals("us-east-1", bill.getRegion());
        assertEquals(100.5, bill.getAmount(), 0.001, "MetricValue.amount 字符串应解析为金额");
        assertEquals("USD", bill.getCurrency());
    }
}
