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

import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.ecs.v2.EcsClient;
import com.huaweicloud.sdk.ecs.v2.model.BatchRebootSeversOption;
import com.huaweicloud.sdk.ecs.v2.model.BatchRebootServersRequestBody;
import com.huaweicloud.sdk.ecs.v2.model.BatchRebootServersRequest;
import com.huaweicloud.sdk.ecs.v2.model.BatchStartServersOption;
import com.huaweicloud.sdk.ecs.v2.model.BatchStartServersRequestBody;
import com.huaweicloud.sdk.ecs.v2.model.BatchStartServersRequest;
import com.huaweicloud.sdk.ecs.v2.model.BatchStopServersOption;
import com.huaweicloud.sdk.ecs.v2.model.BatchStopServersRequestBody;
import com.huaweicloud.sdk.ecs.v2.model.BatchStopServersRequest;
import com.huaweicloud.sdk.ecs.v2.model.ListServersDetailsRequest;
import com.huaweicloud.sdk.ecs.v2.model.ListServersDetailsResponse;
import com.huaweicloud.sdk.ecs.v2.model.ServerAddress;
import com.huaweicloud.sdk.ecs.v2.model.ServerDetail;
import com.huaweicloud.sdk.ecs.v2.model.ServerId;
import com.huaweicloud.sdk.ecs.v2.region.EcsRegion;
import io.voyager1.cloud.CloudCredential;
import io.voyager1.cloud.CloudInstanceInfo;
import io.voyager1.cloud.ICloudProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 华为云 ECS 云厂商实现（M2）
 *
 * @since 2026/8/31
 */
@Component
@Slf4j
public class HuaweiCloudProvider implements ICloudProvider {

    @Override
    public String vendor() {
        return "huawei";
    }

    private EcsClient buildClient(CloudCredential credential, String region) {
        BasicCredentials auth = new BasicCredentials().withAk(credential.getAccessKey()).withSk(credential.getSecretKey());
        return EcsClient.newBuilder()
            .withCredential(auth)
            .withRegion(EcsRegion.valueOf(region))
            .build();
    }

    private String resolveRegion(CloudCredential credential, String region) {
        String r = (region == null || region.isEmpty()) ? credential.getRegion() : region;
        if (r == null || r.isEmpty()) {
            throw new IllegalArgumentException("华为云需要指定区域");
        }
        return r;
    }

    @Override
    public boolean testConnectivity(CloudCredential credential) throws Exception {
        String region = (credential.getRegion() == null || credential.getRegion().isEmpty()) ? "cn-north-4" : credential.getRegion();
        EcsClient client = this.buildClient(credential, region);
        ListServersDetailsRequest request = new ListServersDetailsRequest();
        request.setLimit(1);
        client.listServersDetails(request);
        return true;
    }

    @Override
    public List<CloudInstanceInfo> listInstances(CloudCredential credential, String region) throws Exception {
        String targetRegion = this.resolveRegion(credential, region);
        EcsClient client = this.buildClient(credential, targetRegion);
        ListServersDetailsRequest request = new ListServersDetailsRequest();
        request.setLimit(100);
        ListServersDetailsResponse response = client.listServersDetails(request);
        List<ServerDetail> servers = response.getServers();
        if (servers == null || servers.isEmpty()) {
            return Collections.emptyList();
        }
        List<CloudInstanceInfo> list = new ArrayList<>(servers.size());
        for (ServerDetail server : servers) {
            list.add(this.toInfo(server, targetRegion));
        }
        return list;
    }

    CloudInstanceInfo toInfo(ServerDetail server, String region) {
        CloudInstanceInfo info = new CloudInstanceInfo();
        info.setInstanceId(server.getId());
        info.setName(server.getName());
        info.setStatus(this.normalizeStatus(server.getStatus()));
        info.setRegionId(region);
        if (server.getFlavor() != null) {
            info.setInstanceType(server.getFlavor().getId());
            info.setCpu(this.parseInt(server.getFlavor().getVcpus()));
            info.setMemory(this.parseInt(server.getFlavor().getRam()));
        }
        if (server.getMetadata() != null) {
            info.setOsName(server.getMetadata().get("os_type"));
        }
        this.extractIps(server, info);
        if (server.getTags() != null && !server.getTags().isEmpty()) {
            Map<String, String> tags = new HashMap<>();
            for (String tag : server.getTags()) {
                int idx = tag.indexOf('=');
                if (idx > 0) {
                    tags.put(tag.substring(0, idx), tag.substring(idx + 1));
                }
            }
            info.setTags(tags);
        }
        return info;
    }

    Integer parseInt(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    void extractIps(ServerDetail server, CloudInstanceInfo info) {
        Map<String, List<ServerAddress>> addresses = server.getAddresses();
        if (addresses == null || addresses.isEmpty()) {
            return;
        }
        for (List<ServerAddress> list : addresses.values()) {
            for (ServerAddress addr : list) {
                if (addr.getAddr() == null || addr.getAddr().isEmpty()) {
                    continue;
                }
                String ip = addr.getAddr();
                if (isPrivate(ip)) {
                    if (info.getPrivateIp() == null) {
                        info.setPrivateIp(ip);
                    }
                } else if (info.getPublicIp() == null) {
                    info.setPublicIp(ip);
                }
            }
        }
    }

    boolean isPrivate(String ip) {
        return ip.startsWith("10.") || ip.startsWith("172.") || ip.startsWith("192.168.");
    }

    @Override
    public void startInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        EcsClient client = this.buildClient(credential, this.resolveRegion(credential, region));
        BatchStartServersOption option = new BatchStartServersOption()
            .withServers(Collections.singletonList(new ServerId().withId(instanceId)));
        BatchStartServersRequestBody body = new BatchStartServersRequestBody().withOsStart(option);
        client.batchStartServers(new BatchStartServersRequest().withBody(body));
    }

    @Override
    public void stopInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        EcsClient client = this.buildClient(credential, this.resolveRegion(credential, region));
        BatchStopServersOption option = new BatchStopServersOption()
            .withServers(Collections.singletonList(new ServerId().withId(instanceId)));
        BatchStopServersRequestBody body = new BatchStopServersRequestBody().withOsStop(option);
        client.batchStopServers(new BatchStopServersRequest().withBody(body));
    }

    @Override
    public void rebootInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        EcsClient client = this.buildClient(credential, this.resolveRegion(credential, region));
        BatchRebootSeversOption option = new BatchRebootSeversOption()
            .withServers(Collections.singletonList(new ServerId().withId(instanceId)));
        BatchRebootServersRequestBody body = new BatchRebootServersRequestBody().withReboot(option);
        client.batchRebootServers(new BatchRebootServersRequest().withBody(body));
    }
}
