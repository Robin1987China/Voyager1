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

package io.voyager1.func.openapi.controller;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.RegexPool;
import io.voyager1.util.Validator;

import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.util.ContentType;
import io.voyager1.core.api.ApiResult;
import io.voyager1.event.IAsyncLoad;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.build.BuildExecuteService;
import io.voyager1.build.BuildUtil;
import io.voyager1.build.ResultDirFileAction;
import io.voyager1.common.BaseVoyager1Controller;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerOpenApi;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.interceptor.NotLogin;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.cron.CronUtils;
import io.voyager1.model.BaseEnum;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.enums.BuildStatus;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.dblog.BuildInfoService;
import io.voyager1.service.user.TriggerTokenLogServer;
import io.voyager1.system.Voyager1RuntimeException;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @since 2019/9/4
 */
@RestController
@NotLogin
@Slf4j
public class BuildTriggerApiController extends BaseVoyager1Controller implements IAsyncLoad, Runnable {

    private final BuildInfoService buildInfoService;
    private final BuildExecuteService buildExecuteService;
    private final TriggerTokenLogServer triggerTokenLogServer;
    /**
     * 等待执行构建的队列
     */
    private final Map<String, Queue<BuildCache>> waitQueue = new java.util.concurrent.ConcurrentHashMap<>();

    public BuildTriggerApiController(BuildInfoService buildInfoService,
                                     BuildExecuteService buildExecuteService,
                                     TriggerTokenLogServer triggerTokenLogServer) {
        this.buildInfoService = buildInfoService;
        this.buildExecuteService = buildExecuteService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }


    private Object[] buildParametersEnv(HttpServletRequest request, String body) {
        String contentType = request.getContentType();
        Object[] parametersEnv = new Object[4];
        parametersEnv[0] = "triggerContentType";
        parametersEnv[1] = contentType;
        parametersEnv[2] = "triggerBodyData";
        if (ContentType.isDefault(contentType)) {
            Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
            parametersEnv[3] = JSONObject.toJSONString(paramMap);
        } else {
            // github issues 48
            parametersEnv[3] = body == null ? JakartaServletUtil.getBody(request) : body;
        }
        return parametersEnv;
    }

