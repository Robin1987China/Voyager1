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

import com.volcengine.ApiClient;
import com.volcengine.ecs.EcsApi;
import com.volcengine.ecs.model.DescribeInstancesRequest;
import com.volcengine.ecs.model.DescribeInstancesResponse;
import com.volcengine.ecs.model.InstanceForDescribeInstancesOutput;
import com.volcengine.ecs.model.RebootInstanceRequest;
import com.volcengine.ecs.model.StartInstanceRequest;
import com.volcengine.ecs.model.StopInstanceRequest;
import com.volcengine.ecs.model.TagForDescribeInstancesOutput;
import com.volcengine.sign.Credentials;
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
 * 火山引擎 ECS 云厂商实现（M2）
 *
 * @since 2026/8/31
 */
@Component
@Slf4j
public class VolcengineCloudProvider implements ICloudProvider {

    @Override
    public String vendor() {
        return "volcengine";
    }

    private EcsApi buildApi(CloudCredential credential, String region) {
        ApiClient client = new ApiClient()
            .setCredentials(Credentials.getCredentials(credential.getAccessKey(), credential.getSecretKey()))
            .setRegion(region);
        return new EcsApi(client);
    }

    private String resolveRegion(CloudCredential credential, String region) {
        String r = (region == null || region.isEmpty()) ? credential.getRegion() : region;
        if (r == null || r.isEmpty()) {
            throw new IllegalArgumentException("火山引擎需要指定区域");
        }
        return r;
    }

    @Override
    public boolean testConnectivity(CloudCredential credential) throws Exception {
        String region = (credential.getRegion() == null || credential.getRegion().isEmpty()) ? "cn-beijing" : credential.getRegion();
        EcsApi api = this.buildApi(credential, region);
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.setMaxResults(1);
        api.describeInstances(request);
        return true;
    }

    @Override
    public List<CloudInstanceInfo> listInstances(CloudCredential credential, String region) throws Exception {
        String targetRegion = this.resolveRegion(credential, region);
        EcsApi api = this.buildApi(credential, targetRegion);
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.setMaxResults(100);
        DescribeInstancesResponse response = api.describeInstances(request);
        List<InstanceForDescribeInstancesOutput> instances = response.getInstances();
        if (instances == null || instances.isEmpty()) {
            return Collections.emptyList();
        }
        List<CloudInstanceInfo> list = new ArrayList<>(instances.size());
        for (InstanceForDescribeInstancesOutput instance : instances) {
            list.add(this.toInfo(instance, targetRegion));
        }
        return list;
    }

    CloudInstanceInfo toInfo(InstanceForDescribeInstancesOutput instance, String region) {
        CloudInstanceInfo info = new CloudInstanceInfo();
        info.setInstanceId(instance.getInstanceId());
        info.setName(instance.getInstanceName());
        info.setStatus(this.normalizeStatus(instance.getStatus()));
        info.setRegionId(region);
        info.setZoneId(instance.getZoneId());
        info.setInstanceType(instance.getInstanceTypeId());
        info.setCpu(instance.getCpus());
        info.setMemory(instance.getMemorySize());
        info.setOsName(instance.getOsName());
        info.setExpireTime(instance.getExpiredAt());
        info.setChargeType(instance.getInstanceChargeType());
        if (instance.getEipAddress() != null) {
            info.setPublicIp(instance.getEipAddress().getIpAddress());
        }
        if (instance.getNetworkInterfaces() != null && !instance.getNetworkInterfaces().isEmpty()) {
            info.setPrivateIp(instance.getNetworkInterfaces().get(0).getPrimaryIpAddress());
        }
        if (instance.getTags() != null && !instance.getTags().isEmpty()) {
            Map<String, String> tags = new HashMap<>();
            for (TagForDescribeInstancesOutput tag : instance.getTags()) {
                tags.put(tag.getKey(), tag.getValue());
            }
            info.setTags(tags);
        }
        return info;
    }

    @Override
    public void startInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        EcsApi api = this.buildApi(credential, this.resolveRegion(credential, region));
        StartInstanceRequest request = new StartInstanceRequest();
        request.setInstanceId(instanceId);
        api.startInstance(request);
    }

    @Override
    public void stopInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        EcsApi api = this.buildApi(credential, this.resolveRegion(credential, region));
        StopInstanceRequest request = new StopInstanceRequest();
        request.setInstanceId(instanceId);
        api.stopInstance(request);
    }

    @Override
    public void rebootInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        EcsApi api = this.buildApi(credential, this.resolveRegion(credential, region));
        RebootInstanceRequest request = new RebootInstanceRequest();
        request.setInstanceId(instanceId);
        api.rebootInstance(request);
    }
}
