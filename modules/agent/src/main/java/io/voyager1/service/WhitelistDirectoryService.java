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

package io.voyager1.service;

import io.voyager1.util.CollUtil;
import io.voyager1.util.ObjectUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.AgentConst;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.data.AgentWhitelist;
import io.voyager1.util.JsonFileUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 授权服务
 *
 * @since 2019/2/28
 */
@Service
@Slf4j
public class WhitelistDirectoryService extends BaseOperService<AgentWhitelist> {

    public WhitelistDirectoryService() {
        super(AgentConst.WHITELIST_DIRECTORY);
    }

    /**
     * 获取授权信息配置、如何没有配置或者配置错误将返回新对象
     *
     * @return AgentWhitelist
     */
    public AgentWhitelist getWhitelist() {
        try {
            JSONObject jsonObject = getJSONObject();
            if (jsonObject == null) {
                return new AgentWhitelist();
            }
            return jsonObject.toJavaObject(AgentWhitelist.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new AgentWhitelist();
    }

    /**
     * 单项添加授权
     *
     * @param item 授权
     */
    public void addProjectWhiteList(String item) {
        ArrayList<String> list = new java.util.ArrayList<>(java.util.Arrays.asList(item));
        List<String> checkOk = AgentWhitelist.covertToArray(list, "项目路径授权不能位于Voyager1目录下");

        AgentWhitelist agentWhitelist = getWhitelist();
        List<String> project = agentWhitelist.getProject();
        project = (project != null ? project : new ArrayList<>());
        project.addAll(checkOk);
        project = project.stream()
            .distinct()
            .collect(Collectors.toList());
        agentWhitelist.setProject(project);
        saveWhitelistDirectory(agentWhitelist);
    }

    public boolean checkProjectDirectory(String path) {
        AgentWhitelist agentWhitelist = getWhitelist();
        List<String> list = agentWhitelist.getProject();
        return AgentWhitelist.checkPath(list, path);
    }


    /**
     * 保存授权
     *
     * @param jsonObject 实体
     */
    public void saveWhitelistDirectory(AgentWhitelist jsonObject) {
        String path = getDataFilePath(AgentConst.WHITELIST_DIRECTORY);
        JsonFileUtil.saveJson(path, jsonObject.toJson());
    }
}
