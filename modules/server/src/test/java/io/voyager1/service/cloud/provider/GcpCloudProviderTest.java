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

import com.google.cloud.compute.v1.AccessConfig;
import com.google.cloud.compute.v1.Instance;
import com.google.cloud.compute.v1.NetworkInterface;
import com.google.cloud.compute.v1.Tags;
import io.voyager1.cloud.CloudInstanceInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * GCP provider 响应映射测试（用 protobuf builder 构造假响应，无真实云调用）
 *
 * @since 2026/8/31
 */
public class GcpCloudProviderTest {

    private final GcpCloudProvider provider = new GcpCloudProvider();

    @Test
    public void testToInfoMapping() {
        Instance instance = Instance.newBuilder()
            .setId(12345L)
            .setName("web")
            .setStatus("RUNNING")
            .setZone("projects/my-project/zones/us-central1-a")
            .setMachineType("projects/my-project/zones/us-central1-a/machineTypes/e2-medium")
            .addNetworkInterfaces(NetworkInterface.newBuilder()
                .setNetworkIP("10.0.0.1")
                .addAccessConfigs(AccessConfig.newBuilder().setNatIP("1.2.3.4").build())
                .build())
            .setTags(Tags.newBuilder().addItems("env=prod").build())
            .build();

        CloudInstanceInfo info = provider.toInfo(instance, "us-central1-a");
        assertEquals("12345", info.getInstanceId());
        assertEquals("web", info.getName());
        assertEquals("Running", info.getStatus());
        assertEquals("us-central1-a", info.getRegionId());
        assertEquals("us-central1-a", info.getZoneId(), "zone 应从完整 URL 提取最后一段");
        assertEquals("e2-medium", info.getInstanceType(), "machineType 应从 URL 提取规格");
        assertEquals("10.0.0.1", info.getPrivateIp());
        assertEquals("1.2.3.4", info.getPublicIp());
        assertNotNull(info.getTags());
        assertEquals("prod", info.getTags().get("env"));
    }

    @Test
    public void testToInfoNoNetwork() {
        Instance instance = Instance.newBuilder().setId(1L).setName("bare").build();
        CloudInstanceInfo info = provider.toInfo(instance, "us-central1-a");
        assertEquals("1", info.getInstanceId());
        assertNull(info.getPublicIp());
        assertNull(info.getPrivateIp());
    }

    @Test
    public void testExtractLast() {
        assertEquals("us-central1-a", provider.extractLast("projects/p/zones/us-central1-a"));
        assertEquals("e2-medium", provider.extractLast("projects/p/zones/us-central1-a/machineTypes/e2-medium"));
        assertNull(provider.extractLast(""));
        assertNull(provider.extractLast(null));
    }
}
