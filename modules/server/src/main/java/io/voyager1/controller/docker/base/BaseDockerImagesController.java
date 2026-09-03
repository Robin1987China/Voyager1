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
import io.voyager1.util.IoUtil;
import io.voyager1.util.Tuple;
import io.voyager1.util.IdUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.plugin.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.i18n.I18nThreadUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.docker.DockerInfoService;
import io.voyager1.system.ServerConfig;
import io.voyager1.util.FileUtils;
import io.voyager1.util.LogRecorder;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @since 2022/2/7
 */
@Slf4j
public abstract class BaseDockerImagesController extends BaseDockerController {

    protected final ServerConfig serverConfig;

    public BaseDockerImagesController(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
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
        parameter.put("showAll", getParameter("showAll"));
        parameter.put("dangling", getParameter("dangling"));
        parameter.put("workspaceId", getWorkspaceId());
        List<JSONObject> listContainer = (List<JSONObject>) plugin.execute("listImages", parameter);
        return ApiResult.success("", listContainer);
    }


    /**
     * @return json
     */
    @GetMapping(value = "remove", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> del(@ValidatorItem String id, String imageId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("imageId", imageId);
        parameter.put("workspaceId", getWorkspaceId());
        plugin.execute("removeImage", parameter);
        return ApiResult.success("执行成功");
    }


    /**
     * @return json
     */
    @GetMapping(value = "batchRemove", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> batchRemove(@ValidatorItem String id, String[] imagesIds) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("imagesIds", imagesIds);
        parameter.put("workspaceId", getWorkspaceId());
        plugin.execute("batchRemove", parameter);
        return ApiResult.success("执行成功");
    }

    /**
     * @return json
     */
    @GetMapping(value = "inspect", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> inspect(@ValidatorItem String id, String imageId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("imageId", imageId);
        parameter.put("workspaceId", getWorkspaceId());
        JSONObject inspectImage = (JSONObject) plugin.execute("inspectImage", parameter);
        return ApiResult.success("", inspectImage);
    }

    /**
     * @return json
     */
    @GetMapping(value = "pull-image", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> pullImage(@ValidatorItem String id, String repository) {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("repository", repository);
        parameter.put("workspaceId", getWorkspaceId());
        //
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
        File file = FileUtil.file(serverConfig.getUserTempPath(), "docker-log", uuid + ".log");
        LogRecorder logRecorder = LogRecorder.builder().file(file).build();
        logRecorder.system("start pull {}", repository);
        Consumer<String> logConsumer = logRecorder::info;
        parameter.put("logConsumer", logConsumer);
        I18nThreadUtil.execute(() -> {
            try {
                plugin.execute("pullImage", parameter);
                logRecorder.system("pull end");
            } catch (Exception e) {
                logRecorder.error("拉取异常", e);
            } finally {
                IoUtil.close(logRecorder);
            }

        });
        return ApiResult.success("开始拉取", uuid);
    }

    /**
     *
     */
    @GetMapping(value = "save-image", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public void saveImage(@ValidatorItem String id, String imageId, HttpServletResponse response) {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("imageId", imageId);
        //
        try {
            Tuple saveImage = (Tuple) plugin.execute("saveImage", parameter);
            if (saveImage == null) {
                JakartaServletUtil.write(response, new ApiResult<>(405, "镜像不存在").toString(), MediaType.APPLICATION_JSON_VALUE);
                return;
            }
            InputStream inputStream = saveImage.get(0);
            String name = saveImage.get(1);
            JakartaServletUtil.write(response, inputStream, MediaType.APPLICATION_OCTET_STREAM_VALUE, name);
        } catch (Exception e) {
            log.error("导出镜像异常", e);
            JakartaServletUtil.write(response, new ApiResult<>(500, "导出镜像异常").toString(), MediaType.APPLICATION_JSON_VALUE);
        }
    }

    /**
     *
     */
    @PostMapping(value = "load-image", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> loadImage(@ValidatorItem String id,
                                          MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);
        boolean expression = (extName != null && extName.equalsIgnoreCase("tar"));
        Assert.state(expression, "只支持tar文件");
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("stream", file.getInputStream());
        //
        plugin.execute("loadImage", parameter);
        return new ApiResult<>(200, "导入成功");
    }

    /**
     * 获取拉取的日志
     *
     * @param id   id
     * @param line 需要获取的行号
     * @return json
     */
    @GetMapping(value = "pull-image-log", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> getNowLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String id,
                                              @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "行号错误") int line) {
        File file = FileUtil.file(serverConfig.getUserTempPath(), "docker-log", id + ".log");
        if (!file.exists()) {
            return new ApiResult<>(201, "还没有日志文件");
        }
        JSONObject data = FileUtils.readLogFile(file, line);
        return ApiResult.success("", data);
    }

    /**
     * @return json
     */
    @PostMapping(value = "create-container", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<Object> createContainer(@RequestBody JSONObject jsonObject) throws Exception {
        String id = jsonObject.getString("id");
        Assert.hasText(id, "id 不能为空");
        String imageId = jsonObject.getString("imageId");
        Assert.hasText(imageId, "镜像不能为空");
        String name = jsonObject.getString("name");
        Assert.hasText(name, "容器名称不能为空");

        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.putAll(jsonObject);
        parameter.put("workspaceId", getWorkspaceId());
        plugin.execute("createContainer", parameter);
        return ApiResult.success("创建成功");
    }
}
