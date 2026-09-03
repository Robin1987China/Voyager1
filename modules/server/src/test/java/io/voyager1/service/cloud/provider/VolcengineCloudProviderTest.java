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

import com.volcengine.ecs.model.EipAddressForDescribeInstancesOutput;
import com.volcengine.ecs.model.InstanceForDescribeInstancesOutput;
import com.volcengine.ecs.model.NetworkInterfaceForDescribeInstancesOutput;
import com.volcengine.ecs.model.TagForDescribeInstancesOutput;
import io.voyager1.cloud.CloudInstanceInfo;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 火山引擎 provider 响应映射测试（用 SDK setter 构造假响应，无真实云调用）
 *
 * @since 2026/8/31
 */
public class VolcengineCloudProviderTest {

    private final VolcengineCloudProvider provider = new VolcengineCloudProvider();

    @Test
    public void testToInfoMapping() {
        InstanceForDescribeInstancesOutput instance = new InstanceForDescribeInstancesOutput();
        instance.setInstanceId("i-001");
        instance.setInstanceName("web");
        instance.setStatus("RUNNING");
        instance.setInstanceTypeId("ecs.g2i.large");
        instance.setCpus(2);
        instance.setMemorySize(8192);
        instance.setZoneId("cn-beijing-a");
        instance.setOsName("CentOS");
        instance.setExpiredAt("2027-01-01");
        instance.setInstanceChargeType("PostPaid");

        EipAddressForDescribeInstancesOutput eip = new EipAddressForDescribeInstancesOutput();
        eip.setIpAddress("1.2.3.4");
        instance.setEipAddress(eip);

        NetworkInterfaceForDescribeInstancesOutput nic = new NetworkInterfaceForDescribeInstancesOutput();
        nic.setPrimaryIpAddress("10.0.0.1");
        instance.setNetworkInterfaces(Collections.singletonList(nic));

        TagForDescribeInstancesOutput tag = new TagForDescribeInstancesOutput();
        tag.setKey("env");
        tag.setValue("prod");
        instance.setTags(Collections.singletonList(tag));

        CloudInstanceInfo info = provider.toInfo(instance, "cn-beijing");
        assertEquals("i-001", info.getInstanceId());
        assertEquals("web", info.getName());
        assertEquals("Running", info.getStatus());
        assertEquals("cn-beijing", info.getRegionId());
        assertEquals("cn-beijing-a", info.getZoneId());
        assertEquals("ecs.g2i.large", info.getInstanceType());
        assertEquals(2, info.getCpu());
        assertEquals(8192, info.getMemory());
        assertEquals("CentOS", info.getOsName());
        assertEquals("1.2.3.4", info.getPublicIp());
        assertEquals("10.0.0.1", info.getPrivateIp());
        assertNotNull(info.getTags());
        assertEquals("prod", info.getTags().get("env"));
    }
}
