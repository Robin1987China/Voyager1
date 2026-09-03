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

package io.voyager1.system;

import io.voyager1.util.CollUtil;
import io.voyager1.util.DateException;
import io.voyager1.util.DateTime;
import io.voyager1.util.DateUnit;
import io.voyager1.util.DateUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.NetUtil;
import io.voyager1.util.UrlBuilder;
import io.voyager1.util.ThreadUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.HttpResponse;
import io.voyager1.util.HttpUtil;
import io.voyager1.event.ISystemTask;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.common.ILoadEvent;
import io.voyager1.common.RemoteVersion;
import io.voyager1.common.commander.ProjectCommander;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.AgentAuthorize;
import io.voyager1.configuration.AgentConfig;
import io.voyager1.configuration.ProjectLogConfig;
import io.voyager1.cron.CronUtils;
import io.voyager1.model.RunMode;
import io.voyager1.model.data.NodeProjectInfoModel;
import io.voyager1.script.BaseRunScript;
import io.voyager1.service.manage.ProjectInfoService;
import io.voyager1.socket.ConsoleCommandOp;
import io.voyager1.util.CommandUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自动备份控制台日志，防止日志文件过大
 *
 * @since 2019/3/17
 */
@Slf4j
@Configuration
public class AgentStartInit implements ILoadEvent, ISystemTask {

    private static final String ID = "auto_back_log";
    private final ProjectInfoService projectInfoService;
    private final AgentConfig agentConfig;
    private final AgentAuthorize agentAuthorize;
    private final Voyager1Application voyager1Application;
    private final ProjectCommander projectCommander;
    private final ProjectLogConfig projectLogConfig;


    public AgentStartInit(ProjectInfoService projectInfoService,
                          AgentConfig agentConfig,
                          Voyager1Application voyager1Application,
                          ProjectCommander projectCommander) {
        this.projectInfoService = projectInfoService;
        this.agentConfig = agentConfig;
        this.agentAuthorize = agentConfig.getAuthorize();
        this.voyager1Application = voyager1Application;
        this.projectCommander = projectCommander;
        projectLogConfig = agentConfig.getProject().getLog();
    }


    private void startAutoBackLog() {
        // 获取cron 表达式
        String cron = Opt.ofBlankAble(projectLogConfig.getAutoBackupConsoleCron()).orElse("0 0/10 * * * ?");
        //
        CronUtils.upsert(ID, cron, () -> {
            try {
                List<NodeProjectInfoModel> list = projectInfoService.list();
                if (list == null) {
                    return;
                }
                //
                list.forEach(this::checkProject);
            } catch (Exception e) {
                log.error("定时备份日志失败", e);
            }
        });
    }

    private void checkProject(NodeProjectInfoModel nodeProjectInfoModel) {
        File file = projectInfoService.resolveAbsoluteLogFile(nodeProjectInfoModel);
        if (!file.exists()) {
            return;
        }
        DataSize autoBackSize = projectLogConfig.getAutoBackupSize();
        autoBackSize = Optional.ofNullable(autoBackSize).orElseGet(() -> DataSize.ofMegabytes(50));
        long len = file.length();
        if (len > autoBackSize.toBytes()) {
            try {
                projectCommander.backLog(nodeProjectInfoModel);
            } catch (Exception e) {
                log.warn("auto back log", e);
            }
        }
        // 清理过期的文件
        File logFile = projectInfoService.resolveLogBack(nodeProjectInfoModel);
        DateTime nowTime = DateTime.now();
        List<File> files = FileUtil.loopFiles(logFile, pathname -> {
            DateTime dateTime = DateUtil.date(pathname.lastModified());
            long days = DateUtil.betweenDay(dateTime, nowTime, false);
            long saveDays = projectLogConfig.getSaveDays();
            return days > saveDays;
        });
        files.forEach(FileUtil::del);
    }

    @Override
    public void executeTask() {
        // 启动加载
        RemoteVersion.loadRemoteInfo();
        // 清空脚本缓存
        BaseRunScript.clearRunScript();
        // 清理临时文件
        File tempPath = agentConfig.getTempPath();
        if (FileUtil.exist(tempPath)) {
            File[] files = tempPath.listFiles((dir, name) -> {
                try {
                    DateTime dateTime = DateUtil.parse(name);
                    long between = DateUtil.between(dateTime, DateTime.now(), DateUnit.DAY);
                    // 保留一天以内的
                    return between > 1;
                } catch (DateException dateException) {
                    return false;
                }
            });
            Optional.ofNullable(files).ifPresent(files1 -> {
                for (File file : files1) {
                    CommandUtil.systemFastDel(file);
                }
            });
        }
    }