    /**
     * 构建触发器
     *
     * @param id    构建ID
     * @param token 构建的token
     * @param delay 延迟时间（单位秒）
     * @return json
     */
    @RequestMapping(value = ServerOpenApi.BUILD_TRIGGER_BUILD2, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Integer> trigger2(@PathVariable String id, @PathVariable String token,
                                          HttpServletRequest request,
                                          String delay,
                                          String buildRemark, String useQueue) {
        BuildInfoModel item = buildInfoService.getByKey(id);
        Assert.notNull(item, "没有对应数据");
        UserModel userModel = this.triggerTokenLogServer.getUserByToken(token, buildInfoService.typeName());
        //
        Assert.notNull(userModel, "触发token错误,或者已经失效:-1");

        Assert.state(java.util.Objects.equals(token, item.getTriggerToken()), "触发token错误,或者已经失效");
        // 构建外部参数
        Object[] parametersEnv = this.buildParametersEnv(request, null);
        Integer delay1 = ConvertUtil.toInt(delay, 0);
        if (ConvertUtil.toBool(useQueue, false)) {
            // 提交到队列暂存
            BuildCache buildCache = new BuildCache();
            buildCache.setId(id);
            buildCache.setUserModel(userModel);
            buildCache.setDelay(delay1);
            buildCache.setBuildRemark(buildRemark);
            buildCache.setParametersEnv(parametersEnv);
            //
            Queue<BuildCache> buildCaches = waitQueue.computeIfAbsent(id, s -> new ConcurrentLinkedDeque<>());
            buildCaches.add(buildCache);
            return ApiResult.success("提交任务队列成功,当前队列数：" + buildCaches.size());
        }

        BaseServerController.resetInfo(userModel);
        return buildExecuteService.start(id, userModel, delay1, 1, buildRemark, parametersEnv);
    }

    /**
     * 构建触发器
     * <p>
     * 参数 <code>[
     * {
     * "id":"1",
     * "token":"a",
     * "delay":"0"
     * }
     * ]</code>
     * <p>
     * 响应 <code>[
     * {
     * "id":"1",
     * "token":"a",
     * "delay":"0",
     * "msg":"开始构建",
     * "data":1
     * }
     * ]</code>
     *
     * @return json
     */
    @PostMapping(value = ServerOpenApi.BUILD_TRIGGER_BUILD_BATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<Object>> triggerBatch(HttpServletRequest request) {
        String body = JakartaServletUtil.getBody(request);
        if ((body == null || body.isEmpty())) {
            return new ApiResult<>(405, "请传入 body 参数");
        }
        try {
            // 构建外部参数
            Object[] parametersEnv = this.buildParametersEnv(request, body);
            JSONArray jsonArray = JSONArray.parseArray(body);
            List<Object> collect = jsonArray.stream().peek(o -> {
                JSONObject jsonObject = (JSONObject) o;
                String id = jsonObject.getString("id");
                String token = jsonObject.getString("token");
                Integer delay = jsonObject.getInteger("delay");
                String buildRemark = jsonObject.getString("buildRemark");
                String useQueue = jsonObject.getString("useQueue");
                BuildInfoModel item = buildInfoService.getByKey(id);
                if (item == null) {
                    String value = "没有对应数据";
                    jsonObject.put("msg", value);
                    return;
                }
                UserModel userModel = triggerTokenLogServer.getUserByToken(token, buildInfoService.typeName());
                if (userModel == null) {
                    String value = "对应的用户不存在,触发器已失效";
                    jsonObject.put("msg", value);
                    return;
                }
                //
                if (!java.util.Objects.equals(token, item.getTriggerToken())) {
                    String value = "触发token错误,或者已经失效";
                    jsonObject.put("msg", value);
                    return;
                }
                // 更新字段
                String updateItemErrorMsg = this.updateItem(jsonObject);
                if (updateItemErrorMsg != null) {
                    jsonObject.put("msg", updateItemErrorMsg);
                    return;
                }
                if (ConvertUtil.toBool(useQueue, false)) {
                    // 提交到队列暂存
                    BuildCache buildCache = new BuildCache();
                    buildCache.setId(id);
                    buildCache.setUserModel(userModel);
                    buildCache.setDelay(delay);
                    buildCache.setBuildRemark(buildRemark);
                    buildCache.setParametersEnv(parametersEnv);
                    //
                    Queue<BuildCache> buildCaches = waitQueue.computeIfAbsent(id, s -> new ConcurrentLinkedDeque<>());
                    buildCaches.add(buildCache);
                    jsonObject.put("msg", "提交任务队列成功,当前队列数：" + buildCaches.size());
                } else {
                    BaseServerController.resetInfo(userModel);
                    //
                    ApiResult<Integer> start = buildExecuteService.start(id, userModel, delay, 1, buildRemark, parametersEnv);
                    jsonObject.put("msg", start.getMsg());
                    jsonObject.put("buildId", start.getData());
                }
            }).collect(Collectors.toList());
            return ApiResult.success("触发成功", collect);
        } catch (Exception e) {
            throw new Voyager1RuntimeException("构建触发批量触发异常", e);
            //log.error("构建触发批量触发异常", e);
            //return ApiResult.getString(500, "触发异常", e.getMessage());
        }
    }

    /**
     * 接收参数,修改
     *
     * @param jsonObject 参数
     * @return 错误消息
     */
    private String updateItem(JSONObject jsonObject) {
        String id = jsonObject.getString("id");
        String branchName = jsonObject.getString("branchName");
        String branchTagName = jsonObject.getString("branchTagName");
        String script = jsonObject.getString("script");
        String resultDirFile = jsonObject.getString("resultDirFile");
        String webhook = jsonObject.getString("webhook");
        //
        BuildInfoModel item = new BuildInfoModel();
        if ((branchName != null && !branchName.isEmpty())) {
            item.setBranchName(branchName);
        }
        if ((branchTagName != null && !branchTagName.isEmpty())) {
            item.setBranchTagName(branchTagName);
        }
        if ((script != null && !script.isEmpty())) {
            item.setScript(script);
        }
        if ((resultDirFile != null && !resultDirFile.isEmpty())) {
            ResultDirFileAction parse = ResultDirFileAction.parse(resultDirFile);
            parse.check();
            item.setResultDirFile(resultDirFile);
        }
        if ((webhook != null && !webhook.isEmpty())) {
            if (!Validator.isMatchRegex(RegexPool.URL_HTTP, webhook)) {
                return "WebHooks 地址不合法";
            }
            item.setWebhook(webhook);
        }
        if (ObjectUtil.isNotEmpty(item)) {
            item.setId(id);
            buildInfoService.updateById(item);
        }
        return null;
    }


    /**
     * 批量获取构建状态
     *
     * @return json
     */
    @GetMapping(value = ServerOpenApi.BUILD_TRIGGER_STATUS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> buildStatusGet(@ValidatorItem String id, @ValidatorItem String token) {
        JSONObject statusData = this.getStatusData(id, token);
        return ApiResult.success("", statusData);
    }

    /**
     * 批量获取构建状态
     */
    @GetMapping(value = ServerOpenApi.BUILD_TRIGGER_LOG, produces = MediaType.APPLICATION_JSON_VALUE)
    public void buildLogGet(@ValidatorItem String id,
                            @ValidatorItem String token,
                            @ValidatorItem(ValidatorRule.NUMBERS) Integer buildNumId,
                            HttpServletResponse response) throws IOException {
        BuildInfoModel item = buildInfoService.getByKey(id);
        if (item == null) {
            JakartaServletUtil.write(response, "没有对应数据", ContentType.TEXT_PLAIN.getValue());
            return;
        }
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, buildInfoService.typeName());
        if (userModel == null) {
            JakartaServletUtil.write(response, "对应的用户不存在,触发器已失效", ContentType.TEXT_PLAIN.getValue());
            return;
        }
        //
        if (!java.util.Objects.equals(token, item.getTriggerToken())) {
            JakartaServletUtil.write(response, "触发token错误,或者已经失效", ContentType.TEXT_PLAIN.getValue());
            return;
        }
        File file = BuildUtil.getLogFile(item.getId(), buildNumId);
        if (!FileUtil.isFile(file)) {
            JakartaServletUtil.write(response, "日志文件错误", ContentType.TEXT_PLAIN.getValue());
            return;
        }
        try (BufferedInputStream inputStream = FileUtil.getInputStream(file)) {
            JakartaServletUtil.write(response, inputStream, ContentType.TEXT_PLAIN.getValue());
        }
    }

    /**
     * 批量获取构建状态
     *
     * @return json
     */
    @PostMapping(value = ServerOpenApi.BUILD_TRIGGER_STATUS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<JSONObject>> buildStatusPost(HttpServletRequest request) {
        try {
            String body = JakartaServletUtil.getBody(request);
            JSONArray jsonArray = JSONArray.parseArray(body);
            List<JSONObject> collect = jsonArray.stream().map(o -> {
                JSONObject data = (JSONObject) o;
                String id = data.getString("id");
                String token = data.getString("token");
                return this.getStatusData(id, token);
            }).collect(Collectors.toList());
            return ApiResult.success("", collect);
        } catch (Exception e) {
            log.error("获取构建状态异常", e);
            return new ApiResult<>(500, "发生异常" + e.getMessage());
        }
    }

    private JSONObject getStatusData(String id, String token) {
        JSONObject jsonObject = new JSONObject();
        BuildInfoModel item = buildInfoService.getByKey(id);
        if (item == null) {
            String value = "没有对应数据";
            jsonObject.put("msg", value);
            return jsonObject;
        }
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, buildInfoService.typeName());
        if (userModel == null) {
            String value = "对应的用户不存在,触发器已失效";
            jsonObject.put("msg", value);
            return jsonObject;
        }
        //
        if (!java.util.Objects.equals(token, item.getTriggerToken())) {
            String value = "触发token错误,或者已经失效";
            jsonObject.put("msg", value);
            return jsonObject;
        }
        // 更新字段
        Integer status = item.getStatus();
        BuildStatus buildStatus = BaseEnum.getEnum(BuildStatus.class, status);
        if (buildStatus == null) {
            jsonObject.put("msg", "status code error");
        } else {
            jsonObject.put("msg", buildStatus.getDesc());
            jsonObject.put("statusCode", buildStatus.getCode());
            jsonObject.put("status", buildStatus.name());
        }
        jsonObject.put("buildNumberId", item.getBuildId());
        return jsonObject;
    }

    @Data
    private static class BuildCache {
        private UserModel userModel;
        // 构建外部参数
        private Object[] parametersEnv;

        private Integer delay;

        private String buildRemark;
        private String id;

        private Long taskTime;

        public BuildCache() {
            this.taskTime = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return JSONObject.toJSONString(this);
        }
    }

    @Override
    public void startLoad() {
        ScheduledExecutorService scheduler = Voyager1Application.getScheduledExecutorService();
        scheduler.scheduleWithFixedDelay(this, 0, 5, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        String id = "build_trigger_queue";
        int heartSecond = 5;
        try {
            CronUtils.TaskStat taskStat = CronUtils.getTaskStat(id, String.format("%s 秒执行一次", heartSecond));
            taskStat.onStart();
            //
            this.runQueue();
            taskStat.onSucceeded();
        } catch (Throwable throwable) {
            CronUtils.TaskStat taskStat = CronUtils.getTaskStat(id, String.format("%s 秒执行一次", heartSecond));
            taskStat.onFailed(id, throwable);
        }
    }

    private void runQueue() {
        // 先删除空队列
        Set<Map.Entry<String, Queue<BuildCache>>> entries = waitQueue.entrySet();
        Iterator<Map.Entry<String, Queue<BuildCache>>> entryIterator = entries.iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<String, Queue<BuildCache>> next = entryIterator.next();
            Queue<BuildCache> queue = next.getValue();
            if (queue.isEmpty()) {
                entryIterator.remove();
            }
        }
        int size = waitQueue.size();
        if (size > 0) {
            log.debug("需要处理构建微队列数：{}", size);
            // 遍历队列中的数据
            waitQueue.forEach((buildId, buildCaches) -> {
                synchronized (buildId.intern()) {
                    log.debug("需要处理的 {} 构建队列数：{}", buildId, buildCaches.size());
                    BuildInfoModel item = buildInfoService.getByKey(buildId);
                    if (item == null) {
                        log.error("构建数据不存在：{},任务自动丢弃:{}", buildId, buildCaches.poll());
                        return;
                    }
                    String statusMsg = buildExecuteService.checkStatus(item);
                    if (statusMsg != null) {
                        log.debug("构建任务继续等待:{} {}", buildId, statusMsg);
                        return;
                    }
                    BuildCache cache = buildCaches.poll();
                    if (cache == null) {
                        return;
                    }
                    try {
                        BaseServerController.resetInfo(cache.userModel);
                        ApiResult<Integer> message = buildExecuteService.start(cache.id, cache.userModel, cache.delay, 1, cache.buildRemark, cache.parametersEnv);
                        log.info("构建触发器队列执行结果：{}", message);
                    } catch (Exception e) {
                        log.error("创建构建任务异常", e);
                        // 重新添加任务
                        buildCaches.add(cache);
                    }
                }
            });
        }
    }
}
