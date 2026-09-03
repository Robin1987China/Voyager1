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

import io.voyager1.util.CollStreamUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.CompareUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.NetUtil;
import io.voyager1.util.NumberUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.event.IAsyncLoad;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Const;
import io.voyager1.common.ILoadEvent;
import io.voyager1.common.Voyager1Manifest;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.NodeConfig;
import io.voyager1.cron.CronUtils;
import io.voyager1.core.entity.MachineNodeEntity;
import io.voyager1.core.jpa.JpaBaseService;
import io.voyager1.core.repository.MachineNodeRepository;
import io.voyager1.exception.AgentAuthorizeException;
import io.voyager1.exception.AgentException;
import io.voyager1.func.assets.AssetsExecutorPoolService;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.func.assets.model.MachineNodeStatLogModel;
import io.voyager1.func.system.service.ClusterInfoService;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.node.NodeService;
import io.voyager1.system.ServerConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @since 2023/2/18
 */
@Service
@Slf4j
public class MachineNodeServer extends JpaBaseService<MachineNodeModel, MachineNodeEntity> implements ILoadEvent, IAsyncLoad, Runnable {

    private final NodeService nodeService;
    private final NodeConfig nodeConfig;
    private final MachineNodeStatLogServer machineNodeStatLogServer;
    private final ClusterInfoService clusterInfoService;
    private final AssetsExecutorPoolService assetsExecutorPoolService;
    private final MachineNodeRepository machineNodeRepository;

    private static final String TASK_ID = "system_monitor_node";

    public MachineNodeServer(NodeService nodeService,
                             ServerConfig serverConfig,
                             MachineNodeStatLogServer machineNodeStatLogServer,
                             ClusterInfoService clusterInfoService,
                             AssetsExecutorPoolService assetsExecutorPoolService,
                             MachineNodeRepository machineNodeRepository) {
        this.nodeService = nodeService;
        this.nodeConfig = serverConfig.getNode();
        this.machineNodeStatLogServer = machineNodeStatLogServer;
        this.clusterInfoService = clusterInfoService;
        this.assetsExecutorPoolService = assetsExecutorPoolService;
        this.machineNodeRepository = machineNodeRepository;
    }

    @Override
    protected JpaRepository<MachineNodeEntity, String> repository() {
        return machineNodeRepository;
    }

    @Override
    protected JpaSpecificationExecutor<MachineNodeEntity> specExecutor() {
        return machineNodeRepository;
    }

    @Override
    protected Class<MachineNodeEntity> entityClass() {
        return MachineNodeEntity.class;
    }

    @Override
    protected Class<MachineNodeModel> modelClass() {
        return MachineNodeModel.class;
    }

    @Override
    protected void fillSelectResult(MachineNodeModel data) {
        Optional.ofNullable(data).ifPresent(machineNodeModel -> machineNodeModel.setVoyager1Password(null));
    }

    @Override
    protected void fillInsert(MachineNodeModel machineNodeModel) {
        super.fillInsert(machineNodeModel);
        machineNodeModel.setGroupName((machineNodeModel.getGroupName() == null || machineNodeModel.getGroupName().isEmpty() ? Const.DEFAULT_GROUP_NAME.get() : machineNodeModel.getGroupName()));
        //
        machineNodeModel.setTransportMode(0);
    }

