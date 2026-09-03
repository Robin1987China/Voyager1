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
import io.voyager1.util.URLUtil;
import io.voyager1.util.NumberUtil;
import io.voyager1.util.StrUtil;

import io.voyager1.util.CollStreamUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.CompareUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.DateTime;
import io.voyager1.util.DateUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.CaseInsensitiveMap;
import io.voyager1.util.CaseInsensitiveMap;
import io.voyager1.util.DateTime;
import io.voyager1.util.Task;
import io.voyager1.util.Task;
import io.voyager1.core.db.Entity;
import io.voyager1.util.JschUtil;
import io.voyager1.core.AppType;
import io.voyager1.event.IAsyncLoad;
import com.alibaba.fastjson2.JSONObject;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.common.Const;
import io.voyager1.common.ILoadEvent;
import io.voyager1.common.ServerConst;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.AssetsConfig;
import io.voyager1.cron.CronUtils;
import io.voyager1.core.entity.MachineSshEntity;
import io.voyager1.core.jpa.JpaBaseService;
import io.voyager1.core.repository.MachineSshRepository;
import io.voyager1.func.assets.AssetsExecutorPoolService;
import io.voyager1.func.assets.model.MachineSshModel;
import io.voyager1.func.system.service.ClusterInfoService;
import io.voyager1.model.data.SshModel;
import io.voyager1.plugin.IWorkspaceEnvPlugin;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.plugins.ISshInfo;
import io.voyager1.plugins.JschUtils;
import io.voyager1.service.node.ssh.SshService;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.util.StringUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.nio.charset.StandardCharsets;

/**
 * @since 2023/2/25
 */
@Service
@Slf4j
public class MachineSshServer extends JpaBaseService<MachineSshModel, MachineSshEntity> implements ILoadEvent, IAsyncLoad, Task {
    private static final String CRON_ID = "ssh-monitor";
    @Resource
    @Lazy
    private SshService sshService;

    private final Voyager1Application voyager1Application;
    private final ClusterInfoService clusterInfoService;
    private final AssetsConfig.SshConfig sshConfig;
    private final AssetsExecutorPoolService assetsExecutorPoolService;
    private final MachineSshRepository machineSshRepository;

    public MachineSshServer(Voyager1Application voyager1Application,
                            ClusterInfoService clusterInfoService,
                            AssetsConfig assetsConfig,
                            AssetsExecutorPoolService assetsExecutorPoolService,
                            MachineSshRepository machineSshRepository) {
        this.voyager1Application = voyager1Application;
        this.clusterInfoService = clusterInfoService;
        this.sshConfig = assetsConfig.getSsh();
        this.assetsExecutorPoolService = assetsExecutorPoolService;
        this.machineSshRepository = machineSshRepository;
    }

    @Override
    protected JpaRepository<MachineSshEntity, String> repository() {
        return machineSshRepository;
    }

    @Override
    protected JpaSpecificationExecutor<MachineSshEntity> specExecutor() {
        return machineSshRepository;
    }

    @Override
    protected Class<MachineSshEntity> entityClass() {
        return MachineSshEntity.class;
    }

    @Override
    protected Class<MachineSshModel> modelClass() {
        return MachineSshModel.class;
    }

    @Override
    protected void fillInsert(MachineSshModel machineSshModel) {
        super.fillInsert(machineSshModel);
        machineSshModel.setGroupName((machineSshModel.getGroupName() == null || machineSshModel.getGroupName().isEmpty() ? Const.DEFAULT_GROUP_NAME.get() : machineSshModel.getGroupName()));
        machineSshModel.setStatus((machineSshModel.getStatus() != null ? machineSshModel.getStatus() : 0));
    }

