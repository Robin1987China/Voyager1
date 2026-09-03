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

package io.voyager1.build;

import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.DateTime;
import io.voyager1.util.DateUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.ArrayUtil;
import io.voyager1.util.NumberUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.DigestUtil;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.util.JschUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.plugin.IPlugin;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import lombok.Builder;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.BuildExtConfig;
import io.voyager1.func.assets.model.MachineSshModel;
import io.voyager1.func.assets.server.MachineDockerServer;
import io.voyager1.func.assets.server.ScriptLibraryServer;
import io.voyager1.func.files.service.FileStorageService;
import io.voyager1.model.AfterOpt;
import io.voyager1.model.BaseEnum;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.data.SshModel;
import io.voyager1.model.docker.DockerInfoModel;
import io.voyager1.model.enums.BuildReleaseMethod;
import io.voyager1.model.enums.BuildStatus;
import io.voyager1.model.outgiving.OutGivingModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.outgiving.OutGivingRun;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.plugins.JschUtils;
import io.voyager1.service.docker.DockerInfoService;
import io.voyager1.service.docker.DockerSwarmInfoService;
import io.voyager1.service.node.NodeService;
import io.voyager1.service.node.ssh.SshService;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.system.Voyager1RuntimeException;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.LogRecorder;
import io.voyager1.util.MySftp;
import io.voyager1.util.StringUtil;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 发布管理
 *
 * @since 2019/7/19
 */
@Builder
@Slf4j
public class ReleaseManage {

    private final UserModel userModel;
    private final Integer buildNumberId;
    /**
     * 回滚来源的构建 id
     */
    private Integer fromBuildNumberId;
    private final BuildExtraModule buildExtraModule;
    private final String logId;
    private EnvironmentMapBuilder buildEnv;

    private final LogRecorder logRecorder;
    private File resultFile;
    private Process process;

    private static BuildExecuteService buildExecuteService;
    private static DockerInfoService dockerInfoService;
    private static MachineDockerServer machineDockerServer;
    private static BuildExtConfig buildExtConfig;
    private static FileStorageService fileStorageService;
    private static ScriptLibraryServer scriptLibraryServer;

    private void loadService() {
        buildExecuteService = (buildExecuteService != null ? buildExecuteService : SpringContextHolder.getBean(BuildExecuteService.class));
        dockerInfoService = (dockerInfoService != null ? dockerInfoService : SpringContextHolder.getBean(DockerInfoService.class));
        machineDockerServer = (machineDockerServer != null ? machineDockerServer : SpringContextHolder.getBean(MachineDockerServer.class));
        buildExtConfig = (buildExtConfig != null ? buildExtConfig : SpringContextHolder.getBean(BuildExtConfig.class));
        fileStorageService = (fileStorageService != null ? fileStorageService : SpringContextHolder.getBean(FileStorageService.class));
        scriptLibraryServer = (scriptLibraryServer != null ? scriptLibraryServer : SpringContextHolder.getBean(ScriptLibraryServer.class));
    }

    private Integer getRealBuildNumberId() {
        return (this.fromBuildNumberId != null ? this.fromBuildNumberId : this.buildNumberId);
    }

    private void init() {
        this.loadService();
//        if (this.logRecorder == null) {
//            // 回滚的时候需要重新创建对象
//            File logFile = BuildUtil.getLogFile(buildExtraModule.getId(), this.buildNumberId);
//            this.logRecorder = LogRecorder.builder().file(logFile).build();
//        }
        Assert.notNull(buildEnv, "没有找到任何环境变量");
    }


    private void updateStatus(BuildStatus status, String msg) {
        buildExecuteService.updateStatus(this.buildExtraModule.getId(), this.logId, this.buildNumberId, status, msg);
    }

