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

package io.voyager1.service.pipeline;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.core.entity.PipelineExecuteRecordEntity;
import io.voyager1.core.repository.PipelineExecuteRecordRepository;
import io.voyager1.model.data.PipelineExecuteRecordModel;
import io.voyager1.model.enums.PipelineExecuteStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Pipeline 执行记录服务。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA 仓库（PipelineExecuteRecordRepository），对外契约不变。
 *
 * @since 2026/8/7
 */
@Service
@Slf4j
public class PipelineExecuteRecordService {

    private final PipelineExecuteRecordRepository repository;

    public PipelineExecuteRecordService(PipelineExecuteRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void insert(PipelineExecuteRecordModel record) {
        long now = System.currentTimeMillis();
        PipelineExecuteRecordEntity entity = new PipelineExecuteRecordEntity();
        entity.setId(record.getId() == null || record.getId().isEmpty() ? UUID.randomUUID().toString() : record.getId());
        entity.setCreateTimeMillis(now);
        entity.setModifyTimeMillis(now);
        entity.setPipelineId(record.getPipelineId());
        entity.setTriggerType(record.getTriggerType());
        entity.setStatus(record.getStatus());
        entity.setCurrentStage(record.getCurrentStage());
        entity.setStages(record.getStages());
        entity.setStartTime(record.getStartTime());
        entity.setEndTime(record.getEndTime());
        entity.setOperator(record.getOperator());
        repository.save(entity);
        record.setId(entity.getId());
    }

    @Transactional
    public void updateStatus(String executeId, PipelineExecuteStatus status) {
        PipelineExecuteRecordEntity entity = repository.findById(executeId).orElse(null);
        if (entity == null) {
            return;
        }
        entity.setStatus(status.getCode());
        entity.setModifyTimeMillis(System.currentTimeMillis());
        repository.save(entity);
    }

    @Transactional
    public void updateCurrentStage(String executeId, String stageId) {
        PipelineExecuteRecordEntity entity = repository.findById(executeId).orElse(null);
        if (entity == null) {
            return;
        }
        entity.setCurrentStage(stageId);
        entity.setModifyTimeMillis(System.currentTimeMillis());
        repository.save(entity);
    }

    @Transactional
    public void updateStages(String executeId, String stagesJson, PipelineExecuteStatus status) {
        PipelineExecuteRecordEntity entity = repository.findById(executeId).orElse(null);
        if (entity == null) {
            return;
        }
        JSONArray array = JSON.parseArray(stagesJson);
        JSONArray snapshot = new JSONArray();
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            JSONObject stage = new JSONObject();
            stage.put("id", obj.getString("id"));
            stage.put("type", obj.getString("type"));
            stage.put("status", "wait");
            stage.put("params", obj.getJSONObject("params"));
            snapshot.add(stage);
        }
        entity.setStages(snapshot.toJSONString());
        entity.setModifyTimeMillis(System.currentTimeMillis());
        repository.save(entity);
    }

    @Transactional
    public void appendStageLog(String executeId, String stageId, String log) {
        PipelineExecuteRecordEntity entity = repository.findById(executeId).orElse(null);
        if (entity == null) {
            return;
        }
        String stages = entity.getStages();
        JSONArray snapshot = (stages != null && !stages.isEmpty()) ? JSON.parseArray(stages) : new JSONArray();
        boolean found = false;
        for (int i = 0; i < snapshot.size(); i++) {
            JSONObject obj = snapshot.getJSONObject(i);
            if (StrUtilEquals(obj.getString("id"), stageId)) {
                obj.put("status", "done");
                String prev = obj.getString("log");
                obj.put("log", (prev == null ? "" : prev) + "\n" + log);
                found = true;
                break;
            }
        }
        if (!found) {
            JSONObject stage = new JSONObject();
            stage.put("id", stageId);
            stage.put("status", "done");
            stage.put("log", log);
            snapshot.add(stage);
        }
        entity.setStages(snapshot.toJSONString());
        entity.setModifyTimeMillis(System.currentTimeMillis());
        repository.save(entity);
    }

