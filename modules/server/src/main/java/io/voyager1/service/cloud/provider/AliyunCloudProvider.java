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

import com.aliyun.ecs20140526.Client;
import com.aliyun.ecs20140526.models.DescribeInstancesRequest;
import com.aliyun.ecs20140526.models.DescribeInstancesResponse;
import com.aliyun.ecs20140526.models.DescribeInstancesResponseBody;
import com.aliyun.ecs20140526.models.DescribeInstancesResponseBody.DescribeInstancesResponseBodyInstancesInstance;
import com.aliyun.ecs20140526.models.DescribeInstancesResponseBody.DescribeInstancesResponseBodyInstancesInstanceTagsTag;
import com.aliyun.ecs20140526.models.DescribeRegionsRequest;
import com.aliyun.ecs20140526.models.CreateImageRequest;
import com.aliyun.ecs20140526.models.CreateImageResponse;
import com.aliyun.ecs20140526.models.CreateSnapshotRequest;
import com.aliyun.ecs20140526.models.CreateSnapshotResponse;
import com.aliyun.ecs20140526.models.DeleteSnapshotRequest;
import com.aliyun.ecs20140526.models.DescribeSnapshotsRequest;
import com.aliyun.ecs20140526.models.DescribeSnapshotsResponse;
import com.aliyun.ecs20140526.models.DescribeSnapshotsResponseBody;
import com.aliyun.ecs20140526.models.DescribeSecurityGroupsRequest;
import com.aliyun.ecs20140526.models.DescribeSecurityGroupsResponse;
import com.aliyun.ecs20140526.models.DescribeSecurityGroupsResponseBody;
import com.aliyun.ecs20140526.models.ModifyInstanceSpecRequest;
import com.aliyun.ecs20140526.models.RebootInstanceRequest;
import com.aliyun.ecs20140526.models.StartInstanceRequest;
import com.aliyun.ecs20140526.models.StopInstanceRequest;
import com.aliyun.teaopenapi.models.Config;
import io.voyager1.cloud.CloudBill;
import io.voyager1.cloud.CloudCredential;
import io.voyager1.cloud.CloudInstanceInfo;
import io.voyager1.cloud.CloudScalingGroup;
import io.voyager1.cloud.CloudSecurityGroup;
import io.voyager1.cloud.CloudSnapshot;
import io.voyager1.cloud.ICloudProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 阿里云 ECS 云厂商实现（M1 样板）
 *
 * @since 2026/8/12
 */
@Component
@Slf4j
public class AliyunCloudProvider implements ICloudProvider {

    @Override
    public String vendor() {
        return "aliyun";
    }

    /**
     * 构造阿里云 ECS 客户端
     */
    private Client buildClient(CloudCredential credential, String region) throws Exception {
        Config config = new Config()
            .setAccessKeyId(credential.getAccessKey())
            .setAccessKeySecret(credential.getSecretKey());
        config.setEndpoint(this.endpoint(region));
        if ((region != null && !region.isEmpty())) {
            config.setRegionId(region);
        }
        return new Client(config);
    }

    private String endpoint(String region) {
        return (region == null || region.isEmpty()) ? "ecs.aliyuncs.com" : "ecs." + region + ".aliyuncs.com";
    }

    @Override
    public boolean testConnectivity(CloudCredential credential) throws Exception {
        Client client = this.buildClient(credential, credential.getRegion());
        // 用 describeRegions 探测凭证是否有效（不依赖具体区域）
        DescribeRegionsRequest request = new DescribeRegionsRequest();
        client.describeRegions(request);
        return true;
    }

    @Override
    public List<CloudInstanceInfo> listInstances(CloudCredential credential, String region) throws Exception {
        String targetRegion = (region == null || region.isEmpty()) ? credential.getRegion() : region;
        if ((targetRegion == null || targetRegion.isEmpty())) {
            throw new IllegalArgumentException("阿里云拉取实例需要指定区域");
        }
        Client client = this.buildClient(credential, targetRegion);
        DescribeInstancesRequest request = new DescribeInstancesRequest()
            .setRegionId(targetRegion)
            .setPageSize(100);
        DescribeInstancesResponse response = client.describeInstances(request);
        DescribeInstancesResponseBody body = response.getBody();
        DescribeInstancesResponseBody.DescribeInstancesResponseBodyInstances instances = (body == null ? null : body.getInstances());
        if (instances == null || instances.getInstance() == null) {
            return Collections.emptyList();
        }
        return instances.getInstance().stream().map(this::toInfo).collect(Collectors.toList());
    }

