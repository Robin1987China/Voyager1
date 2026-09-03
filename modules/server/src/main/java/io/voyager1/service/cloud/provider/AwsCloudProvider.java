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

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.costexplorer.model.DateInterval;
import software.amazon.awssdk.services.costexplorer.model.GetCostAndUsageRequest;
import software.amazon.awssdk.services.costexplorer.model.GetCostAndUsageResponse;
import software.amazon.awssdk.services.costexplorer.model.Granularity;
import software.amazon.awssdk.services.costexplorer.model.Group;
import software.amazon.awssdk.services.costexplorer.model.GroupDefinition;
import software.amazon.awssdk.services.costexplorer.model.GroupDefinitionType;
import software.amazon.awssdk.services.costexplorer.model.MetricValue;
import software.amazon.awssdk.services.costexplorer.model.ResultByTime;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.AttributeValue;
import software.amazon.awssdk.services.ec2.model.CreateImageRequest;
import software.amazon.awssdk.services.ec2.model.CreateImageResponse;
import software.amazon.awssdk.services.ec2.model.CreateSnapshotRequest;
import software.amazon.awssdk.services.ec2.model.CreateSnapshotResponse;
import software.amazon.awssdk.services.ec2.model.DeleteSnapshotRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSnapshotsResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.RebootInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.ec2.model.Snapshot;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Tag;
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

/**
 * AWS EC2 云厂商实现（M2）
 *
 * @since 2026/8/31
 */
@Component
@Slf4j
public class AwsCloudProvider implements ICloudProvider {

    @Override
    public String vendor() {
        return "aws";
    }

