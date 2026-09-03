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

package io.voyager1.controller.build;

import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.RegexPool;
import io.voyager1.util.Tuple;
import io.voyager1.util.Validator;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.plugin.IPlugin;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.IDockerConfigPlugin;
import io.voyager1.build.BuildExecuteService;
import io.voyager1.build.BuildUtil;
import io.voyager1.build.DockerYmlDsl;
import io.voyager1.build.ResultDirFileAction;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerConst;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.configuration.BuildExtConfig;
import io.voyager1.func.assets.server.MachineDockerServer;
import io.voyager1.model.AfterOpt;
import io.voyager1.model.BaseEnum;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.data.RepositoryModel;
import io.voyager1.model.data.SshModel;
import io.voyager1.model.enums.BuildReleaseMethod;
import io.voyager1.model.script.ScriptModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.dblog.BuildInfoService;
import io.voyager1.service.dblog.DbBuildHistoryLogService;
import io.voyager1.service.dblog.RepositoryService;
import io.voyager1.service.docker.DockerInfoService;
import io.voyager1.service.node.ssh.SshService;
import io.voyager1.service.script.ScriptServer;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 构建列表，新版本，数据存放到数据库，不再是文件了
 * 以前的数据会在程序启动时插入到数据库中
 *
 * @since 2021-08-09
 */
@RestController
@Feature(cls = ClassFeature.BUILD)
public class BuildInfoController extends BaseServerController {

    private final DbBuildHistoryLogService dbBuildHistoryLogService;
    private final SshService sshService;
    private final BuildInfoService buildInfoService;
    private final RepositoryService repositoryService;
    private final BuildExecuteService buildExecuteService;
    private final DockerInfoService dockerInfoService;
    private final ScriptServer scriptServer;
    private final BuildExtConfig buildExtConfig;
    protected final MachineDockerServer machineDockerServer;

    public BuildInfoController(DbBuildHistoryLogService dbBuildHistoryLogService,
                               SshService sshService,
                               BuildInfoService buildInfoService,
                               RepositoryService repositoryService,
                               BuildExecuteService buildExecuteService,
                               DockerInfoService dockerInfoService,
                               ScriptServer scriptServer,
                               BuildExtConfig buildExtConfig,
                               MachineDockerServer machineDockerServer) {
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
        this.sshService = sshService;
        this.buildInfoService = buildInfoService;
        this.repositoryService = repositoryService;
        this.buildExecuteService = buildExecuteService;
        this.dockerInfoService = dockerInfoService;
        this.scriptServer = scriptServer;
        this.buildExtConfig = buildExtConfig;
        this.machineDockerServer = machineDockerServer;
    }

