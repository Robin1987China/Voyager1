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

import io.voyager1.cloud.CloudCredential;
import io.voyager1.cloud.CloudInstanceInfo;
import io.voyager1.cloud.ICloudProvider;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CloudProviderRegistry 厂商路由测试
 *
 * @since 2026/8/31
 */
public class CloudProviderRegistryTest {

    private final ICloudProvider aliyun = provider("aliyun");
    private final ICloudProvider aws = provider("aws");
    private final CloudProviderRegistry registry = new CloudProviderRegistry(Arrays.asList(aliyun, aws));

    private ICloudProvider provider(String vendor) {
        return new ICloudProvider() {
            @Override
            public String vendor() {
                return vendor;
            }

            @Override
            public boolean testConnectivity(CloudCredential credential) {
                return true;
            }

            @Override
            public List<CloudInstanceInfo> listInstances(CloudCredential credential, String region) {
                return Collections.emptyList();
            }

            @Override
            public void startInstance(CloudCredential credential, String region, String instanceId) {
            }

            @Override
            public void stopInstance(CloudCredential credential, String region, String instanceId) {
            }

            @Override
            public void rebootInstance(CloudCredential credential, String region, String instanceId) {
            }
        };
    }

    @Test
    public void testGetByVendor() {
        assertSame(aliyun, registry.get("aliyun"));
        assertSame(aws, registry.get("aws"));
    }

    @Test
    public void testUnknownVendorThrows() {
        assertThrows(IllegalArgumentException.class, () -> registry.get("unknown"));
    }

    @Test
    public void testContains() {
        assertTrue(registry.contains("aliyun"));
        assertTrue(registry.contains("aws"));
        assertFalse(registry.contains("unknown"));
    }
}