    @Transactional
    public void updateStageStatus(String executeId, String stageId, String status) {
        PipelineExecuteRecordEntity entity = repository.findById(executeId).orElse(null);
        if (entity == null) {
            return;
        }
        JSONArray snapshot = (entity.getStages() != null && !entity.getStages().isEmpty()) ? JSON.parseArray(entity.getStages()) : new JSONArray();
        for (int i = 0; i < snapshot.size(); i++) {
            JSONObject obj = snapshot.getJSONObject(i);
            if (StrUtilEquals(obj.getString("id"), stageId)) {
                obj.put("status", status);
                break;
            }
        }
        entity.setStages(snapshot.toJSONString());
        entity.setModifyTimeMillis(System.currentTimeMillis());
        repository.save(entity);
    }

    @Transactional
    public void finish(String executeId, PipelineExecuteStatus status, Long endTime, String remark) {
        PipelineExecuteRecordEntity entity = repository.findById(executeId).orElse(null);
        if (entity == null) {
            return;
        }
        entity.setStatus(status.getCode());
        entity.setEndTime(endTime != null ? endTime : System.currentTimeMillis());
        entity.setModifyTimeMillis(System.currentTimeMillis());
        repository.save(entity);
    }

    @Transactional
    public void updateStageResult(String executeId, String stageId, String resultJson) {
        PipelineExecuteRecordEntity entity = repository.findById(executeId).orElse(null);
        if (entity == null) {
            return;
        }
        JSONArray snapshot = (entity.getStages() != null && !entity.getStages().isEmpty()) ? JSON.parseArray(entity.getStages()) : new JSONArray();
        for (int i = 0; i < snapshot.size(); i++) {
            JSONObject obj = snapshot.getJSONObject(i);
            if (StrUtilEquals(obj.getString("id"), stageId)) {
                obj.put("result", JSON.parseObject(resultJson));
                break;
            }
        }
        entity.setStages(snapshot.toJSONString());
        entity.setModifyTimeMillis(System.currentTimeMillis());
        repository.save(entity);
    }

    public JSONObject getStageResult(String executeId, String stageId) {
        PipelineExecuteRecordEntity entity = repository.findById(executeId).orElse(null);
        if (entity == null || entity.getStages() == null || entity.getStages().isEmpty()) {
            return null;
        }
        JSONArray snapshot = JSON.parseArray(entity.getStages());
        for (int i = 0; i < snapshot.size(); i++) {
            JSONObject obj = snapshot.getJSONObject(i);
            if (StrUtilEquals(obj.getString("id"), stageId)) {
                return obj.getJSONObject("result");
            }
        }
        return null;
    }

    @Transactional
    public void delByKey(String executeId) {
        repository.deleteById(executeId);
    }

    public PipelineExecuteRecordModel getByKey(String executeId) {
        PipelineExecuteRecordEntity entity = repository.findById(executeId).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    public List<PipelineExecuteRecordModel> listByPipelineId(String pipelineId) {
        return repository.findByPipelineIdOrderByCreateTimeMillisDesc(pipelineId)
            .stream().map(this::toModel).collect(Collectors.toList());
    }

    private PipelineExecuteRecordModel toModel(PipelineExecuteRecordEntity entity) {
        PipelineExecuteRecordModel model = PipelineExecuteRecordModel.builder()
            .pipelineId(entity.getPipelineId())
            .triggerType(entity.getTriggerType())
            .status(entity.getStatus())
            .currentStage(entity.getCurrentStage())
            .stages(entity.getStages())
            .startTime(entity.getStartTime())
            .endTime(entity.getEndTime())
            .operator(entity.getOperator())
            .build();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        return model;
    }

    private boolean StrUtilEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
