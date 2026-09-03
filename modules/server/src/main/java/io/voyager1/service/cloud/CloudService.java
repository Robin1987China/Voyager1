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

package io.voyager1.service.cloud;

import com.alibaba.fastjson2.JSON;
import io.voyager1.cloud.CloudCredential;
import io.voyager1.cloud.CloudInstanceInfo;
import io.voyager1.cloud.CloudScalingGroup;
import io.voyager1.cloud.CloudSecurityGroup;
import io.voyager1.cloud.CloudSnapshot;
import io.voyager1.cloud.ICloudProvider;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.core.entity.CloudAccountEntity;
import io.voyager1.core.repository.CloudAccountRepository;
import io.voyager1.encrypt.AESEncryptor;
import io.voyager1.model.data.CloudAccountModel;
import io.voyager1.model.data.CloudInstanceModel;
import io.voyager1.service.cloud.provider.CloudProviderRegistry;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 云资产服务（云账号 + 云实例 → 机器部署目标）。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA 仓库（CloudAccountRepository），对外契约不变。
 *
 * @since 2026/8/9
 */
@Service
@Slf4j
public class CloudService {

    private static final String ENC_PREFIX = "ENC:";

    private final CloudAccountRepository repository;
    private final CloudProviderRegistry providerRegistry;
    private final EntityManager entityManager;

    public CloudService(CloudAccountRepository repository, CloudProviderRegistry providerRegistry, EntityManager entityManager) {
        this.repository = repository;
        this.providerRegistry = providerRegistry;
        this.entityManager = entityManager;
    }

    /**
     * 执行原生 SQL（测试清理用），返回影响行数。
     */
    @Transactional
    public int execute(String sql) {
        return entityManager.createNativeQuery(sql).executeUpdate();
    }

    /**
     * 保存云账号（AK 加密存储）。
     */
    @Transactional
    public String saveAccount(String id, String name, String vendor, String accessKey, String secretKey, String extraKey, String region, String remark) {
        Assert.hasText(name, "账号名称不能为空");
        Assert.hasText(vendor, "云厂商不能为空");
        Assert.hasText(accessKey, "AccessKey 不能为空");
        long now = System.currentTimeMillis();
        CloudAccountEntity entity;
        if (id == null || id.isEmpty()) {
            entity = new CloudAccountEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setCreateTimeMillis(now);
        } else {
            entity = repository.findById(id).orElse(null);
            Assert.notNull(entity, "云账号不存在: " + id);
        }
        entity.setModifyTimeMillis(now);
        entity.setName(name);
        entity.setVendor(vendor);
        entity.setAccessKey(this.encrypt(accessKey));
        entity.setSecretKey(this.encrypt(secretKey));
        entity.setExtraKey(this.encrypt(extraKey));
        entity.setRegion(region);
        entity.setRemark(remark);
        repository.save(entity);
        return entity.getId();
    }

    /**
     * 云账号列表（凭证脱敏回显）。
     */
    public List<CloudAccountModel> listAccounts() {
        List<CloudAccountEntity> list = repository.findAll(Sort.by(Sort.Direction.DESC, "createTimeMillis"));
        return list.stream().map(entity -> {
            CloudAccountModel account = toModel(entity);
            account.setAccessKey(this.mask(account.getAccessKey(), false));
            account.setSecretKey(this.mask(account.getSecretKey(), true));
            account.setExtraKey(this.mask(account.getExtraKey(), true));
            return account;
        }).collect(Collectors.toList());
    }

