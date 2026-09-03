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

import io.voyager1.util.TimedCache;
import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.MapUtil;
import io.voyager1.util.CharsetUtil;
import io.voyager1.util.IdUtil;
import io.voyager1.util.ObjectUtil;
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
import io.voyager1.service.docker.DockerSwarmInfoService;
import io.voyager1.system.ServerConfig;
import io.voyager1.util.FileUtils;
import io.voyager1.util.LogRecorder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.nio.charset.StandardCharsets;

/**
 * @since 2022/2/14
 */
@Slf4j
public abstract class BaseDockerSwarmServiceController extends BaseDockerController {
    private static final TimedCache<String, Set<String>> LOG_CACHE = new TimedCache<>(30 * 1000);
    protected final ServerConfig serverConfig;

    public BaseDockerSwarmServiceController(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        // 30 秒检查一次
        LOG_CACHE.schedulePrune(30 * 1000);
        // 监控过期
        LOG_CACHE.setListener((key, userIds) -> {
            try {
                log.debug("异步资源过期,需要主动关闭,{} {}", key, userIds);
                IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
                Map<String, Object> map = java.util.Map.of("uuid", key);
                plugin.execute("closeAsyncResource", map);
                //
                for (String userId : userIds) {
                    File file = FileUtil.file(serverConfig.getUserTempPath(userId), "docker-swarm-log", key + ".log");
                    FileUtil.del(file);
                }
            } catch (Exception e) {
                log.error("关闭资源失败", e);
            }
        });
    }

    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<JSONObject>> list(
        @ValidatorItem String id,
        String serviceId, String serviceName) throws Exception {
        //
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> map = this.toDockerParameter(id);
        map.put("id", serviceId);
        map.put("name", serviceName);
        List<JSONObject> listSwarmNodes = (List<JSONObject>) plugin.execute("listServices", map);
        return new ApiResult<>(200, "", listSwarmNodes);
    }

    @PostMapping(value = "task-list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<JSONObject>> taskList(
        @ValidatorItem String id,
        String serviceId, String taskId, String taskName, String taskNode, String taskState) throws Exception {
        //
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> map = this.toDockerParameter(id);
        map.put("id", taskId);
        map.put("serviceId", serviceId);
        map.put("name", taskName);
        map.put("node", taskNode);
        map.put("state", taskState);
        List<JSONObject> listSwarmNodes = (List<JSONObject>) plugin.execute("listTasks", map);
        return new ApiResult<>(200, "", listSwarmNodes);
    }

    @GetMapping(value = "del", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<List<JSONObject>> del(@ValidatorItem String id, @ValidatorItem String serviceId) throws Exception {
        //
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> map = this.toDockerParameter(id);
        map.put("serviceId", serviceId);
        plugin.execute("removeService", map);
        return new ApiResult<>(200, "删除服务成功");
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<List<JSONObject>> edit(@RequestBody JSONObject jsonObject) throws Exception {
        //
        String id = jsonObject.getString("id");
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> map = this.toDockerParameter(id);
        map.putAll(jsonObject);
        plugin.execute("updateService", map);
        return new ApiResult<>(200, "修改服务成功");
    }


    /**
     * @return json
     */
    @GetMapping(value = "start-log", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> pullImage(@ValidatorItem String id,
                                          @ValidatorItem String type,
                                          @ValidatorItem String dataId,
                                          Integer tail,
                                          String since,
                                          Boolean timestamps) {
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put((type != null && type.equalsIgnoreCase("service")) ? "serviceId" : "taskId", dataId);
        //
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
        File file = FileUtil.file(serverConfig.getUserTempPath(), "docker-swarm-log", uuid + ".log");
        LogRecorder logRecorder = LogRecorder.builder().file(file).build();

        logRecorder.system("start pull {}", dataId);
        logRecorder.info("");
        Consumer<String> logConsumer = logRecorder::append;
        parameter.put("charset", StandardCharsets.UTF_8);
        parameter.put("consumer", logConsumer);
        //
        tail = (tail != null ? tail : 50);
        tail = Math.max(tail, 1);
        parameter.put("tail", tail);
        //parameter.put("since", since);
        parameter.put("timestamps", timestamps);
        // 操作id
        parameter.put("uuid", uuid);
        I18nThreadUtil.execute(() -> {
            try {
                plugin.execute((type != null && type.equalsIgnoreCase("service")) ? "logService" : "logTask", parameter);
                logRecorder.system("pull end");
            } catch (Exception e) {
                logRecorder.error("拉取日志异常", e);
            } finally {
                IoUtil.close(logRecorder);
            }
        });
        // 添加到缓存中
        LOG_CACHE.put(uuid, CollUtil.newHashSet(getUser().getId()));
        return ApiResult.success("开始拉取", uuid);
    }

    /**
     * 获取拉取的日志
     *
     * @param id   id
     * @param line 需要获取的行号
     * @return json
     */
    @GetMapping(value = "pull-log", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> getNowLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String id,
                                              @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "行号错误") int line) {
        File file = FileUtil.file(serverConfig.getUserTempPath(), "docker-swarm-log", id + ".log");
        if (!file.exists()) {
            return new ApiResult<>(201, "还没有日志文件");
        }
        JSONObject data = FileUtils.readLogFile(file, line);
        // 更新缓存，避免超时被清空
        synchronized (BaseDockerSwarmServiceController.class) {
            Set<String> userIds = (LOG_CACHE.get(id) != null ? LOG_CACHE.get(id) : new HashSet<>());
            userIds.add(getUser().getId());
            LOG_CACHE.put(id, userIds);
        }
        return ApiResult.success("", data);
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
        File file = FileUtil.file(serverConfig.getUserTempPath(), "docker-swarm-log", id + ".log");
        if (!file.exists()) {
            JakartaServletUtil.write(response, new ApiResult<>(201, "还没有日志文件").toString(), MediaType.APPLICATION_JSON_VALUE);
            return;
        }
        JakartaServletUtil.write(response, file);
    }
}
