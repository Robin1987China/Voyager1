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

import io.voyager1.core.entity.CloudInstanceEntity;
import io.voyager1.core.repository.CloudInstanceRepository;
import io.voyager1.model.data.CloudInstanceModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 云实例服务（实例管理 + 导入为 SSH 机器）。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA 仓库（CloudInstanceRepository），对外契约不变。
 *
 * @since 2026/8/9
 */
@Service
@Slf4j
public class CloudInstanceService {

    private final CloudInstanceRepository repository;

    public CloudInstanceService(CloudInstanceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public String saveInstance(CloudInstanceModel instance) {
        long now = System.currentTimeMillis();
        CloudInstanceEntity entity;
        if (instance.getId() == null || instance.getId().isEmpty()) {
            entity = new CloudInstanceEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setCreateTimeMillis(now);
        } else {
            entity = repository.findById(instance.getId()).orElse(new CloudInstanceEntity());
            if (entity.getId() == null) {
                entity.setId(instance.getId());
                entity.setCreateTimeMillis(now);
            }
        }
        entity.setModifyTimeMillis(now);
        entity.setAccountId(instance.getAccountId());
        entity.setInstanceId(instance.getInstanceId());
        entity.setName(instance.getName());
        entity.setPublicIp(instance.getPublicIp());
        entity.setPrivateIp(instance.getPrivateIp());
        entity.setStatus(instance.getStatus());
        entity.setGroupName(instance.getGroupName());
        entity.setMachineId(instance.getMachineId());
        entity.setRegionId(instance.getRegionId());
        entity.setZoneId(instance.getZoneId());
        entity.setInstanceType(instance.getInstanceType());
        entity.setCpu(instance.getCpu());
        entity.setMemory(instance.getMemory());
        entity.setOsName(instance.getOsName());
        entity.setExpireTime(instance.getExpireTime());
        entity.setChargeType(instance.getChargeType());
        entity.setTags(instance.getTags());
        repository.save(entity);
        return entity.getId();
    }

    /**
     * 按 id 更新（等价 saveInstance，保留旧 BaseDbService 兼容方法名）。
     */
    @Transactional
    public void updateById(CloudInstanceModel model) {
        this.saveInstance(model);
    }

    public List<CloudInstanceModel> listByAccount(String accountId) {
        List<CloudInstanceEntity> entities = (accountId != null && !accountId.isEmpty())
            ? repository.findByAccountIdOrderByCreateTimeMillisDesc(accountId)
            : repository.findAll();
        return entities.stream().map(this::toModel).collect(Collectors.toList());
    }

    public CloudInstanceModel findByInstanceId(String accountId, String instanceId) {
        CloudInstanceEntity entity = repository.findFirstByAccountIdAndInstanceIdOrderByCreateTimeMillisDesc(accountId, instanceId);
        return entity == null ? null : toModel(entity);
    }

    public CloudInstanceModel getByKey(String id) {
        CloudInstanceEntity entity = repository.findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    @Transactional
    public String importAsMachine(String id, String sshUser, Integer sshPort, String password) {
        CloudInstanceEntity entity = repository.findById(id).orElse(null);
        if (entity == null) {
            return null;
        }
        String host = (entity.getPublicIp() != null && !entity.getPublicIp().isEmpty()) ? entity.getPublicIp() : entity.getPrivateIp();
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("实例没有可用的 IP");
        }
        io.voyager1.func.assets.model.MachineSshModel machine = new io.voyager1.func.assets.model.MachineSshModel();
        machine.setId(UUID.randomUUID().toString());
        machine.setHostName((entity.getName() == null || entity.getName().isEmpty() ? host : entity.getName()));
        machine.setHost(host);
        machine.setPort(sshPort == null ? 22 : sshPort);
        machine.setUser((sshUser == null || sshUser.isEmpty() ? "root" : sshUser));
        machine.setPassword(password);
        io.voyager1.common.SpringContextHolder.getBean(io.voyager1.func.assets.server.MachineSshServer.class)
            .insert(machine);
        entity.setMachineId(machine.getId());
        entity.setModifyTimeMillis(System.currentTimeMillis());
        repository.save(entity);
        log.info("云实例导入为机器: {} -> {}", id, machine.getId());
        return machine.getId();
    }

    private CloudInstanceModel toModel(CloudInstanceEntity entity) {
        CloudInstanceModel model = CloudInstanceModel.builder()
            .accountId(entity.getAccountId())
            .instanceId(entity.getInstanceId())
            .name(entity.getName())
            .publicIp(entity.getPublicIp())
            .privateIp(entity.getPrivateIp())
            .status(entity.getStatus())
            .groupName(entity.getGroupName())
            .machineId(entity.getMachineId())
            .regionId(entity.getRegionId())
            .zoneId(entity.getZoneId())
            .instanceType(entity.getInstanceType())
            .cpu(entity.getCpu())
            .memory(entity.getMemory())
            .osName(entity.getOsName())
            .expireTime(entity.getExpireTime())
            .chargeType(entity.getChargeType())
            .tags(entity.getTags())
            .build();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        return model;
    }
}
