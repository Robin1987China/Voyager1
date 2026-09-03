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
import io.voyager1.core.entity.PipelineConfigEntity;
import io.voyager1.core.repository.PipelineConfigRepository;
import io.voyager1.model.data.PipelineConfigModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import io.voyager1.cron.CronUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Pipeline 配置服务（CRUD + JSON 解析）。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA 仓库（PipelineConfigRepository），对外契约不变。
 *
 * @since 2026/8/7
 */
@Service
@Slf4j
public class PipelineConfigService {

    private final PipelineConfigRepository repository;

    public PipelineConfigService(PipelineConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public String saveConfig(String id, String name, String buildId, String triggers, String stages, Boolean enabled, String remark) {
        Assert.hasText(name, "名称不能为空");
        Assert.hasText(buildId, "buildId 不能为空");
        Assert.hasText(stages, "阶段配置不能为空");
        JSONArray stageArray = JSON.parseArray(stages);
        Assert.state((stageArray != null && !stageArray.isEmpty()), "阶段配置不能为空");
        long now = System.currentTimeMillis();
        PipelineConfigEntity entity;
        if (id == null || id.isEmpty()) {
            entity = new PipelineConfigEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setCreateTimeMillis(now);
        } else {
            entity = repository.findById(id).orElse(null);
            Assert.notNull(entity, "Pipeline 配置不存在: " + id);
        }
        entity.setModifyTimeMillis(now);
        entity.setName(name);
        entity.setBuildId(buildId);
        entity.setTriggers(triggers);
        entity.setStages(stages);
        entity.setEnabled(enabled == null || enabled ? 1 : 0);
        entity.setRemark(remark);
        repository.save(entity);
        this.registerTriggers(toModel(entity));
        return entity.getId();
    }

    private void registerTriggers(PipelineConfigModel model) {
        CronUtils.remove("pipeline:" + model.getId());
        if ((model.getTriggers() == null || model.getTriggers().isEmpty())) {
            return;
        }
        JSONArray array = JSON.parseArray(model.getTriggers());
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            if ("cron".equalsIgnoreCase(obj.getString("type"))) {
                String cron = obj.getString("cron");
                if ((cron != null && !cron.isEmpty())) {
                    String pipelineId = model.getId();
                    CronUtils.upsert("pipeline:" + pipelineId, cron, new io.voyager1.util.Task() {
                        @Override
                        public void execute() {
                            io.voyager1.common.SpringContextHolder.getBean(io.voyager1.service.pipeline.PipelineExecutorService.class)
                                .trigger(pipelineId, "cron", "system");
                        }
                    });
                    log.info("Pipeline 注册定时触发: id={} cron={}", pipelineId, cron);
                }
            }
        }
    }

    @Transactional
    public void deleteConfig(String id) {
        CronUtils.remove("pipeline:" + id);
        repository.deleteById(id);
    }

    public void restoreCronTriggers() {
        for (PipelineConfigModel model : this.listAll()) {
            this.registerTriggers(model);
        }
    }

    private List<PipelineConfigModel> listAll() {
        return repository.findAll().stream().map(this::toModel).collect(Collectors.toList());
    }

    public PipelineConfigModel getByKey(String id) {
        PipelineConfigEntity entity = repository.findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    @Transactional
    public void delByKey(String id) {
        repository.deleteById(id);
    }

    public String getWebhookToken(PipelineConfigModel model) {
        if ((model.getTriggers() == null || model.getTriggers().isEmpty())) {
            return null;
        }
        JSONArray array = JSON.parseArray(model.getTriggers());
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            if ("webhook".equalsIgnoreCase(obj.getString("type"))) {
                return obj.getString("token");
            }
        }
        return null;
    }

    public PipelineConfigModel checkWebhook(String id, String token) {
        PipelineConfigModel model = this.getByKey(id);
        if (model == null) {
            return null;
        }
        String saved = this.getWebhookToken(model);
        if ((saved == null || saved.isEmpty()) || !saved.equals(token)) {
            return null;
        }
        return model;
    }

    public List<PipelineStage> parseStages(PipelineConfigModel model) {
        List<PipelineStage> stages = new ArrayList<>();
        if ((model.getStages() == null || model.getStages().isEmpty())) {
            return stages;
        }
        JSONArray array = JSON.parseArray(model.getStages());
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            PipelineStage stage = new PipelineStage();
            stage.setId(obj.getString("id"));
            stage.setType(obj.getString("type"));
            stage.setParams(obj.getJSONObject("params"));
            stage.setNeeds(obj.getList("needs", String.class));
            if ((stage.getId() == null || stage.getId().isEmpty())) {
                stage.setId("stage-" + i);
            }
            stages.add(stage);
        }
        return stages;
    }

    public List<PipelineConfigModel> listByBuildId(String buildId) {
        return repository.findByBuildIdOrderByCreateTimeMillisDesc(buildId)
            .stream().map(this::toModel).collect(Collectors.toList());
    }

    private PipelineConfigModel toModel(PipelineConfigEntity entity) {
        PipelineConfigModel model = PipelineConfigModel.builder()
            .name(entity.getName())
            .buildId(entity.getBuildId())
            .triggers(entity.getTriggers())
            .stages(entity.getStages())
            .enabled(entity.getEnabled() != null && entity.getEnabled() == 1)
            .remark(entity.getRemark())
            .build();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        return model;
    }

    public enum StageType {
        BUILD, EXEC, PUBLISH, APPROVAL
    }

    @Data
    public static class PipelineStage {
        private String id;
        private String type;
        private Map<String, Object> params;
        private List<String> needs;
    }
}