    /**
     * 不修改为发布中状态
     */
    public String start(Consumer<Long> consumer, BuildInfoModel buildInfoModel) throws Exception {
        this.init();
        this.resultFile = buildExtraModule.resultDirFile(this.getRealBuildNumberId());
        this.buildEnv.put("BUILD_RESULT_FILE", FileUtil.getAbsolutePath(this.resultFile));
        this.buildEnv.put("BUILD_RESULT_DIR_FILE", buildExtraModule.getResultDirFile());
        //
        this.updateStatus(BuildStatus.PubIng, "开始发布中");
        if (FileUtil.isEmpty(this.resultFile)) {
            String info = "发布的文件或者文件夹为空,不能继续发布";
            logRecorder.systemError(info);
            return info;
        }
        long resultFileSize = FileUtil.size(this.resultFile);
        logRecorder.system("开始执行发布，需要发布的文件大小：{}", FileUtil.readableFileSize(resultFileSize));
        Optional.ofNullable(consumer).ifPresent(consumer1 -> consumer1.accept(resultFileSize));
        // 先同步到文件管理中心
        Boolean syncFileStorage = this.buildExtraModule.getSyncFileStorage();
        if (syncFileStorage != null && syncFileStorage) {
            // 处理保留天数
            Integer fileStorageKeepDay =
                Optional.ofNullable(this.buildExtraModule.getFileStorageKeepDay())
                    .map(integer -> ConvertUtil.toInt(buildExtraModule.getFileStorageKeepDay()))
                    .filter(integer -> integer > 0)
                    .orElse(null);
            String keepMsg = fileStorageKeepDay == null ? "" : String.format("，保留天数：%s", fileStorageKeepDay);
            logRecorder.system("开始同步到文件管理中心{}", keepMsg);
            boolean tarGz = this.buildEnv.getBool(BuildUtil.USE_TAR_GZ, false);
            File dirPackage = BuildUtil.loadDirPackage(this.buildExtraModule.getId(), this.getRealBuildNumberId(), this.resultFile, tarGz, (unZip, file) -> file);
            String string = "构建来源,";
            String successMd5 = fileStorageService.addFile(dirPackage, 1,
                buildInfoModel.getWorkspaceId(),
                string + buildInfoModel.getName(),
                // 默认的别名码为构建id
                (buildInfoModel.getAliasCode() == null || buildInfoModel.getAliasCode().isEmpty() ? buildInfoModel.getId() : buildInfoModel.getAliasCode()),
                fileStorageKeepDay);
            if (successMd5 != null) {
                logRecorder.system("构建产物文件成功同步到文件管理中心，{}", successMd5);
            } else {
                logRecorder.systemWarning("构建产物文件同步到文件管理中心失败，当前文件已经存文件管理中心存在啦");
            }
        }
        //
        int releaseMethod = this.buildExtraModule.getReleaseMethod();
        logRecorder.system("发布的方式：{}", BaseEnum.getDescByCode(BuildReleaseMethod.class, releaseMethod));

        if (releaseMethod == BuildReleaseMethod.Outgiving.getCode()) {
            //
            this.doOutGiving();
        } else if (releaseMethod == BuildReleaseMethod.Project.getCode()) {
            this.doProject();
        } else if (releaseMethod == BuildReleaseMethod.Ssh.getCode()) {
            this.doSsh();
        } else if (releaseMethod == BuildReleaseMethod.LocalCommand.getCode()) {
            return this.localCommand();
        } else if (releaseMethod == BuildReleaseMethod.DockerImage.getCode()) {
            return this.doDockerImage();
        } else if (releaseMethod == BuildReleaseMethod.No.getCode()) {
            return null;
        } else {
            String format = String.format("没有实现的发布分发：%s", releaseMethod);
            logRecorder.systemError(format);
            return format;
        }
        return null;
    }

