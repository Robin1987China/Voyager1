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

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollStreamUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.CompareUtil;
import io.voyager1.util.DateUtil;
import io.voyager1.util.ExceptionUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.Task;
import io.voyager1.core.db.Entity;
import io.voyager1.event.IAsyncLoad;
import io.voyager1.plugin.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.Const;
import io.voyager1.common.ILoadEvent;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.AssetsConfig;
import io.voyager1.cron.CronUtils;
import io.voyager1.core.entity.MachineDockerEntity;
import io.voyager1.core.jpa.JpaBaseService;
import io.voyager1.core.repository.MachineDockerRepository;
import io.voyager1.func.assets.AssetsExecutorPoolService;
import io.voyager1.func.assets.model.MachineDockerModel;
import io.voyager1.func.assets.model.MachineSshModel;
import io.voyager1.func.cert.service.CertificateInfoService;
import io.voyager1.func.system.model.ClusterInfoModel;
import io.voyager1.func.system.service.ClusterInfoService;
import io.voyager1.model.docker.DockerInfoModel;
import io.voyager1.model.docker.DockerSwarmInfoMode;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.docker.DockerInfoService;
import io.voyager1.service.docker.DockerSwarmInfoService;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import jakarta.annotation.Resource;
import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.function.Supplier;

/**
 * @since 2023/3/3
 */
@Service
@Slf4j
public class MachineDockerServer extends JpaBaseService<MachineDockerModel, MachineDockerEntity> implements ILoadEvent, IAsyncLoad, Task {
    private static final String CRON_ID = "docker-monitor";
    private final MachineSshServer machineSshServer;
    private final DockerInfoService dockerInfoService;
    private final DockerSwarmInfoService dockerSwarmInfoService;
    private final ClusterInfoService clusterInfoService;
    private final AssetsConfig.DockerConfig dockerConfig;
    private final AssetsExecutorPoolService assetsExecutorPoolService;
    private final MachineDockerRepository machineDockerRepository;
    @Resource
    @Lazy
    private CertificateInfoService certificateInfoService;


    public MachineDockerServer(MachineSshServer machineSshServer,
                               DockerInfoService dockerInfoService,
                               DockerSwarmInfoService dockerSwarmInfoService,
                               ClusterInfoService clusterInfoService,
                               AssetsConfig assetsConfig,
                               AssetsExecutorPoolService assetsExecutorPoolService,
                               MachineDockerRepository machineDockerRepository) {
        this.machineSshServer = machineSshServer;
        this.dockerInfoService = dockerInfoService;
        this.dockerSwarmInfoService = dockerSwarmInfoService;
        this.clusterInfoService = clusterInfoService;
        this.dockerConfig = assetsConfig.getDocker();
        this.assetsExecutorPoolService = assetsExecutorPoolService;
        this.machineDockerRepository = machineDockerRepository;
    }

    @Override
    protected JpaRepository<MachineDockerEntity, String> repository() {
        return machineDockerRepository;
    }

    @Override
    protected JpaSpecificationExecutor<MachineDockerEntity> specExecutor() {
        return machineDockerRepository;
    }

    @Override
    protected Class<MachineDockerEntity> entityClass() {
        return MachineDockerEntity.class;
    }

    @Override
    protected Class<MachineDockerModel> modelClass() {
        return MachineDockerModel.class;
    }

    @Override
    protected void fillInsert(MachineDockerModel machineDockerModel) {
        super.fillInsert(machineDockerModel);
        machineDockerModel.setGroupName((machineDockerModel.getGroupName() == null || machineDockerModel.getGroupName().isEmpty() ? Const.DEFAULT_GROUP_NAME.get() : machineDockerModel.getGroupName()));
        machineDockerModel.setStatus((machineDockerModel.getStatus() != null ? machineDockerModel.getStatus() : 0));
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        long count = this.count();
        if (count != 0) {
            log.debug("机器 DOCKER 表已经存在 {} 条数据，不需要修复机器 DOCKER 数据", count);
            return;
        }
        List<DockerInfoModel> list = dockerInfoService.list(false);
        if ((list == null || list.isEmpty())) {
            log.debug("没有任何 DOCKER 信息,不需要修复机器 DOCKER 数据");
            return;
        }
        Map<String, List<DockerInfoModel>> map = CollStreamUtil.groupByKey(list, DockerInfoModel::getHost);
        List<MachineDockerModel> models = new ArrayList<>(map.size());
        for (Map.Entry<String, List<DockerInfoModel>> entry : map.entrySet()) {
            List<DockerInfoModel> value = entry.getValue();
            // 排序，最近更新过优先
            value.sort((o1, o2) -> CompareUtil.compare(o2.getModifyTimeMillis(), o1.getModifyTimeMillis()));
            DockerInfoModel first = (value == null || value.isEmpty() ? null : value.get(0));
            if (value.size() > 1) {
                log.warn("DOCKER 地址 {} 存在多个数据，将自动合并使用 {} DOCKER 的配置信息", entry.getKey(), first.getName());
            }
            models.add(this.dockerInfoToMachineDocker(first));
        }
        this.insert(models);
        log.info("成功修复 {} 条机器 DOCKER 数据", models.size());
        // 更新 docker 的机器id
        for (MachineDockerModel value : models) {
            Entity entity = Entity.create();
            entity.set("machineDockerId", value.getId());
            {
                //
                Entity where = Entity.create();
                where.set("host", value.getHost());
                int update = dockerInfoService.update(entity, where);
                Assert.state(update > 0, "更新 DOCKER 表机器id 失败：" + value.getName());
            }
        }
    }


