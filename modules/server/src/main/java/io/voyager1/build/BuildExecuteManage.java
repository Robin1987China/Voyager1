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
import io.voyager1.util.BetweenFormatter;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.FileCopier;
import io.voyager1.util.Opt;
import io.voyager1.util.Tuple;

import io.voyager1.util.UrlQuery;
import io.voyager1.util.ThreadUtil;
import io.voyager1.util.ArrayUtil;
import io.voyager1.util.EnumUtil;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.model.BaseIdModel;
import io.voyager1.plugin.IPlugin;
import lombok.Builder;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerConst;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.i18n.I18nThreadUtil;
import io.voyager1.configuration.BuildExtConfig;
import io.voyager1.exception.LogRecorderCloseException;
import io.voyager1.func.assets.server.MachineDockerServer;
import io.voyager1.func.assets.server.ScriptLibraryServer;
import io.voyager1.func.files.service.FileStorageService;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.data.CommandExecLogModel;
import io.voyager1.model.data.RepositoryModel;
import io.voyager1.model.data.WorkspaceModel;
import io.voyager1.model.docker.DockerInfoModel;
import io.voyager1.model.enums.BuildReleaseMethod;
import io.voyager1.model.enums.BuildStatus;
import io.voyager1.model.log.BuildHistoryLog;
import io.voyager1.model.script.ScriptExecuteLogModel;
import io.voyager1.model.script.ScriptModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.dblog.BuildInfoService;
import io.voyager1.service.dblog.DbBuildHistoryLogService;
import io.voyager1.service.docker.DockerInfoService;
import io.voyager1.service.script.ScriptExecuteLogServer;
import io.voyager1.service.script.ScriptServer;
import io.voyager1.service.system.WorkspaceService;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.util.*;
import io.voyager1.webhook.DefaultWebhookPluginImpl;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

/**
 * @since 2023/3/30
 */
@Builder
@Slf4j
public class BuildExecuteManage implements Runnable {
    /**
     * 缓存构建中
     */
    public static final Map<String, BuildExecuteManage> BUILD_MANAGE_MAP = new java.util.concurrent.ConcurrentHashMap<>();

    private final TaskData taskData;
    private final BuildExtraModule buildExtraModule;
    private final String logId;
    //
    private Process process;
    private LogRecorder logRecorder;
    private File gitFile;
    private Thread currentThread;
    private ReleaseManage releaseManage;
    private String language;

    /**
     * 提交任务时间
     */
    private Long submitTaskTime;

    private static BuildExecuteService buildExecuteService;
    private static ScriptServer scriptServer;
    private static ScriptExecuteLogServer scriptExecuteLogServer;
    private static BuildInfoService buildService;
    private static DbBuildHistoryLogService dbBuildHistoryLogService;
    private static DockerInfoService dockerInfoService;
    private static MachineDockerServer machineDockerServer;
    private static BuildExtConfig buildExtConfig;
    private static FileStorageService fileStorageService;
    private static BuildExecutorPoolService buildExecutorPoolService;
    private static WorkspaceService workspaceService;
    private static ScriptLibraryServer scriptLibraryServer;

    private void loadService() {
        buildExecuteService = (buildExecuteService != null ? buildExecuteService : SpringContextHolder.getBean(BuildExecuteService.class));
        scriptServer = (scriptServer != null ? scriptServer : SpringContextHolder.getBean(ScriptServer.class));
        scriptExecuteLogServer = (scriptExecuteLogServer != null ? scriptExecuteLogServer : SpringContextHolder.getBean(ScriptExecuteLogServer.class));
        buildService = (buildService != null ? buildService : SpringContextHolder.getBean(BuildInfoService.class));
        dbBuildHistoryLogService = (dbBuildHistoryLogService != null ? dbBuildHistoryLogService : SpringContextHolder.getBean(DbBuildHistoryLogService.class));
        dockerInfoService = (dockerInfoService != null ? dockerInfoService : SpringContextHolder.getBean(DockerInfoService.class));
        machineDockerServer = (machineDockerServer != null ? machineDockerServer : SpringContextHolder.getBean(MachineDockerServer.class));
        buildExtConfig = (buildExtConfig != null ? buildExtConfig : SpringContextHolder.getBean(BuildExtConfig.class));
        fileStorageService = (fileStorageService != null ? fileStorageService : SpringContextHolder.getBean(FileStorageService.class));
        buildExecutorPoolService = (buildExecutorPoolService != null ? buildExecutorPoolService : SpringContextHolder.getBean(BuildExecutorPoolService.class));
        workspaceService = (workspaceService != null ? workspaceService : SpringContextHolder.getBean(WorkspaceService.class));
        scriptLibraryServer = (scriptLibraryServer != null ? scriptLibraryServer : SpringContextHolder.getBean(ScriptLibraryServer.class));
    }

    /**
     * 正在构建的数量
     *
     * @return 构建数量
     */
    public static Set<String> buildKeys() {
        return BUILD_MANAGE_MAP.keySet();
    }


    /**
     * 提交任务
     */
    public void submitTask() {
        this.loadService();
        submitTaskTime = System.currentTimeMillis();
        language = I18nMessageUtil.getLanguageByRequest();
        // 创建线程池
        ThreadPoolExecutor threadPoolExecutor = buildExecutorPoolService.getThreadPoolExecutor();
        //
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        File logFile = BuildUtil.getLogFile(buildInfoModel.getId(), buildInfoModel.getBuildId());
        this.logRecorder = LogRecorder.builder().file(logFile).build();
        //
        int queueSize = threadPoolExecutor.getQueue().size();
        int size = BUILD_MANAGE_MAP.size();
        logRecorder.system("当前构建中任务数：{},队列中任务数：{} {}", size, queueSize,
            size > buildExtConfig.getPoolSize() ? "构建任务开始进入队列等待...." : "");
        //BuildInfoManage manage = new BuildInfoManage(taskData);
        BUILD_MANAGE_MAP.put(buildInfoModel.getId(), this);
        threadPoolExecutor.execute(this);
    }