    /**
     * 版本号递增
     *
     * @param dockerTagIncrement 是否开启版本号递增
     * @param dockerTag          当前版本号
     * @return 递增后到版本号
     */
    private String dockerTagIncrement(Boolean dockerTagIncrement, String dockerTag) {
        if (dockerTagIncrement == null || !dockerTagIncrement) {
            return dockerTag;
        }
        List<String> list = io.voyager1.util.ConvertUtil.splitTrim(dockerTag, ",");
        return list.stream()
            .map(s -> {
                List<String> tag = io.voyager1.util.ConvertUtil.splitTrim(s, ":");
                String version = (tag == null || tag.isEmpty() ? null : tag.get(tag.size() - 1));
                List<String> versionList = io.voyager1.util.ConvertUtil.splitTrim(version, ".");
                int tagSize = (tag == null ? 0 : tag.size());
                if (tagSize <= 1 || (versionList == null ? 0 : versionList.size()) <= 1) {
                    logRecorder.systemWarning("version number incrementing error, no match for . or :");
                    return s;
                }
                boolean match = false;
                for (int i = versionList.size() - 1; i >= 0; i--) {
                    String versionParting = versionList.get(i);
                    int versionPartingInt = ConvertUtil.toInt(versionParting, Integer.MIN_VALUE);
                    if (versionPartingInt != Integer.MIN_VALUE) {
                        versionList.set(i, this.buildNumberId + "");
                        match = true;
                        break;
                    }
                }
                tag.set(tagSize - 1, String.join(".", versionList));
                String newVersion = String.join(":", tag);
                if (match) {
                    logRecorder.system("docker 镜像 tag 版本号递增 {} -> {}", s, newVersion);
                } else {
                    logRecorder.systemWarning("版本号递增错误，没有数字版本号 {} ", s);
                }
                return newVersion;
            })
            .collect(Collectors.joining(","));
    }