    private MachineDockerModel dockerInfoToMachineDocker(DockerInfoModel dockerInfoModel) {
        MachineDockerModel machineDockerModel = new MachineDockerModel();
        machineDockerModel.setName(dockerInfoModel.getName());
        machineDockerModel.setHost(dockerInfoModel.getHost());
        machineDockerModel.setTlsVerify(dockerInfoModel.getTlsVerify());
        machineDockerModel.setHeartbeatTimeout(dockerInfoModel.getHeartbeatTimeout());
        //
        machineDockerModel.setSwarmNodeId(dockerInfoModel.getSwarmNodeId());
        machineDockerModel.setSwarmId(dockerInfoModel.getSwarmId());
        //
        machineDockerModel.setRegistryEmail(dockerInfoModel.getRegistryEmail());
        machineDockerModel.setRegistryUrl(dockerInfoModel.getRegistryUrl());
        machineDockerModel.setRegistryUsername(dockerInfoModel.getRegistryUsername());
        machineDockerModel.setRegistryPassword(dockerInfoModel.getRegistryPassword());
        return machineDockerModel;
    }

    @Override
    public void startLoad() {
        String monitorCron = dockerConfig.getMonitorCron();
        String cron = (monitorCron == null || monitorCron.isEmpty() ? "0 0/1 * * * ?" : monitorCron);
        CronUtils.add(CRON_ID, cron, () -> MachineDockerServer.this);
    }

    @Override
    public void execute() {
        Entity entity = new Entity();
        if (clusterInfoService.isMultiServer()) {
            // 查询对应分组的数据
            ClusterInfoModel current = clusterInfoService.getCurrent();
            String linkGroup = current.getLinkGroup();
            List<String> linkGroups = io.voyager1.util.ConvertUtil.splitTrim(linkGroup, ",");
            if ((linkGroups == null || linkGroups.isEmpty())) {
                log.warn("当前集群还未绑定分组,不能监控 Docker 资产信息");
                return;
            }
            entity.set("groupName", linkGroups);
        }
        List<MachineDockerModel> list = this.listByEntity(entity, false);
        if ((list == null || list.isEmpty())) {
            return;
        }
        this.checkList(list);
    }

    private void checkList(List<MachineDockerModel> monitorModels) {
        monitorModels.forEach(monitorModel -> assetsExecutorPoolService.execute(() -> this.updateMonitor(monitorModel)));
    }

