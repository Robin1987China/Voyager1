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

package io.voyager1.service.system;

import io.voyager1.util.ReflectUtil;
import io.voyager1.model.BaseJsonModel;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.core.entity.SystemParameterEntity;
import io.voyager1.core.repository.SystemParameterRepository;
import io.voyager1.model.data.SystemParametersModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;

/**
 * 系统参数服务（key-value，value 为 JSON 字符串）。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA 仓库（SystemParameterRepository），对外契约不变。
 *
 * @since 2021/12/2
 */
@Service
@Slf4j
public class SystemParametersServer {

    private final SystemParameterRepository repository;

    public SystemParametersServer(SystemParameterRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void upsert(String name, BaseJsonModel jsonModel, String desc) {
        SystemParametersModel systemParametersModel = new SystemParametersModel();
        systemParametersModel.setId(name);
        systemParametersModel.setValue(jsonModel.toJson().toString());
        systemParametersModel.setDescription(desc);
        this.upsertModel(systemParametersModel);
    }

    @Transactional
    public void upsert(String name, Object data, String desc) {
        SystemParametersModel systemParametersModel = new SystemParametersModel();
        systemParametersModel.setId(name);
        systemParametersModel.setValue(JSONObject.toJSONString(data));
        systemParametersModel.setDescription(desc);
        this.upsertModel(systemParametersModel);
    }

    private void upsertModel(SystemParametersModel model) {
        long now = System.currentTimeMillis();
        SystemParameterEntity entity = repository.findById(model.getId()).orElse(null);
        if (entity == null) {
            entity = new SystemParameterEntity();
            entity.setId(model.getId());
            entity.setCreateTimeMillis(now);
        }
        entity.setModifyTimeMillis(now);
        entity.setValue(model.getValue());
        entity.setDescription(model.getDescription());
        repository.save(entity);
    }

    public <T> T getConfig(String name, Class<T> cls) {
        return this.getConfig(name, cls, null);
    }

    public <T> T getConfig(String name, Class<T> cls, Function<T, T> mapTo) {
        SystemParametersModel parametersModel = this.getByKey(name);
        if (parametersModel == null) {
            return null;
        }
        T jsonToBean = parametersModel.jsonToBean(cls);
        if (mapTo == null) {
            return jsonToBean;
        }
        return mapTo.apply(jsonToBean);
    }

    public <T> T getConfigDefNewInstance(String name, Class<T> cls) {
        T config;
        try {
            config = this.getConfig(name, cls);
        } catch (Exception e) {
            log.error("读取系统参数异常", e);
            return ReflectUtil.newInstance(cls);
        }
        return config == null ? ReflectUtil.newInstance(cls) : config;
    }

    @Transactional
    public void delByKey(String name) {
        repository.deleteById(name);
    }

    private SystemParametersModel getByKey(String name) {
        SystemParameterEntity entity = repository.findById(name).orElse(null);
        if (entity == null) {
            return null;
        }
        SystemParametersModel model = new SystemParametersModel();
        model.setId(entity.getId());
        model.setValue(entity.getValue());
        model.setDescription(entity.getDescription());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        return model;
    }
}