    /**
     * 取消任务(拒绝执行)
     */
    public void rejectedExecution() {
        ThreadPoolExecutor threadPoolExecutor = buildExecutorPoolService.getThreadPoolExecutor();
        int queueSize = threadPoolExecutor.getQueue().size();
        int limitPoolSize = threadPoolExecutor.getPoolSize();
        int corePoolSize = threadPoolExecutor.getCorePoolSize();
        String format = String.format("当前构建中任务数：{}, 队列中任务数：{} 构建任务等待超时或者超出最大等待数量, 当前运行中的任务数：{}/{}, 取消执行当前构建", BUILD_MANAGE_MAP.size(), queueSize, limitPoolSize, corePoolSize);
        logRecorder.system(format);
        this.cancelTask(format);
    }

    /**
     * 取消任务
     */
    private void cancelTask(String desc) {
        CommandUtil.kill(process);
        ApacheExecUtil.kill(this.logId);
        Integer buildMode = taskData.buildInfoModel.getBuildMode();
        if (buildMode != null && buildMode == 1) {
            // 容器构建 删除容器
            try {
                Optional.ofNullable(taskData.dockerParameter).ifPresent(parameter -> {
                    IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
                    parameter.put("containerId", taskData.buildContainerId);
                    try {
                        plugin.execute("removeContainer", parameter);
                    } catch (Exception e) {
                        throw Lombok.sneakyThrow(e);
                    }
                });

            } catch (Exception e) {
                log.warn("清理构建资源失败", e);
            }
        }
        String buildId = taskData.buildInfoModel.getId();
        buildExecuteService.updateStatus(buildId, logId, taskData.buildInfoModel.getBuildId(), BuildStatus.Cancel, desc);
        Optional.ofNullable(currentThread).ifPresent(Thread::interrupt);
        BUILD_MANAGE_MAP.remove(buildId);
        IoUtil.close(logRecorder);
    }