    @Override
    protected void fillSelectResult(MachineSshModel data) {
        if (data == null) {
            return;
        }
        if (!(data.getPassword() != null && data.getPassword().toLowerCase().startsWith(ServerConst.REF_WORKSPACE_ENV.toLowerCase()))) {
            // 隐藏密码字段
            data.setPassword(null);
        }
        //data.setPassword(null);
        data.setPrivateKey(null);
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        long count = this.count();
        if (count != 0) {
            log.debug("机器 SSH 表已经存在 {} 条数据，不需要修复机器 SSH 数据", count);
            return;
        }
        // 迁移旧数据
        List<SshModel> list = sshService.list(false);
        if ((list == null || list.isEmpty())) {
            log.debug("没有任何ssh信息,不需要修复机器 SSH 数据");
            return;
        }
        Map<String, List<SshModel>> sshMap = CollStreamUtil.groupByKey(list, sshModel -> String.format("%s %s %s %s", sshModel.getHost(), sshModel.getPort(), sshModel.getUser(), sshModel.getConnectType()));
        List<MachineSshModel> machineSshModels = new ArrayList<>(sshMap.size());
        for (Map.Entry<String, List<SshModel>> entry : sshMap.entrySet()) {
            List<SshModel> value = entry.getValue();
            // 排序，最近更新过优先
            value.sort((o1, o2) -> CompareUtil.compare(o2.getModifyTimeMillis(), o1.getModifyTimeMillis()));
            SshModel first = (value == null || value.isEmpty() ? null : value.get(0));
            if (value.size() > 1) {
                log.warn("SSH 地址 {} 存在多个数据，将自动合并使用 {} SSH的配置信息", entry.getKey(), first.getName());
            }
            machineSshModels.add(this.sshInfoToMachineSsh(first));
        }
        this.insert(machineSshModels);
        log.info("成功修复 {} 条机器 SSH 数据", machineSshModels.size());
        // 更新 ssh 的机器id
        for (MachineSshModel value : machineSshModels) {
            int update = sshService.updateMachineSshId(value.getId(), value.getHost(), value.getPort(), value.getUser(), value.getConnectType());
            Assert.state(update > 0, "更新 SSH 表机器id 失败：" + value.getName());
        }
    }

