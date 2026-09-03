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

import com.huaweicloud.sdk.ecs.v2.model.ServerAddress;
import com.huaweicloud.sdk.ecs.v2.model.ServerDetail;
import com.huaweicloud.sdk.ecs.v2.model.ServerFlavor;
import io.voyager1.cloud.CloudInstanceInfo;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 华为云 provider 响应映射测试（用 SDK setter 构造假响应，无真实云调用）
 *
 * @since 2026/8/31
 */
public class HuaweiCloudProviderTest {

    private final HuaweiCloudProvider provider = new HuaweiCloudProvider();

    @Test
    public void testToInfoMapping() {
        ServerDetail server = new ServerDetail();
        server.setId("i-001");
        server.setName("web");
        server.setStatus("ACTIVE");

        ServerFlavor flavor = new ServerFlavor();
        flavor.setId("s6.large.2");
        flavor.setVcpus("2");
        flavor.setRam("4096");
        server.setFlavor(flavor);

        Map<String, List<ServerAddress>> addresses = new HashMap<>();
        ServerAddress publicAddr = new ServerAddress();
        publicAddr.setAddr("1.2.3.4");
        publicAddr.setVersion("4");
        addresses.put("public", Collections.singletonList(publicAddr));
        ServerAddress privateAddr = new ServerAddress();
        privateAddr.setAddr("10.0.0.1");
        addresses.put("private", Collections.singletonList(privateAddr));
        server.setAddresses(addresses);

        server.setTags(Collections.singletonList("env=prod"));

        CloudInstanceInfo info = provider.toInfo(server, "cn-north-4");
        assertEquals("i-001", info.getInstanceId());
        assertEquals("web", info.getName());
        assertEquals("Active", info.getStatus(), "华为云 ACTIVE 应规范化为 Active");
        assertEquals("cn-north-4", info.getRegionId());
        assertEquals("s6.large.2", info.getInstanceType());
        assertEquals(2, info.getCpu());
        assertEquals(4096, info.getMemory(), "华为云 ram 字符串应解析为整数 MB");
        assertEquals("1.2.3.4", info.getPublicIp());
        assertEquals("10.0.0.1", info.getPrivateIp());
        assertNotNull(info.getTags());
        assertEquals("prod", info.getTags().get("env"));
    }

    @Test
    public void testParseIntAndIsPrivate() {
        assertEquals(2, provider.parseInt("2"));
        assertEquals(null, provider.parseInt(""));
        assertEquals(null, provider.parseInt("abc"));
        assertTrue(provider.isPrivate("10.0.0.1"));
        assertTrue(provider.isPrivate("192.168.1.1"));
    }
}