    /**
     * 监控 容器
     *
     * @param dockerInfoModel docker
     */
    public boolean updateMonitor(MachineDockerModel dockerInfoModel) {
        try {
            DockerInfoModel model = new DockerInfoModel();
            model.setMachineDockerId(dockerInfoModel.getId());
            IPlugin pluginCheck = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
            Map<String, Object> parameter = this.toParameter(dockerInfoModel);
            //
            JSONObject info = pluginCheck.execute("info", parameter, JSONObject.class);
            //
            MachineDockerModel update = new MachineDockerModel();
            update.setId(dockerInfoModel.getId());
            update.setStatus(1);
            update.setLastHeartbeatTime(System.currentTimeMillis());
            //
            update.setDockerVersion(info.getString("serverVersion"));
            JSONObject swarm = info.getJSONObject("swarm");
            //
            IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
            JSONObject swarmData = null;
            try {
                if (dockerInfoModel.isControlAvailable()) {
                    swarmData = plugin.execute("inSpectSwarm", this.toParameter(dockerInfoModel), JSONObject.class);
                } else {
                    // 找到管理节点
                    MachineDockerModel managerDocker = this.getMachineDockerBySwarmId(dockerInfoModel.getSwarmId());
                    swarmData = plugin.execute("inSpectSwarm", this.toParameter(managerDocker), JSONObject.class);
                }
            } catch (Exception e) {
                log.debug("获取 {} docker 集群失败 {}", dockerInfoModel.getName(), e.getMessage());
            }
            Optional.ofNullable(swarmData).ifPresent(jsonObject -> {
                String swarmId = jsonObject.getString("id");
                update.setSwarmCreatedAt(DateUtil.parse(jsonObject.getString("createdAt")).getTime());
                update.setSwarmUpdatedAt(DateUtil.parse(jsonObject.getString("updatedAt")).getTime());
                update.setSwarmId(swarmId);
            });
            Optional.ofNullable(swarm).ifPresent(jsonObject -> {
                String nodeId = jsonObject.getString("nodeID");
                String nodeAddr = jsonObject.getString("nodeAddr");
                boolean controlAvailable = jsonObject.getBooleanValue("controlAvailable");
                update.setSwarmControlAvailable(controlAvailable);
                update.setSwarmNodeAddr(nodeAddr);
                update.setSwarmNodeId(nodeId);
            });
            if ((update.getSwarmNodeId() == null || update.getSwarmNodeId().isEmpty())) {
                // 集群退出
                update.restSwarm();
            }
            update.setFailureMsg("");
            update.setCertExist(this.checkCertPath(dockerInfoModel));
            this.updateById(update);
            //
            return true;
        } catch (Exception e) {
            String message = e.getMessage();
            if (ExceptionUtil.isCausedBy(e, NoSuchFileException.class)) {
                log.error("监控 docker[{}] 异常 {}", dockerInfoModel.getName(), message);
            } else if ((message != null && message.toLowerCase().contains("Connection timed out".toLowerCase()))) {
                log.error("监控 docker[{}] 超时 {}", dockerInfoModel.getName(), message);
            } else {
                log.error("监控 docker[{}] 异常", dockerInfoModel.getName(), e);
            }
            this.updateStatus(dockerInfoModel.getId(), 0, message);
            return false;
        }
    }

