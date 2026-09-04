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

package io.voyager1.monitor;

import io.voyager1.util.CollUtil;
import io.voyager1.util.ExceptionUtil;
import io.voyager1.util.EnumUtil;
import io.voyager1.util.IdUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.Task;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.core.api.ApiResult;
import io.voyager1.plugin.IPlugin;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.i18n.I18nThreadUtil;
import io.voyager1.model.data.MonitorModel;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.log.MonitorNotifyLog;
import io.voyager1.model.node.ProjectInfoCacheModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.dblog.DbMonitorNotifyLogService;
import io.voyager1.service.monitor.MonitorService;
import io.voyager1.service.node.NodeService;
import io.voyager1.service.node.ProjectInfoCacheService;
import io.voyager1.service.user.UserService;
import io.voyager1.webhook.DefaultWebhookPluginImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 监控执行器
 *
 * @since 2021/12/14
 */
@Slf4j
public class MonitorItem implements Task {


    private final DbMonitorNotifyLogService dbMonitorNotifyLogService;
    private final UserService userService;
    private final MonitorService monitorService;
    private final ProjectInfoCacheService projectInfoCacheService;
    private final NodeService nodeService;
    private final String monitorId;
    private MonitorModel monitorModel;

    public MonitorItem(String id) {
        this.dbMonitorNotifyLogService = SpringContextHolder.getBean(DbMonitorNotifyLogService.class);
        this.userService = SpringContextHolder.getBean(UserService.class);
        this.monitorService = SpringContextHolder.getBean(MonitorService.class);
        this.nodeService = SpringContextHolder.getBean(NodeService.class);
        this.projectInfoCacheService = SpringContextHolder.getBean(ProjectInfoCacheService.class);
        this.monitorId = id;
    }

    @Override
    public void execute() {
        // 重新查询
        this.monitorModel = monitorService.getByKey(monitorId);
        try {
            I18nMessageUtil.setLanguage(this.monitorModel.getUseLanguage());
            List<MonitorModel.NodeProject> nodeProjects = monitorModel.projects();
            //
            List<Boolean> collect = nodeProjects.stream()
                .map(nodeProject -> {
                    String nodeId = nodeProject.getNode();
                    NodeModel nodeModel = nodeService.getByKey(nodeId);
                    if (nodeModel == null) {
                        log.error("监控项：{} 对应的节点不存在 {}", this.monitorModel.getName(), nodeId);
                        return true;
                    }
                    return this.reqNodeStatus(nodeModel, nodeProject.getProjects());
                })
                .filter(aBoolean -> !aBoolean)
                .collect(Collectors.toList());
            boolean allRun = (collect == null || collect.isEmpty());
            // 报警状态
            monitorService.setAlarm(monitorModel.getId(), !allRun);
        } finally {
            I18nMessageUtil.clearLanguage();
        }
    }

