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

import com.azure.resourcemanager.compute.models.PowerState;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.compute.models.VirtualMachineSizeTypes;
import io.voyager1.cloud.CloudInstanceInfo;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Azure provider 响应映射测试（Mockito mock VirtualMachine 接口，无真实云调用）
 *
 * @since 2026/8/31
 */
public class AzureCloudProviderTest {

    private final AzureCloudProvider provider = new AzureCloudProvider();

    @Test
    public void testToInfoMapping() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.id()).thenReturn("/subscriptions/sub/resourceGroups/rg/providers/Microsoft.Compute/virtualMachines/web");
        Mockito.when(vm.name()).thenReturn("web");
        Mockito.when(vm.powerState()).thenReturn(PowerState.RUNNING);
        Mockito.when(vm.regionName()).thenReturn("eastus");
        Mockito.when(vm.size()).thenReturn(VirtualMachineSizeTypes.STANDARD_D2S_V3);
        Map<String, String> tags = new HashMap<>();
        tags.put("env", "prod");
        Mockito.when(vm.tags()).thenReturn(tags);

        CloudInstanceInfo info = provider.toInfo(vm);
        assertEquals("/subscriptions/sub/resourceGroups/rg/providers/Microsoft.Compute/virtualMachines/web", info.getInstanceId());
        assertEquals("web", info.getName());
        assertEquals("Running", info.getStatus(), "Azure RUNNING 应规范化为 Running");
        assertEquals("eastus", info.getRegionId());
        assertEquals("Standard_D2s_v3", info.getInstanceType());
        assertNotNull(info.getTags());
        assertEquals("prod", info.getTags().get("env"));
    }

    @Test
    public void testToInfoNullStateAndTags() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.id()).thenReturn("/subscriptions/sub/resourceGroups/rg/providers/Microsoft.Compute/virtualMachines/bare");
        Mockito.when(vm.name()).thenReturn("bare");
        Mockito.when(vm.powerState()).thenReturn(null);
        Mockito.when(vm.tags()).thenReturn(null);

        CloudInstanceInfo info = provider.toInfo(vm);
        assertNull(info.getStatus());
        assertNull(info.getTags());
        assertNull(info.getInstanceType());
    }
}
