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

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.compute.v1.AccessConfig;
import com.google.cloud.compute.v1.Instance;
import com.google.cloud.compute.v1.InstancesClient;
import com.google.cloud.compute.v1.InstancesSettings;
import com.google.cloud.compute.v1.NetworkInterface;
import io.voyager1.cloud.CloudCredential;
import io.voyager1.cloud.CloudInstanceInfo;
import io.voyager1.cloud.ICloudProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GCP Compute Engine 云厂商实现（M2）。
 * <p>
 * 凭证映射：accessKey=服务账号 clientEmail，secretKey=私钥(PEM)，extraKey=projectId，region=zone。
 *
 * @since 2026/8/31
 */
@Component
@Slf4j
public class GcpCloudProvider implements ICloudProvider {

    private static final String COMPUTE_SCOPE = "https://www.googleapis.com/auth/compute";

    @Override
    public String vendor() {
        return "gcp";
    }

    private InstancesClient buildClient(CloudCredential credential) throws IOException {
        ServiceAccountCredentials credentials = ServiceAccountCredentials.fromPkcs8(
            null,
            credential.getAccessKey(),
            credential.getSecretKey(),
            null,
            Collections.singletonList(COMPUTE_SCOPE));
        InstancesSettings settings = InstancesSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
            .build();
        return InstancesClient.create(settings);
    }

    private String resolveProject(CloudCredential credential) {
        String project = credential.getExtraKey();
        if (project == null || project.isEmpty()) {
            throw new IllegalArgumentException("GCP 需要 projectId（请填写额外凭证）");
        }
        return project;
    }

    private String resolveZone(CloudCredential credential, String zone) {
        String z = (zone == null || zone.isEmpty()) ? credential.getRegion() : zone;
        if (z == null || z.isEmpty()) {
            throw new IllegalArgumentException("GCP 需要指定 zone");
        }
        return z;
    }

    @Override
    public boolean testConnectivity(CloudCredential credential) throws Exception {
        String project = this.resolveProject(credential);
        String zone = this.resolveZone(credential, null);
        try (InstancesClient client = this.buildClient(credential)) {
            client.list(project, zone).getPage().getPageElementCount();
            return true;
        }
    }

    @Override
    public List<CloudInstanceInfo> listInstances(CloudCredential credential, String region) throws Exception {
        String project = this.resolveProject(credential);
        String zone = this.resolveZone(credential, region);
        List<CloudInstanceInfo> list = new ArrayList<>();
        try (InstancesClient client = this.buildClient(credential)) {
            for (Instance instance : client.list(project, zone).iterateAll()) {
                list.add(this.toInfo(instance, zone));
            }
        }
        return list;
    }

    CloudInstanceInfo toInfo(Instance instance, String zone) {
        CloudInstanceInfo info = new CloudInstanceInfo();
        info.setInstanceId(String.valueOf(instance.getId()));
        info.setName(instance.getName());
        info.setStatus(this.normalizeStatus(instance.getStatus()));
        info.setRegionId(zone);
        info.setZoneId(this.extractLast(instance.getZone()));
        info.setInstanceType(this.extractLast(instance.getMachineType()));
        if (instance.getNetworkInterfacesCount() > 0) {
            NetworkInterface nic = instance.getNetworkInterfaces(0);
            info.setPrivateIp(nic.getNetworkIP());
            if (nic.getAccessConfigsCount() > 0) {
                AccessConfig accessConfig = nic.getAccessConfigs(0);
                info.setPublicIp(accessConfig.getNatIP());
            }
        }
        if (instance.hasTags() && instance.getTags().getItemsList() != null) {
            Map<String, String> tags = new HashMap<>();
            for (String item : instance.getTags().getItemsList()) {
                int idx = item.indexOf('=');
                if (idx > 0) {
                    tags.put(item.substring(0, idx), item.substring(idx + 1));
                }
            }
            info.setTags(tags);
        }
        return info;
    }

    String extractLast(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        int idx = url.lastIndexOf('/');
        return idx >= 0 ? url.substring(idx + 1) : url;
    }

    @Override
    public void startInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        try (InstancesClient client = this.buildClient(credential)) {
            client.startAsync(this.resolveProject(credential), this.resolveZone(credential, region), instanceId).get();
        }
    }

    @Override
    public void stopInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        try (InstancesClient client = this.buildClient(credential)) {
            client.stopAsync(this.resolveProject(credential), this.resolveZone(credential, region), instanceId).get();
        }
    }

    @Override
    public void rebootInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        try (InstancesClient client = this.buildClient(credential)) {
            client.resetAsync(this.resolveProject(credential), this.resolveZone(credential, region), instanceId).get();
        }
    }
}