    CloudInstanceInfo toInfo(DescribeInstancesResponseBodyInstancesInstance instance) {
        CloudInstanceInfo info = new CloudInstanceInfo();
        info.setInstanceId(instance.getInstanceId());
        info.setName(instance.getInstanceName());
        info.setStatus(this.normalizeStatus(instance.getStatus()));
        info.setRegionId(instance.getRegionId());
        info.setZoneId(instance.getZoneId());
        info.setInstanceType(instance.getInstanceType());
        info.setCpu(instance.getCpu());
        info.setMemory(instance.getMemory());
        info.setOsName(instance.getOSName());
        info.setExpireTime(instance.getExpiredTime());
        info.setChargeType(instance.getInstanceChargeType());
        // 公网/内网 IP（取第一个）
        if (instance.getPublicIpAddress() != null && instance.getPublicIpAddress().getIpAddress() != null
            && !instance.getPublicIpAddress().getIpAddress().isEmpty()) {
            info.setPublicIp(instance.getPublicIpAddress().getIpAddress().get(0));
        }
        if (instance.getInnerIpAddress() != null && instance.getInnerIpAddress().getIpAddress() != null
            && !instance.getInnerIpAddress().getIpAddress().isEmpty()) {
            info.setPrivateIp(instance.getInnerIpAddress().getIpAddress().get(0));
        }
        // 标签
        if (instance.getTags() != null && instance.getTags().getTag() != null) {
            Map<String, String> tags = new HashMap<>();
            for (DescribeInstancesResponseBodyInstancesInstanceTagsTag tag : instance.getTags().getTag()) {
                tags.put(tag.getTagKey(), tag.getTagValue());
            }
            info.setTags(tags);
        }
        return info;
    }