    /**
     * load build list with params
     *
     * @return json
     */
    @RequestMapping(value = "/build/list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<BuildInfoModel>> getBuildList(HttpServletRequest request) {
        // load list with page
        PageResultDto<BuildInfoModel> page = buildInfoService.listPage(request);
        page.each(buildInfoModel -> {
            // 获取源码目录是否存在
            File source = BuildUtil.getSourceById(buildInfoModel.getId());
            buildInfoModel.setSourceDirExist(FileUtil.exist(source));
            //
            File file = BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId(), buildInfoModel.getResultDirFile());
            buildInfoModel.setResultHasFile(FileUtil.exist(file));
        });
        return ApiResult.success("", page);
    }

    /**
     * load build list with params
     *
     * @return json
     */
    @GetMapping(value = "/build/get", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<BuildInfoModel> getBuildListAll(String id, HttpServletRequest request) {
        // load list with page
        BuildInfoModel buildInfoModel = buildInfoService.getByKey(id, request);
        Assert.notNull(buildInfoModel, "不存在对应的构建");
        // 获取源码目录是否存在
        File source = BuildUtil.getSourceById(buildInfoModel.getId());
        buildInfoModel.setSourceDirExist(FileUtil.exist(source));
        //
        File file = BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId(), buildInfoModel.getResultDirFile());
        buildInfoModel.setResultHasFile(FileUtil.exist(file));
        return ApiResult.success("", buildInfoModel);
    }

    /**
     * load build list with params
     *
     * @return json
     */
    @GetMapping(value = "/build/list_group_all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<String>> getBuildGroupAll(HttpServletRequest request) {
        // load list with page
        List<String> group = buildInfoService.listGroup(request);
        return ApiResult.success("", group);
    }

    /**
     * edit build info
     *
     * @param id            构建ID
     * @param name          构建名称
     * @param repositoryId  仓库ID
     * @param resultDirFile 构建产物目录
     * @param script        构建命令
     * @param releaseMethod 发布方法
     * @param branchName    分支名称
     * @param webhook       webhook
     * @param extraData     构建的其他信息
     * @param autoBuildCron 自动构建表达是
     * @param branchTagName 标签名
     * @return json
     */
    @RequestMapping(value = "/build/edit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> updateBuild(String id,
                                            @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建名称不能为空") String name,
                                            @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "仓库信息不能为空") String repositoryId,
                                            @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建产物目录不能为空,长度1-200", range = "1:200") String resultDirFile,
                                            @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建命令不能为空") String script,
                                            @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "发布方法不正确") int releaseMethod,
                                            String branchName, String branchTagName, String webhook, String autoBuildCron,
                                            String extraData, String group,
                                            @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "构建方式不正确") int buildMode,
                                            String aliasCode,
                                            @ValidatorItem(value = ValidatorRule.NUMBERS, msg = "请填写正确的保留天数") Integer resultKeepDay,
                                            String buildEnvParameter,
                                            HttpServletRequest request) {
        // 根据 repositoryId 查询仓库信息
        RepositoryModel repositoryModel = repositoryService.getByKey(repositoryId, request);
        Assert.notNull(repositoryModel, "无效的仓库信息");
        // 如果是 GIT 需要检测分支是否存在
        if (RepositoryModel.RepoType.Git.getCode() == repositoryModel.getRepoType()) {
            Assert.hasText(branchName, "请选择分支");
        } else if (RepositoryModel.RepoType.Svn.getCode() == repositoryModel.getRepoType()) {
            // 如果是 SVN
            branchName = "trunk";
        }
        ResultDirFileAction resultDirFileAction = ResultDirFileAction.parse(resultDirFile);
        resultDirFileAction.check();
        //
        Assert.state(buildMode == 0 || buildMode == 1, "请选择正确的构建方式");
        if (buildMode == 1) {
            // 验证 dsl 内容
            this.checkDocker(script, request);
            // 容器构建不能使用 ant 模式
            Assert.state(resultDirFileAction.getType() == ResultDirFileAction.Type.ORIGINAL, "容器构建的产物路径不能使用 ant 模式");
        } else {
            if ((script != null && script.startsWith(ServerConst.REF_SCRIPT))) {
                String scriptId = (script != null && script.startsWith(ServerConst.REF_SCRIPT) ? script.substring(ServerConst.REF_SCRIPT.length()) : script);
                ScriptModel keyAndGlobal = scriptServer.getByKeyAndGlobal(scriptId, request, "请选择正确的脚本");
                Assert.notNull(keyAndGlobal, "请选择正确的脚本");
            }
        }
        if (buildExtConfig.isCheckDeleteCommand()) {
            // 判断删除命令
            Assert.state(!CommandUtil.checkContainsDel(script), "构建命令不能包含删除命令");
        }
        // 查询构建信息
        BuildInfoModel buildInfoModel = buildInfoService.getByKey(id, request);
        buildInfoModel = (buildInfoModel != null ? buildInfoModel : new BuildInfoModel());
        // 设置参数
        Opt.ofBlankAble(webhook).ifPresent(s -> Validator.validateMatchRegex(RegexPool.URL_HTTP, s, "WebHooks 地址不合法"));
        Opt.ofBlankAble(aliasCode).ifPresent(s -> Validator.validateGeneral(s, "别名码只能是英文、数字"));
        //
        buildInfoModel.setAutoBuildCron(this.checkCron(autoBuildCron));
        buildInfoModel.setWebhook(webhook);
        buildInfoModel.setRepositoryId(repositoryId);
        buildInfoModel.setName(name);
        buildInfoModel.setAliasCode(aliasCode);
        buildInfoModel.setBranchName(branchName);
        buildInfoModel.setBranchTagName(branchTagName);
        buildInfoModel.setResultDirFile(resultDirFile);
        buildInfoModel.setScript(script);
        buildInfoModel.setGroup(group);
        buildInfoModel.setResultKeepDay(resultKeepDay);
        buildInfoModel.setBuildMode(buildMode);
        buildInfoModel.setBuildEnvParameter(buildEnvParameter);
        // 发布方式
        BuildReleaseMethod releaseMethod1 = BaseEnum.getEnum(BuildReleaseMethod.class, releaseMethod);
        Assert.notNull(releaseMethod1, "发布方法不正确");
        buildInfoModel.setReleaseMethod(releaseMethod1.getCode());
        // 把 extraData 信息转换成 JSON 字符串 ,不能直接使用 io.voyager1.build.BuildExtraModule
        JSONObject jsonObject = JSON.parseObject(extraData);

        // 验证发布方式 和 extraData 信息
        if (releaseMethod1 == BuildReleaseMethod.Project) {
            this.formatProject(jsonObject);
        } else if (releaseMethod1 == BuildReleaseMethod.Ssh) {
            this.formatSsh(jsonObject, request);
        } else if (releaseMethod1 == BuildReleaseMethod.Outgiving) {
            this.formatOutGiving(jsonObject);
        } else if (releaseMethod1 == BuildReleaseMethod.LocalCommand) {
            this.formatLocalCommand(jsonObject);
            jsonObject.put("releaseMethodDataId", "LocalCommand");
        } else if (releaseMethod1 == BuildReleaseMethod.DockerImage) {
            // dockerSwarmId default
            String dockerSwarmId = this.formatDocker(jsonObject, request);
            jsonObject.put("releaseMethodDataId", dockerSwarmId);
        }
        // 检查关联数据ID
        buildInfoModel.setReleaseMethodDataId(jsonObject.getString("releaseMethodDataId"));
        if (buildInfoModel.getReleaseMethod() != BuildReleaseMethod.No.getCode()) {
            Assert.hasText(buildInfoModel.getReleaseMethodDataId(), "没有发布分发对应关联数据ID");
        }
        // 验证服务端脚本
        String noticeScriptId = jsonObject.getString("noticeScriptId");
        if ((noticeScriptId != null && !noticeScriptId.isEmpty())) {
            List<String> list = io.voyager1.util.ConvertUtil.splitTrim(noticeScriptId, ",");
            for (String noticeScriptIdItem : list) {
                ScriptModel scriptModel = scriptServer.getByKey(noticeScriptIdItem, request);
                Assert.notNull(scriptModel, "不存在对应的服务端脚本,请重新选择");
            }
        }
        buildInfoModel.setExtraData(jsonObject.toJSONString());

        // 新增构建信息
        if ((id == null || id.isEmpty())) {
            // set default buildId
            buildInfoModel.setBuildId(0);
            buildInfoService.insert(buildInfoModel);
            return ApiResult.success("添加成功", buildInfoModel.getId());
        }

        buildInfoService.updateById(buildInfoModel, request);
        return ApiResult.success("修改成功", buildInfoModel.getId());
    }

    private void checkDocker(String script, HttpServletRequest request) {
        String workspaceId = buildInfoService.getCheckUserWorkspace(request);
        DockerYmlDsl build = DockerYmlDsl.build(script);
        //
        IDockerConfigPlugin plugin = (IDockerConfigPlugin) PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        build.check(dockerInfoService, machineDockerServer, workspaceId, plugin);
        //
        String fromTag = build.getFromTag();
        if ((fromTag != null && !fromTag.isEmpty())) {
            //
            int count = dockerInfoService.countByTag(workspaceId, fromTag);
            Assert.state(count > 0, fromTag + " 没有找到任何 docker。可能docker tag 填写不正确，需要为 docker 配置标签");
        }
    }

    /**
     * 验证构建信息
     * 当发布方式为【SSH】的时候
     *
     * @param jsonObject 配置信息
     */
    private void formatSsh(JSONObject jsonObject, HttpServletRequest request) {
        // 发布方式
        String releaseMethodDataId = jsonObject.getString("releaseMethodDataId_3");
        Assert.hasText(releaseMethodDataId, "请选择分发SSH项");

        String releasePath = jsonObject.getString("releasePath");
        Assert.hasText(releasePath, "请输入发布到ssh中的目录");
        releasePath = FileUtil.normalize(releasePath);
        String releaseCommand = jsonObject.getString("releaseCommand");
        List<String> strings = io.voyager1.util.ConvertUtil.splitTrim(releaseMethodDataId, ",");
        for (String releaseMethodDataIdItem : strings) {
            SshModel sshServiceItem = sshService.getByKey(releaseMethodDataIdItem, request);
            Assert.notNull(sshServiceItem, "没有对应的ssh项");
            //
            if (releasePath.startsWith("/")) {
                // 以根路径开始
                List<String> fileDirs = sshServiceItem.fileDirs();
                Assert.notEmpty(fileDirs, sshServiceItem.getName() + "此ssh未授权操作此目录");

                boolean find = false;
                for (String fileDir : fileDirs) {
                    if (FileUtil.isSub(new File(fileDir), new File(releasePath))) {
                        find = true;
                    }
                }
                Assert.state(find, sshServiceItem.getName() + "此ssh未授权操作此目录");
            }
            // 发布命令
            if ((releaseCommand != null && !releaseCommand.isEmpty())) {
                int length = releaseCommand.length();
                Assert.state(length <= 4000, "发布命令长度限制在4000字符");
                //return ApiResult.getString(405, "请输入发布命令");
                String[] commands = releaseCommand.split(java.util.regex.Pattern.quote("\n"));

                for (String commandItem : commands) {
                    boolean checkInputItem = SshModel.checkInputItem(sshServiceItem, commandItem);
                    Assert.state(checkInputItem, sshServiceItem.getName() + "发布命令中包含禁止执行的命令");
                }
            }
        }
        jsonObject.put("releaseMethodDataId", releaseMethodDataId);
    }

    private String formatDocker(JSONObject jsonObject, HttpServletRequest request) {
        // 发布命令
        String dockerfile = jsonObject.getString("dockerfile");
        Assert.hasText(dockerfile, "请填写要执行的 Dockerfile 路径");
        String fromTag = jsonObject.getString("fromTag");
        if ((fromTag != null && !fromTag.isEmpty())) {
            Assert.hasText(fromTag, "请填要执行 docker 标签");
            String workspaceId = dockerInfoService.getCheckUserWorkspace(request);
            int count = dockerInfoService.countByTag(workspaceId, fromTag);
            Assert.state(count > 0, "docker tag 填写不正确,没有找到任何docker");
        }
        String dockerTag = jsonObject.getString("dockerTag");
        Assert.hasText(dockerTag, "请填写镜像标签");
        //
        String dockerSwarmId = jsonObject.getString("dockerSwarmId");
        if ((dockerSwarmId == null || dockerSwarmId.isEmpty())) {
            return "DockerImage";
        }
        String dockerSwarmServiceName = jsonObject.getString("dockerSwarmServiceName");
        Assert.hasText(dockerSwarmServiceName, "请填写集群中的服务名");
        return dockerSwarmId;
    }

    private void formatLocalCommand(JSONObject jsonObject) {
        // 发布命令
        String releaseCommand = jsonObject.getString("releaseCommand");
        if ((releaseCommand != null && !releaseCommand.isEmpty())) {
            int length = releaseCommand.length();
            Assert.state(length <= 4000, "发布命令长度限制在4000字符");
        }
    }

    private void formatOutGiving(JSONObject jsonObject) {
        String releaseMethodDataId = jsonObject.getString("releaseMethodDataId_1");
        Assert.hasText(releaseMethodDataId, "请选择分发项目");
        jsonObject.put("releaseMethodDataId", releaseMethodDataId);
        //
        this.checkProjectSecondaryDirectory(jsonObject);
    }

    /**
     * 验证构建信息
     * 当发布方式为【项目】的时候
     *
     * @param jsonObject 配置信息
     */
    private void formatProject(JSONObject jsonObject) {
        String releaseMethodDataId2Node = jsonObject.getString("releaseMethodDataId_2_node");
        String releaseMethodDataId2Project = jsonObject.getString("releaseMethodDataId_2_project");

        Assert.state(!((releaseMethodDataId2Node == null || releaseMethodDataId2Node.isEmpty()) || (releaseMethodDataId2Project == null || releaseMethodDataId2Project.isEmpty())), "请选择节点和项目");
        jsonObject.put("releaseMethodDataId", String.format("%s:%s", releaseMethodDataId2Node, releaseMethodDataId2Project));
        //
        String afterOpt = jsonObject.getString("afterOpt");
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, ConvertUtil.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择打包后的操作");
        //
        String clearOld = jsonObject.getString("clearOld");
        jsonObject.put("afterOpt", afterOpt1.getCode());
        jsonObject.put("clearOld", ConvertUtil.toBool(clearOld, false));
        //
        this.checkProjectSecondaryDirectory(jsonObject);
    }

    private void checkProjectSecondaryDirectory(JSONObject jsonObject) {
        //
        String projectSecondaryDirectory = jsonObject.getString("projectSecondaryDirectory");
        Opt.ofBlankAble(projectSecondaryDirectory).ifPresent(s -> FileUtils.checkSlip(s, e -> new IllegalArgumentException("二级目录不能越级：" + e.getMessage())));
    }

    /**
     * 获取分支信息
     *
     * @param repositoryId 仓库id
     * @return json
     * @throws Exception 异常
     */
    @RequestMapping(value = "/build/branch-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> branchList(
        @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "仓库ID不能为空") String repositoryId) throws Exception {
        // 根据 repositoryId 查询仓库信息
        RepositoryModel repositoryModel = repositoryService.getByKey(repositoryId, false);
        Assert.notNull(repositoryModel, "无效的仓库信息");
        //
        Assert.state(repositoryModel.getRepoType() == 0, "只有 GIT 仓库才有分支信息");
        IPlugin plugin = PluginFactory.getPlugin("git-clone");
        Map<String, Object> map = repositoryModel.toMap();
        Tuple branchAndTagList = (Tuple) plugin.execute("branchAndTagList", map);
        Assert.notNull(branchAndTagList, "没有任何分支");
        JSONObject jsonObject = new JSONObject();
        List<Object> collection = branchAndTagList.toList();
        jsonObject.put("branch", (0 < collection.size() ? collection.get(0) : null));
        jsonObject.put("tags", (1 < collection.size() ? collection.get(1) : null));
        return ApiResult.success("", jsonObject);
    }


    /**
     * 删除构建信息
     *
     * @param id 构建ID
     * @return json
     */
    @PostMapping(value = "/build/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> delete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据id") String id, HttpServletRequest request) {
        this.delById(id, request);
        return ApiResult.success("删除成功,并且清理历史构建产物成功");
    }


    private void delById(String id, HttpServletRequest request) {
        // 查询构建信息
        BuildInfoModel buildInfoModel = buildInfoService.getByKey(id, request);
        Objects.requireNonNull(buildInfoModel, "没有对应数据");
        //
        String e = buildExecuteService.checkStatus(buildInfoModel);
        Assert.isNull(e, () -> e);
        // 删除构建历史
        dbBuildHistoryLogService.delByWorkspace(request, entity -> entity.set("buildDataId", buildInfoModel.getId()));
        // 删除构建信息文件
        File file = BuildUtil.getBuildDataFile(buildInfoModel.getId());
        // 快速删除
        boolean fastDel = CommandUtil.systemFastDel(file);
        //
        Assert.state(!fastDel, "清理历史构建产物失败,已经重新尝试");
        // 删除构建信息数据
        buildInfoService.delByKey(buildInfoModel.getId(), request);
    }

    /**
     * 批量删除构建信息
     *
     * @param ids 构建ID
     * @return json
     */
    @PostMapping(value = "/build/batch-delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> batchDelete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据id") String ids, HttpServletRequest request) {
        List<String> list = io.voyager1.util.ConvertUtil.splitTrim(ids, ",");
        for (String s : list) {
            this.delById(s, request);
        }
        return ApiResult.success("删除成功,并且清理历史构建产物成功");
    }


    /**
     * 清除构建信息
     *
     * @param id 构建ID
     * @return json
     */
    @PostMapping(value = "/build/clean-source", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<Object> cleanSource(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据id") String id, HttpServletRequest request) {
        // 查询构建信息
        BuildInfoModel buildInfoModel = buildInfoService.getByKey(id, request);
        Objects.requireNonNull(buildInfoModel, "没有对应数据");
        File source = BuildUtil.getSourceById(buildInfoModel.getId());
        // 快速删除
        boolean fastDel = CommandUtil.systemFastDel(source);
        //
        Assert.state(!fastDel, "删除文件失败,请检查");
        return ApiResult.success("清理成功");
    }

    /**
     * 排序
     *
     * @param id        节点ID
     * @param method    方法
     * @param compareId 比较的ID
     * @return msg
     */
    @GetMapping(value = "/build/sort-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> sortItem(@ValidatorItem String id, @ValidatorItem String method, String compareId, HttpServletRequest request) {
        if ((method != null && method.equalsIgnoreCase("top"))) {
            buildInfoService.sortToTop(id, request);
        } else if ((method != null && method.equalsIgnoreCase("up"))) {
            buildInfoService.sortMoveUp(id, compareId, request);
        } else if ((method != null && method.equalsIgnoreCase("down"))) {
            buildInfoService.sortMoveDown(id, compareId, request);
        } else {
            return new ApiResult<>(400, "不支持的方式" + method);
        }
        return new ApiResult<>(200, "操作成功");
    }

}