    private String doDockerImage() {
        // 生成临时目录
        File tempPath = FileUtil.file(Voyager1Application.getInstance().getTempPath(), "build_temp", "docker_image", this.buildExtraModule.getId() + "-" + this.buildNumberId);
        try {
            File sourceFile = BuildUtil.getSourceById(this.buildExtraModule.getId());
            FileUtil.copyContent(sourceFile, tempPath, true);
            // 将产物文件 copy 到本地仓库目录
            File historyPackageFile = BuildUtil.getHistoryPackageFile(buildExtraModule.getId(), this.getRealBuildNumberId(), "/");
            FileUtil.copyContent(historyPackageFile, tempPath, true);
            // env file
            Map<String, String> envMap = buildEnv.environment();
            //File envFile = FileUtil.file(tempPath, ".env");
            String dockerTag = StringUtil.formatStrByMap(this.buildExtraModule.getDockerTag(), envMap);
            //
            dockerTag = this.dockerTagIncrement(this.buildExtraModule.getDockerTagIncrement(), dockerTag);
            // docker file
            String moduleDockerfile = this.buildExtraModule.getDockerfile();
            List<String> list = io.voyager1.util.ConvertUtil.splitTrim(moduleDockerfile, ":");
            String dockerFile = (list == null || list.isEmpty() ? null : list.get(list.size() - 1));
            File dockerfile = FileUtil.file(tempPath, dockerFile);
            if (!FileUtil.isFile(dockerfile)) {
                String format = String.format("仓库目录下没有找到 Dockerfile 文件: %s", dockerFile);
                logRecorder.systemError(format);
                return format;
            }
            File baseDir = FileUtil.file(tempPath, list.size() == 1 ? "/" : (0 < list.size() ? list.get(0) : null));
            //
            String fromTag = this.buildExtraModule.getFromTag();
            // 根据 tag 查询
            List<DockerInfoModel> dockerInfoModels = dockerInfoService
                .queryByTag(this.buildExtraModule.getWorkspaceId(), fromTag);
            Map<String, Object> map = machineDockerServer.dockerParameter(dockerInfoModels);
            if (map == null) {
                String format = String.format("%s 没有可用的 docker server", fromTag);
                logRecorder.systemError(format);
                return format;
            }
            //String dockerBuildArgs = this.buildExtraModule.getDockerBuildArgs();
            for (DockerInfoModel infoModel : dockerInfoModels) {
                boolean done = this.doDockerImage(infoModel, envMap, dockerfile, baseDir, dockerTag, this.buildExtraModule);
                if (!done) {
                    logRecorder.systemWarning("容器构建异常：{} -> {}", infoModel.getName(), dockerTag);
                    if (buildExtraModule.strictlyEnforce()) {
                        return "严格模式下镜像构建失败,终止任务";
                    }
                }
            }
            // 推送 - 只选择一个 docker 服务来推送到远程仓库
            Boolean pushToRepository = this.buildExtraModule.getPushToRepository();
            Boolean pushToRepositoryAfterDelete = this.buildExtraModule.getPushToRepositoryAfterDelete();
            if (pushToRepository != null && pushToRepository) {
                List<String> repositoryList = io.voyager1.util.ConvertUtil.splitTrim(dockerTag, ",");
                Map<String, Object> map2 = new HashMap<>(map);
                for (String repositoryItem : repositoryList) {
                    Object registryUrlObj = map2.get("registryUrl");
                    String registryUrl = (registryUrlObj == null || registryUrlObj.toString().isEmpty()) ? "" : registryUrlObj.toString();
                    Object name = map2.get("name");
                    logRecorder.system("开始推送镜像到远程仓库：({}),{} {}{}", name, registryUrl, repositoryItem, System.lineSeparator());
                    //
                    map2.put("repository", repositoryItem);
                    Consumer<String> logConsumer = logRecorder::info;
                    map2.put("logConsumer", logConsumer);
                    IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
                    try {
                        plugin.execute("pushImage", map2);
                    } catch (Exception e) {
                        logRecorder.error("推送镜像调用容器异常", e);
                    }
                }
            }
            // 发布 docker 服务
            this.updateSwarmService(dockerTag, this.buildExtraModule.getDockerSwarmId(), this.buildExtraModule.getDockerSwarmServiceName());
            // 推送后删除本地镜像
            if (pushToRepository != null && pushToRepository && pushToRepositoryAfterDelete != null && pushToRepositoryAfterDelete) {
                // 删除本地镜像
                List<String> repositoryList = io.voyager1.util.ConvertUtil.splitTrim(dockerTag, ",");
                Map<String, Object> map2 = new HashMap<>(map);
                for (String repositoryItem : repositoryList) {
                    Object name = map2.get("name");
                    logRecorder.system("推送镜像结束后自动删除本地镜像：({}),{} {}", name, repositoryItem, System.lineSeparator());
                    map2.put("imageId", repositoryItem);
                    IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
                    try {
                        plugin.execute("removeImage", map2);
                    } catch (Exception e) {
                        logRecorder.error("删除本地镜像失败", e);
                    }
                }
            }
        } finally {
            CommandUtil.systemFastDel(tempPath);
        }
        return null;
    }

    private void updateSwarmService(String dockerTag, String swarmId, String serviceName) {
        if ((swarmId == null || swarmId.isEmpty())) {
            return;
        }
        List<String> splitTrim = io.voyager1.util.ConvertUtil.splitTrim(dockerTag, ",");
        String first = (splitTrim == null || splitTrim.isEmpty() ? null : splitTrim.get(0));
        logRecorder.system("start update swarm service: {} use image {}", serviceName, first);
        Map<String, Object> pluginMap = machineDockerServer.dockerParameter(swarmId);
        pluginMap.put("serviceId", serviceName);
        pluginMap.put("image", first);
        try {
            IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
            plugin.execute("updateServiceImage", pluginMap);
        } catch (Exception e) {
            logRecorder.error("更新容器服务调用容器异常", e);
            throw Lombok.sneakyThrow(e);
        }
    }

