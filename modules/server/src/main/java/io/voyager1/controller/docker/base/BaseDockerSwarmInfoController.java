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

package io.voyager1.controller.docker.base;

import io.voyager1.core.api.ApiResult;
import io.voyager1.plugin.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.docker.DockerSwarmInfoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Map;

/**
 * @since 2022/2/13
 */
public abstract class BaseDockerSwarmInfoController extends BaseDockerController {

    @PostMapping(value = "node-list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<JSONObject>> nodeList(
        @ValidatorItem String id,
        String nodeId, String nodeName, String nodeRole) throws Exception {
        //
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> map = this.toDockerParameter(id);
        map.put("id", nodeId);
        map.put("name", nodeName);
        map.put("role", nodeRole);
        List<JSONObject> listSwarmNodes = (List<JSONObject>) plugin.execute("listSwarmNodes", map);
        return new ApiResult<>(200, "", listSwarmNodes);
    }


    /**
     * 修改节点信息
     *
     * @param id 集群ID
     * @return json
     */
    @PostMapping(value = "update", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> update(@ValidatorItem String id,
                                      @ValidatorItem String nodeId,
                                      @ValidatorItem String availability,
                                      @ValidatorItem String role) throws Exception {
        //
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> map = this.toDockerParameter(id);
        map.put("nodeId", nodeId);
        map.put("availability", availability);
        map.put("role", role);
        plugin.execute("updateSwarmNode", map);
        return new ApiResult<>(200, "修改成功");
    }
}