    @Override
    public void startInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        Client client = this.buildClient(credential, region);
        StartInstanceRequest request = new StartInstanceRequest().setInstanceId(instanceId);
        client.startInstance(request);
    }

    @Override
    public void stopInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        Client client = this.buildClient(credential, region);
        StopInstanceRequest request = new StopInstanceRequest().setInstanceId(instanceId);
        client.stopInstance(request);
    }

    @Override
    public void rebootInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        Client client = this.buildClient(credential, region);
        RebootInstanceRequest request = new RebootInstanceRequest().setInstanceId(instanceId);
        client.rebootInstance(request);
    }

    @Override
    public void resizeInstance(CloudCredential credential, String region, String instanceId, String newInstanceType) throws Exception {
        Client client = this.buildClient(credential, region);
        ModifyInstanceSpecRequest request = new ModifyInstanceSpecRequest()
            .setInstanceId(instanceId)
            .setInstanceType(newInstanceType);
        client.modifyInstanceSpec(request);
    }

    @Override
    public String createSnapshot(CloudCredential credential, String region, String diskId, String snapshotName) throws Exception {
        Client client = this.buildClient(credential, region);
        CreateSnapshotRequest request = new CreateSnapshotRequest()
            .setDiskId(diskId)
            .setSnapshotName(snapshotName);
        CreateSnapshotResponse response = client.createSnapshot(request);
        return (response.getBody() != null ? response.getBody().getSnapshotId() : null);
    }

    @Override
    public List<CloudSnapshot> listSnapshots(CloudCredential credential, String region) throws Exception {
        Client client = this.buildClient(credential, region);
        DescribeSnapshotsRequest request = new DescribeSnapshotsRequest();
        DescribeSnapshotsResponse response = client.describeSnapshots(request);
        DescribeSnapshotsResponseBody body = response.getBody();
        if (body == null || body.getSnapshots() == null || body.getSnapshots().getSnapshot() == null) {
            return Collections.emptyList();
        }
        List<CloudSnapshot> list = new ArrayList<>();
        for (DescribeSnapshotsResponseBody.DescribeSnapshotsResponseBodySnapshotsSnapshot snapshot : body.getSnapshots().getSnapshot()) {
            CloudSnapshot info = new CloudSnapshot();
            info.setSnapshotId(snapshot.getSnapshotId());
            info.setName(snapshot.getSnapshotName());
            info.setStatus(this.normalizeStatus(snapshot.getStatus()));
            info.setRegionId(region);
            info.setCreateTime(snapshot.getCreationTime());
            info.setDiskId(snapshot.getSourceDiskId());
            list.add(info);
        }
        return list;
    }

    @Override
    public void deleteSnapshot(CloudCredential credential, String region, String snapshotId) throws Exception {
        Client client = this.buildClient(credential, region);
        DeleteSnapshotRequest request = new DeleteSnapshotRequest().setSnapshotId(snapshotId);
        client.deleteSnapshot(request);
    }

    @Override
    public List<CloudSecurityGroup> listSecurityGroups(CloudCredential credential, String region) throws Exception {
        Client client = this.buildClient(credential, region);
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
        DescribeSecurityGroupsResponse response = client.describeSecurityGroups(request);
        DescribeSecurityGroupsResponseBody body = response.getBody();
        if (body == null || body.getSecurityGroups() == null || body.getSecurityGroups().getSecurityGroup() == null) {
            return Collections.emptyList();
        }
        List<CloudSecurityGroup> list = new ArrayList<>();
        for (DescribeSecurityGroupsResponseBody.DescribeSecurityGroupsResponseBodySecurityGroupsSecurityGroup sg : body.getSecurityGroups().getSecurityGroup()) {
            CloudSecurityGroup info = new CloudSecurityGroup();
            info.setSecurityGroupId(sg.getSecurityGroupId());
            info.setName(sg.getSecurityGroupName());
            info.setDescription(sg.getDescription());
            info.setRegionId(region);
            list.add(info);
        }
        return list;
    }

    @Override
    public String createImage(CloudCredential credential, String region, String instanceId, String imageName) throws Exception {
        Client client = this.buildClient(credential, region);
        CreateImageRequest request = new CreateImageRequest()
            .setInstanceId(instanceId)
            .setImageName(imageName);
        CreateImageResponse response = client.createImage(request);
        return (response.getBody() != null ? response.getBody().getImageId() : null);
    }

    @Override
    public List<CloudScalingGroup> listScalingGroups(CloudCredential credential, String region) throws Exception {
        String targetRegion = (region == null || region.isEmpty()) ? credential.getRegion() : region;
        if (targetRegion == null || targetRegion.isEmpty()) {
            throw new IllegalArgumentException("阿里云需要指定区域");
        }
        com.aliyun.teaopenapi.models.Config essConfig = new com.aliyun.teaopenapi.models.Config()
            .setAccessKeyId(credential.getAccessKey())
            .setAccessKeySecret(credential.getSecretKey());
        essConfig.setEndpoint("ess." + targetRegion + ".aliyuncs.com");
        com.aliyun.ess20220222.Client essClient = new com.aliyun.ess20220222.Client(essConfig);
        com.aliyun.ess20220222.models.DescribeScalingGroupsRequest request =
            new com.aliyun.ess20220222.models.DescribeScalingGroupsRequest()
                .setRegionId(targetRegion)
                .setPageSize(100);
        com.aliyun.ess20220222.models.DescribeScalingGroupsResponse response = essClient.describeScalingGroups(request);
        List<CloudScalingGroup> list = new ArrayList<>();
        if (response.getBody() != null && response.getBody().getScalingGroups() != null) {
            for (com.aliyun.ess20220222.models.DescribeScalingGroupsResponseBody.DescribeScalingGroupsResponseBodyScalingGroups sg : response.getBody().getScalingGroups()) {
                CloudScalingGroup info = new CloudScalingGroup();
                info.setScalingGroupId(sg.getScalingGroupId());
                info.setName(sg.getScalingGroupName());
                info.setStatus(this.normalizeStatus(sg.getLifecycleState()));
                info.setRegionId(targetRegion);
                info.setMinSize(sg.getMinSize());
                info.setMaxSize(sg.getMaxSize());
                info.setCurrentSize(sg.getTotalCapacity());
                list.add(info);
            }
        }
        return list;
    }

    @Override
    public List<CloudBill> listBills(CloudCredential credential, String region, String billingCycle) throws Exception {
        com.aliyun.teaopenapi.models.Config bssConfig = new com.aliyun.teaopenapi.models.Config()
            .setAccessKeyId(credential.getAccessKey())
            .setAccessKeySecret(credential.getSecretKey());
        bssConfig.setEndpoint("business.aliyuncs.com");
        com.aliyun.bssopenapi20171214.Client bssClient = new com.aliyun.bssopenapi20171214.Client(bssConfig);
        com.aliyun.bssopenapi20171214.models.QueryInstanceBillRequest request =
            new com.aliyun.bssopenapi20171214.models.QueryInstanceBillRequest()
                .setBillingCycle(billingCycle)
                .setPageNum(1)
                .setPageSize(100);
        com.aliyun.bssopenapi20171214.models.QueryInstanceBillResponse response = bssClient.queryInstanceBill(request);
        List<CloudBill> list = new ArrayList<>();
        if (response.getBody() != null && response.getBody().getData() != null
            && response.getBody().getData().getItems() != null && response.getBody().getData().getItems().getItem() != null) {
            for (com.aliyun.bssopenapi20171214.models.QueryInstanceBillResponseBody.QueryInstanceBillResponseBodyDataItemsItem item : response.getBody().getData().getItems().getItem()) {
                list.add(this.billFromItem(item));
            }
        }
        return list;
    }

    CloudBill billFromItem(com.aliyun.bssopenapi20171214.models.QueryInstanceBillResponseBody.QueryInstanceBillResponseBodyDataItemsItem item) {
        CloudBill bill = new CloudBill();
        bill.setBillDate(item.getBillingDate());
        bill.setServiceName(item.getProductName());
        bill.setResourceId(item.getInstanceID());
        bill.setRegion(item.getRegion());
        bill.setAmount(item.getPretaxGrossAmount() != null ? item.getPretaxGrossAmount().doubleValue() : null);
        bill.setCurrency(item.getCurrency());
        return bill;
    }
}