    private boolean doDockerImage(DockerInfoModel dockerInfoModel, Map<String, String> envMap, File dockerfile, File baseDir, String dockerTag, BuildExtraModule extraModule) {
        logRecorder.system("{} 开始构建镜像 {}{}", dockerInfoModel.getName(), dockerTag, System.lineSeparator());
        Map<String, Object> map = machineDockerServer.dockerParameter(dockerInfoModel);
        //.toParameter();
        map.put("Dockerfile", dockerfile);
        map.put("baseDirectory", baseDir);
        //
        map.put("tags", dockerTag);
        map.put("buildArgs", extraModule.getDockerBuildArgs());
        map.put("pull", extraModule.getDockerBuildPull());
        map.put("noCache", extraModule.getDockerNoCache());
        map.put("labels", extraModule.getDockerImagesLabels());
        map.put("env", envMap);
        Consumer<String> logConsumer = logRecorder::append;
        map.put("logConsumer", logConsumer);
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        try {
            return (boolean) plugin.execute("buildImage", map);
        } catch (Exception e) {
            log.error("构建镜像调用容器异常", e);
            logRecorder.error("构建镜像调用容器异常", e);
            return false;
        }
    }

    /**
     * 本地命令执行
     */
    private String localCommand() {
        // 执行命令
        String releaseCommand = this.buildExtraModule.getReleaseCommand();
        if ((releaseCommand == null || releaseCommand.isEmpty())) {
            logRecorder.systemError("没有需要执行的命令");
            return null;
        }
        logRecorder.system("{} 开始执行 {}", DateUtil.now(), System.lineSeparator());
        // 替换脚本库 // 替换全局变量
        releaseCommand = scriptLibraryServer.referenceReplace(releaseCommand);
        File sourceFile = BuildUtil.getSourceById(this.buildExtraModule.getId());
        Map<String, String> envFileMap = buildEnv.environment();

        InputStream templateInputStream = ExtConfigBean.getConfigResourceInputStream("/exec/template." + CommandUtil.SUFFIX);
        String s1 = IoUtil.readUtf8(templateInputStream);
        int waitFor = Voyager1Application.getInstance()
            .execScript(s1 + releaseCommand, file -> {
                try {
                    return CommandUtil.execWaitFor(file, sourceFile, envFileMap, "", (s, process) -> {
                        ReleaseManage.this.process = process;
                        logRecorder.info(s);
                    });
                } catch (IOException | InterruptedException e) {
                    throw Lombok.sneakyThrow(e);
                }
            });
        ReleaseManage.this.process = null;
        logRecorder.system("执行发布脚本的退出码是：{}", waitFor);
        // 判断是否为严格执行
        if (buildExtraModule.strictlyEnforce()) {
            return waitFor == 0 ? null : String.format("执行发布命令退出码非0，%s", waitFor);
        }
        return null;
    }

    /**
     * ssh 发布
     */
    private void doSsh() throws IOException {
        String releaseMethodDataId = this.buildExtraModule.getReleaseMethodDataId();
        SshService sshService = SpringContextHolder.getBean(SshService.class);
        List<String> strings = io.voyager1.util.ConvertUtil.splitTrim(releaseMethodDataId, ",");
        for (String releaseMethodDataIdItem : strings) {
            SshModel item = sshService.getByKey(releaseMethodDataIdItem, false);
            if (item == null) {
                logRecorder.systemError("没有找到对应的ssh项：{}", releaseMethodDataIdItem);
                continue;
            }
            this.doSsh(item, sshService);
        }
    }