    /**
     * 尝试开启项目
     */
    private void autoStartProject() {
        List<NodeProjectInfoModel> allProject = projectInfoService.list();
        if ((allProject == null || allProject.isEmpty())) {
            return;
        }
        List<NodeProjectInfoModel> startList = allProject.stream()
            .filter(nodeProjectInfoModel -> nodeProjectInfoModel.getAutoStart() != null && nodeProjectInfoModel.getAutoStart())
            .collect(Collectors.toList());
        ThreadUtil.execute(() -> {
            for (NodeProjectInfoModel nodeProjectInfoModel : startList) {
                try {
                    if (!projectCommander.isRun(nodeProjectInfoModel)) {
                        projectCommander.execCommand(ConsoleCommandOp.start, nodeProjectInfoModel);
                    }
                } catch (Exception e) {
                    log.warn("自动启动项目失败：{} {}", nodeProjectInfoModel.getId(), e.getMessage());
                }
            }
        });
        // 迁移备份日志文件
        allProject.stream()
            .filter(nodeProjectInfoModel -> nodeProjectInfoModel.getRunMode() != RunMode.Link)
            .filter(nodeProjectInfoModel -> (nodeProjectInfoModel.getLogPath() == null || nodeProjectInfoModel.getLogPath().isEmpty()))
            .forEach(nodeProjectInfoModel -> {
                String logPath = new File(nodeProjectInfoModel.allLib()).getParent();
                String log1 = FileUtil.normalize(String.format("%s/%s.log", logPath, nodeProjectInfoModel.getId()));
                File logBack = new File(log1 + "_back");
                if (FileUtil.isDirectory(logBack)) {
                    File resolveLogBack = projectInfoService.resolveLogBack(nodeProjectInfoModel);
                    FileUtil.mkdir(resolveLogBack);
                    log.info("自动迁移存在备份日志 {} -> {}", logBack.getAbsolutePath(), resolveLogBack);
                    FileUtil.moveContent(logBack, resolveLogBack, true);
                    FileUtil.del(logBack);
                }
                if (FileUtil.isFile(log1)) {
                    if (projectCommander.isRun(nodeProjectInfoModel)) {
                        log.warn("存在旧版项目日志但项目在运行中需要停止运行后手动迁移：{} {}", nodeProjectInfoModel.getName(), log1);
                    } else {
                        File resolveLogBack = projectInfoService.resolveLogBack(nodeProjectInfoModel);
                        FileUtil.mkdir(resolveLogBack);
                        log.info("自动迁移存在日志 {} -> {}", log1, resolveLogBack);
                        FileUtil.move(FileUtil.file(log1), resolveLogBack, true);
                    }
                }
            });
    }


    /**
     * 自动推送插件端信息到服务端
     *
     * @param url 服务端url
     */
    public void autoPushToServer(String url) {
        url = (url != null && url.endsWith("'") ? url.substring(0, url.length() - 1) : url);
        url = (url != null && url.startsWith("'") ? url.substring(1) : url);
        UrlBuilder urlBuilder = UrlBuilder.ofHttp(url);
        String networkName = (String) urlBuilder.getQuery().get("networkName");
        //
        LinkedHashSet<InetAddress> localAddressList = NetUtil.localAddressList(networkInterface -> (networkName == null || networkName.isEmpty()) || java.util.Objects.equals(networkName, networkInterface.getName()), address -> {
            // 非loopback地址，指127.*.*.*的地址
            return !address.isLoopbackAddress()
                // 需为IPV4地址
                && address instanceof Inet4Address;
        });
        if ((networkName != null && !networkName.isEmpty()) && (localAddressList == null || localAddressList.isEmpty())) {
            log.warn("No usable IP found by NIC name,{}", networkName);
        }
        Set<String> ips = localAddressList.stream().map(InetAddress::getHostAddress).filter(StrUtil::isNotEmpty).collect(Collectors.toSet());
        urlBuilder.addQuery("ips", String.join(",", ips));
        urlBuilder.addQuery("loginName");
        urlBuilder.addQuery("loginPwd", agentAuthorize.getAgentPwd());
        int port = voyager1Application.getPort();
        urlBuilder.addQuery("port", port + "");
        //
        String build = urlBuilder.build();
        try (HttpResponse execute = HttpUtil.createGet(build, true).execute()) {
            String body = execute.body();
            log.info("推送注册结果:{}", body);
        }
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        this.startAutoBackLog();
        this.autoStartProject();
    }
}
