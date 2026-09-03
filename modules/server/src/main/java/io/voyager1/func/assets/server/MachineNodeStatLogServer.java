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

package io.voyager1.func.assets.server;

import io.voyager1.configuration.NodeConfig;
import io.voyager1.core.entity.MachineNodeStatLogEntity;
import io.voyager1.core.jpa.DataService;
import io.voyager1.core.repository.MachineNodeStatLogRepository;
import io.voyager1.event.ISystemTask;
import io.voyager1.func.assets.model.MachineNodeStatLogModel;
import io.voyager1.system.ServerConfig;
import io.voyager1.util.DateUtil;
import io.voyager1.util.DateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 机器节点统计日志服务。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA 仓库（MachineNodeStatLogRepository），对外契约不变。
 *
 * @since 2023/2/18
 */
@Service
@Slf4j
public class MachineNodeStatLogServer implements DataService<MachineNodeStatLogModel>, ISystemTask {

    private final MachineNodeStatLogRepository repository;
    private final NodeConfig nodeConfig;

    public MachineNodeStatLogServer(MachineNodeStatLogRepository repository, ServerConfig serverConfig) {
        this.repository = repository;
        this.nodeConfig = serverConfig.getNode();
    }

    @Override
    public MachineNodeStatLogModel getByKey(String id) {
        MachineNodeStatLogEntity entity = repository.findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    @Transactional
    public void insert(MachineNodeStatLogModel model) {
        long now = System.currentTimeMillis();
        MachineNodeStatLogEntity entity = new MachineNodeStatLogEntity();
        entity.setId(model.getId() == null || model.getId().isEmpty() ? UUID.randomUUID().toString() : model.getId());
        entity.setCreateTimeMillis(now);
        entity.setModifyTimeMillis(now);
        copyFields(model, entity);
        repository.save(entity);
        model.setId(entity.getId());
    }

    public List<MachineNodeStatLogModel> listByMachineId(String machineId, int limit) {
        return repository.findByMachineIdOrderByMonitorTimeDesc(machineId, PageRequest.of(0, limit))
            .stream().map(this::toModel).collect(Collectors.toList());
    }

    public List<MachineNodeStatLogModel> listByMachineIdAndTimeRange(String machineId, long start, long end, int limit) {
        return repository.findByMachineIdAndMonitorTimeBetweenOrderByMonitorTimeDesc(machineId, start, end, PageRequest.of(0, limit))
            .stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void executeTask() {
        int statLogKeepDays = nodeConfig.getStatLogKeepDays();
        log.debug("统计日志保留天数 {}", statLogKeepDays);
        if (statLogKeepDays <= 0) {
            return;
        }
        DateTime dateTime = DateUtil.beginOfDay(DateTime.now());
        dateTime = DateUtil.offsetDay(dateTime, -statLogKeepDays);
        long threshold = dateTime.getTime();
        int del = (int) repository.deleteByMonitorTimeLessThan(threshold);
        log.info("自动清理 {} 条机器节点统计日志", del);
    }

    private void copyFields(MachineNodeStatLogModel model, MachineNodeStatLogEntity entity) {
        entity.setMachineId(model.getMachineId());
        entity.setCpuTicks(model.getCpuTicks());
        entity.setOccupyCpu(model.getOccupyCpu());
        entity.setOccupyMemory(model.getOccupyMemory());
        entity.setOccupySwapMemory(model.getOccupySwapMemory());
        entity.setOccupyVirtualMemory(model.getOccupyVirtualMemory());
        entity.setOccupyDisk(model.getOccupyDisk());
        entity.setMonitorTime(model.getMonitorTime());
        entity.setNetworkDelay(model.getNetworkDelay());
        entity.setNetTxBytes(model.getNetTxBytes());
        entity.setNetRxBytes(model.getNetRxBytes());
    }

    private MachineNodeStatLogModel toModel(MachineNodeStatLogEntity entity) {
        MachineNodeStatLogModel model = new MachineNodeStatLogModel();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        model.setMachineId(entity.getMachineId());
        model.setCpuTicks(entity.getCpuTicks());
        model.setOccupyCpu(entity.getOccupyCpu());
        model.setOccupyMemory(entity.getOccupyMemory());
        model.setOccupySwapMemory(entity.getOccupySwapMemory());
        model.setOccupyVirtualMemory(entity.getOccupyVirtualMemory());
        model.setOccupyDisk(entity.getOccupyDisk());
        model.setMonitorTime(entity.getMonitorTime());
        model.setNetworkDelay(entity.getNetworkDelay());
        model.setNetTxBytes(entity.getNetTxBytes());
        model.setNetRxBytes(entity.getNetRxBytes());
        return model;
    }
}