    private void doSsh(SshModel item, SshService sshService) throws IOException {
        Map<String, String> envFileMap = buildEnv.environment();
        MachineSshModel machineSshModel = sshService.getMachineSshModel(item);
        Session session = null;
        ChannelSftp channelSftp = null;
        try {
            session = sshService.getSessionByModel(machineSshModel);
            Charset charset = machineSshModel.charset();
            int timeout = machineSshModel.timeout();
            String releasePath = this.buildExtraModule.getReleasePath();
            envFileMap.put("SSH_RELEASE_PATH", releasePath);
            // 执行发布前命令
            if ((this.buildExtraModule.getReleaseBeforeCommand() != null && !this.buildExtraModule.getReleaseBeforeCommand().isEmpty())) {
                //
                logRecorder.system("开始执行 {} 发布前命令", item.getName());
                JschUtils.execCallbackLine(session, charset, timeout, this.buildExtraModule.getReleaseBeforeCommand(), "", envFileMap, logRecorder::info);
            }

            if ((releasePath == null || releasePath.isEmpty())) {
                logRecorder.systemWarning("发布目录为空");
            } else {
                logRecorder.system("{} {} 开始上传 FTP 文件{}", DateUtil.now(), item.getName(), System.lineSeparator());
                MySftp.ProgressMonitor sftpProgressMonitor = sshService.createProgressMonitor(logRecorder);
                MySftp sftp = new MySftp(session, charset, timeout, sftpProgressMonitor);
                channelSftp = sftp.getClient();
                String prefix = "";
                if (!(releasePath != null && releasePath.startsWith("/"))) {
                    prefix = sftp.pwd();
                }
                String normalizePath = FileUtil.normalize(prefix + "/" + releasePath);
                if (this.buildExtraModule.isClearOld()) {
                    try {
                        if (sftp.exist(normalizePath)) {
                            sftp.delDir(normalizePath);
                        }
                    } catch (Exception e) {
                        if (!(e.getMessage() != null && e.getMessage().toLowerCase().startsWith("No such file".toLowerCase()))) {
                            logRecorder.error("清除构建产物失败", e);
                        }
                    }
                }
                sftp.syncUpload(this.resultFile, normalizePath);
                logRecorder.system("{} ftp upload done", item.getName());
            }
            // 执行发布后命令
            if ((this.buildExtraModule.getReleaseCommand() == null || this.buildExtraModule.getReleaseCommand().isEmpty())) {
                logRecorder.systemWarning("没有需要执行发布后的ssh命令");
                return;
            }
            //
            logRecorder.system("开始执行 {} 发布后命令", item.getName());
            JschUtils.execCallbackLine(session, charset, timeout, this.buildExtraModule.getReleaseCommand(), "", envFileMap, logRecorder::info);
        } finally {
            JschUtil.close(channelSftp);
            JschUtil.close(session);
        }
    }