    /**
     * 检查节点节点对信息
     *
     * @param nodeModel 节点
     * @param projects  项目
     * @return true 所有项目都正常
     */
    private boolean reqNodeStatus(NodeModel nodeModel, List<String> projects) {
        if (projects == null || projects.isEmpty()) {
            return true;
        }
        List<Boolean> collect = projects.stream()
            .map(id -> {
                //
                String title;
                String context;
                try {
                    //查询项目运行状态
                    ApiResult<JSONObject> jsonMessage = NodeForward.request(nodeModel, NodeUrl.Manage_GetProjectStatus, "id", id);
                    if (jsonMessage.success()) {
                        JSONObject jsonObject = jsonMessage.getData();
                        int pid = jsonObject.getIntValue("pId");
                        String statusMsg = jsonObject.getString("statusMsg");
                        boolean runStatus = this.checkNotify(monitorModel, nodeModel, id, pid > 0, statusMsg);
                        // 检查副本
                        List<Boolean> booleanList = null;
                        JSONArray copys = jsonObject.getJSONArray("copys");
                        if ((copys != null && !copys.isEmpty())) {
                            booleanList = copys.stream()
                                .map(o -> {
                                    JSONObject jsonObject1 = (JSONObject) o;

                                    boolean status = jsonObject1.getBooleanValue("status");
                                    return MonitorItem.this.checkNotify(monitorModel, nodeModel, id, status, "");
                                })
                                .filter(aBoolean -> !aBoolean)
                                .collect(Collectors.toList());
                        }
                        return runStatus && (booleanList == null || booleanList.isEmpty());
                    } else {
                        title = String.format("【%s】节点的状态码异常：%s", nodeModel.getName(), jsonMessage.getCode());
                        context = jsonMessage.toString();
                    }
                } catch (Exception e) {
                    log.error("监控 {} 节点异常 {}", nodeModel.getName(), e.getMessage());
                    //
                    title = String.format("【%s】节点的运行状态异常", nodeModel.getName());
                    context = java.util.Arrays.toString(e.getStackTrace());
                }
                MonitorNotifyLog monitorNotifyLog = new MonitorNotifyLog();
                monitorNotifyLog.setStatus(false);
                monitorNotifyLog.setTitle(title);
                monitorNotifyLog.setContent(context);
                monitorNotifyLog.setCreateTime(System.currentTimeMillis());
                monitorNotifyLog.setNodeId(nodeModel.getId());
                monitorNotifyLog.setProjectId(id);
                monitorNotifyLog.setMonitorId(monitorModel.getId());
                // 获取上次状态
                MonitorNotifyLog preData = this.getPreData(monitorModel.getId(), nodeModel.getId(), id);
                boolean pre = preData == null || preData.status();
                if (pre) {
                    // 上次正常
                    this.notifyMsg(nodeModel, monitorNotifyLog);
                } else {
                    Integer silenceTime = this.monitorModel.getSilenceTime();
                    if (silenceTime != null) {
                        TimeUnit timeUnit = EnumUtil.fromString(TimeUnit.class, this.monitorModel.getSilenceUnit(), TimeUnit.MINUTES);
                        long millis = timeUnit.toMillis(this.monitorModel.getSilenceTime());
                        if (preData.getCreateTime() + millis < System.currentTimeMillis()) {
                            // 上次不正常并且过了沉默时间
                            this.notifyMsg(nodeModel, monitorNotifyLog);
                        }
                    }
                }
                return false;
            })
            .filter(aBoolean -> !aBoolean)
            .collect(Collectors.toList());
        return (collect == null || collect.isEmpty());
    }

    /**
     * 检查状态
     *
     * @param monitorModel 监控信息
     * @param nodeModel    节点信息
     * @param id           项目id
     * @param runStatus    当前运行状态
     */
    private boolean checkNotify(MonitorModel monitorModel, NodeModel nodeModel, String id, boolean runStatus, String statusMsg) {
        // 获取上次状态
        String copyMsg = "";
        MonitorNotifyLog preData = this.getPreData(monitorModel.getId(), nodeModel.getId(), id);
        boolean pre = preData == null || preData.status();
        ProjectInfoCacheModel projectInfoCacheModel = projectInfoCacheService.getData(nodeModel.getId(), id);
        String projectName = id;
        if (projectInfoCacheModel != null) {
            projectName = String.format("%s/%s", projectInfoCacheModel.getName(), id);
        }
        String title = null;
        String context = null;
        //查询项目运行状态
        if (runStatus) {
            if (!pre) {
                // 上次是异常状态
                title = String.format("【%s】节点的【%s】项目%s已经恢复正常运行", nodeModel.getName(), projectName, copyMsg);
                context = "";
            }
        } else {
            //
            if (monitorModel.autoRestart()) {
                // 执行重启
                try {
                    ApiResult<String> reJson = NodeForward.request(nodeModel, NodeUrl.Manage_Operate, "id", id, "opt", "restart");
                    if (reJson.success()) {
                        // 重启成功
                        runStatus = true;
                        title = String.format("【%s】节点的【%s】项目%s已经停止，已经执行重启操作, 结果成功", nodeModel.getName(), projectName, copyMsg);
                    } else {
                        title = String.format("【%s】节点的【%s】项目%s已经停止，已经执行重启操作, 结果失败", nodeModel.getName(), projectName, copyMsg);
                    }
                    context = "重启结果：" + reJson;
                } catch (Exception e) {
                    log.error("执行重启操作", e);
                    title = String.format("【%s】节点的【%s】项目%s已经停止，重启操作异常", nodeModel.getName(), projectName, copyMsg);
                    context = java.util.Arrays.toString(e.getStackTrace());
                }
            } else {
                title = String.format("【%s】节点的【%s】项目%s已经没有运行", nodeModel.getName(), projectName, copyMsg);
                context = "请及时检查";
            }
        }
        if (!pre && !runStatus) {
            // 上一次是异常，并且当前还是异常
            Integer silenceTime = this.monitorModel.getSilenceTime();
            if (silenceTime == null) {
                log.warn("触发报警信息自动忽略，当前处于持续报警中,{}", monitorModel.getName());
                return false;
            }

            TimeUnit timeUnit = EnumUtil.fromString(TimeUnit.class, this.monitorModel.getSilenceUnit(), TimeUnit.MINUTES);
            long millis = timeUnit.toMillis(this.monitorModel.getSilenceTime());
            if (preData.getCreateTime() + millis > System.currentTimeMillis()) {
                if (preData.getNotifyStatus() != null && preData.getNotifyStatus()) {
                    // 通知成功
                    log.warn("触发报警信息自动忽略，上次通知成功并且当前处于持续报警中,{}", monitorModel.getName());
                    return false;
                }
            }
        }
        MonitorNotifyLog monitorNotifyLog = new MonitorNotifyLog();
        monitorNotifyLog.setStatus(runStatus);
        monitorNotifyLog.setTitle(title);
        monitorNotifyLog.setContent(String.format("报警内容：%s 状态消息：%s", context, statusMsg));
        monitorNotifyLog.setCreateTime(System.currentTimeMillis());
        monitorNotifyLog.setNodeId(nodeModel.getId());
        monitorNotifyLog.setProjectId(id);
        monitorNotifyLog.setMonitorId(monitorModel.getId());
        //
        this.notifyMsg(nodeModel, monitorNotifyLog);
        return runStatus;
    }