    private MachineSshModel sshInfoToMachineSsh(SshModel sshModel) {
        MachineSshModel machineSshModel = new MachineSshModel();
        machineSshModel.setName(sshModel.getName());
        machineSshModel.setGroupName(sshModel.getGroup());
        machineSshModel.setHost(sshModel.getHost());
        machineSshModel.setPort(sshModel.getPort());
        machineSshModel.setUser(sshModel.getUser());
        machineSshModel.setCharset(sshModel.getCharset());
        machineSshModel.setTimeout(sshModel.getTimeout());
        machineSshModel.setPrivateKey(sshModel.getPrivateKey());
        machineSshModel.setPassword(sshModel.getPassword());
        machineSshModel.setConnectType(sshModel.getConnectType());
        machineSshModel.setCreateTimeMillis(sshModel.getCreateTimeMillis());
        machineSshModel.setModifyTimeMillis(sshModel.getModifyTimeMillis());
        machineSshModel.setModifyUser(sshModel.getModifyUser());
        return machineSshModel;
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public void startLoad() {
        String monitorCron = sshConfig.getMonitorCron();
        String cron = (monitorCron == null || monitorCron.isEmpty() ? "0 0/1 * * * ?" : monitorCron);
        CronUtils.add(CRON_ID, cron, () -> MachineSshServer.this);
    }

    @Override
    public void execute() {
        Entity entity = new Entity();
        if (clusterInfoService.isMultiServer()) {
            String linkGroup = clusterInfoService.getCurrent().getLinkGroup();
            List<String> linkGroups = io.voyager1.util.ConvertUtil.splitTrim(linkGroup, ",");
            if ((linkGroups == null || linkGroups.isEmpty())) {
                log.warn("当前集群还未绑定分组,不能监控 SSH 资产信息");
                return;
            }
            entity.set("groupName", linkGroups);
        }
        List<MachineSshModel> list = this.listByEntity(entity, false);
        if ((list == null || list.isEmpty())) {
            return;
        }
        this.checkList(list);
    }

    private void checkList(List<MachineSshModel> monitorModels) {
        monitorModels.forEach(monitorModel -> assetsExecutorPoolService.execute(() -> this.updateMonitor(monitorModel)));
    }

    /**
     * 执行监控 ssh
     *
     * @param machineSshModel 资产 ssh
     */
    private void updateMonitor(MachineSshModel machineSshModel) {
        List<String> monitorGroupName = sshConfig.getDisableMonitorGroupName();
        if (CollUtil.containsAny(monitorGroupName, new java.util.ArrayList<>(java.util.Arrays.asList(machineSshModel.getGroupName())))) {
            // 禁用监控
            if (machineSshModel.getStatus() != null && machineSshModel.getStatus() == 2) {
                // 不需要更新
                return;
            }
            this.updateStatus(machineSshModel.id(), 2, "禁用监控");
            return;
        }
        Session session = null;
        try {
            InputStream sshExecTemplateInputStream = ExtConfigBean.getConfigResourceInputStream("/ssh/monitor-script.sh");
            String sshExecTemplate = IoUtil.readUtf8(sshExecTemplateInputStream);
            Map<String, String> map = new HashMap<>(10);
            map.put("VOYAGER1_AGENT_PID_TAG", AppType.Agent.getTag());
            sshExecTemplate = StringUtil.formatStrByMap(sshExecTemplate, map);
            Charset charset = machineSshModel.charset();
            //
            session = this.getSessionByModelNoFill(machineSshModel);
            int timeout = machineSshModel.timeout();
            List<String> listStr = new ArrayList<>();
            List<String> error = new ArrayList<>();
            JschUtils.execCallbackLine(session, charset, timeout, sshExecTemplate, "", listStr::add, error::add);
            this.updateMonitorInfo(machineSshModel, listStr, error);
        } catch (Exception e) {
            String message = e.getMessage();
            if ((message != null && message.toLowerCase().contains("timeout".toLowerCase()))) {
                log.error("监控 ssh[{}] 超时 {}", machineSshModel.getName(), message);
            } else {
                log.error("监控 ssh[{}] 异常", machineSshModel.getName(), e);
            }
            this.updateStatus(machineSshModel.getId(), 0, message);
        } finally {
            JschUtil.close(session);
        }
    }

    /**
     * 解析监控执行结果
     *
     * @param machineSshModel 监控的ssh
     * @param listStr         结果信息
     * @param errorList       错误信息
     */
    private void updateMonitorInfo(MachineSshModel machineSshModel, List<String> listStr, List<String> errorList) {
        String error = String.join("\n", errorList);
        if ((error != null && !error.isEmpty())) {
            log.error("{} ssh 监控执行存在异常信息：{}", machineSshModel.getName(), error);
        }
        if (log.isDebugEnabled()) {
            log.debug("{} ssh 监控信息结果：{} {}", machineSshModel.getName(), String.join("\n", listStr), error);
        }
        if ((listStr == null || listStr.isEmpty())) {
            this.updateStatus(machineSshModel.getId(), 1, error);
            return;
        }
        Map<String, List<String>> map = new CaseInsensitiveMap<>(listStr.size());
        for (String strItem : listStr) {
            String key = StrUtil.subBefore(strItem, ":", false);
            List<String> list = map.computeIfAbsent(key, s2 -> new ArrayList<>());
            list.add(StrUtil.subAfter(strItem, ":", false));
        }
        MachineSshModel update = new MachineSshModel();
        update.setId(machineSshModel.getId());
        update.setStatus(1);
        update.setOsName(this.getFirstValue(map, "os name"));
        update.setOsVersion(this.getFirstValue(map, "os version"));
        update.setOsLoadAverage(CollUtil.join(map.get("load average"), ","));
        String uptime = this.getFirstValue(map, "uptime");
        if ((uptime != null && !uptime.isEmpty())) {
            try {
                // 可能有时区问题
                DateTime dateTime = DateUtil.parse(uptime);
                update.setOsSystemUptime((System.currentTimeMillis() - dateTime.getTime()));
            } catch (Exception e) {
                error = error + " 解析系统启动时间错误：" + e.getMessage();
                update.setOsSystemUptime(0L);
            }
        }
        update.setOsCpuCores(ConvertUtil.toInt(this.getFirstValue(map, "cpu core"), 0));
        update.setHostName(this.getFirstValue(map, "hostname"));
        update.setOsCpuIdentifierName(this.getFirstValue(map, "model name"));
        // kb
        Long memoryTotal = ConvertUtil.toLong(this.getFirstValue(map, "memory total"), 0L);
        Long memoryUsed = ConvertUtil.toLong(this.getFirstValue(map, "memory used"), 0L);
        update.setOsMoneyTotal(memoryTotal * 1024);
        error = Opt.ofBlankAble(error).map(s -> ",错误信息：" + s).orElse("");
        update.setStatusMsg("执行成功" + error);
        update.setOsOccupyCpu(ConvertUtil.toDouble(this.getFirstValue(map, "cpu usage"), -0D));
        if (memoryTotal > 0) {
            update.setOsOccupyMemory(NumberUtil.div(memoryUsed, memoryTotal, 2));
        } else {
            update.setOsOccupyMemory(-0D);
        }
        List<String> list = map.get("disk info");
        update.setOsMaxOccupyDisk(-0D);
        update.setOsMaxOccupyDiskName("");
        if ((list != null && !list.isEmpty())) {
            long total = 0;
            for (String s : list) {
                List<String> trim = io.voyager1.util.ConvertUtil.splitTrim(s, ":");
                long total1 = ConvertUtil.toLong((1 < trim.size() ? trim.get(1) : null), 0L);
                total += total1;
                long used = ConvertUtil.toLong((2 < trim.size() ? trim.get(2) : null), 0L);
                // 计算最大的硬盘占用
                if (total1 > 0) {
                    Double osMaxOccupyDisk = update.getOsMaxOccupyDisk();
                    osMaxOccupyDisk = (osMaxOccupyDisk != null ? osMaxOccupyDisk : 0D);
                    double occupyDisk = NumberUtil.div(used, total1, 2);
                    if (occupyDisk > osMaxOccupyDisk) {
                        update.setOsMaxOccupyDisk(occupyDisk);
                        update.setOsMaxOccupyDiskName((trim == null || trim.isEmpty() ? null : trim.get(0)));
                    }
                }
            }
            update.setOsFileStoreTotal(total * 1024);
        }
        update.setJavaVersion(this.getFirstValue(map, "java version"));
        update.setVoyager1AgentPid(ConvertUtil.toInt(this.getFirstValue(map, "voyager1 agent pid")));
        //
        String dockerPath = this.getFirstValue(map, "docker path");
        String dockerVersion = this.getFirstValue(map, "docker version");
        if (StrUtil.isAllNotEmpty(dockerVersion, dockerPath)) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("version", dockerVersion);
            jsonObject.put("path", dockerPath);
            update.setDockerInfo(jsonObject.toString());
        } else {
            update.setDockerInfo("");
        }
        this.updateById(update);
    }