    /**
     * 差异上传发布
     *
     * @param nodeModel 节点
     * @param projectId 项目ID
     * @param afterOpt  发布后的操作
     */
    private void diffSyncProject(NodeModel nodeModel, String projectId, AfterOpt afterOpt, boolean clearOld) {
        File resultFile = this.resultFile;
        String resultFileParent = resultFile.isFile() ?
            FileUtil.getAbsolutePath(resultFile.getParent()) : FileUtil.getAbsolutePath(this.resultFile);
        //
        List<File> files = FileUtil.loopFiles(resultFile);
        List<JSONObject> collect = files.stream().map(file -> {
            //
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", StringUtil.delStartPath(file, resultFileParent, true));
            jsonObject.put("sha1", DigestUtil.sha1(file));
            return jsonObject;
        }).collect(Collectors.toList());
        //
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", projectId);
        jsonObject.put("data", collect);
        String directory = this.buildExtraModule.getProjectSecondaryDirectory();
        directory = (directory == null || directory.isEmpty() ? "/" : directory);
        jsonObject.put("dir", directory);
        ApiResult<JSONObject> requestBody = NodeForward.requestBody(nodeModel, NodeUrl.MANAGE_FILE_DIFF_FILE, jsonObject);
        Assert.state(requestBody.success(), "对比项目文件失败：" + requestBody);

        JSONObject data = requestBody.getData();
        JSONArray diff = data.getJSONArray("diff");
        JSONArray del = data.getJSONArray("del");
        int delSize = (del == null ? 0 : del.size());
        int diffSize = (diff == null ? 0 : diff.size());
        if (clearOld) {
            logRecorder.system("对比文件结果，产物文件 {} 个、需要上传 {} 个、需要删除 {} 个", (collect == null ? 0 : collect.size()), (diff == null ? 0 : diff.size()), delSize);
        } else {
            logRecorder.system("对比文件结果，产物文件 {} 个、需要上传 {} 个", (collect == null ? 0 : collect.size()), (diff == null ? 0 : diff.size()));
        }
        // 清空发布才先执行删除
        if (delSize > 0 && clearOld) {
            jsonObject.put("data", del);
            requestBody = NodeForward.requestBody(nodeModel, NodeUrl.MANAGE_FILE_BATCH_DELETE, jsonObject);
            Assert.state(requestBody.success(), "删除项目文件失败：" + requestBody);
        }
        for (int i = 0; i < diffSize; i++) {
            boolean last = (i == diffSize - 1);
            JSONObject diffData = (JSONObject) diff.get(i);
            String name = diffData.getString("name");
            File file = FileUtil.file(resultFileParent, name);
            //
            String startPath = StringUtil.delStartPath(file, resultFileParent, false);
            startPath = FileUtil.normalize(startPath + "/" + directory);
            //
            Set<Integer> progressRangeList = ConcurrentHashMap.newKeySet((int) Math.floor((float) 100 / buildExtConfig.getLogReduceProgressRatio()));
            int finalI = i;
            ApiResult<String> jsonMessage = OutGivingRun.fileUpload(file, file.getName(), startPath,
                projectId, false, last ? afterOpt : AfterOpt.No, nodeModel, false,
                this.buildExtraModule.getProjectUploadCloseFirst(), (total, progressSize) -> {
                    double progressPercentage = Math.floor(((float) progressSize / total) * 100);
                    int progressRange = (int) Math.floor(progressPercentage / buildExtConfig.getLogReduceProgressRatio());
                    if (progressRangeList.add(progressRange)) {
                        //  total, progressSize
                        String info = "上传文件进度：{}[{}/{}] {}/{} {} ";
                        logRecorder.system(info, file.getName(),
                            (finalI + 1), diffSize,
                            FileUtil.readableFileSize(progressSize), FileUtil.readableFileSize(total),
                            NumberUtil.formatPercent(((float) progressSize / total), 0)
                        );
                    }
                });
            Assert.state(jsonMessage.success(), "同步项目文件失败：" + jsonMessage);
            if (last) {
                // 最后一个
                logRecorder.system("发布项目包成功：{}", jsonMessage);
            }
        }
    }

    /**
     * 发布项目
     */
    private void doProject() {
        //AfterOpt afterOpt, boolean clearOld, boolean diffSync
        AfterOpt afterOpt = BaseEnum.getEnum(AfterOpt.class, this.buildExtraModule.getAfterOpt(), AfterOpt.No);
        boolean clearOld = this.buildExtraModule.isClearOld();
        boolean diffSync = this.buildExtraModule.isDiffSync();
        String releaseMethodDataId = this.buildExtraModule.getReleaseMethodDataId();
        String[] strings = releaseMethodDataId.split(java.util.regex.Pattern.quote(":"));
        if (ArrayUtil.length(strings) != 2) {
            throw new IllegalArgumentException(releaseMethodDataId + " error");
        }
        NodeService nodeService = SpringContextHolder.getBean(NodeService.class);
        NodeModel nodeModel = nodeService.getByKey(strings[0]);
        Objects.requireNonNull(nodeModel, "节点不存在");
        String projectId = strings[1];
        if (diffSync) {
            this.diffSyncProject(nodeModel, projectId, afterOpt, clearOld);
            return;
        }
        boolean tarGz = this.buildEnv.getBool(BuildUtil.USE_TAR_GZ, false);
        ApiResult<String> jsonMessage = BuildUtil.loadDirPackage(this.buildExtraModule.getId(), this.getRealBuildNumberId(), this.resultFile, tarGz, (unZip, zipFile) -> {
            String name = zipFile.getName();
            Set<Integer> progressRangeList = ConcurrentHashMap.newKeySet((int) Math.floor((float) 100 / buildExtConfig.getLogReduceProgressRatio()));
            return OutGivingRun.fileUpload(zipFile, zipFile.getName(),
                this.buildExtraModule.getProjectSecondaryDirectory(),
                projectId,
                unZip,
                afterOpt,
                nodeModel, clearOld, this.buildExtraModule.getProjectUploadCloseFirst(), (total, progressSize) -> {
                    double progressPercentage = Math.floor(((float) progressSize / total) * 100);
                    int progressRange = (int) Math.floor(progressPercentage / buildExtConfig.getLogReduceProgressRatio());
                    if (progressRangeList.add(progressRange)) {
                        logRecorder.system("上传文件进度：{} {}/{} {}", name,
                            FileUtil.readableFileSize(progressSize), FileUtil.readableFileSize(total),
                            NumberUtil.formatPercent(((float) progressSize / total), 0));
                    }
                });
        });
        if (jsonMessage.success()) {
            logRecorder.system("发布项目包成功：{}", jsonMessage);
        } else {
            throw new Voyager1RuntimeException("发布项目包失败：" + jsonMessage);
        }
    }