    /**
     * 获取上次是否也为异常状态
     *
     * @param monitorId 监控id
     * @param nodeId    节点id
     * @param projectId 项目id
     * @return true 为正常状态,false 异常状态
     */
    private boolean getPreStatus(String monitorId, String nodeId, String projectId) {
        MonitorNotifyLog entity1 = this.getPreData(monitorId, nodeId, projectId);
        return entity1 == null || entity1.status();
    }

    /**
     * 获取上次是否也为异常状态
     *
     * @param monitorId 监控id
     * @param nodeId    节点id
     * @param projectId 项目id
     * @return data
     */
    private MonitorNotifyLog getPreData(String monitorId, String nodeId, String projectId) {
        // 检查是否已经触发通知

        MonitorNotifyLog monitorNotifyLog = new MonitorNotifyLog();
        monitorNotifyLog.setNodeId(nodeId);
        monitorNotifyLog.setProjectId(projectId);
        monitorNotifyLog.setMonitorId(monitorId);

        return dbMonitorNotifyLogService.getByMonitorId(monitorId);
    }

    private void notifyMsg(NodeModel nodeModel, MonitorNotifyLog monitorNotifyLog) {
        ProjectInfoCacheModel projectInfoCacheModel = projectInfoCacheService.getData(nodeModel.getId(), monitorNotifyLog.getProjectId());
        this.notifyMsg(nodeModel, monitorNotifyLog, projectInfoCacheModel);
    }

    private void notifyMsg(NodeModel nodeModel, MonitorNotifyLog monitorNotifyLog, ProjectInfoCacheModel projectInfoCacheModel) {
        if (projectInfoCacheModel == null) {
            log.error("监控的项目信息丢失不能正常发送监控通知：{} => {}", monitorModel.getName(), monitorNotifyLog.getTitle());
            return;
        }
        List<String> notify = monitorModel.notifyUser();
        // 发送通知
        if (monitorNotifyLog.getTitle() == null) {
            return;
        }
        monitorNotifyLog.setWorkspaceId(projectInfoCacheModel.getWorkspaceId());
        //
        notify.forEach(notifyUser -> this.sendNotifyMsgToUser(monitorNotifyLog, notifyUser));
        //
        this.sendNotifyMsgToWebhook(monitorNotifyLog, nodeModel, projectInfoCacheModel, monitorModel.getWebhook());
    }