    private Ec2Client buildClient(CloudCredential credential, String region) {
        AwsBasicCredentials awsCred = AwsBasicCredentials.create(credential.getAccessKey(), credential.getSecretKey());
        return Ec2Client.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(awsCred))
            .build();
    }

    private String resolveRegion(CloudCredential credential, String region) {
        String r = (region == null || region.isEmpty()) ? credential.getRegion() : region;
        if (r == null || r.isEmpty()) {
            throw new IllegalArgumentException("AWS 需要指定区域");
        }
        return r;
    }

    @Override
    public boolean testConnectivity(CloudCredential credential) throws Exception {
        String region = (credential.getRegion() == null || credential.getRegion().isEmpty()) ? "us-east-1" : credential.getRegion();
        Ec2Client client = this.buildClient(credential, region);
        client.describeInstances(DescribeInstancesRequest.builder().maxResults(5).build());
        return true;
    }

    @Override
    public List<CloudInstanceInfo> listInstances(CloudCredential credential, String region) throws Exception {
        String targetRegion = this.resolveRegion(credential, region);
        Ec2Client client = this.buildClient(credential, targetRegion);
        DescribeInstancesResponse response = client.describeInstances(DescribeInstancesRequest.builder().build());
        if (response == null || response.reservations() == null) {
            return Collections.emptyList();
        }
        List<CloudInstanceInfo> list = new ArrayList<>();
        for (Reservation reservation : response.reservations()) {
            if (reservation.instances() == null) {
                continue;
            }
            for (Instance instance : reservation.instances()) {
                list.add(this.toInfo(instance, targetRegion));
            }
        }
        return list;
    }

    CloudInstanceInfo toInfo(Instance instance, String region) {
        CloudInstanceInfo info = new CloudInstanceInfo();
        info.setInstanceId(instance.instanceId());
        info.setName(this.nameFromTags(instance));
        info.setStatus(this.normalizeStatus(instance.state() != null && instance.state().name() != null ? instance.state().name().toString() : null));
        info.setRegionId(region);
        if (instance.placement() != null) {
            info.setZoneId(instance.placement().availabilityZone());
        }
        info.setInstanceType(instance.instanceType() != null ? instance.instanceType().toString() : null);
        if (instance.cpuOptions() != null && instance.cpuOptions().coreCount() != null) {
            info.setCpu(instance.cpuOptions().coreCount());
        }
        info.setPublicIp(instance.publicIpAddress());
        info.setPrivateIp(instance.privateIpAddress());
        info.setChargeType("PostPaid");
        if (instance.tags() != null && !instance.tags().isEmpty()) {
            Map<String, String> tags = new HashMap<>();
            for (Tag tag : instance.tags()) {
                tags.put(tag.key(), tag.value());
            }
            info.setTags(tags);
        }
        return info;
    }

    /**
     * AWS 实例名称约定存于 Name 标签
     */
    String nameFromTags(Instance instance) {
        if (instance.tags() != null) {
            for (Tag tag : instance.tags()) {
                if ("Name".equals(tag.key())) {
                    return tag.value();
                }
            }
        }
        return null;
    }

    @Override
    public void startInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        Ec2Client client = this.buildClient(credential, this.resolveRegion(credential, region));
        client.startInstances(StartInstancesRequest.builder().instanceIds(instanceId).build());
    }

    @Override
    public void stopInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        Ec2Client client = this.buildClient(credential, this.resolveRegion(credential, region));
        client.stopInstances(StopInstancesRequest.builder().instanceIds(instanceId).build());
    }

    @Override
    public void rebootInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        Ec2Client client = this.buildClient(credential, this.resolveRegion(credential, region));
        client.rebootInstances(RebootInstancesRequest.builder().instanceIds(instanceId).build());
    }

    @Override
    public void resizeInstance(CloudCredential credential, String region, String instanceId, String newInstanceType) throws Exception {
        Ec2Client client = this.buildClient(credential, this.resolveRegion(credential, region));
        client.modifyInstanceAttribute(ModifyInstanceAttributeRequest.builder()
            .instanceId(instanceId)
            .instanceType(AttributeValue.builder().value(newInstanceType).build())
            .build());
    }

    @Override
    public String createSnapshot(CloudCredential credential, String region, String diskId, String snapshotName) throws Exception {
        Ec2Client client = this.buildClient(credential, this.resolveRegion(credential, region));
        CreateSnapshotResponse response = client.createSnapshot(CreateSnapshotRequest.builder()
            .volumeId(diskId)
            .description(snapshotName)
            .build());
        return response.snapshotId();
    }

    @Override
    public List<CloudSnapshot> listSnapshots(CloudCredential credential, String region) throws Exception {
        Ec2Client client = this.buildClient(credential, this.resolveRegion(credential, region));
        DescribeSnapshotsResponse response = client.describeSnapshots();
        List<CloudSnapshot> list = new ArrayList<>();
        if (response.snapshots() != null) {
            for (Snapshot snapshot : response.snapshots()) {
                CloudSnapshot info = new CloudSnapshot();
                info.setSnapshotId(snapshot.snapshotId());
                info.setName(snapshot.description());
                info.setStatus(this.normalizeStatus(snapshot.state() != null ? snapshot.state().toString() : null));
                info.setRegionId(region);
                info.setCreateTime(snapshot.startTime() != null ? snapshot.startTime().toString() : null);
                info.setDiskId(snapshot.volumeId());
                list.add(info);
            }
        }
        return list;
    }

    @Override
    public void deleteSnapshot(CloudCredential credential, String region, String snapshotId) throws Exception {
        Ec2Client client = this.buildClient(credential, this.resolveRegion(credential, region));
        client.deleteSnapshot(DeleteSnapshotRequest.builder().snapshotId(snapshotId).build());
    }

    @Override
    public List<CloudSecurityGroup> listSecurityGroups(CloudCredential credential, String region) throws Exception {
        Ec2Client client = this.buildClient(credential, this.resolveRegion(credential, region));
        DescribeSecurityGroupsResponse response = client.describeSecurityGroups();
        List<CloudSecurityGroup> list = new ArrayList<>();
        if (response.securityGroups() != null) {
            for (SecurityGroup sg : response.securityGroups()) {
                CloudSecurityGroup info = new CloudSecurityGroup();
                info.setSecurityGroupId(sg.groupId());
                info.setName(sg.groupName());
                info.setDescription(sg.description());
                info.setRegionId(region);
                list.add(info);
            }
        }
        return list;
    }

    @Override
    public String createImage(CloudCredential credential, String region, String instanceId, String imageName) throws Exception {
        Ec2Client client = this.buildClient(credential, this.resolveRegion(credential, region));
        CreateImageResponse response = client.createImage(CreateImageRequest.builder()
            .instanceId(instanceId)
            .name(imageName)
            .build());
        return response.imageId();
    }

    @Override
    public List<CloudScalingGroup> listScalingGroups(CloudCredential credential, String region) throws Exception {
        String targetRegion = this.resolveRegion(credential, region);
        AutoScalingClient client = AutoScalingClient.builder()
            .region(Region.of(targetRegion))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(credential.getAccessKey(), credential.getSecretKey())))
            .build();
        DescribeAutoScalingGroupsResponse response = client.describeAutoScalingGroups();
        List<CloudScalingGroup> list = new ArrayList<>();
        if (response.autoScalingGroups() != null) {
            for (AutoScalingGroup asg : response.autoScalingGroups()) {
                CloudScalingGroup info = new CloudScalingGroup();
                info.setScalingGroupId(asg.autoScalingGroupName());
                info.setName(asg.autoScalingGroupName());
                info.setStatus(this.normalizeStatus(asg.status()));
                info.setRegionId(targetRegion);
                info.setMinSize(asg.minSize());
                info.setMaxSize(asg.maxSize());
                info.setCurrentSize(asg.desiredCapacity());
                list.add(info);
            }
        }
        return list;
    }

    @Override
    public List<CloudBill> listBills(CloudCredential credential, String region, String billingCycle) throws Exception {
        String start = billingCycle + "-01";
        String end = billingCycle + "-31";
        CostExplorerClient client = CostExplorerClient.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(credential.getAccessKey(), credential.getSecretKey())))
            .build();
        GetCostAndUsageRequest request = GetCostAndUsageRequest.builder()
            .timePeriod(DateInterval.builder().start(start).end(end).build())
            .granularity(Granularity.DAILY)
            .metrics(Collections.singletonList("UnblendedCost"))
            .groupBy(GroupDefinition.builder().type(GroupDefinitionType.DIMENSION).key("SERVICE").build())
            .build();
        GetCostAndUsageResponse response = client.getCostAndUsage(request);
        List<CloudBill> list = new ArrayList<>();
        for (ResultByTime rbt : response.resultsByTime()) {
            String date = rbt.timePeriod() != null ? rbt.timePeriod().start() : null;
            for (Group group : rbt.groups()) {
                list.add(this.billFromGroup(group, region, date));
            }
        }
        return list;
    }

    CloudBill billFromGroup(Group group, String region, String date) {
        CloudBill bill = new CloudBill();
        bill.setBillDate(date);
        bill.setServiceName(group.keys() != null && !group.keys().isEmpty() ? group.keys().get(0) : null);
        bill.setResourceId(null);
        bill.setRegion(region);
        MetricValue mv = group.metrics() != null ? group.metrics().get("UnblendedCost") : null;
        bill.setAmount(mv != null && mv.amount() != null ? Double.parseDouble(mv.amount()) : null);
        bill.setCurrency(mv != null ? mv.unit() : "USD");
        return bill;
    }
}
