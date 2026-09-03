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

package io.voyager1.func.system.service;

import io.voyager1.Voyager1Application;
import io.voyager1.common.ServerConst;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.common.Voyager1Manifest;
import io.voyager1.configuration.ClusterConfig;
import io.voyager1.core.api.ApiResult;
import io.voyager1.core.entity.ClusterInfoEntity;
import io.voyager1.core.jpa.DataService;
import io.voyager1.core.jpa.JpaQuerySupport;
import io.voyager1.model.PageResultDto;
import io.voyager1.util.JakartaServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import io.voyager1.core.repository.ClusterInfoRepository;
import io.voyager1.cron.CronUtils;
import io.voyager1.event.IAsyncLoad;
import io.voyager1.func.assets.server.MachineDockerServer;
import io.voyager1.func.assets.server.MachineNodeServer;
import io.voyager1.func.assets.server.MachineSshServer;
import io.voyager1.func.system.model.ClusterInfoModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.system.WorkspaceService;
import io.voyager1.system.ServerConfig;
import io.voyager1.util.NetUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 集群信息服务。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA 仓库（ClusterInfoRepository），对外契约不变。
 *
 * @since 2023/8/19
 */
@Service
@Slf4j
public class ClusterInfoService implements DataService<ClusterInfoModel>, IAsyncLoad, Runnable {

    private final ClusterInfoRepository repository;
    private final ClusterConfig clusterConfig;
    private static final String TASK_ID = "system_monitor_cluster";

    private final WorkspaceService workspaceService;
    private boolean multiServer = false;

    public ClusterInfoService(ClusterInfoRepository repository, ServerConfig serverConfig, WorkspaceService workspaceService) {
        this.repository = repository;
        this.clusterConfig = serverConfig.getCluster();
        this.workspaceService = workspaceService;
    }