    private void sendNotifyMsgToWebhook(MonitorNotifyLog monitorNotifyLog, NodeModel nodeModel, ProjectInfoCacheModel projectInfoCacheModel, String webhook) {
        if ((webhook == null || webhook.isEmpty())) {
            return;
        }
        IPlugin plugin = PluginFactory.getPlugin("webhook");
        Map<String, Object> map = new HashMap<>(10);
        map.put("VOYAGER1_WEBHOOK_EVENT", DefaultWebhookPluginImpl.WebhookEvent.MONITOR);
        map.put("monitorId", monitorModel.getId());
        map.put("monitorName", monitorModel.getName());
        map.put("nodeId", monitorNotifyLog.getNodeId());
        map.put("nodeName", nodeModel.getName());
        map.put("runStatus", monitorNotifyLog.getStatus());
        map.put("projectId", monitorNotifyLog.getProjectId());
        if (projectInfoCacheModel != null) {
            map.put("projectName", projectInfoCacheModel.getName());
        }
        map.put("title", monitorNotifyLog.getTitle());
        map.put("content", monitorNotifyLog.getContent());
        //
        monitorNotifyLog.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
        monitorNotifyLog.setNotifyStyle(MonitorModel.NotifyType.webhook.getCode());
        monitorNotifyLog.setNotifyObject(webhook);
        //
        dbMonitorNotifyLogService.insert(monitorNotifyLog);
        String logId = monitorNotifyLog.getId();
        I18nThreadUtil.execute(() -> {
            try {
                plugin.execute(webhook, map);
                dbMonitorNotifyLogService.updateStatus(logId, true, null);
            } catch (Exception e) {
                log.error("WebHooks 调用错误", e);
                dbMonitorNotifyLogService.updateStatus(logId, false, java.util.Arrays.toString(e.getStackTrace()));
            }
        });
    }

    private void sendNotifyMsgToUser(MonitorNotifyLog monitorNotifyLog, String notifyUser) {
        UserModel item = userService.getByKey(notifyUser);
        boolean success = false;
        if (item != null) {
            // 邮箱
            String email = item.getEmail();
            if ((email != null && !email.isEmpty())) {
                monitorNotifyLog.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
                MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.mail, email);
                monitorNotifyLog.setNotifyStyle(notify1.getStyle());
                monitorNotifyLog.setNotifyObject(notify1.getValue());
                //
                dbMonitorNotifyLogService.insert(monitorNotifyLog);
                this.send(notify1, monitorNotifyLog.getId(), monitorNotifyLog.getTitle(), monitorNotifyLog.getContent());
                success = true;
            }
            // dingding
            String dingDing = item.getDingDing();
            if ((dingDing != null && !dingDing.isEmpty())) {
                monitorNotifyLog.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
                MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.dingding, dingDing);
                monitorNotifyLog.setNotifyStyle(notify1.getStyle());
                monitorNotifyLog.setNotifyObject(notify1.getValue());
                //
                dbMonitorNotifyLogService.insert(monitorNotifyLog);
                this.send(notify1, monitorNotifyLog.getId(), monitorNotifyLog.getTitle(), monitorNotifyLog.getContent());
                success = true;
            }
            // 企业微信
            String workWx = item.getWorkWx();
            if ((workWx != null && !workWx.isEmpty())) {
                monitorNotifyLog.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
                MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.workWx, workWx);
                monitorNotifyLog.setNotifyStyle(notify1.getStyle());
                monitorNotifyLog.setNotifyObject(notify1.getValue());
                //
                dbMonitorNotifyLogService.insert(monitorNotifyLog);
                this.send(notify1, monitorNotifyLog.getId(), monitorNotifyLog.getTitle(), monitorNotifyLog.getContent());
                success = true;
            }
        }
        if (success) {
            return;
        }
        monitorNotifyLog.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
        monitorNotifyLog.setNotifyObject("报警联系人异常");
        monitorNotifyLog.setNotifyStyle(MonitorModel.NotifyType.mail.getCode());
        monitorNotifyLog.setNotifyStatus(false);
        String userNotFound = "联系人不存在";
        String notifyError = "报警联系人异常:" + (item == null ? userNotFound : "");
        monitorNotifyLog.setNotifyError(notifyError);
        dbMonitorNotifyLogService.insert(monitorNotifyLog);
    }

    private void send(MonitorModel.Notify notify, String logId, String title, String context) {
        // 异常发送
        I18nThreadUtil.execute(() -> {
            try {
                NotifyUtil.send(notify, title, context);
                dbMonitorNotifyLogService.updateStatus(logId, true, null);
            } catch (Exception e) {
                log.error("发送报警通知异常", e);
                dbMonitorNotifyLogService.updateStatus(logId, false, java.util.Arrays.toString(e.getStackTrace()));
            }
        });
    }
}
