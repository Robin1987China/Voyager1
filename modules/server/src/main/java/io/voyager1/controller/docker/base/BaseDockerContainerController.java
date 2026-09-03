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

import io.voyager1.util.FileUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.plugin.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.docker.DockerInfoService;
import io.voyager1.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @since 2022/2/7
 */
@Slf4j
public abstract class BaseDockerContainerController extends BaseDockerController {
    protected final ServerConfig serverConfig;

    protected BaseDockerContainerController(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @GetMapping(value = "info", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> info(@ValidatorItem String id) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        JSONObject info = plugin.execute("info", parameter, JSONObject.class);
        return ApiResult.success("", info);
    }

    @PostMapping(value = "prune", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    @SystemPermission
    public ApiResult<Object> prune(@ValidatorItem String id, @ValidatorItem String pruneType, String labels, String until, String dangling) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("pruneType", pruneType);
        parameter.put("labels", labels);
        parameter.put("until", until);
        parameter.put("dangling", dangling);
        //
        Long spaceReclaimed = plugin.execute("prune", parameter, Long.class);
        spaceReclaimed = (spaceReclaimed != null ? spaceReclaimed : 0L);
        return ApiResult.success("修剪完成,总回收空间：" + FileUtil.readableFileSize(spaceReclaimed));
    }

    /**
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<JSONObject>> list(@ValidatorItem String id) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("name", getParameter("name"));
        parameter.put("containerId", getParameter("containerId"));
        parameter.put("imageId", getParameter("imageId"));
        parameter.put("showAll", getParameter("showAll"));
        List<JSONObject> listContainer = (List<JSONObject>) plugin.execute("listContainer", parameter);
        return ApiResult.success("", listContainer);
    }

    /**
     * @return json
     */
    @PostMapping(value = "list-compose", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<JSONObject>> listCompose(@ValidatorItem String id) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("name", getParameter("name"));
        parameter.put("containerId", getParameter("containerId"));
        parameter.put("imageId", getParameter("imageId"));
        parameter.put("showAll", getParameter("showAll"));
        List<JSONObject> listContainer = (List<JSONObject>) plugin.execute("listComposeContainer", parameter);
        return ApiResult.success("", listContainer);
    }

    /**
     * @return json
     */
    @GetMapping(value = "remove", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> del(@ValidatorItem String id, String containerId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        plugin.execute("removeContainer", parameter);
        return ApiResult.success("执行成功");
    }

    /**
     * @return json
     */
    @GetMapping(value = "start", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<Object> start(@ValidatorItem String id, String containerId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        plugin.execute("startContainer", parameter);
        return ApiResult.success("执行成功");
    }


    /**
     * @return json
     */
    @GetMapping(value = "stop", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<Object> stop(@ValidatorItem String id, String containerId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        plugin.execute("stopContainer", parameter);
        return ApiResult.success("执行成功");
    }


    /**
     * @return json
     */
    @GetMapping(value = "restart", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<Object> restart(@ValidatorItem String id, String containerId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        plugin.execute("restartContainer", parameter);
        return ApiResult.success("执行成功");
    }

    /**
     * @return json
     */
    @GetMapping(value = "stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<Map<String, JSONObject>> stats(@ValidatorItem String id, String containerId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        Map<String, JSONObject> stats = (Map<String, JSONObject>) plugin.execute("stats", parameter);
        return ApiResult.success("执行成功", stats);
    }

    /**
     * @return json
     */
    @GetMapping(value = "inspect-container", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<JSONObject> inspectContainer(@ValidatorItem String id, @ValidatorItem String containerId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        JSONObject results = (JSONObject) plugin.execute("inspectContainer", parameter);
        return ApiResult.success("执行成功", results);
    }

    /**
     * 修改容器配置
     * NanoCpus：以 10-9 (十億分之一) 個 CPU 為單位的 CPU 配額。 例如，250000000 nanocpus = 0.25 CPU。
     *
     * @return json
     */
    @PostMapping(value = "update-container", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<JSONObject> updateContainer(@RequestBody JSONObject jsonObject) throws Exception {
        // @ValidatorItem String id, String containerId
        String id = jsonObject.getString("id");
        Assert.hasText(id, "id 不能为空");
        jsonObject.remove("id");
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.putAll(jsonObject);
        JSONObject results = (JSONObject) plugin.execute("updateContainer", parameter);
        return ApiResult.success("执行成功", results);
    }

    /**
     * drop old container and create new container
     *
     * @return json
     */
    @PostMapping(value = "rebuild-container", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<Object> reBuildContainer(@RequestBody JSONObject jsonObject) throws Exception {
        String id = jsonObject.getString("id");
        String containerId = jsonObject.getString("containerId");
        Assert.hasText(id, "id 不能为空");
        String imageId = jsonObject.getString("imageId");
        Assert.hasText(imageId, "镜像不能为空");
        String name = jsonObject.getString("name");
        Assert.hasText(name, "容器名称不能为空");

        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);

        // drop old container
        if ((containerId != null && !containerId.isEmpty())) {
            parameter.put("containerId", containerId);
            try {
                plugin.execute("removeContainer", parameter);
            } catch (com.github.dockerjava.api.exception.NotFoundException notFoundException) {
                log.warn(notFoundException.getMessage());
            }
        }

        // create new container
        parameter.putAll(jsonObject);
        plugin.execute("createContainer", parameter);
        return ApiResult.success("重建成功");
    }

    /**
     * 下载拉取的日志
     *
     * @param id id
     */
    @GetMapping(value = "download-log")
    @Feature(method = MethodFeature.DOWNLOAD)
    public void downloadLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String id,
                            HttpServletResponse response) {
        File file = FileUtil.file(serverConfig.getUserTempPath(), "docker-log", id + ".log");
        if (!file.exists()) {
            JakartaServletUtil.write(response, new ApiResult<>(201, "还没有日志文件").toString(), MediaType.APPLICATION_JSON_VALUE);
            return;
        }
        JakartaServletUtil.write(response, file);
    }
}