    @Override
    public ClusterInfoModel getByKey(String id) {
        ClusterInfoEntity entity = repository.findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    @Transactional
    public void insert(ClusterInfoModel model) {
        long now = System.currentTimeMillis();
        ClusterInfoEntity entity = new ClusterInfoEntity();
        entity.setId(model.getId() == null || model.getId().isEmpty() ? java.util.UUID.randomUUID().toString() : model.getId());
        entity.setCreateTimeMillis(now);
        entity.setModifyTimeMillis(now);
        copyFields(model, entity);
        repository.save(entity);
        model.setId(entity.getId());
    }

    @Transactional
    public void updateById(ClusterInfoModel model) {
        ClusterInfoEntity entity = repository.findById(model.getId()).orElse(null);
        if (entity == null) {
            return;
        }
        entity.setModifyTimeMillis(System.currentTimeMillis());
        copyFields(model, entity);
        repository.save(entity);
    }

    @Transactional
    public void delByKey(String id) {
        repository.deleteById(id);
    }

    public List<ClusterInfoModel> list() {
        return repository.findAll().stream().map(this::toModel).collect(Collectors.toList());
    }

    public PageResultDto<ClusterInfoModel> listPage(HttpServletRequest request) {
        return this.listPage(JakartaServletUtil.getParamMap(request));
    }

    public PageResultDto<ClusterInfoModel> listPage(java.util.Map<String, String> paramMap) {
        Page<ClusterInfoEntity> page = repository.findAll(
            JpaQuerySupport.specification(paramMap), JpaQuerySupport.pageable(paramMap));
        List<ClusterInfoModel> result = page.getContent().stream().map(this::toModel).collect(Collectors.toList());
        return JpaQuerySupport.toPageResult(page, result);
    }

    public long count() {
        return repository.count();
    }

    public ClusterInfoModel getCurrent() {
        ClusterInfoModel clusterInfoModel = this.getByKey(Voyager1Manifest.getInstance().getInstallId());
        Assert.notNull(clusterInfoModel, "当前集群不存在");
        return clusterInfoModel;
    }

    public boolean isMultiServer() {
        return multiServer;
    }

    @Override
    public void startLoad() {
        int heartSecond = clusterConfig.getHeartSecond();
        ScheduledExecutorService scheduler = Voyager1Application.getScheduledExecutorService();
        scheduler.scheduleWithFixedDelay(this, 0, heartSecond, TimeUnit.SECONDS);
        this.multiServer = this.count() > 1;
    }

    @Override
    public void run() {
        int heartSecond = clusterConfig.getHeartSecond();
        try {
            CronUtils.TaskStat taskStat = CronUtils.getTaskStat(TASK_ID, String.format("%s 秒执行一次", heartSecond));
            taskStat.onStart();
            this.multiServer = this.count() > 1;
            Voyager1Manifest voyager1Manifest = Voyager1Manifest.getInstance();
            String installId = voyager1Manifest.getInstallId();
            ClusterInfoModel byKey = this.getByKey(installId);
            if (byKey == null) {
                this.insert(this.createDefault(installId));
                this.bindDefault(installId);
                return;
            }
            ClusterInfoModel clusterInfoModel = new ClusterInfoModel();
            clusterInfoModel.setId(byKey.getId());
            if (!java.util.Objects.equals(byKey.getClusterId(), clusterConfig.getId())) {
                log.warn("集群ID 发生变化：{} -> {}", byKey.getClusterId(), clusterConfig.getId());
                clusterInfoModel.setClusterId(clusterConfig.getId());
            }
            clusterInfoModel.setLocalHostName(NetUtil.getLocalHostName());
            clusterInfoModel.setLastHeartbeat(System.currentTimeMillis());
            clusterInfoModel.setVoyager1Version(voyager1Manifest.getVersion());
            try {
                String url = byKey.getUrl();
                if ((url == null || url.isEmpty())) {
                    clusterInfoModel.setStatusMsg("未配置地址");
                } else {
                    this.testUrl(url);
                    clusterInfoModel.setStatusMsg("OK");
                }
            } catch (Exception e) {
                clusterInfoModel.setStatusMsg(e.getMessage());
            }
            this.updateById(clusterInfoModel);
            List<ClusterInfoEntity> clusterInfoModels = repository.findByClusterIdAndIdNot(clusterConfig.getId(), installId);
            for (ClusterInfoEntity infoModel : clusterInfoModels) {
                log.error("{} 集群ID冲突：{} {}", clusterConfig.getId(), infoModel.getId(), infoModel.getName());
            }
            taskStat.onSucceeded();
        } catch (Throwable throwable) {
            CronUtils.TaskStat taskStat = CronUtils.getTaskStat(TASK_ID, String.format("%s 秒执行一次", heartSecond));
            taskStat.onFailed(TASK_ID, throwable);
        }
    }

    private void testUrl(String url) {
        io.voyager1.util.UrlBuilder urlBuilder = io.voyager1.util.UrlBuilder.ofHttp(url);
        urlBuilder.addPath(ServerConst.CHECK_SYSTEM);
        String fullUrl = urlBuilder.build();
        try {
            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(fullUrl))
                .timeout(java.time.Duration.ofSeconds(30))
                .GET()
                .build();
            java.net.http.HttpResponse<String> httpResponse = java.net.http.HttpClient.newHttpClient()
                .send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
            JSONObject jsonObject = JSONObject.parseObject(httpResponse.body());
            int code = jsonObject.getIntValue(ApiResult.CODE);
            Assert.state(code == ApiResult.DEFAULT_SUCCESS_CODE, () -> {
                String msg = jsonObject.getString(ApiResult.MSG);
                msg = (msg == null || msg.isEmpty()) ? jsonObject.toString() : msg;
                return String.format("集群状态码异常：%s %s", code, msg);
            });
            JSONObject data = jsonObject.getJSONObject("data");
            Assert.notNull(data, "集群响应信息不正确,请确认集群地址是正确的服务端地址");
            boolean expression = data.containsKey("routerBase") && data.containsKey("extendPlugins");
            Assert.state(expression, "填写的集群地址不正确");
        } catch (Exception e) {
            log.error("检查集群信息异常", e);
            throw new IllegalArgumentException("填写的集群地址检查异常,请确认集群地址是正确的服务端地址," + e.getMessage());
        }
    }

