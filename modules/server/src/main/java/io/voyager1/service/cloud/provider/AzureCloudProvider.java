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

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import io.voyager1.cloud.CloudCredential;
import io.voyager1.cloud.CloudInstanceInfo;
import io.voyager1.cloud.ICloudProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Azure 虚拟机云厂商实现（M2）。
 * <p>
 * 凭证映射：accessKey=clientId，secretKey=clientSecret，extraKey=tenantId。
 *
 * @since 2026/8/31
 */
@Component
@Slf4j
public class AzureCloudProvider implements ICloudProvider {

    @Override
    public String vendor() {
        return "azure";
    }

    private AzureResourceManager buildClient(CloudCredential credential) {
        TokenCredential tokenCredential = new ClientSecretCredentialBuilder()
            .tenantId(credential.getExtraKey())
            .clientId(credential.getAccessKey())
            .clientSecret(credential.getSecretKey())
            .build();
        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
        return AzureResourceManager.authenticate(tokenCredential, profile).withDefaultSubscription();
    }

    @Override
    public boolean testConnectivity(CloudCredential credential) throws Exception {
        AzureResourceManager azure = this.buildClient(credential);
        azure.virtualMachines().list().stream().findFirst();
        return true;
    }

    @Override
    public List<CloudInstanceInfo> listInstances(CloudCredential credential, String region) throws Exception {
        AzureResourceManager azure = this.buildClient(credential);
        List<CloudInstanceInfo> list = new ArrayList<>();
        for (VirtualMachine vm : azure.virtualMachines().list()) {
            list.add(this.toInfo(vm));
        }
        return list;
    }

    CloudInstanceInfo toInfo(VirtualMachine vm) {
        CloudInstanceInfo info = new CloudInstanceInfo();
        info.setInstanceId(vm.id());
        info.setName(vm.name());
        info.setStatus(this.normalizeStatus(this.enumValue(vm.powerState() != null ? vm.powerState().toString() : null)));
        info.setRegionId(vm.regionName());
        info.setInstanceType(this.enumValue(vm.size() != null ? vm.size().toString() : null));
        if (vm.tags() != null && !vm.tags().isEmpty()) {
            info.setTags(new HashMap<>(vm.tags()));
        }
        return info;
    }

    /**
     * Azure ExpandableStringEnum 的 toString 形如 "PowerState/running"，取斜杠后的实际值
     */
    String enumValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int idx = value.indexOf('/');
        return idx >= 0 ? value.substring(idx + 1) : value;
    }

    /**
     * 从 Azure 资源 ID 解析 resourceGroup 与 VM 名称
     */
    private String[] parseResourceGroupAndName(String id) {
        String rg = null;
        String name = null;
        String[] parts = id.split("/");
        for (int i = 0; i < parts.length; i++) {
            if ("resourceGroups".equals(parts[i]) && i + 1 < parts.length) {
                rg = parts[i + 1];
            }
            if ("virtualMachines".equals(parts[i]) && i + 1 < parts.length) {
                name = parts[i + 1];
            }
        }
        return new String[]{rg, name};
    }

    @Override
    public void startInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        AzureResourceManager azure = this.buildClient(credential);
        String[] rgName = this.parseResourceGroupAndName(instanceId);
        azure.virtualMachines().start(rgName[0], rgName[1]);
    }

    @Override
    public void stopInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        AzureResourceManager azure = this.buildClient(credential);
        String[] rgName = this.parseResourceGroupAndName(instanceId);
        azure.virtualMachines().powerOff(rgName[0], rgName[1]);
    }

    @Override
    public void rebootInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        AzureResourceManager azure = this.buildClient(credential);
        String[] rgName = this.parseResourceGroupAndName(instanceId);
        azure.virtualMachines().restart(rgName[0], rgName[1]);
    }
}