    /**
     * 按主键查询云账号。
     */
    public CloudAccountModel getByKey(String id) {
        CloudAccountEntity entity = repository.findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    private String encrypt(String value) {
        if ((value == null || value.isEmpty()) || value.startsWith(ENC_PREFIX)) {
            return value;
        }
        try {
            return ENC_PREFIX + AESEncryptor.getInstance().encrypt(value);
        } catch (Exception e) {
            throw new IllegalStateException("凭证加密失败", e);
        }
    }

    private String decrypt(String value) {
        if ((value == null || value.isEmpty()) || !value.startsWith(ENC_PREFIX)) {
            return value;
        }
        try {
            return AESEncryptor.getInstance().decrypt(value.substring(ENC_PREFIX.length()));
        } catch (Exception e) {
            log.warn("凭证解密失败，按原文处理: {}", e.getMessage());
            return value;
        }
    }

    private String mask(String value, boolean full) {
        String plain = this.decrypt(value);
        if ((plain == null || plain.isEmpty())) {
            return "";
        }
        if (full) {
            return "******";
        }
        if (plain.length() <= 8) {
            return "****";
        }
        return plain.substring(0, 4) + "****" + plain.substring(plain.length() - 4);
    }

    public void importInstances(String accountId, List<CloudInstanceModel> instances) {
        Assert.hasText(accountId, "云账号不能为空");
        Assert.notNull(instances, "实例列表不能为空");
        CloudInstanceService instanceService = SpringContextHolder.getBean(CloudInstanceService.class);
        for (CloudInstanceModel instance : instances) {
            instance.setAccountId(accountId);
            instanceService.saveInstance(instance);
        }
    }

    public CloudCredential decryptCredential(String accountId) {
        CloudAccountModel account = this.getByKey(accountId);
        Assert.notNull(account, "云账号不存在: " + accountId);
        CloudCredential credential = new CloudCredential();
        credential.setVendor(account.getVendor());
        credential.setAccessKey(this.decrypt(account.getAccessKey()));
        credential.setSecretKey(this.decrypt(account.getSecretKey()));
        credential.setExtraKey(this.decrypt(account.getExtraKey()));
        credential.setRegion(account.getRegion());
        return credential;
    }

    public boolean testConnectivity(String accountId) {
        CloudCredential credential = this.decryptCredential(accountId);
        try {
            return this.providerRegistry.get(credential.getVendor()).testConnectivity(credential);
        } catch (Exception e) {
            log.warn("云账号连通性校验失败: {}", e.getMessage());
            return false;
        }
    }

    public int syncInstances(String accountId) {
        CloudAccountModel account = this.getByKey(accountId);
        Assert.notNull(account, "云账号不存在: " + accountId);
        CloudCredential credential = this.decryptCredential(accountId);
        List<CloudInstanceInfo> infos;
        try {
            infos = this.providerRegistry.get(credential.getVendor()).listInstances(credential, null);
        } catch (Exception e) {
            throw new IllegalStateException("同步云实例失败: " + e.getMessage(), e);
        }
        CloudInstanceService instanceService = SpringContextHolder.getBean(CloudInstanceService.class);
        int count = 0;
        for (CloudInstanceInfo info : infos) {
            CloudInstanceModel model = new CloudInstanceModel();
            model.setAccountId(accountId);
            model.setInstanceId(info.getInstanceId());
            model.setName(info.getName());
            model.setStatus(info.getStatus());
            model.setPublicIp(info.getPublicIp());
            model.setPrivateIp(info.getPrivateIp());
            model.setRegionId(info.getRegionId());
            model.setZoneId(info.getZoneId());
            model.setInstanceType(info.getInstanceType());
            model.setCpu(info.getCpu());
            model.setMemory(info.getMemory());
            model.setOsName(info.getOsName());
            model.setExpireTime(info.getExpireTime());
            model.setChargeType(info.getChargeType());
            model.setTags(this.tagsToJson(info.getTags()));
            CloudInstanceModel exist = instanceService.findByInstanceId(accountId, info.getInstanceId());
            if (exist != null) {
                model.setId(exist.getId());
                model.setMachineId(exist.getMachineId());
                model.setGroupName(exist.getGroupName());
                instanceService.updateById(model);
            } else {
                instanceService.saveInstance(model);
            }
            count++;
        }
        return count;
    }

    public void operateInstance(String accountId, String instanceId, String action) {
        CloudInstanceService instanceService = SpringContextHolder.getBean(CloudInstanceService.class);
        CloudInstanceModel instance = instanceService.findByInstanceId(accountId, instanceId);
        Assert.notNull(instance, "实例不存在: " + instanceId);
        CloudCredential credential = this.decryptCredential(accountId);
        String region = (instance.getRegionId() != null && !instance.getRegionId().isEmpty()) ? instance.getRegionId() : credential.getRegion();
        try {
            ICloudProvider provider = this.providerRegistry.get(credential.getVendor());
            switch (action) {
                case "start":
                    provider.startInstance(credential, region, instanceId);
                    break;
                case "stop":
                    provider.stopInstance(credential, region, instanceId);
                    break;
                case "reboot":
                    provider.rebootInstance(credential, region, instanceId);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的操作: " + action);
            }
        } catch (Exception e) {
            throw new IllegalStateException("实例操作失败: " + e.getMessage(), e);
        }
    }

    public void resizeInstance(String accountId, String instanceId, String newInstanceType) {
        CloudInstanceService instanceService = SpringContextHolder.getBean(CloudInstanceService.class);
        CloudInstanceModel instance = instanceService.findByInstanceId(accountId, instanceId);
        Assert.notNull(instance, "实例不存在: " + instanceId);
        CloudCredential credential = this.decryptCredential(accountId);
        String region = (instance.getRegionId() != null && !instance.getRegionId().isEmpty()) ? instance.getRegionId() : credential.getRegion();
        try {
            this.providerRegistry.get(credential.getVendor()).resizeInstance(credential, region, instanceId, newInstanceType);
        } catch (Exception e) {
            throw new IllegalStateException("规格变配失败: " + e.getMessage(), e);
        }
    }

    public String createSnapshot(String accountId, String diskId, String snapshotName) {
        CloudCredential credential = this.decryptCredential(accountId);
        try {
            return this.providerRegistry.get(credential.getVendor()).createSnapshot(credential, credential.getRegion(), diskId, snapshotName);
        } catch (Exception e) {
            throw new IllegalStateException("创建快照失败: " + e.getMessage(), e);
        }
    }

    public List<CloudSnapshot> listSnapshots(String accountId) {
        CloudCredential credential = this.decryptCredential(accountId);
        try {
            return this.providerRegistry.get(credential.getVendor()).listSnapshots(credential, credential.getRegion());
        } catch (Exception e) {
            throw new IllegalStateException("查询快照失败: " + e.getMessage(), e);
        }
    }

    public void deleteSnapshot(String accountId, String snapshotId) {
        CloudCredential credential = this.decryptCredential(accountId);
        try {
            this.providerRegistry.get(credential.getVendor()).deleteSnapshot(credential, credential.getRegion(), snapshotId);
        } catch (Exception e) {
            throw new IllegalStateException("删除快照失败: " + e.getMessage(), e);
        }
    }

    public List<CloudSecurityGroup> listSecurityGroups(String accountId) {
        CloudCredential credential = this.decryptCredential(accountId);
        try {
            return this.providerRegistry.get(credential.getVendor()).listSecurityGroups(credential, credential.getRegion());
        } catch (Exception e) {
            throw new IllegalStateException("查询安全组失败: " + e.getMessage(), e);
        }
    }

    public String createImage(String accountId, String instanceId, String imageName) {
        CloudInstanceService instanceService = SpringContextHolder.getBean(CloudInstanceService.class);
        CloudInstanceModel instance = instanceService.findByInstanceId(accountId, instanceId);
        Assert.notNull(instance, "实例不存在: " + instanceId);
        CloudCredential credential = this.decryptCredential(accountId);
        String region = (instance.getRegionId() != null && !instance.getRegionId().isEmpty()) ? instance.getRegionId() : credential.getRegion();
        try {
            return this.providerRegistry.get(credential.getVendor()).createImage(credential, region, instanceId, imageName);
        } catch (Exception e) {
            throw new IllegalStateException("创建镜像失败: " + e.getMessage(), e);
        }
    }

    public List<CloudScalingGroup> listScalingGroups(String accountId) {
        CloudCredential credential = this.decryptCredential(accountId);
        try {
            return this.providerRegistry.get(credential.getVendor()).listScalingGroups(credential, credential.getRegion());
        } catch (Exception e) {
            throw new IllegalStateException("查询伸缩组失败: " + e.getMessage(), e);
        }
    }

    private String tagsToJson(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(tags);
    }

    public int syncAllAccounts() {
        List<CloudAccountEntity> accounts = repository.findAll(Sort.by(Sort.Direction.ASC, "createTimeMillis"));
        int total = 0;
        for (CloudAccountEntity account : accounts) {
            try {
                total += this.syncInstances(account.getId());
            } catch (Exception e) {
                log.warn("同步云账号 {} 实例失败: {}", account.getId(), e.getMessage());
            }
        }
        return total;
    }

    private CloudAccountModel toModel(CloudAccountEntity entity) {
        CloudAccountModel model = CloudAccountModel.builder()
            .name(entity.getName())
            .vendor(entity.getVendor())
            .accessKey(entity.getAccessKey())
            .secretKey(entity.getSecretKey())
            .extraKey(entity.getExtraKey())
            .region(entity.getRegion())
            .remark(entity.getRemark())
            .build();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        return model;
    }
}