    private void bindDefault(String installId) {
        long count = this.count();
        if (count != 1) {
            log.debug("系统中存在多个集群,不需要自动绑定数据");
            return;
        }
        String sql = "update " + workspaceService.getTableName() + " set clusterInfoId=?";
        workspaceService.execute(sql, installId);
        List<String> list = this.listLinkGroups();
        String join = String.join(",", list);
        ClusterInfoModel clusterInfoModel = new ClusterInfoModel();
        clusterInfoModel.setId(installId);
        clusterInfoModel.setLinkGroup(join);
        this.updateById(clusterInfoModel);
    }

    public List<String> listLinkGroups() {
        MachineDockerServer machineDockerServer = SpringContextHolder.getBean(MachineDockerServer.class);
        MachineNodeServer machineNodeServer = SpringContextHolder.getBean(MachineNodeServer.class);
        MachineSshServer machineSshServer = SpringContextHolder.getBean(MachineSshServer.class);
        List<String> nodeGroup = machineNodeServer.listGroupName();
        List<String> sshGroup = machineSshServer.listGroupName();
        List<String> dockerGroup = machineDockerServer.listGroupName();
        List<String> all = new ArrayList<>();
        all.addAll(nodeGroup);
        all.addAll(sshGroup);
        all.addAll(dockerGroup);
        return all.stream().distinct().collect(Collectors.toList());
    }

    private ClusterInfoModel createDefault(String installId) {
        ClusterInfoModel clusterInfoModel = new ClusterInfoModel();
        clusterInfoModel.setId(installId);
        clusterInfoModel.setName("默认集群");
        clusterInfoModel.setCreateUser(UserModel.SYSTEM_ADMIN);
        clusterInfoModel.setClusterId(clusterConfig.getId());
        clusterInfoModel.setLastHeartbeat(System.currentTimeMillis());
        return clusterInfoModel;
    }

    public boolean online(ClusterInfoModel clusterInfoModel) {
        if (clusterInfoModel == null) {
            return false;
        }
        Long lastHeartbeat = clusterInfoModel.getLastHeartbeat();
        if (lastHeartbeat == null) {
            return false;
        }
        long millis = TimeUnit.SECONDS.toMillis(clusterConfig.getHeartSecond());
        return lastHeartbeat > System.currentTimeMillis() - millis;
    }

    private void copyFields(ClusterInfoModel model, ClusterInfoEntity entity) {
        // 仅覆盖非空字段（心跳更新是部分字段，空字段保留实体原值）
        if (model.getModifyUser() != null) {
            entity.setModifyUser(model.getModifyUser());
        }
        if (model.getName() != null) {
            entity.setName(model.getName());
        }
        if (model.getClusterId() != null) {
            entity.setClusterId(model.getClusterId());
        }
        if (model.getUrl() != null) {
            entity.setUrl(model.getUrl());
        }
        if (model.getLinkGroup() != null) {
            entity.setLinkGroup(model.getLinkGroup());
        }
        if (model.getLastHeartbeat() != null) {
            entity.setLastHeartbeat(model.getLastHeartbeat());
        }
        if (model.getLocalHostName() != null) {
            entity.setLocalHostName(model.getLocalHostName());
        }
        if (model.getVoyager1Version() != null) {
            entity.setVoyager1Version(model.getVoyager1Version());
        }
        if (model.getStatusMsg() != null) {
            entity.setStatusMsg(model.getStatusMsg());
        }
    }

    private ClusterInfoModel toModel(ClusterInfoEntity entity) {
        ClusterInfoModel model = new ClusterInfoModel();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        model.setModifyUser(entity.getModifyUser());
        model.setName(entity.getName());
        model.setClusterId(entity.getClusterId());
        model.setUrl(entity.getUrl());
        model.setLinkGroup(entity.getLinkGroup());
        model.setLastHeartbeat(entity.getLastHeartbeat());
        model.setLocalHostName(entity.getLocalHostName());
        model.setVoyager1Version(entity.getVoyager1Version());
        model.setStatusMsg(entity.getStatusMsg());
        return model;
    }
}