    /**
     * 分发包
     */
    private void doOutGiving() throws ExecutionException, InterruptedException {
        String releaseMethodDataId = this.buildExtraModule.getReleaseMethodDataId();
        String projectSecondaryDirectory = this.buildExtraModule.getProjectSecondaryDirectory();
        //
        String selectProject = buildEnv.get("dispatchSelectProject");
        boolean tarGz = buildEnv.getBool(BuildUtil.USE_TAR_GZ, false);
        Future<OutGivingModel.Status> statusFuture = BuildUtil.loadDirPackage(this.buildExtraModule.getId(), this.getRealBuildNumberId(), this.resultFile, tarGz, (unZip, zipFile) -> {
            OutGivingRun.OutGivingRunBuilder outGivingRunBuilder = OutGivingRun.builder()
                .id(releaseMethodDataId)
                .file(zipFile)
                .logRecorder(logRecorder)
                .userModel(userModel)
                .mode("build-trigger")
                .modeData(buildExtraModule.getId())
                .unzip(unZip)
                // 由构建配置决定是否删除
                .doneDeleteFile(false)
                .projectSecondaryDirectory(projectSecondaryDirectory)
                .stripComponents(0);
            return outGivingRunBuilder.build().startRun(selectProject);
        });
        //OutGivingRun.startRun(releaseMethodDataId, zipFile, userModel, unZip, 0);
        logRecorder.system("开始执行分发包啦，请到分发中查看详情状态");
        OutGivingModel.Status status = statusFuture.get();
        logRecorder.system("分发结果：{}", status.getDesc());
    }

    /**
     * 回滚
     *
     * @param item 构建对象
     */
    public void rollback(BuildInfoModel item) {
        try {
            BaseServerController.resetInfo(userModel);
            this.init();
            //
            buildEnv.eachStr(logRecorder::system);
            logRecorder.system("开始回滚：{}", DateTime.now());
            //
            String errorMsg = this.start(null, item);
            String emptied = (errorMsg == null || errorMsg.isEmpty() ? "ok" : errorMsg);
            logRecorder.system("执行回滚结束：{}", emptied);
            if (errorMsg == null) {
                this.updateStatus(BuildStatus.PubSuccess, "发布成功");
            } else {
                this.updateStatus(BuildStatus.PubError, errorMsg);
            }
        } catch (Exception e) {
            log.error("执行发布异常", e);
            logRecorder.error("执行发布异常", e);
            this.updateStatus(BuildStatus.PubError, e.getMessage());
        } finally {
            IoUtil.close(this.logRecorder);
        }
    }
}