    private String getFirstValue(Map<String, List<String>> map, String name) {
        List<String> list = map.get(name);
        String first = (list == null || list.isEmpty() ? null : list.get(0));
        // 内存获取可能最后存在 ：
        return (first != null && first.endsWith(":") ? first.substring(0, first.length() - ":".length()) : first);
    }

    /**
     * 更新 ssh状态
     *
     * @param id     ID
     * @param status 状态值
     * @param msg    错误消息
     */
    private void updateStatus(String id, int status, String msg) {
        MachineSshModel machineSshModel = new MachineSshModel();
        machineSshModel.setId(id);
        machineSshModel.setStatus(status);
        machineSshModel.setStatusMsg(msg);
        //
        machineSshModel.setOsLoadAverage("-");
        machineSshModel.setOsOccupyCpu(-1D);
        machineSshModel.setOsMaxOccupyDisk(-1D);
        machineSshModel.setOsOccupyMemory(-1D);
        machineSshModel.setDockerInfo("");
        machineSshModel.setJavaVersion("");
        machineSshModel.setVoyager1AgentPid(0);
        this.updateById(machineSshModel);
    }

    /**
     * 获取 ssh 回话
     * GLOBAL
     *
     * @param sshModel sshModel
     * @return session
     */
    public Session getSessionByModel(MachineSshModel sshModel) {
        MachineSshModel model = this.getByKey(sshModel.getId(), false);
        Optional.ofNullable(model).ifPresent(machineSshModel -> {
            sshModel.setPassword((sshModel.getPassword() == null || sshModel.getPassword().isEmpty() ? machineSshModel.getPassword() : sshModel.getPassword()));
            sshModel.setPrivateKey((sshModel.getPrivateKey() == null || sshModel.getPrivateKey().isEmpty() ? machineSshModel.getPrivateKey() : sshModel.getPrivateKey()));
        });
        return this.getSessionByModelNoFill(sshModel);
    }