    /**
     * 验证 证书文件是否存在
     *
     * @param dockerInfoModel docker
     * @return true 证书文件存在
     */
    private boolean checkCertPath(MachineDockerModel dockerInfoModel) {
        try {
            File filePath = certificateInfoService.getFilePath(dockerInfoModel.getCertInfo());
            if (filePath == null) {
                return false;
            }
            IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
            return (boolean) plugin.execute("certPath", "certPath", filePath.getAbsolutePath());
        } catch (Exception e) {
            log.warn("检查 docker 证书异常 {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证 证书文件是否存在
     *
     * @param path 路径
     * @return true 证书文件存在
     */
    public boolean checkCertPath(String path) {
        try {
            IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
            return (boolean) plugin.execute("certPath", "certPath", path);
        } catch (Exception e) {
            log.warn("检查 docker 证书异常 {}", e.getMessage());
            return false;
        }
    }

    public void updateSwarmInfo(String id, JSONObject swarmData, JSONObject info) {
        //
        JSONObject swarm = info.getJSONObject("swarm");
        Assert.notNull(swarm, "集群信息不完整,不能操作");
        String nodeAddr = swarm.getString("nodeAddr");
        String nodeId = swarm.getString("nodeID");
        Assert.hasText(nodeAddr, "没有节点地址,不能继续操作");
        //
        Date createdAt = swarmData.getDate("createdAt");
        Date updatedAt = swarmData.getDate("updatedAt");
        String swarmId = swarmData.getString("id");
        //
        MachineDockerModel machineDockerModel = new MachineDockerModel();
        machineDockerModel.setSwarmUpdatedAt(createdAt.getTime());
        machineDockerModel.setSwarmUpdatedAt(updatedAt.getTime());
        machineDockerModel.setSwarmId(swarmId);
        machineDockerModel.setSwarmNodeAddr(nodeAddr);
        machineDockerModel.setSwarmNodeId(nodeId);
        boolean controlAvailable = swarm.getBooleanValue("controlAvailable");
        machineDockerModel.setSwarmControlAvailable(controlAvailable);
        machineDockerModel.setId(id);
        this.updateById(machineDockerModel);
    }

    /**
     * 更新 容器状态
     *
     * @param id     ID
     * @param status 状态值
     * @param msg    错误消息
     */
    private void updateStatus(String id, int status, String msg) {
        MachineDockerModel dockerInfoModel = new MachineDockerModel();
        dockerInfoModel.setId(id);
        dockerInfoModel.setStatus(status);
        dockerInfoModel.setFailureMsg(msg);
        this.updateById(dockerInfoModel);
    }

    public Map<String, Object> dockerParameter(DockerInfoModel dockerInfoModel) {
        String machineDockerId = dockerInfoModel.getMachineDockerId();
        MachineDockerModel machineDockerModel = this.getByKey(machineDockerId, false);
        Assert.notNull(machineDockerModel, "没有找到对应的 docker 信息");
        Integer status = machineDockerModel.getStatus();
        Assert.state(status != null && status == 1, String.format("当前 %s docker 不在线", machineDockerModel.getName()));
        return this.toParameter(machineDockerModel);
    }

    /**
     * 跟进 docker 列表找到一个可用的 docker 信息
     *
     * @param dockerInfoModels docker 列表
     * @return map
     */
    public Map<String, Object> dockerParameter(List<DockerInfoModel> dockerInfoModels) {
        for (DockerInfoModel dockerInfoModel : dockerInfoModels) {
            String machineDockerId = dockerInfoModel.getMachineDockerId();
            MachineDockerModel machineDockerModel = this.getByKey(machineDockerId, false);
            if (machineDockerModel != null) {
                Integer status = machineDockerModel.getStatus();
                if (status != null && status == 1) {
                    Map<String, Object> parameter = this.toParameter(machineDockerModel);
                    // 更新名称
                    parameter.put("name", dockerInfoModel.getName());
                    return parameter;
                }
            }
        }
        return null;
    }

    /**
     * 通过集群 id 获取 docker 管理参数
     *
     * @param workspaceSwarmId 集群id
     * @return map
     */
    public Map<String, Object> dockerParameter(String workspaceSwarmId) {
        MachineDockerModel first = this.getMachineDocker(workspaceSwarmId);
        Assert.notNull(first, "没有找到集群管理节点");
        Integer status = first.getStatus();
        Assert.state(status != null && status == 1, String.format("当前 %s docker 集群没有管理节点在线", first.getName()));
        return toParameter(first);
    }

    private MachineDockerModel getMachineDocker(String workspaceSwarmId) {
        DockerSwarmInfoMode swarmInfoMode = dockerSwarmInfoService.getByKey(workspaceSwarmId);
        Assert.notNull(swarmInfoMode, "没有找到对应的集群信息");
        String modeSwarmId = swarmInfoMode.getSwarmId();
        //
        return this.getMachineDockerBySwarmId(modeSwarmId);
    }

    public MachineDockerModel getMachineDockerBySwarmId(String swarmId) {
        //
        MachineDockerModel dockerInfoModel = this.tryMachineDockerBySwarmId(swarmId);
        Assert.notNull(dockerInfoModel, "当前集群未找到任何管理节点");
        return dockerInfoModel;
    }

    public MachineDockerModel tryMachineDockerBySwarmId(String swarmId) {
        if ((swarmId == null || swarmId.isEmpty())) {
            return null;
        }
        //
        MachineDockerModel dockerInfoModel = new MachineDockerModel();
        dockerInfoModel.setSwarmId(swarmId);
        dockerInfoModel.setSwarmControlAvailable(true);
        List<MachineDockerModel> machineDockerModels = this.listByBean(dockerInfoModel, false);
        if (machineDockerModels == null) {
            return null;
        }
        // 跟进在线情况排序
        machineDockerModels.sort((o1, o2) -> CompareUtil.compare(o2.getStatus(), o1.getStatus()));
        return (machineDockerModels == null || machineDockerModels.isEmpty() ? null : machineDockerModels.get(0));
    }

    /**
     * 插件 插件参数 map
     *
     * @return 插件需要使用到到参数
     */
    public Map<String, Object> toParameter(MachineDockerModel machineDockerModel) {
        Map<String, Object> parameter = new HashMap<>(10);
        parameter.put("dockerHost", machineDockerModel.getHost());
        parameter.put("name", machineDockerModel.getName());
        parameter.put("registryUsername", machineDockerModel.getRegistryUsername());
        parameter.put("registryPassword", machineDockerModel.getRegistryPassword());
        parameter.put("registryEmail", machineDockerModel.getRegistryEmail());
        parameter.put("registryUrl", machineDockerModel.getRegistryUrl());
        parameter.put("timeout", machineDockerModel.getHeartbeatTimeout());
        if (machineDockerModel.getTlsVerify()) {
            File filePath = certificateInfoService.getFilePath(machineDockerModel.getCertInfo());
            Assert.notNull(filePath, "docker 证书文件丢失");
            parameter.put("dockerCertPath", filePath.getAbsolutePath());
        }
        if (Boolean.TRUE.equals(machineDockerModel.getEnableSsh()) && (machineDockerModel.getMachineSshId() != null && !machineDockerModel.getMachineSshId().isEmpty())) {
            // 添加SSH的操作Session
            MachineSshModel sshModel = machineSshServer.getByKey(machineDockerModel.getMachineSshId());
            Assert.notNull(sshModel, "ssh 信息不存在啦");
            // 需要关闭之前的连接，避免阻塞
            parameter.put("closeBefore", true);
            parameter.put("sshUseSudo", machineDockerModel.getSshUseSudo());
            parameter.put("session", (Supplier<Session>) () -> machineSshServer.getSessionByModel(sshModel));
        }
        return parameter;
    }
}
