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

package io.voyager1.func.system.controller;

import io.voyager1.util.CacheObj;
import io.voyager1.util.LFUCache;
import io.voyager1.util.CollUtil;
import io.voyager1.util.DateTime;
import io.voyager1.util.FileUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.event.ICacheTask;
import io.voyager1.event.ISystemTask;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.build.BuildExecuteManage;
import io.voyager1.build.BuildUtil;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Voyager1Manifest;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.i18n.I18nThreadUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.configuration.ClusterConfig;
import io.voyager1.configuration.SystemConfig;
import io.voyager1.controller.LoginControl;
import io.voyager1.cron.CronUtils;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.socket.ServiceFileTailWatcher;
import io.voyager1.system.ServerConfig;
import io.voyager1.system.db.DataInitEvent;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.SyncFinisherUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * 缓存管理
 *
 * @since 2019/7/20
 */
@RestController
@RequestMapping(value = "system")
@Feature(cls = ClassFeature.SYSTEM_CACHE)
@SystemPermission
@Slf4j
public class CacheManageController extends BaseServerController implements ICacheTask, ISystemTask {

    private long dataSize;
    private long oldJarsSize;
    private long tempFileSize;

    private final Voyager1Application voyager1Application;
    private final DataInitEvent dataInitEvent;
    private final ClusterConfig clusterConfig;
    private final SystemConfig systemConfig;
    /**
     * 标记是否正在刷新缓存
     */
    private boolean refreshCacheIng = false;

    public CacheManageController(Voyager1Application voyager1Application,
                                 DataInitEvent dataInitEvent,
                                 ServerConfig serverConfig) {
        this.voyager1Application = voyager1Application;
        this.dataInitEvent = dataInitEvent;
        this.clusterConfig = serverConfig.getCluster();
        this.systemConfig = serverConfig.getSystem();
    }

    /**
     * get server's cache data
     * 获取 Server 的缓存数据
     *
     * @return json
     */
    @PostMapping(value = "server-cache", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<Map<String, Object>> serverCache() {
        Map<String, Object> map = new HashMap<>(10);
        map.put("cacheFileSize", this.tempFileSize);
        map.put("dataSize", this.dataSize);
        map.put("oldJarsSize", this.oldJarsSize);
        {
            LFUCache<String, Integer> lfuCache = LoginControl.LFU_CACHE;
            List<CacheObj<String, Integer>> list = new java.util.ArrayList<>();
            lfuCache.cacheObjIterator().forEachRemaining(list::add);
            map.put("errorIp", list);
        }
        int oneLineCount = ServiceFileTailWatcher.getOneLineCount();
        map.put("readFileOnLineCount", oneLineCount);
        map.put("cacheBuildFileSize", BuildUtil.buildCacheSize);
        map.put("taskList", CronUtils.list());
        map.put("pluginSize", PluginFactory.size());
        map.put("shardingSize", BaseServerController.SHARDING_IDS.size());
        map.put("buildKeys", BuildExecuteManage.buildKeys());
        map.put("syncFinisKeys", SyncFinisherUtil.keys());
        map.put("dateTime", DateTime.now().toString());
        map.put("timeZoneId", TimeZone.getDefault().getID());
        map.put("errorWorkspace", dataInitEvent.getErrorWorkspaceTable());
        map.put("clusterId", clusterConfig.getId());
        Voyager1Manifest voyager1Manifest = Voyager1Manifest.getInstance();
        map.put("installId", voyager1Manifest.getInstallId());
        map.put("tempPath", voyager1Application.getTempPath().getAbsolutePath());
        map.put("dataPath", voyager1Application.getDataPath());
        map.put("buildPath", BuildUtil.getBuildDataDir());
        map.put("timerMatchSecond", systemConfig.isTimerMatchSecond());
        //
        return ApiResult.success("", map);
    }

    /**
     * 获取节点中的缓存
     *
     * @return json
     */
    @RequestMapping(value = "node_cache.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> nodeCache(HttpServletRequest request, @ValidatorItem String machineId) {
        return this.tryRequestMachine(machineId, request, NodeUrl.Cache);

//        return Optional.ofNullable(message).orElseGet(() -> {
//            List<JSONObject> data = DirTreeUtil.getTreeData(LogbackConfig.getPath());
//            return ApiResult.success("", data);
//        });
//        return NodeForward.request(getNode(), request, NodeUrl.Cache).toString();
    }

    /**
     * 清空缓存
     *
     * @param type 类型
     * @return json
     */
    @RequestMapping(value = "clearCache.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> clearCache(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "类型错误") String type,
                                           String machineId,
                                           HttpServletRequest request) {
        switch (type) {
            case "serviceCacheFileSize": {
                File tempPath = Voyager1Application.getInstance().getTempPath();
                boolean clean = CommandUtil.systemFastDel(tempPath);
                Assert.state(!clean, "清空文件缓存失败");
                break;
            }
            case "serviceIpSize":
                LoginControl.LFU_CACHE.clear();
                break;
            case "serviceOldJarsSize": {
                File oldJarsPath = Voyager1Manifest.getOldJarsPath();
                boolean clean = CommandUtil.systemFastDel(oldJarsPath);
                Assert.state(!clean, "清空旧版本重新包失败");
                break;
            }
            default:
                return this.tryRequestMachine(machineId, request, NodeUrl.ClearCache);
        }
        return ApiResult.success("清空成功");
    }

    /**
     * 清理错误的工作空间数据
     *
     * @param tableName 类型
     * @return json
     */
    @GetMapping(value = "clear-error-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> clearErrorWorkspace(@ValidatorItem String tableName) {
        dataInitEvent.clearErrorWorkspace(tableName);
        return ApiResult.success("清理成功");
    }

    @GetMapping(value = "async-refresh-cache", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> refresh() {
        Assert.state(!this.refreshCacheIng, "正在刷新缓存中,请勿重复刷新");
        I18nThreadUtil.execute(() -> {
            try {
                this.refreshCacheIng = true;
                this.executeTask();
            } catch (Exception e) {
                log.error("手动刷新缓存异常", e);
            } finally {
                this.refreshCacheIng = false;
            }
        });
        return ApiResult.success("异步刷新中请稍后刷新页面查看");
    }

    @Override
    public void refreshCache() {
        File file = voyager1Application.getTempPath();
        this.tempFileSize = FileUtil.size(file);
        File oldJarsPath = Voyager1Manifest.getOldJarsPath();
        this.oldJarsSize = FileUtil.size(oldJarsPath);
    }

    @Override
    public void executeTask() {
        this.dataSize = voyager1Application.dataSize();
        BuildUtil.reloadCacheSize();
    }
}