    /**
     * 获取 ssh 回话
     * GLOBAL
     *
     * @param sshModel sshModel
     * @return session
     */
    public Session getSessionByModelNoFill(ISshInfo sshModel) {
        String workspaceId = ServerConst.WORKSPACE_GLOBAL;
        if (sshModel instanceof MachineSshModel) {
            SshModel sshModel1 = sshService.getByMachineSshId(((MachineSshModel) sshModel).getId());
            if (sshModel1 != null) {
                workspaceId = sshModel1.getWorkspaceId();
            }
        }
        Assert.notNull(sshModel, "没有对应 SSH 信息");
        Session session = null;
        int timeout = sshModel.timeout();
        MachineSshModel.ConnectType connectType = sshModel.connectType();
        String user = sshModel.user();
        String password = sshModel.password();
        // 转化密码字段
        IWorkspaceEnvPlugin plugin = (IWorkspaceEnvPlugin) PluginFactory.getPlugin(IWorkspaceEnvPlugin.PLUGIN_NAME);
        try {
            user = plugin.convertRefEnvValue(workspaceId, user);
            password = plugin.convertRefEnvValue(workspaceId, password);
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
        if (connectType == MachineSshModel.ConnectType.PASS) {
            session = JschUtil.openSession(sshModel.host(), sshModel.port(), user, password, timeout);

        } else if (connectType == MachineSshModel.ConnectType.PUBKEY) {
            File rsaFile = null;
            String privateKey = sshModel.privateKey();
            byte[] passwordByte = (password == null || password.isEmpty()) ? null : StrUtil.bytes(password);
            //sshModel.password();
            if ((privateKey != null && privateKey.startsWith(URLUtil.FILE_URL_PREFIX))) {
                String rsaPath = (privateKey != null && privateKey.startsWith(URLUtil.FILE_URL_PREFIX) ? privateKey.substring(URLUtil.FILE_URL_PREFIX.length()) : privateKey);
                rsaFile = FileUtil.file(rsaPath);
            } else if ((privateKey != null && privateKey.startsWith(JschUtils.HEADER))) {
                // 直接采用 private key content 登录，无需写入文件
                session = JschUtils.createSession(sshModel.host(),
                    sshModel.port(),
                    user,
                    (privateKey == null ? null : privateKey.trim()),
                    passwordByte);
            } else if ((privateKey == null || privateKey.isEmpty())) {
                File home = FileUtil.getUserHomeDir();
                Assert.notNull(home, "用户目录没有找到");
                File identity = FileUtil.file(home, ".ssh", "identity");
                rsaFile = FileUtil.isFile(identity) ? identity : null;
                File idRsa = FileUtil.file(home, ".ssh", "id_rsa");
                rsaFile = FileUtil.isFile(idRsa) ? idRsa : rsaFile;
                File idDsa = FileUtil.file(home, ".ssh", "id_dsa");
                rsaFile = FileUtil.isFile(idDsa) ? idDsa : rsaFile;
                Assert.notNull(rsaFile, "用户目录没有找到私钥信息");
            } else {
                //这里的实现，用于把 private key 写入到一个临时文件中，此方式不太采取
                File tempPath = voyager1Application.getTempPath();
                String sshFile = (sshModel.id() == null || sshModel.id().isEmpty() ? java.util.UUID.randomUUID().toString().replace("-", "") : sshModel.id());
                rsaFile = FileUtil.file(tempPath, "ssh", sshFile);
                FileUtil.writeString(privateKey, rsaFile, StandardCharsets.UTF_8);
            }
            // 如果是私钥正文，则 session 已经初始化了
            if (session == null) {
                // 简要私钥文件是否存在
                Assert.state(FileUtil.isFile(rsaFile), "私钥文件不存在：" + FileUtil.getAbsolutePath(rsaFile));
                session = JschUtil.createSession(sshModel.host(),
                    sshModel.port(), user, FileUtil.getAbsolutePath(rsaFile), passwordByte);
            }
            try {
                session.setServerAliveInterval(timeout);
                session.setServerAliveCountMax(5);
            } catch (JSchException e) {
                log.warn("配置 ssh serverAliveInterval 错误", e);
            }
            try {
                session.connect(timeout);
            } catch (JSchException e) {
                throw Lombok.sneakyThrow(e);
            }
        } else {
            throw new IllegalArgumentException("不支持的模式");
        }

        return session;
    }
}