    /**
     * 同步数据，兼容低版本数据
     *
     * @param applicationContext 应用上下文
     * @throws Exception 异常
     */
    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        long count = this.count();
        if (count != 0) {
            log.debug("节点机器表已经存在 {} 条数据，不需要修复机器数据", count);
            return;
        }
        // 迁移旧数据
        List<NodeModel> list = nodeService.list(false);
        if ((list == null || list.isEmpty())) {
            log.debug("没有任何节点信息,不需要修复机器数据");
            return;
        }
        // delete from MACHINE_NODE_INFO;
        // drop table MACHINE_NODE_INFO;
        Map<String, List<NodeModel>> nodeUrlMap = CollStreamUtil.groupByKey(list, NodeModel::getUrl);
        List<MachineNodeModel> machineNodeModels = new ArrayList<>(nodeUrlMap.size());
        for (Map.Entry<String, List<NodeModel>> entry : nodeUrlMap.entrySet()) {
            List<NodeModel> value = entry.getValue();
            // 排序，最近更新过优先
            value.sort((o1, o2) -> CompareUtil.compare(o2.getModifyTimeMillis(), o1.getModifyTimeMillis()));
            NodeModel first = (value == null || value.isEmpty() ? null : value.get(0));
            if (value.size() > 1) {
                log.warn("节点地址 {} 存在多个数据，将自动合并使用 {} 节点的配置信息", entry.getKey(), first.getName());
            }
            machineNodeModels.add(this.nodeInfoToMachineNode(first));
        }
        this.insert(machineNodeModels);
        log.info("成功修复 {} 条机器节点数据", machineNodeModels.size());
        // 更新节点的机器id
        for (MachineNodeModel value : machineNodeModels) {
            int update = nodeService.updateMachineIdByUrl(value.getId(), value.getVoyager1Url());
            Assert.state(update > 0, "更新节点表机器 id 失败：" + value.getName());
        }
    }

    /**
     * 保证在数据库启动成功之后
     *
     * @return 想要比数据库晚加载
     */
    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE + 1;
    }

    /**
     * 节点对象转机器对象
     *
     * @param nodeModel 节点
     * @return 机器对象
     */
    private MachineNodeModel nodeInfoToMachineNode(NodeModel nodeModel) {
        MachineNodeModel machineNodeModel = new MachineNodeModel();
        machineNodeModel.setName(nodeModel.getName());
        machineNodeModel.setGroupName(nodeModel.getGroup());
        machineNodeModel.setTransportMode(0);
        machineNodeModel.setStatus(0);
        machineNodeModel.setVoyager1Timeout(nodeModel.getTimeOut());
        machineNodeModel.setVoyager1Url(nodeModel.getUrl());
        machineNodeModel.setVoyager1Username(nodeModel.getLoginName());
        machineNodeModel.setVoyager1Password(nodeModel.getLoginPwd());
        machineNodeModel.setVoyager1Protocol(nodeModel.getProtocol());
        machineNodeModel.setVoyager1HttpProxy(nodeModel.getHttpProxy());
        machineNodeModel.setVoyager1HttpProxyType(nodeModel.getHttpProxyType());
        machineNodeModel.setModifyUser(nodeModel.getModifyUser());
        machineNodeModel.setCreateTimeMillis(nodeModel.getCreateTimeMillis());
        machineNodeModel.setModifyTimeMillis(nodeModel.getModifyTimeMillis());
        return machineNodeModel;
    }

    @Override
    public void startLoad() {
        // 启动心跳检测
        int heartSecond = nodeConfig.getHeartSecond();
        ScheduledExecutorService scheduler = Voyager1Application.getScheduledExecutorService();
        scheduler.scheduleWithFixedDelay(this, 0, heartSecond, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        Entity entity = new Entity();
        if (clusterInfoService.isMultiServer()) {
            String linkGroup = clusterInfoService.getCurrent().getLinkGroup();
            List<String> linkGroups = io.voyager1.util.ConvertUtil.splitTrim(linkGroup, ",");
            if ((linkGroups == null || linkGroups.isEmpty())) {
                log.warn("当前集群还未绑定分组,不能监控集群节点资产信息");
                return;
            }
            entity.set("groupName", linkGroups);
        }
        entity.set("transportMode", 0);
        int heartSecond = nodeConfig.getHeartSecond();
        try {
            CronUtils.TaskStat taskStat = CronUtils.getTaskStat(TASK_ID, String.format("%s 秒执行一次", heartSecond));
            taskStat.onStart();
            //MachineNodeModel machineNodeModel = new MachineNodeModel();
            //machineNodeModel.setTransportMode(0);
            List<MachineNodeModel> machineNodeModels = this.listByEntity(entity);
            this.checkList(machineNodeModels);
            taskStat.onSucceeded();
        } catch (Throwable throwable) {
            CronUtils.TaskStat taskStat = CronUtils.getTaskStat(TASK_ID, String.format("%s 秒执行一次", heartSecond));
            taskStat.onFailed(TASK_ID, throwable);
        }
    }


    private void checkList(List<MachineNodeModel> machineNodeModels) {
        if ((machineNodeModels == null || machineNodeModels.isEmpty())) {
            return;
        }
        machineNodeModels.forEach(machineNodeModel -> {
            // 超时时间统一，避免长时间无响应
            machineNodeModel.setVoyager1Timeout(30);
            //
            assetsExecutorPoolService.execute(() -> {
                try {
                    BaseServerController.resetInfo(UserModel.EMPTY);
                    long timeMillis = System.currentTimeMillis();
                    ApiResult<JSONObject> message = NodeForward.request(machineNodeModel, NodeUrl.GetStatInfo, new JSONObject());
                    int networkTime = (int) (System.currentTimeMillis() - timeMillis);
                    JSONObject jsonObject;
                    if (message.success()) {
                        jsonObject = message.getData(JSONObject.class);
                    } else {
                        // 状态码错
                        this.updateStatus(machineNodeModel, 3, message.toString());
                        return;
                    }
                    jsonObject.put("networkDelay", networkTime);
                    this.saveStatInfo(machineNodeModel, jsonObject);
                } catch (AgentAuthorizeException agentException) {
                    this.updateStatus(machineNodeModel, 2, agentException.getMessage());
                } catch (AgentException e) {
                    this.updateStatus(machineNodeModel, 0, e.getMessage());
                } catch (Exception e) {
                    this.updateStatus(machineNodeModel, 0, e.getMessage());
                    log.error("获取节点监控信息失败", e);
                } finally {
                    BaseServerController.removeEmpty();
                }
            });
        });
    }

    /**
     * 更新统计信息
     *
     * @param machineNode 机器数据
     * @param data        统计数据
     */
    private void saveStatInfo(MachineNodeModel machineNode, JSONObject data) {
        MachineNodeModel machineNodeModel = new MachineNodeModel();
        machineNodeModel.setId(machineNode.getId());
        String oshiError = data.getString("oshiError");
        if ((oshiError == null || oshiError.isEmpty())) {
            machineNodeModel.setStatus(1);
            machineNodeModel.setStatusMsg("ok");
        } else {
            machineNodeModel.setStatus(4);
            machineNodeModel.setStatusMsg(oshiError);
        }
        int networkDelay = data.getIntValue("networkDelay");
        int systemSleep = data.getIntValue("systemSleep");
        // 减去系统固定休眠时间
        networkDelay = networkDelay - systemSleep;
        machineNodeModel.setNetworkDelay(networkDelay);
        // voyager1 相关信息
        JSONObject voyager1Info = data.getJSONObject("voyager1Info");
        Optional.ofNullable(voyager1Info).ifPresent(jsonObject -> {
            Optional.ofNullable(jsonObject.getJSONObject("voyager1Manifest"))
                .ifPresent(jsonObject1 -> {
                    machineNodeModel.setVoyager1Version(jsonObject1.getString("version"));
                    machineNodeModel.setVoyager1BuildTime(jsonObject1.getString("timeStamp"));
                    machineNodeModel.setOsName(jsonObject1.getString("osName"));
                    machineNodeModel.setVoyager1Uptime(jsonObject1.getLong("upTime"));
                    machineNodeModel.setInstallId(jsonObject1.getString("installId"));
                });
            machineNodeModel.setVoyager1ProjectCount(jsonObject.getIntValue("projectCount"));
            machineNodeModel.setVoyager1ScriptCount(jsonObject.getIntValue("scriptCount"));
            //
            machineNodeModel.setJvmFreeMemory(jsonObject.getLongValue("freeMemory"));
            machineNodeModel.setJvmTotalMemory(jsonObject.getLongValue("totalMemory"));
            machineNodeModel.setJavaVersion(jsonObject.getString("javaVersion"));
        });
        // 基础状态信息
        MachineNodeStatLogModel machineNodeStatLogModel = new MachineNodeStatLogModel();
        machineNodeStatLogModel.setMachineId(machineNodeModel.getId());
        machineNodeStatLogModel.setNetworkDelay(networkDelay);
        //
        JSONObject extendInfo = new JSONObject();
        Optional.ofNullable(data.getJSONObject("simpleStatus")).ifPresent(jsonObject -> {
            machineNodeModel.setOsOccupyMemory((jsonObject.getDouble("memory") != null ? jsonObject.getDouble("memory") : -1D));
            machineNodeModel.setOsOccupyDisk((jsonObject.getDouble("disk") != null ? jsonObject.getDouble("disk") : -1D));
            machineNodeModel.setOsOccupyCpu((jsonObject.getDouble("cpu") != null ? jsonObject.getDouble("cpu") : -1D));
            //
            machineNodeStatLogModel.setOccupyCpu(machineNodeModel.getOsOccupyCpu());
            machineNodeStatLogModel.setCpuTicks(StrUtil.toStringOrNull(jsonObject.getByPath("cpuInfo.ticks")));
            machineNodeStatLogModel.setOccupyMemory(machineNodeModel.getOsOccupyMemory());
            machineNodeStatLogModel.setOccupyDisk(machineNodeModel.getOsOccupyDisk());
            machineNodeStatLogModel.setOccupySwapMemory(jsonObject.getDouble("swapMemory"));
            machineNodeStatLogModel.setOccupyVirtualMemory(jsonObject.getDouble("virtualMemory"));
            machineNodeStatLogModel.setNetTxBytes(jsonObject.getLong("netTxBytes"));
            machineNodeStatLogModel.setNetRxBytes(jsonObject.getLong("netRxBytes"));
            machineNodeStatLogModel.setMonitorTime(jsonObject.getLongValue("time"));
            //
            extendInfo.put("monitorIfsNames", jsonObject.getString("monitorIfsNames"));
        });
        // 系统信息
        Optional.ofNullable(data.getJSONObject("systemInfo")).ifPresent(jsonObject -> {
            machineNodeModel.setOsSystemUptime(jsonObject.getLong("systemUptime"));
            machineNodeModel.setOsVersion(jsonObject.getString("osVersion"));
            machineNodeModel.setHostName(jsonObject.getString("hostName"));
            machineNodeModel.setOsHardwareVersion(jsonObject.getString("hardwareVersion"));
            machineNodeModel.setHostIpv4s(CollUtil.join(jsonObject.getList("hostIpv4s", String.class), ","));
            machineNodeModel.setOsCpuIdentifierName(jsonObject.getString("osCpuIdentifierName"));
            machineNodeModel.setOsCpuCores(jsonObject.getInteger("osCpuCores"));
            machineNodeModel.setOsMoneyTotal(jsonObject.getLong("osMoneyTotal"));
            machineNodeModel.setOsSwapTotal(jsonObject.getLong("osSwapTotal"));
            machineNodeModel.setOsVirtualMax(jsonObject.getLong("osVirtualMax"));
            List<Double> osLoadAverage = jsonObject.getList("osLoadAverage", Double.class);
            if (osLoadAverage != null) {
                // 保留两位小数
                osLoadAverage = osLoadAverage.stream()
                    .map(aDouble -> NumberUtil.div(aDouble, (Double) 1D, 2))
                    .collect(Collectors.toList());
            }
            machineNodeModel.setOsLoadAverage(osLoadAverage == null ? null : osLoadAverage.stream().map(String::valueOf).collect(Collectors.joining(",")));
            machineNodeModel.setOsFileStoreTotal(jsonObject.getLong("osFileStoreTotal"));
        });
        machineNodeModel.setExtendInfo(extendInfo.toString());
        this.updateById(machineNodeModel);
        if (machineNodeStatLogModel.getMonitorTime() != null) {
            machineNodeStatLogServer.insert(machineNodeStatLogModel);
        }
        //
        Optional.ofNullable(voyager1Info).ifPresent(jsonObject -> {
            JSONObject workspaceStat = jsonObject.getJSONObject("workspaceStat");
            if (workspaceStat == null) {
                return;
            }
            for (Map.Entry<String, Object> entry : workspaceStat.entrySet()) {
                String key = entry.getKey();
                JSONObject value = (JSONObject) entry.getValue();
                int projectCount = value.getIntValue("projectCount", 0);
                int scriptCount = value.getIntValue("scriptCount", 0);
                nodeService.updateProjectScriptCount(machineNodeModel.getId(), key, projectCount, scriptCount);
            }
        });
    }

    /**
     * 更新机器状态
     *
     * @param machineNode 机器信息
     * @param status      状态
     * @param msg         状态消息
     */
    private void updateStatus(MachineNodeModel machineNode, int status, String msg) {
        MachineNodeModel machineNodeModel = new MachineNodeModel();
        machineNodeModel.setId(machineNode.getId());
        machineNodeModel.setStatus(status);
        machineNodeModel.setStatusMsg(msg);
        // 将信息置空，避免影响排序
        machineNodeModel.setNetworkDelay(-9999_999);
        machineNodeModel.setOsOccupyCpu(-99D);
        machineNodeModel.setOsOccupyMemory(-99D);
        machineNodeModel.setOsOccupyDisk(-99D);
        this.updateById(machineNodeModel);
    }

    private MachineNodeModel resolveMachineData(HttpServletRequest request) {
        // 创建对象
        MachineNodeModel machineNodeModel = JakartaServletUtil.toBean(request, MachineNodeModel.class, true);
        Assert.hasText(machineNodeModel.getName(), "请填写机器名称");
        Assert.hasText(machineNodeModel.getVoyager1Url(), "请填写 节点地址");
        Assert.hasText(machineNodeModel.getVoyager1Username(), "请填写节点账号");

        Assert.hasText(machineNodeModel.getVoyager1Protocol(), "请选择协议");
        //
        MachineNodeModel update = new MachineNodeModel();
        update.setId(machineNodeModel.getId());
        update.setGroupName(machineNodeModel.getGroupName());
        update.setName(machineNodeModel.getName());
        update.setVoyager1HttpProxy(machineNodeModel.getVoyager1HttpProxy());
        update.setVoyager1HttpProxyType(machineNodeModel.getVoyager1HttpProxyType());
        update.setVoyager1Url(machineNodeModel.getVoyager1Url());
        update.setVoyager1Protocol(machineNodeModel.getVoyager1Protocol());
        update.setVoyager1Username(machineNodeModel.getVoyager1Username());
        update.setVoyager1Password(machineNodeModel.getVoyager1Password());
        update.setVoyager1Timeout(machineNodeModel.getVoyager1Timeout());
        update.setTemplateNode(machineNodeModel.getTemplateNode());
        update.setTransportEncryption(machineNodeModel.getTransportEncryption());
        return update;
    }

    public boolean existsByUrl(String voyager1Url, String id) {
        Assert.hasText(voyager1Url, "节点地址不能为空");
        //
        Entity entity = Entity.create();
        entity.set("voyager1Url", voyager1Url);
        if ((id != null && !id.isEmpty())) {
            entity.set("id", String.format(" <> %s", id));
        }
        return this.exists(entity);
    }

    public MachineNodeModel getByUrl(String voyager1Url) {
        MachineNodeModel machineNodeModel = new MachineNodeModel();
        machineNodeModel.setVoyager1Url(voyager1Url);
        List<MachineNodeModel> machineNodeModels = this.listByBean(machineNodeModel);
        return (machineNodeModels == null || machineNodeModels.isEmpty() ? null : machineNodeModels.get(0));
    }

    public void update(HttpServletRequest request) {
        MachineNodeModel machineNodeModel = this.resolveMachineData(request);
        boolean exists = this.existsByUrl(machineNodeModel.getVoyager1Url(), machineNodeModel.getId());
        Assert.state(!exists, "对应的节点已经存在啦");
        this.testNode(machineNodeModel);
        // 更新状态
        machineNodeModel.setStatus(1);
        //
        this.testHttpProxy(machineNodeModel.getVoyager1HttpProxy());
        //
        if ((machineNodeModel.getId() != null && !machineNodeModel.getId().isEmpty())) {
            this.updateById(machineNodeModel);
        } else {
            this.insert(machineNodeModel);
        }
    }

    /**
     * 测试节点是否可以访问
     *
     * @param nodeModel 节点信息
     */
    public void testNode(MachineNodeModel nodeModel) {
        //
        int timeout = (nodeModel.getVoyager1Timeout() != null ? nodeModel.getVoyager1Timeout() : 0);
        // 检查是否可用默认为5秒，避免太长时间无法连接一直等待
        nodeModel.setVoyager1Timeout(5);
        //
        ApiResult<Voyager1Manifest> objectJsonMessage = NodeForward.request(nodeModel, "", NodeUrl.Info, "nodeId", nodeModel.getId());
        try {
            Voyager1Manifest voyager1Manifest = objectJsonMessage.getData(Voyager1Manifest.class);
            Assert.notNull(voyager1Manifest, "节点连接失败，请检查节点是否在线");
        } catch (Exception e) {
            log.error("节点连接失败，请检查节点是否在线", e);
            throw new IllegalStateException("节点返回信息异常,请检查节点地址是否配置正确或者代理配置是否正确");
        }
        //
        nodeModel.setVoyager1Timeout(timeout);
    }

    /**
     * 探测 http proxy 是否可用
     *
     * @param httpProxy http proxy
     */
    public void testHttpProxy(String httpProxy) {
        if ((httpProxy != null && !httpProxy.isEmpty())) {
            List<String> split = io.voyager1.util.ConvertUtil.splitTrim(httpProxy, ":");
            Assert.isTrue((split == null ? 0 : split.size()) == 2, "HTTP代理地址格式不正确");
            String host = split.get(0);
            int port = ConvertUtil.toInt(split.get(1), 0);
            Assert.isTrue((host != null && !host.isEmpty()) && NetUtil.isValidPort(port), "HTTP代理地址格式不正确");
            //
            try {
                NetUtil.netCat(host, port, "".getBytes());
            } catch (Exception e) {
                log.warn("HTTP代理地址不可用:" + httpProxy, e);
                throw new IllegalArgumentException("HTTP代理地址不可用:" + e.getMessage());
            }
        }
    }

    public void insertAndNode(MachineNodeModel machineNodeModel, String workspaceId) {
        this.insert(machineNodeModel);
        //
        this.insertNode(machineNodeModel, workspaceId);
    }

    /**
     * 根据机器添加 节点
     *
     * @param machineNodeModel 机器信息
     * @param workspaceId      工作空间
     */
    public void insertNode(MachineNodeModel machineNodeModel, String workspaceId) {
        NodeModel nodeModel = this.createModel(machineNodeModel, workspaceId);
        nodeService.insert(nodeModel);
    }

    private NodeModel createModel(MachineNodeModel machineNodeModel, String workspaceId) {
        NodeModel nodeModel = new NodeModel();
        nodeModel.setMachineId(machineNodeModel.getId());
        nodeModel.setWorkspaceId(workspaceId);
        nodeModel.setName(machineNodeModel.getName());
        nodeModel.setOpenStatus(1);
        nodeModel.setGroup(machineNodeModel.getGroupName());
        return nodeModel;
    }
}
