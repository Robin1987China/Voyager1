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

import io.voyager1.core.entity.ScriptLibraryEntity;
import io.voyager1.core.jpa.DataService;
import io.voyager1.core.jpa.JpaQuerySupport;
import io.voyager1.core.repository.ScriptLibraryRepository;
import io.voyager1.func.assets.model.ScriptLibraryModel;
import io.voyager1.model.PageResultDto;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.util.PatternPool;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 脚本库服务。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA 仓库（ScriptLibraryRepository），对外契约不变。
 *
 * @since 2024/6/1
 */
@Service
@Slf4j
public class ScriptLibraryServer implements DataService<ScriptLibraryModel> {

    private final ScriptLibraryRepository repository;
    private final Pattern pattern = PatternPool.get("G@\\(\"(.*?)\"\\)", Pattern.DOTALL);
    private final String placeholder = "VOYAGER1____PLACEHOLDER";

    public ScriptLibraryServer(ScriptLibraryRepository repository) {
        this.repository = repository;
    }

    @Override
    public ScriptLibraryModel getByKey(String id) {
        ScriptLibraryEntity entity = repository.findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    @Transactional
    public void delByKey(String id) {
        repository.deleteById(id);
    }

    @Transactional
    public void insert(ScriptLibraryModel model) {
        long now = System.currentTimeMillis();
        ScriptLibraryEntity entity = new ScriptLibraryEntity();
        entity.setId(model.getId() == null || model.getId().isEmpty() ? UUID.randomUUID().toString() : model.getId());
        entity.setCreateTimeMillis(now);
        entity.setModifyTimeMillis(now);
        copyFields(model, entity);
        repository.save(entity);
        model.setId(entity.getId());
    }

    @Transactional
    public void updateById(ScriptLibraryModel model) {
        ScriptLibraryEntity entity = repository.findById(model.getId()).orElse(null);
        if (entity == null) {
            this.insert(model);
            return;
        }
        entity.setModifyTimeMillis(System.currentTimeMillis());
        copyFields(model, entity);
        repository.save(entity);
    }

    public PageResultDto<ScriptLibraryModel> listPage(HttpServletRequest request) {
        return this.listPage(JakartaServletUtil.getParamMap(request));
    }

    public PageResultDto<ScriptLibraryModel> listPage(Map<String, String> paramMap) {
        Page<ScriptLibraryEntity> page = repository.findAll(
            JpaQuerySupport.specification(paramMap), JpaQuerySupport.pageable(paramMap));
        List<ScriptLibraryModel> result = page.getContent().stream().map(this::toModel).collect(Collectors.toList());
        return JpaQuerySupport.toPageResult(page, result);
    }

    public List<ScriptLibraryModel> listByTag(String tag) {
        return repository.findByTag(tag).stream().map(this::toModel).collect(Collectors.toList());
    }

    public boolean existsByTag(String tag, String excludeId) {
        for (ScriptLibraryEntity entity : repository.findByTag(tag)) {
            if (excludeId == null || excludeId.isEmpty() || !excludeId.equals(entity.getId())) {
                return true;
            }
        }
        return false;
    }

    public String referenceReplace(String script) {
        if ((script == null || script.isEmpty())) {
            return script;
        }
        Map<String, ScriptLibraryModel> map = new HashMap<>(3);
        Matcher matcher = pattern.matcher(script);
        StringBuffer modified = new StringBuffer();
        while (matcher.find()) {
            String tag = matcher.group(1);
            ScriptLibraryModel scriptLibraryModel = map.get(tag);
            if (scriptLibraryModel == null) {
                List<ScriptLibraryModel> libraryModels = this.listByTag(tag);
                scriptLibraryModel = (libraryModels == null || libraryModels.isEmpty() ? null : libraryModels.get(0));
                if (scriptLibraryModel != null) {
                    map.put(tag, scriptLibraryModel);
                }
            }
            Assert.notNull(scriptLibraryModel, String.format("未找到脚本库信息:%s, 请检查引用标记是否正确或者脚本是否被删除", tag));
            String modelScript = scriptLibraryModel.getScript();
            modelScript = modelScript.replace("${", placeholder);
            matcher.appendReplacement(modified, modelScript);
        }
        matcher.appendTail(modified);
        return modified.toString().replace(placeholder, "${");
    }

    private void copyFields(ScriptLibraryModel model, ScriptLibraryEntity entity) {
        entity.setModifyUser(model.getModifyUser());
        entity.setCreateUser(model.getCreateUser());
        entity.setTag(model.getTag());
        entity.setDescription(model.getDescription());
        entity.setScript(model.getScript());
        entity.setMachineIds(model.getMachineIds());
        entity.setVersion(model.getVersion());
    }

    private ScriptLibraryModel toModel(ScriptLibraryEntity entity) {
        ScriptLibraryModel model = new ScriptLibraryModel();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        model.setModifyUser(entity.getModifyUser());
        model.setCreateUser(entity.getCreateUser());
        model.setTag(entity.getTag());
        model.setDescription(entity.getDescription());
        model.setScript(entity.getScript());
        model.setMachineIds(entity.getMachineIds());
        model.setVersion(entity.getVersion());
        return model;
    }
}