    /**
     * 打包构建产物
     */
    private String packageFile() {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        Integer buildMode = taskData.buildInfoModel.getBuildMode();
        String resultDirFile = buildInfoModel.getResultDirFile();
        String excludeReleaseAnt = this.buildExtraModule.getExcludeReleaseAnt();
        boolean releaseHideFile = (this.buildExtraModule.getReleaseHideFile() != null ? this.buildExtraModule.getReleaseHideFile() : false);
        List<String> excludeReleaseAnts = io.voyager1.util.ConvertUtil.splitTrim(excludeReleaseAnt, ",");
        ResultDirFileAction resultDirFileAction = ResultDirFileAction.parse(resultDirFile);
        final int[] excludeReleaseAntCount = {0};
        Predicate<String> predicate = file -> {
            if ((excludeReleaseAnts == null || excludeReleaseAnts.isEmpty())) {
                return true;
            }
            for (String releaseAnt : excludeReleaseAnts) {
                if (AntPathUtil.ANT_PATH_MATCHER.match(releaseAnt, file)) {
                    // 过滤
                    excludeReleaseAntCount[0]++;
                    return false;
                }
            }
            return true;
        };
        if (buildMode != null && buildMode == 1) {
            // 容器构建直接下载到 结果目录
            File toFile = BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId(), resultDirFileAction.getPath());
            if (!FileUtil.exist(toFile)) {
                String format = String.format("%s 不存在，处理构建产物失败", resultDirFileAction.getPath());
                logRecorder.systemError(format);
                return format;
            }
            logRecorder.system("备份产物 {} {}", resultDirFileAction.getPath(), buildInfoModel.getBuildId());
            return null;
        }
        if (resultDirFileAction.getType() == ResultDirFileAction.Type.ANT_PATH) {
            // 通配模式
            List<String> paths = AntPathUtil.antPathMatcher(this.gitFile, resultDirFileAction.getPath());
            int matcherSize = (paths == null ? 0 : paths.size());
            if (matcherSize <= 0) {
                String format = String.format("%s 没有匹配到任何文件", resultDirFileAction.getPath());
                logRecorder.systemError(format);
                return format;
            }
            logRecorder.system("{} 模糊匹配到 {} 个文件", resultDirFileAction.getPath(), matcherSize);
            String antSubMatch = resultDirFileAction.antSubMatch();
            ResultDirFileAction.AntFileUploadMode antFileUploadMode = resultDirFileAction.getAntFileUploadMode();
            Assert.notNull(antFileUploadMode, "没有配置文件上传模式");
            File historyPackageFile = BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId(), "/");
            int subMatchCount = paths.stream()
                .filter(s -> {
                    // 需要能满足二级匹配
                    return (antSubMatch == null || antSubMatch.isEmpty()) || AntPathUtil.ANT_PATH_MATCHER.matchStart(antSubMatch + "**", s);
                })
                .filter(predicate)
                .mapToInt(path -> {
                    File toFile;
                    if (antFileUploadMode == ResultDirFileAction.AntFileUploadMode.KEEP_DIR) {
                        // 剔除文件夹层级
                        List<String> list = io.voyager1.util.ConvertUtil.splitTrim(path, "/");
                        int notMathIndex;
                        int pathItemSize = list.size();
                        if ((antSubMatch == null || antSubMatch.isEmpty()) || java.util.Objects.equals(antSubMatch, "/")) {
                            notMathIndex = 0;
                        } else {
                            notMathIndex = ArrayUtil.INDEX_NOT_FOUND;
                            for (int i = pathItemSize - 1; i >= 0; i--) {
                                String suffix = i == pathItemSize - 1 ? "" : "/";
                                String itemS = "/" + CollUtil.join(list.subList(0, i + 1), "/") + suffix;
                                if (AntPathUtil.ANT_PATH_MATCHER.match(antSubMatch, itemS)) {
                                    notMathIndex = i + 1;
                                    // 结束本次循环
                                    break;
                                }
                            }
                            if (notMathIndex == ArrayUtil.INDEX_NOT_FOUND) {
                                return 0;
                            }
                        }
                        // 保留文件夹层级
                        String itemEnd = CollUtil.join(list.subList(notMathIndex, pathItemSize), "/");
                        toFile = FileUtil.file(historyPackageFile, itemEnd);
                    } else if (antFileUploadMode == ResultDirFileAction.AntFileUploadMode.SAME_DIR) {
                        toFile = historyPackageFile;
                    } else {
                        throw new IllegalStateException("暂不支持的模式：" + antFileUploadMode);
                    }
                    // 创建文件夹，避免出现文件全部为相关文件名（result）
                    BuildUtil.mkdirHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId());
                    File srcFile = FileUtil.file(this.gitFile, path);
                    //
                    FileCopier.create(srcFile, toFile)
                        .setCopyContentIfDir(true)
                        .setOverride(true)
                        .setCopyAttributes(true)
                        .setCopyFilter(file -> releaseHideFile || !file.isHidden())
                        .copy();
                    return 1;
                }).sum();
            if (subMatchCount <= 0) {
                String format = String.format("%s 没有匹配到任何文件", antSubMatch);
                logRecorder.systemError(format);
                return format;
            }
            logRecorder.system("{} 二级目录模糊匹配到 {} 个文件, 当前文件保留方式 {}", antSubMatch, subMatchCount, antFileUploadMode);
            // 更新产物路径为普通路径
            dbBuildHistoryLogService.updateResultDirFile(this.logId, "/");
            buildInfoModel.setResultDirFile("/");
            this.buildExtraModule.setResultDirFile("/");
        } else if (resultDirFileAction.getType() == ResultDirFileAction.Type.ORIGINAL) {
            File file = FileUtil.file(this.gitFile, resultDirFile);
            if (!file.exists()) {
                String format = String.format("%s 不存在，处理构建产物失败", resultDirFile);
                logRecorder.systemError(format);
                return format;
            }
            BuildUtil.mkdirHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId());
            File toFile = BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId(), resultDirFile);
            //
            String rootDir = FileUtil.getAbsolutePath(gitFile);
            FileCopier.create(file, toFile)
                .setCopyContentIfDir(true)
                .setOverride(true)
                .setCopyAttributes(true)
                .setCopyFilter(file12 -> {
                    if (!releaseHideFile && file12.isHidden()) {
                        return false;
                    }
                    String subPath = FileUtil.subPath(rootDir, file12);
                    subPath = FileUtil.normalize("/" + subPath);
                    return predicate.test(subPath);
                })
                .copy();
        }
        if ((excludeReleaseAnts != null && !excludeReleaseAnts.isEmpty())) {
            logRecorder.system("{} 累积过滤：{} 个文件 ", excludeReleaseAnt, excludeReleaseAntCount[0]);
        }
        return null;
    }

    /**
     * 准备构建
     *
     * @return false 执行异常需要结束
     */
    private String startReady() {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        this.gitFile = BuildUtil.getSourceById(buildInfoModel.getId());

        Integer delay = taskData.delay;
        logRecorder.system("开始构建 #{} 构建执行路径 : {}", buildInfoModel.getBuildId(), FileUtil.getAbsolutePath(this.gitFile));
        if (delay != null && delay > 0) {
            // 延迟执行
            logRecorder.system("执行等待 {} 秒", delay);
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(delay));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        // 删除缓存
        Boolean cacheBuild = this.buildExtraModule.getCacheBuild();
        if (cacheBuild != null && !cacheBuild) {
            logRecorder.system("删除构建缓存");
            CommandUtil.systemFastDel(this.gitFile);
        }
        // 构建环境变量
        appendBuildDefaultEnv(taskData.environmentMapBuilder, buildInfoModel);
        return null;
    }

    public static void appendBuildDefaultEnv(EnvironmentMapBuilder environmentMapBuilder, BuildInfoModel buildInfoModel) {
        //
        File gitFile = BuildUtil.getSourceById(buildInfoModel.getId());
        environmentMapBuilder.put("BUILD_ID", buildInfoModel.getId());
        environmentMapBuilder.put("BUILD_NAME", buildInfoModel.getName());
        environmentMapBuilder.put("BUILD_SOURCE_FILE", FileUtil.getAbsolutePath(gitFile));
        environmentMapBuilder.put("BUILD_NUMBER_ID", String.valueOf(buildInfoModel.getBuildId()));
        environmentMapBuilder.put("BUILD_ORIGINAL_RESULT_DIR_FILE", buildInfoModel.getResultDirFile());
        // 配置的分支名称，可能存在模糊匹配的情况
        environmentMapBuilder.put("BUILD_CONFIG_BRANCH_NAME", buildInfoModel.getBranchName());
    }


    /**
     * 拉取代码后并缓存环境变量
     *
     * @return pull 的结果
     */
    private String pullAndCacheBuildEnv() {
        String pull = this.pull();
        if (pull == null) {
            BuildHistoryLog buildInfoModel = new BuildHistoryLog();
            buildInfoModel.setId(logId);
            buildInfoModel.setBuildEnvCache(taskData.environmentMapBuilder.toDataJsonStr());
            //
            buildInfoModel.setRepositoryLastCommitId(taskData.repositoryLastCommitId);
            buildInfoModel.setRepositoryLastCommitMsg(taskData.repositoryLastCommitMsg);
            dbBuildHistoryLogService.updateById(buildInfoModel);
        }
        return pull;
    }

    /**
     * 拉取代码
     *
     * @return false 执行异常需要结束
     */
    private String pull() {
        RepositoryModel repositoryModel = taskData.repositoryModel;
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        try {
            String msg;
            Integer repoTypeCode = repositoryModel.getRepoType();
            RepositoryModel.RepoType repoType = EnumUtil.likeValueOf(RepositoryModel.RepoType.class, repoTypeCode);
            Boolean checkRepositoryDiff = Optional.ofNullable(taskData.checkRepositoryDiff).orElse(buildExtraModule.getCheckRepositoryDiff());
            String repositoryLastCommitId = buildInfoModel.getRepositoryLastCommitId();
            if (repoType == RepositoryModel.RepoType.Git) {
                // git with password
                IPlugin plugin = PluginFactory.getPlugin("git-clone");
                Map<String, Object> map = repositoryModel.toMap();
                // 指定 clone 深度
                Integer cloneDepth = buildExtraModule.getCloneDepth();
                map.put("depth", cloneDepth);
                if (cloneDepth != null) {
                    // 使用系统
                    map.put("gitProcessType", "SystemGit");
                }
                Tuple tuple = (Tuple) plugin.execute("branchAndTagList", map);
                //GitUtil.getBranchAndTagList(repositoryModel);
                Assert.notNull(tuple, "获取仓库分支失败");
                map.put("reduceProgressRatio", buildExtConfig.getLogReduceProgressRatio());
                map.put("logWriter", logRecorder.getPrintWriter());
                map.put("savePath", gitFile);
                map.put("strictlyEnforce", buildExtraModule.strictlyEnforce());
                // 模糊匹配 标签
                String branchTagName = buildInfoModel.getBranchTagName();
                String[] result;
                if ((branchTagName != null && !branchTagName.isEmpty())) {
                    String newBranchTagName = fuzzyMatch(tuple.get(1), branchTagName);
                    if ((newBranchTagName == null || newBranchTagName.isEmpty())) {
                        String format = String.format("%s Did not match the corresponding tag", branchTagName);
                        logRecorder.systemError(format);
                        return format;
                    }
                    // author wesleyjzy 2022.11.28 map.put("branchName", newBranchName);
                    map.put("tagName", newBranchTagName);
                    //author wesleyjzy 2022.11.28 buildEnv.put("BUILD_BRANCH_NAME", newBranchName);
                    taskData.environmentMapBuilder.put("BUILD_TAG_NAME", newBranchTagName);
                    // 标签拉取模式
                    logRecorder.system("repository tag [{}] clone pull from {}", branchTagName, newBranchTagName);
                    result = (String[]) plugin.execute("pullByTag", map);
                } else {
                    String branchName = buildInfoModel.getBranchName();
                    // 模糊匹配分支
                    String newBranchName = fuzzyMatch(tuple.get(0), branchName);
                    if ((newBranchName == null || newBranchName.isEmpty())) {
                        String format = String.format("%s Did not match the corresponding branch", branchName);
                        logRecorder.systemError(format);
                        //buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, this.taskData.buildInfoModel.getBuildId(), BuildStatus.Error);
                        return format;
                    }
                    // 分支模式
                    map.put("branchName", newBranchName);
                    // 真实使用的分支名
                    taskData.environmentMapBuilder.put("BUILD_BRANCH_NAME", newBranchName);
                    logRecorder.system("repository [{}] clone pull from {}", branchName, newBranchName);
                    result = (String[]) plugin.execute("pull", map);
                }
                msg = result[1];
                // 判断是否执行失败
                String errorMsg = ArrayUtil.get(result, 2);
                if (errorMsg != null) {
                    logRecorder.systemError("拉取代码失败：{}", errorMsg);
                    return errorMsg;
                }
                // 判断hash 码和上次构建是否一致
                if (checkRepositoryDiff != null && checkRepositoryDiff) {
                    if (java.util.Objects.equals(repositoryLastCommitId, result[0])) {
                        // 如果一致，则不构建
                        String format = String.format("仓库代码没有任何变动终止本次构建：%s %s", result[0], msg);
                        logRecorder.systemError(format);
                        throw new DiyInterruptException(format);
                    }
                }
                taskData.repositoryLastCommitId = result[0];
                taskData.repositoryLastCommitMsg = msg;
            } else if (repoType == RepositoryModel.RepoType.Svn) {
                // svn
                Map<String, Object> map = repositoryModel.toMap();

                IPlugin plugin = PluginFactory.getPlugin("svn-clone");
                String[] result = (String[]) plugin.execute(gitFile, map);
                //msg = SvnKitUtil.checkOut(repositoryModel, gitFile);
                msg = ArrayUtil.get(result, 1);
                // 判断版本号和上次构建是否一致
                if (checkRepositoryDiff != null && checkRepositoryDiff) {
                    if (java.util.Objects.equals(repositoryLastCommitId, result[0])) {
                        // 如果一致，则不构建
                        String format = String.format("仓库代码没有任何变动终止本次构建：%s", result[0]);
                        logRecorder.systemError(format);
                        throw new DiyInterruptException(format);
                    }
                }
                taskData.repositoryLastCommitId = result[0];
                taskData.repositoryLastCommitMsg = msg;
            } else {
                String format = String.format("不支持的类型：%s", repoType.getDesc());
                logRecorder.systemError(format);
                return format;
            }
            taskData.environmentMapBuilder.put("BUILD_COMMIT_ID", taskData.repositoryLastCommitId);
            logRecorder.system(msg);
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
        // env file
        String attachEnv = this.buildExtraModule.getAttachEnv();
        Opt.ofBlankAble(attachEnv).ifPresent(s -> {
            UrlQuery of = UrlQuery.of(attachEnv, StandardCharsets.UTF_8);
            Map<String, String> queryMap = of.getQueryMap();
            logRecorder.system("读取附加变量：{} {}", attachEnv, (queryMap == null ? 0 : queryMap.size()));
            //
            Optional.ofNullable(queryMap).ifPresent(map -> {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    CharSequence value = entry.getValue();
                    if (value != null) {
                        taskData.environmentMapBuilder.put(String.valueOf(entry.getKey()), String.valueOf(value));
                    }
                }
            });
            Map<String, String> envFileMap = FileUtils.readEnvFile(this.gitFile, s);
            taskData.environmentMapBuilder.putStr(envFileMap);
        });
        // 输出环境变量
        taskData.environmentMapBuilder.eachStr(logRecorder::system);
        return null;
    }

    private String dockerCommand() {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        String script = buildInfoModel.getScript();
        DockerYmlDsl dockerYmlDsl = DockerYmlDsl.build(script);
        String fromTag = dockerYmlDsl.getFromTag();
        // 根据 tag 查询
        List<DockerInfoModel> dockerInfoModels = dockerInfoService
            .queryByTag(buildInfoModel.getWorkspaceId(), fromTag);
        Map<String, Object> map = machineDockerServer.dockerParameter(dockerInfoModels);
        Assert.notNull(map, fromTag + " 没有可用的 docker server");
        taskData.dockerParameter = new HashMap<>(map);
        logRecorder.system("use docker {}", map.get("name"));
        logRecorder.info("");
        String workingDir = "/home/voyager1/";

        map.put("runsOn", dockerYmlDsl.getRunsOn());
        map.put("workingDir", workingDir);
        map.put("hostConfig", dockerYmlDsl.getHostConfig());
        map.put("tempDir", Voyager1Application.getInstance().getTempPath());
        String buildInfoModelId = buildInfoModel.getId();
        taskData.buildContainerId = "voyager1-build-" + buildInfoModelId;
        map.put("dockerName", taskData.buildContainerId);
        map.put("logRecorder", logRecorder);
        //
        List<String> copy = (dockerYmlDsl.getCopy() != null ? dockerYmlDsl.getCopy() : new ArrayList<>());
        // 将仓库文件上传到容器
        copy.add(FileUtil.getAbsolutePath(this.gitFile) + ":" + workingDir + ":" + "true");
        map.put("copy", copy);
        map.put("binds", (dockerYmlDsl.getBinds() != null ? dockerYmlDsl.getBinds() : new ArrayList<>()));

        Map<String, String> dockerEnv = (dockerYmlDsl.getEnv() != null ? dockerYmlDsl.getEnv() : new HashMap<>(10));
        Map<String, String> env = taskData.environmentMapBuilder.environment();
        env.putAll(dockerEnv);
        env.put("VOYAGER1_BUILD_ID", buildInfoModelId);
        env.put("VOYAGER1_WORKING_DIR", workingDir);
        map.put("env", env);
        map.put("steps", dockerYmlDsl.getSteps());
        // 构建产物
        String resultDirFile = buildInfoModel.getResultDirFile();
        String resultFile = FileUtil.normalize(workingDir + "/" + resultDirFile);
        map.put("resultFile", resultFile);
        // 产物输出目录
        File toFile = BuildUtil.getHistoryPackageFile(buildInfoModelId, buildInfoModel.getBuildId(), resultDirFile);
        map.put("resultFileOut", FileUtil.getAbsolutePath(toFile));
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        try {
            Object execute = plugin.execute("build", map);
            int resultCode = ConvertUtil.toInt(execute, -100);
            // 严格模式
            if (buildExtraModule.strictlyEnforce()) {
                return resultCode == 0 ? null : String.format("执行命令退出码非0，%s", resultCode);
            }
        } catch (Exception e) {
            logRecorder.error("构建调用容器异常", e);
            return e.getMessage();
        }
        return null;
    }

    /**
     * 执行构建命令
     *
     * @return false 执行异常需要结束
     */
    private String executeCommand() {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        Integer buildMode = buildInfoModel.getBuildMode();
        if (buildMode != null && buildMode == 1) {
            // 容器构建
            return this.dockerCommand();
        }
        String script = buildInfoModel.getScript();
        if ((script == null || script.isEmpty())) {
            String info = "没有需要执行的命令";
            logRecorder.systemError(info);
            return info;
        }
        if ((script != null && script.startsWith(ServerConst.REF_SCRIPT))) {
            String scriptId = (script != null && script.startsWith(ServerConst.REF_SCRIPT) ? script.substring(ServerConst.REF_SCRIPT.length()) : script);
            ScriptModel keyAndGlobal = scriptServer.getByKey(scriptId);
            Assert.notNull(keyAndGlobal, "请选择正确的脚本");
            script = keyAndGlobal.getContext();
            logRecorder.system("引入脚本内容：{}[{}]", keyAndGlobal.getName(), scriptId);
        }
        // 替换脚本库 // 替换全局变量
        script = scriptLibraryServer.referenceReplace(script);
        Map<String, String> environment = taskData.environmentMapBuilder.environment();

        InputStream templateInputStream = ExtConfigBean.getConfigResourceInputStream("/exec/template." + CommandUtil.SUFFIX);
        String s1 = IoUtil.readUtf8(templateInputStream);
        try {
            int waitFor = Voyager1Application.getInstance()
                .execScript(s1 + script, file -> {
                    try {
                        String execMode = this.buildExtraModule.getCommandExecMode();
                        // ApacheExecUtil.exec
                        if (java.util.Objects.equals(execMode, "apache_exec")) {
                            return ApacheExecUtil.exec(this.logId, file, this.gitFile, environment, "", logRecorder);
                        } else {
                            return CommandUtil.execWaitFor(file, this.gitFile, environment, "", (s, process) -> {
                                BuildExecuteManage.this.process = process;
                                logRecorder.info(s);
                            });
                        }
                    } catch (IOException | InterruptedException e) {
                        throw Lombok.sneakyThrow(e);
                    }
                });
            BuildExecuteManage.this.process = null;
            logRecorder.system("执行脚本的退出码是：{}", waitFor);
            // 判断是否为严格执行
            if (buildExtraModule.strictlyEnforce()) {
                return waitFor == 0 ? null : String.format("执行命令退出码非0，%s", waitFor);
            }
        } catch (Exception e) {
            logRecorder.error("执行异常", e);
            return e.getMessage();
        }
        return null;
    }

    /**
     * 打包发布
     *
     * @return false 执行需要结束
     */
    private String release() {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        UserModel userModel = taskData.userModel;
        // 发布文件
        this.releaseManage = ReleaseManage.builder()
            .buildNumberId(buildInfoModel.getBuildId())
            .buildExtraModule(buildExtraModule)
            .userModel(userModel)
            .logId(logId)
            .buildEnv(taskData.environmentMapBuilder)
            .logRecorder(logRecorder)
            .build();
        try {
            return releaseManage.start(resultFileSize -> taskData.resultFileSize = resultFileSize, buildInfoModel);
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
    }

    /**
     * 结束流程
     *
     * @return 流程执行是否成功
     */
    private String finish() {
        BuildInfoModel buildInfoModel1 = taskData.buildInfoModel;
        if ((taskData.repositoryLastCommitId != null && !taskData.repositoryLastCommitId.isEmpty())) {
            BuildInfoModel buildInfoModel = new BuildInfoModel();
            buildInfoModel.setId(buildInfoModel1.getId());
            buildInfoModel.setRepositoryLastCommitId(taskData.repositoryLastCommitId);
            buildService.updateById(buildInfoModel);
        }
        //
        BuildStatus buildStatus = buildInfoModel1.getReleaseMethod() != BuildReleaseMethod.No.getCode() ? BuildStatus.PubSuccess : BuildStatus.Success;
        buildExecuteService.updateStatus(buildInfoModel1.getId(), this.logId, this.taskData.buildInfoModel.getBuildId(), buildStatus, "任务正常结束");
        // 判断是否保留产物
        Boolean saveBuildFile = this.buildExtraModule.getSaveBuildFile();
        if (saveBuildFile != null && !saveBuildFile) {
            // 删除 产物文件夹
            File historyPackageFile = BuildUtil.getHistoryPackageFile(buildExtraModule.getId(), buildInfoModel1.getBuildId(), "/");
            CommandUtil.systemFastDel(historyPackageFile);
            // 被删除后
            this.taskData.resultFileSize = 0L;
        }
        return null;
    }

    private Map<String, IProcessItem> createProcess() {
        // 初始化构建流程 准备构建->拉取仓库代码->执行构建命令->打包产物->发布产物->构建结束
        Map<String, IProcessItem> suppliers = new LinkedHashMap<>(10);
        suppliers.put("startReady", new IProcessItem() {
            @Override
            public String name() {
                return "准备构建";
            }

            @Override
            public String execute() {
                return BuildExecuteManage.this.startReady();
            }
        });
        suppliers.put("pull", new IProcessItem() {
            @Override
            public String name() {
                return "拉取仓库代码";
            }

            @Override
            public String execute() {
                return BuildExecuteManage.this.pullAndCacheBuildEnv();
            }
        });
        suppliers.put("executeCommand", new IProcessItem() {
            @Override
            public String name() {
                return "执行构建命令";
            }

            @Override
            public String execute() {
                return BuildExecuteManage.this.executeCommand();
            }
        });
        suppliers.put("packageFile", new IProcessItem() {
            @Override
            public String name() {
                return "打包产物";
            }

            @Override
            public String execute() {
                return BuildExecuteManage.this.packageFile();
            }
        });
        suppliers.put("release", new IProcessItem() {
            @Override
            public String name() {
                return "发布产物";
            }

            @Override
            public String execute() {
                return BuildExecuteManage.this.release();
            }
        });
        suppliers.put("finish", new IProcessItem() {
            @Override
            public String name() {
                return "构建结束";
            }

            @Override
            public String execute() {
                return BuildExecuteManage.this.finish();
            }
        });
        return suppliers;
    }

    /**
     * 清理构建资源
     */
    private void clearResources() {
        //
        BuildInfoModel buildInfoModel1 = taskData.buildInfoModel;
        File historyPackageZipFile = BuildUtil.getHistoryPackageZipFile(buildExtraModule.getId(), buildInfoModel1.getBuildId());
        CommandUtil.systemFastDel(historyPackageZipFile);
        // 计算文件占用大小
        long size = logRecorder.size();
        BuildHistoryLog buildInfoModel = new BuildHistoryLog();
        buildInfoModel.setId(logId);
        buildInfoModel.setResultFileSize(taskData.resultFileSize);
        buildInfoModel.setBuildLogFileSize(size);
        dbBuildHistoryLogService.updateById(buildInfoModel);
    }

    public void runTask() {
        currentThread = Thread.currentThread();
        logRecorder.system("开始执行构建任务,任务等待时间：{}", StringUtil.formatBetween(System.currentTimeMillis() - submitTaskTime, BetweenFormatter.Level.MILLISECOND));

        // 判断任务是否被取消
        BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(this.logId);
        if (buildHistoryLog == null) {
            logRecorder.systemError("构建记录丢失,无法继续构建");
            return;
        }
        if (buildHistoryLog.getStatus() == null || buildHistoryLog.getStatus() == BuildStatus.Cancel.getCode()) {
            logRecorder.systemError("构建状态异常或者被取消");
            return;
        }
        BuildInfoModel buildInfoModel = this.taskData.buildInfoModel;
        buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Ing, "开始构建,构建线程执行");
        //
        Map<String, IProcessItem> processItemMap = this.createProcess();
        // 依次执行流程，发生异常结束整个流程
        String processName = "";
        long startTime = System.currentTimeMillis();
        if (taskData.triggerBuildType == 2) {
            // 系统触发构建
            BaseServerController.resetInfo(UserModel.EMPTY);
        } else {
            BaseServerController.resetInfo(taskData.userModel);
        }

        try {
            boolean stop = false;
            for (Map.Entry<String, IProcessItem> stringSupplierEntry : processItemMap.entrySet()) {
                processName = stringSupplierEntry.getKey();
                IProcessItem processItem = stringSupplierEntry.getValue();
                //
                long processItemStartTime = System.currentTimeMillis();
                logRecorder.system("开始执行 {}流程", processItem.name());
                String interruptMsg = this.asyncWebHooks(processName);
                if (interruptMsg != null) {
                    // 事件脚本中断构建流程
                    logRecorder.system("执行中断 {} 流程,原因事件脚本中断", processItem.name());
                    this.asyncWebHooks("stop", "process", processName, "statusMsg", interruptMsg);
                    buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Interrupt, interruptMsg);
                    stop = true;
                    break;
                }
                String errorMsg = processItem.execute();
                if (errorMsg != null) {
                    // 有条件结束构建流程
                    logRecorder.systemError("执行异常[{}]流程：{}", processItem.name(), errorMsg);
                    this.asyncWebHooks("stop", "process", processName, "statusMsg", errorMsg);
                    buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Error, errorMsg);
                    stop = true;
                    break;
                }
                logRecorder.system("执行结束 {}流程,耗时：{}", processItem.name(), StringUtil.formatBetween(System.currentTimeMillis() - processItemStartTime, BetweenFormatter.Level.MILLISECOND));
            }
            if (!stop) {
                // 没有执行 stop
                this.asyncWebHooks("success");
            }
        } catch (LogRecorderCloseException logRecorderCloseException) {
            log.warn("构建日志记录器已关闭,可能手动取消停止构建,流程:{}", processName);
            String string = "日志记录器异常关闭";
            buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Error, string);
            this.asyncWebHooks("error", "process", processName, "statusMsg", string);
        } catch (DiyInterruptException diyInterruptException) {
            // 主动中断
            this.asyncWebHooks("stop", "process", processName, "statusMsg", diyInterruptException.getMessage());
            buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Interrupt, diyInterruptException.getMessage());
        } catch (java.util.concurrent.CancellationException interruptException) {
            // 异常中断
            String string = "系统中断异常";
            this.asyncWebHooks("stop", "process", processName, "statusMsg", string);
            buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Interrupt, string);
        } catch (Exception e) {
            buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Error, e.getMessage());
            logRecorder.error("构建失败:" + processName, e);
            this.asyncWebHooks("error", "process", processName, "statusMsg", e.getMessage());
        } finally {
            this.clearResources();
            logRecorder.system("构建结束-累计耗时:{}", StringUtil.formatBetween(System.currentTimeMillis() - startTime, BetweenFormatter.Level.MILLISECOND));
            this.asyncWebHooks("done");
            IoUtil.close(logRecorder);
            BaseServerController.removeAll();
        }
    }

    public void run() {
        BuildInfoModel buildInfoModel = this.taskData.buildInfoModel;
        try {
            I18nMessageUtil.setLanguage(this.language);
            this.runTask();
        } catch (Exception e) {
            log.error("构建发生未知错误", e);
        } finally {
            BUILD_MANAGE_MAP.remove(buildInfoModel.getId());
            I18nMessageUtil.clearLanguage();
        }
    }

    /**
     * 执行 webhooks 通知
     *
     * @param type  类型
     * @param other 其他参数
     * @return 是否还继续整个构建流程
     */
    private String asyncWebHooks(String type, Object... other) {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        Map<String, Object> map = new HashMap<>(15);
        //
        for (int i = 0; i < other.length; i += 2) {
            map.put(other[i].toString(), other[i + 1]);
        }
        map.put("buildId", buildInfoModel.getId());
        map.put("buildNumberId", this.taskData.buildInfoModel.getBuildId());
        map.put("buildName", buildInfoModel.getName());
        map.put("buildSourceFile", FileUtil.getAbsolutePath(this.gitFile));
        map.put("type", type);
        if (taskData.repositoryLastCommitId != null) {
            map.put("commitId", taskData.repositoryLastCommitId);
        }
        if (taskData.repositoryLastCommitMsg != null) {
            map.put("commitMsg", taskData.repositoryLastCommitMsg);
        }
        map.put("triggerBuildType", taskData.triggerBuildType);
        map.put("triggerTime", System.currentTimeMillis());
        String triggerUser = Optional.ofNullable(taskData.userModel).map(BaseIdModel::getId).orElse(UserModel.SYSTEM_ADMIN);
        map.put("triggerUser", triggerUser);
        String resultDirFile = buildExtraModule.getResultDirFile();
        map.put("buildResultDirFile", resultDirFile);
        map.put("buildResultFile", BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), this.taskData.buildInfoModel.getBuildId(), resultDirFile));
        //
        map.put("releaseMethod", buildInfoModel.getReleaseMethod());
        String workspaceId = buildInfoModel.getWorkspaceId();
        map.put("workspaceId", workspaceId);
        WorkspaceModel model = workspaceService.getByKey(workspaceId);
        if (model != null) {
            map.put("clusterInfoId", model.getClusterInfoId());
            map.put("workspaceName", model.getName());
        }

        Opt.ofBlankAble(buildInfoModel.getWebhook())
            .ifPresent(s ->
                I18nThreadUtil.execute(() -> {
                    try {
                        IPlugin plugin = PluginFactory.getPlugin("webhook");
                        map.put("VOYAGER1_WEBHOOK_EVENT", DefaultWebhookPluginImpl.WebhookEvent.BUILD);
                        plugin.execute(s, map);
                    } catch (Exception e) {
                        log.error("WebHooks 调用错误", e);
                    }
                })
            );
        // 执行对应的事件脚本
        try {
            return this.noticeScript(type, map);
        } catch (Exception e) {
            log.error("noticeScript 调用错误", e);
            logRecorder.error("执行事件脚本错误", e);
            // 执行事件脚本发送异常不终止构建流程
            return null;
        }
    }

    /**
     * 执行事件脚本
     *
     * @param type 事件类型
     * @param map  相关参数
     * @return 是否还继续整个构建流程
     */
    private String noticeScript(String type, Map<String, Object> map) {
        String noticeScriptId = this.buildExtraModule.getNoticeScriptId();
        if ((noticeScriptId == null || noticeScriptId.isEmpty())) {
            return null;
        }
        List<String> list = io.voyager1.util.ConvertUtil.splitTrim(noticeScriptId, ",");
        for (String noticeScriptIdItem : list) {
            String error = this.noticeScript(noticeScriptIdItem, type, map);
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    /**
     * 执行事件脚本
     *
     * @param noticeScriptId 脚本id
     * @param type           事件类型
     * @param map            相关参数
     * @return 是否还继续整个构建流程
     */
    private String noticeScript(String noticeScriptId, String type, Map<String, Object> map) {
        ScriptModel scriptModel = scriptServer.getByKey(noticeScriptId);
        if (scriptModel == null) {
            logRecorder.systemWarning("事件脚本不存在:{} {}", type, noticeScriptId);
            return null;
        }
        // 判断是否包含需要执行的事件
        if (!StrUtil.containsAnyIgnoreCase(scriptModel.getDescription(), type, "all")) {
            log.warn("忽略执行事件脚本 {} {} {}", type, scriptModel.getName(), noticeScriptId);
            return null;
        }
        logRecorder.system("开始执行事件脚本： {}", type);
        // 环境变量
        Map<String, String> environment = taskData.environmentMapBuilder.environment(map);
        ScriptExecuteLogModel logModel = scriptExecuteLogServer.create(scriptModel, 3, this.taskData.buildInfoModel.getWorkspaceId());
        File logFile = scriptModel.logFile(logModel.getId());
        File scriptFile = null;
        LogRecorder scriptLog = LogRecorder.builder().file(logFile).build();
        final String[] lastMsg = new String[1];
        try {
            // 创建执行器
            scriptFile = scriptExecuteLogServer.toExecLogFile(scriptModel);
            scriptExecuteLogServer.updateStatus(logModel.getId(), CommandExecLogModel.Status.ING);
            int waitFor;
            try {
                // 输出环境变量
                taskData.environmentMapBuilder.eachStr(s -> {
                    logRecorder.system(s);
                    scriptLog.info(s);
                }, map);
                //
                waitFor = CommandUtil.execWaitFor(scriptFile, null, environment, null, (s, process) -> {
                    logRecorder.info(s);
                    scriptLog.info(s);
                    lastMsg[0] = s;
                });
            } catch (IOException | InterruptedException e) {
                scriptExecuteLogServer.updateStatus(logModel.getId(), CommandExecLogModel.Status.ERROR);
                throw Lombok.sneakyThrow(e);
            }
            logRecorder.system("执行 {} 类型脚本的退出码是：{}", type, waitFor);
            scriptExecuteLogServer.updateStatus(logModel.getId(), CommandExecLogModel.Status.DONE, waitFor);
            // 判断是否为严格执行
            if (buildExtraModule.strictlyEnforce() && waitFor != 0) {
                //logRecorder.systemError("严格执行模式，事件脚本返回状态码异常");
                return "严格执行模式，事件脚本返回状态码异常," + waitFor;
            }
            if ((lastMsg[0] != null && lastMsg[0].toLowerCase().startsWith("interrupt " + type.toLowerCase()))) {
                return "事件脚本中断：" + lastMsg[0];
            }
            return null;
        } finally {
            try {
                FileUtil.del(scriptFile);
            } catch (Exception ignored) {
            }
            IoUtil.close(scriptLog);
        }
    }

    /**
     * 取消构建
     *
     * @param id id
     * @return bool
     */
    public static boolean cancelTaskById(String id) {
        return Optional.ofNullable(BuildExecuteManage.BUILD_MANAGE_MAP.get(id)).map(buildExecuteManage1 -> {
            buildExecuteManage1.cancelTask("手动取消任务");
            return true;
        }).orElse(false);
    }

    /**
     * 模糊匹配
     *
     * @param list    待匹配待列表
     * @param pattern 迷糊的表达式
     * @return 匹配到到值
     */
    private static String fuzzyMatch(List<String> list, String pattern) {
        Assert.notEmpty(list, "仓库没有任何分支或者标签");
        if (AntPathUtil.ANT_PATH_MATCHER.isPattern(pattern)) {
            List<String> collect = list.stream().filter(s -> AntPathUtil.ANT_PATH_MATCHER.match(pattern, s)).collect(Collectors.toList());
            return (collect == null || collect.isEmpty() ? null : collect.get(0));
        }
        return pattern;
    }
}
