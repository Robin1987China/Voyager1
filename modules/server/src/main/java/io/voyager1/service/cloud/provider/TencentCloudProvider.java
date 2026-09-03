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

import com.alibaba.fastjson2.JSONObject;
import com.tencentcloudapi.billing.v20180709.BillingClient;
import com.tencentcloudapi.billing.v20180709.models.BillDetail;
import com.tencentcloudapi.billing.v20180709.models.DescribeBillDetailRequest;
import com.tencentcloudapi.billing.v20180709.models.DescribeBillDetailResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.cvm.v20170312.CvmClient;
import com.tencentcloudapi.cvm.v20170312.models.DescribeInstancesRequest;
import com.tencentcloudapi.cvm.v20170312.models.DescribeInstancesResponse;
import com.tencentcloudapi.cvm.v20170312.models.Instance;
import com.tencentcloudapi.cvm.v20170312.models.RebootInstancesRequest;
import com.tencentcloudapi.cvm.v20170312.models.ResetInstancesTypeRequest;
import com.tencentcloudapi.cvm.v20170312.models.StartInstancesRequest;
import com.tencentcloudapi.cvm.v20170312.models.StopInstancesRequest;
import com.tencentcloudapi.cvm.v20170312.models.Tag;
import io.voyager1.cloud.CloudBill;
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
 * 腾讯云 CVM 云厂商实现（M2）
 *
 * @since 2026/8/31
 */
@Component
@Slf4j
public class TencentCloudProvider implements ICloudProvider {

    @Override
    public String vendor() {
        return "tencent";
    }

    private CvmClient buildClient(CloudCredential credential, String region) {
        Credential cred = new Credential(credential.getAccessKey(), credential.getSecretKey());
        return new CvmClient(cred, region);
    }

    private String resolveRegion(CloudCredential credential, String region) {
        String r = (region == null || region.isEmpty()) ? credential.getRegion() : region;
        if (r == null || r.isEmpty()) {
            throw new IllegalArgumentException("腾讯云需要指定区域");
        }
        return r;
    }

    @Override
    public boolean testConnectivity(CloudCredential credential) throws Exception {
        String region = (credential.getRegion() == null || credential.getRegion().isEmpty()) ? "ap-guangzhou" : credential.getRegion();
        CvmClient client = this.buildClient(credential, region);
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.setLimit(1L);
        client.DescribeInstances(request);
        return true;
    }

    @Override
    public List<CloudInstanceInfo> listInstances(CloudCredential credential, String region) throws Exception {
        String targetRegion = this.resolveRegion(credential, region);
        CvmClient client = this.buildClient(credential, targetRegion);
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.setLimit(100L);
        DescribeInstancesResponse response = client.DescribeInstances(request);
        Instance[] instanceSet = response.getInstanceSet();
        if (instanceSet == null || instanceSet.length == 0) {
            return Collections.emptyList();
        }
        List<CloudInstanceInfo> list = new ArrayList<>(instanceSet.length);
        for (Instance instance : instanceSet) {
            list.add(this.toInfo(instance, targetRegion));
        }
        return list;
    }

    CloudInstanceInfo toInfo(Instance instance, String region) {
        CloudInstanceInfo info = new CloudInstanceInfo();
        info.setInstanceId(instance.getInstanceId());
        info.setName(instance.getInstanceName());
        info.setStatus(this.normalizeStatus(instance.getInstanceState()));
        info.setRegionId(region);
        if (instance.getPlacement() != null) {
            info.setZoneId(instance.getPlacement().getZone());
        }
        info.setInstanceType(instance.getInstanceType());
        if (instance.getCPU() != null) {
            info.setCpu(instance.getCPU().intValue());
        }
        if (instance.getMemory() != null) {
            info.setMemory((int) (instance.getMemory() * 1024)); // GB -> MB
        }
        info.setOsName(instance.getOsName());
        info.setExpireTime(instance.getExpiredTime());
        info.setChargeType(instance.getInstanceChargeType());
        String[] publicIps = instance.getPublicIpAddresses();
        if (publicIps != null && publicIps.length > 0) {
            info.setPublicIp(publicIps[0]);
        }
        String[] privateIps = instance.getPrivateIpAddresses();
        if (privateIps != null && privateIps.length > 0) {
            info.setPrivateIp(privateIps[0]);
        }
        Tag[] tags = instance.getTags();
        if (tags != null && tags.length > 0) {
            Map<String, String> map = new HashMap<>();
            for (Tag tag : tags) {
                map.put(tag.getKey(), tag.getValue());
            }
            info.setTags(map);
        }
        return info;
    }

    @Override
    public void startInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        CvmClient client = this.buildClient(credential, this.resolveRegion(credential, region));
        StartInstancesRequest request = new StartInstancesRequest();
        request.setInstanceIds(new String[]{instanceId});
        client.StartInstances(request);
    }

    @Override
    public void stopInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        CvmClient client = this.buildClient(credential, this.resolveRegion(credential, region));
        StopInstancesRequest request = new StopInstancesRequest();
        request.setInstanceIds(new String[]{instanceId});
        client.StopInstances(request);
    }

    @Override
    public void rebootInstance(CloudCredential credential, String region, String instanceId) throws Exception {
        CvmClient client = this.buildClient(credential, this.resolveRegion(credential, region));
        RebootInstancesRequest request = new RebootInstancesRequest();
        request.setInstanceIds(new String[]{instanceId});
        client.RebootInstances(request);
    }

    @Override
    public void resizeInstance(CloudCredential credential, String region, String instanceId, String newInstanceType) throws Exception {
        CvmClient client = this.buildClient(credential, this.resolveRegion(credential, region));
        ResetInstancesTypeRequest request = new ResetInstancesTypeRequest();
        request.setInstanceIds(new String[]{instanceId});
        request.setInstanceType(newInstanceType);
        client.ResetInstancesType(request);
    }

    @Override
    public List<CloudBill> listBills(CloudCredential credential, String region, String billingCycle) throws Exception {
        BillingClient client = new BillingClient(new Credential(credential.getAccessKey(), credential.getSecretKey()), "");
        DescribeBillDetailRequest request = new DescribeBillDetailRequest();
        request.setMonth(billingCycle);
        request.setLimit(100L);
        DescribeBillDetailResponse response = client.DescribeBillDetail(request);
        List<CloudBill> list = new ArrayList<>();
        BillDetail[] detailSet = response.getDetailSet();
        if (detailSet != null) {
            for (BillDetail detail : detailSet) {
                list.add(this.billFromItem(detail));
            }
        }
        return list;
    }

    CloudBill billFromItem(BillDetail detail) {
        CloudBill bill = new CloudBill();
        bill.setBillDate(detail.getBillDay());
        bill.setServiceName(detail.getBusinessCodeName());
        bill.setResourceId(detail.getResourceId());
        bill.setRegion(detail.getRegionName());
        bill.setAmount(this.parseAmount(detail.getPriceInfo()));
        bill.setCurrency("CNY");
        return bill;
    }

    Double parseAmount(String[] priceInfo) {
        if (priceInfo == null || priceInfo.length == 0) {
            return null;
        }
        try {
            JSONObject json = JSONObject.parseObject(priceInfo[0]);
            String cost = json.getString("realTotalCost");
            if (cost == null || cost.isEmpty()) {
                cost = json.getString("totalCost");
            }
            return cost != null && !cost.isEmpty() ? Double.parseDouble(cost) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
