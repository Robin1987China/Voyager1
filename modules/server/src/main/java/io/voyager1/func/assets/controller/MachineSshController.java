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

package io.voyager1.func.assets.controller;
import org.springframework.data.domain.Sort;
import io.voyager1.util.CsvUtil;
import io.voyager1.util.CsvRow;
import io.voyager1.util.CsvData;

import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.DateTime;
import io.voyager1.util.CharsetDetector;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.NetUtil;
import io.voyager1.util.StrSplitter;
import io.voyager1.util.CsvReadConfig;
import io.voyager1.util.CsvReader;
import io.voyager1.util.CsvWriter;
import io.voyager1.util.StrUtil;
import io.voyager1.util.EnumUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.URLUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.util.JschUtil;
import io.voyager1.core.api.ApiResult;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.interceptor.PermissionInterceptor;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.configuration.AssetsConfig;
import io.voyager1.func.BaseGroupNameController;
import io.voyager1.func.assets.model.MachineSshModel;
import io.voyager1.func.assets.server.MachineSshServer;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.AgentWhitelist;
import io.voyager1.model.data.SshModel;
import io.voyager1.model.data.WorkspaceModel;
import io.voyager1.model.log.SshTerminalExecuteLog;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.dblog.SshTerminalExecuteLogService;
import io.voyager1.service.node.ssh.SshService;
import io.voyager1.service.system.WorkspaceService;
import io.voyager1.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;

/**
 * @since 2023/2/25
 */
@RestController
@RequestMapping(value = "/system/assets/ssh")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE_SSH)
@SystemPermission
@Slf4j
public class MachineSshController extends BaseGroupNameController {

    private final MachineSshServer machineSshServer;
    private final SshService sshService;
    private final SshTerminalExecuteLogService sshTerminalExecuteLogService;
    private final WorkspaceService workspaceService;
    private final ServerConfig serverConfig;
    private final AssetsConfig.SshConfig sshConfig;

    public MachineSshController(MachineSshServer machineSshServer,
                                SshService sshService,
                                SshTerminalExecuteLogService sshTerminalExecuteLogService,
                                WorkspaceService workspaceService,
                                ServerConfig serverConfig,
                                AssetsConfig assetsConfig) {
        super(machineSshServer);
        this.machineSshServer = machineSshServer;
        this.sshService = sshService;
        this.sshTerminalExecuteLogService = sshTerminalExecuteLogService;
        this.workspaceService = workspaceService;
        this.serverConfig = serverConfig;
        this.sshConfig = assetsConfig.getSsh();
    }

    @PostMapping(value = "list-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<MachineSshModel>> listJson(HttpServletRequest request) {
        PageResultDto<MachineSshModel> pageResultDto = machineSshServer.listPage(request);
        return ApiResult.success("", pageResultDto);
    }

    @PostMapping(value = "search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<MachineSshModel>> listJson(HttpServletRequest request, String name, int limit, String mustId) {
        Entity entity = new Entity();
        if ((name != null && !name.isBlank())) {
            entity.set("name", "like %" + name + "%");
        }
        List<MachineSshModel> sshModelList = machineSshServer.queryList(entity, limit, Sort.by(Sort.Order.desc("createTimeMillis")));
        if ((mustId != null && !mustId.isBlank()) && (sshModelList != null && !sshModelList.isEmpty())) {
            // 存在 id 并且搜索结果不为空
            boolean noneMatch = sshModelList.stream().noneMatch(machineSshModel -> java.util.Objects.equals(machineSshModel.id(), mustId));
            if (noneMatch) {
                MachineSshModel server = machineSshServer.getByKey(mustId);
                if (server != null) {
                    sshModelList.add(server);
                }
            }
        }
        return ApiResult.success("", sshModelList);
    }


    @Override
    @GetMapping(value = "list-group", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<Collection<String>> listGroup() {
        Collection<String> list = dbService.listGroupName();
        // 合并配置禁用分组
        List<String> monitorGroupName = sshConfig.getDisableMonitorGroupName();
        if (monitorGroupName != null) {
            list.addAll(monitorGroupName);
            //
            list.remove("*");
            list = new HashSet<>(list);
        }
        return ApiResult.success("", list);
    }

    /**
     * 编辑
     *
     * @param name        名称
     * @param host        端口
     * @param user        用户名
     * @param password    密码
     * @param connectType 连接方式
     * @param privateKey  私钥
     * @param port        端口
     * @param charset     编码格式
     * @param id          ID
     * @return json
     */
    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> save(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "参数错误ssh名称不能为空") String name,
                                     @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "参数错误host不能为空") String host,
                                     @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "参数错误user不能为空") String user,
                                     String password,
                                     MachineSshModel.ConnectType connectType,
                                     String privateKey,
                                     @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "参数错误port错误") int port,
                                     String charset,
                                     String id,
                                     Integer timeout,
                                     String allowEditSuffix,
                                     String groupName) {
        boolean add = (id == null || id.isEmpty());
        if (add) {
            // 优先判断参数 如果是 password 在修改时可以不填写
            if (connectType == MachineSshModel.ConnectType.PASS) {
                Assert.hasText(password, "请填写登录密码");
            } else if (connectType == MachineSshModel.ConnectType.PUBKEY) {
                //Assert.hasText(privateKey, "请填写证书内容");
            }
        } else {
            boolean exists = machineSshServer.exists(new MachineSshModel(id));
            Assert.state(exists, "不存在对应ssh");
        }
        MachineSshModel sshModel = new MachineSshModel();
        sshModel.setId(id);
        sshModel.setGroupName(groupName);
        sshModel.setHost(host);
        // 如果密码传递不为空就设置值 因为上面已经判断了只有修改的情况下 password 才可能为空
        if (password != null && !password.isEmpty()) sshModel.setPassword(password);
        if ((privateKey != null && privateKey.startsWith(URLUtil.FILE_URL_PREFIX))) {
            String rsaPath = (privateKey != null && privateKey.startsWith(URLUtil.FILE_URL_PREFIX) ? privateKey.substring(URLUtil.FILE_URL_PREFIX.length()) : privateKey);
            Assert.state(FileUtil.isFile(rsaPath), "配置的私钥文件不存在");
        }
        if (privateKey != null) sshModel.setPrivateKey(privateKey);

        // 获取允许编辑的后缀
        List<String> allowEditSuffixList = AgentWhitelist.parseToList(allowEditSuffix, "允许编辑的文件后缀不能为空");
        sshModel.allowEditSuffix(allowEditSuffixList);
        sshModel.setPort(port);
        sshModel.setUser(user);
        sshModel.setName(name);
        sshModel.setConnectType(connectType.name());
        sshModel.setTimeout(timeout);
        try {
            Charset.forName(charset);
            sshModel.setCharset(charset);
        } catch (Exception e) {
            return new ApiResult<>(405, "请填写正确的编码格式," + e.getMessage());
        }
        // 判断重复
        Entity entity = Entity.create();
        entity.set("host", sshModel.getHost());
        entity.set("port", sshModel.getPort());
        entity.set("user", sshModel.getUser());
        entity.set("connectType", sshModel.getConnectType());
        Opt.ofBlankAble(id).ifPresent(s -> entity.set("id", String.format(" <> %s", s)));
        boolean exists = machineSshServer.exists(entity);
        Assert.state(!exists, "对应的SSH已经存在啦");
        try {

            String workspaceId = getWorkspaceId();
            Session session = machineSshServer.getSessionByModel(sshModel);
            JschUtil.close(session);
        } catch (Exception e) {
            log.warn("ssh连接失败", e);
            return new ApiResult<>(505, "ssh连接失败,请检查用户名、密码、host、端口等填写是否正确，超时时间是否合理：" + e.getMessage());
        }
        sshModel.setStatus(1);
        int i = add ? machineSshServer.insert(sshModel) : machineSshServer.updateById(sshModel);
        return ApiResult.success("操作成功");
    }


    @PostMapping(value = "delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> delete(@ValidatorItem String id) {
        long count = sshService.countByMachine(id);
        Assert.state(count <= 0, String.format("当前机器SSH还关联%s个ssh，不能直接删除（需要提前解绑或者删除关联数据后才能删除）", count));
        machineSshServer.delByKey(id);
        return ApiResult.success("操作成功");
    }

    /**
     * 执行记录
     *
     * @return json
     */
    @PostMapping(value = "log-list-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SSH_TERMINAL_LOG, method = MethodFeature.LIST)
    public ApiResult<PageResultDto<SshTerminalExecuteLog>> logListData(HttpServletRequest request) {
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        PageResultDto<SshTerminalExecuteLog> pageResult = sshTerminalExecuteLogService.listPage(paramMap);
        return ApiResult.success("获取成功", pageResult);
    }

    @GetMapping(value = "list-workspace-ssh", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<SshModel>> listWorkspaceSsh(@ValidatorItem String id) {
        MachineSshModel machineSshModel = machineSshServer.getByKey(id);
        Assert.notNull(machineSshModel, "没有对应的机器");
        SshModel sshModel = new SshModel();
        sshModel.setMachineSshId(id);
        List<SshModel> modelList = sshService.listByBean(sshModel);
        modelList = Optional.ofNullable(modelList).orElseGet(ArrayList::new);
        for (SshModel model : modelList) {
            model.setWorkspace(workspaceService.getByKey(model.getWorkspaceId()));
        }
        return ApiResult.success("", modelList);
    }

    /**
     * 保存工作空间配置
     *
     * @param fileDirs          文件夹
     * @param id                ID
     * @param notAllowedCommand 禁止输入的命令
     * @return json
     */
    @PostMapping(value = "save-workspace-config", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> saveWorkspaceConfig(
        String fileDirs,
        @ValidatorItem String id,
        String notAllowedCommand,
        String allowEditSuffix) {
        SshModel sshModel = new SshModel(id);
        // 目录
        if ((fileDirs == null || fileDirs.isEmpty())) {
            sshModel.fileDirs(null);
        } else {
            List<String> list = StrSplitter.splitTrim(fileDirs, "\n", true);
            for (String s : list) {
                String normalize = FileUtil.normalize(s + "/");
                int count = StrUtil.count(normalize, "/");
                Assert.state(count >= 2, "ssh 授权目录不能是根目录");
            }
            //
            UserModel userModel = getUser();
            Assert.state(!userModel.isDemoUser(), PermissionInterceptor.DEMO_TIP);
            sshModel.fileDirs(list);
        }
        sshModel.setNotAllowedCommand(notAllowedCommand);
        // 获取允许编辑的后缀
        List<String> allowEditSuffixList = AgentWhitelist.parseToList(allowEditSuffix, "允许编辑的文件后缀不能为空");
        sshModel.allowEditSuffix(allowEditSuffixList);
        sshService.updateById(sshModel);
        return ApiResult.success("操作成功");
    }

    /**
     * 将 ssh 分配到指定工作空间
     *
     * @param ids         ssh id
     * @param workspaceId 工作空间id
     * @return json
     */
    @PostMapping(value = "distribute", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> distribute(@ValidatorItem String ids, @ValidatorItem String workspaceId) {
        List<String> list = io.voyager1.util.ConvertUtil.splitTrim(ids, ",");
        for (String id : list) {
            MachineSshModel machineSshModel = machineSshServer.getByKey(id);
            Assert.notNull(machineSshModel, "没有对应的ssh");
            boolean exists = workspaceService.exists(new WorkspaceModel(workspaceId));
            Assert.state(exists, "不存在对应的工作空间");
            //
            if (!sshService.existsSsh2(workspaceId, id)) {
                //
                sshService.insert(machineSshModel, workspaceId);
            }
        }

        return ApiResult.success("操作成功");
    }

    /**
     * edit
     *
     * @param id ssh id
     * @return json
     */
    @PostMapping(value = "rest-hide-field", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> restHideField(@ValidatorItem String id) {
        MachineSshModel machineSshModel = new MachineSshModel();
        machineSshModel.setId(id);
        machineSshModel.setPassword("");
        machineSshModel.setPrivateKey("");
        machineSshServer.updateById(machineSshModel);
        return new ApiResult<>(200, "操作成功");
    }

    /**
     * 下载导入模板
     */
    @GetMapping(value = "import-template", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = "ssh导入模板.csv";
        this.setApplicationHeader(response, fileName);
        //
        CsvWriter writer = CsvUtil.getWriter(response.getWriter());
        writer.writeLine("name", "groupName", "host", "port", "user", "password", "charset", "connectType", "privateKey", "timeout");
        writer.flush();
    }


    /**
     * 导出数据
     */
    @GetMapping(value = "export-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void exportData(HttpServletResponse response, HttpServletRequest request) throws IOException {
        String prefix = "导出的 ssh 数据";
        String fileName = prefix + DateTime.now().toString("yyyy-MM-dd") + ".csv";
        this.setApplicationHeader(response, fileName);
        //
        CsvWriter writer = CsvUtil.getWriter(response.getWriter());
        int pageInt = 0;
        writer.writeLine("name", "groupName", "host", "port", "user", "password", "charset", "connectType", "privateKey", "timeout");
        while (true) {
            Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
            paramMap.remove("workspaceId");
            // 下一页
            paramMap.put("page", String.valueOf(++pageInt));
            PageResultDto<MachineSshModel> listPage = machineSshServer.listPage(paramMap, false);
            if (listPage.isEmpty()) {
                break;
            }
            listPage.getResult()
                .stream()
                .map((Function<MachineSshModel, List<Object>>) machineSshModel -> new java.util.ArrayList<>(java.util.Arrays.asList(machineSshModel.getName())))
                .map(objects -> objects.stream().map(StrUtil::toStringOrNull).toArray(String[]::new))
                .forEach(writer::writeLine);
            if (java.util.Objects.equals(listPage.getPage(), listPage.getTotalPage())) {
                // 最后一页
                break;
            }
        }
        writer.flush();
    }

    /**
     * 导入数据
     *
     * @return json
     */
    @PostMapping(value = "import-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public ApiResult<String> importData(MultipartFile file) throws IOException {
        Assert.notNull(file, "没有上传文件");
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);
        boolean csv = StrUtil.endWithIgnoreCase(extName, "csv");
        Assert.state(csv, "不允许的文件格式");
        assert originalFilename != null;
        File csvFile = FileUtil.file(serverConfig.getUserTempPath(), originalFilename);
        int addCount = 0, updateCount = 0;
        Charset fileCharset;
        try {
            file.transferTo(csvFile);
            fileCharset = CharsetDetector.detect(csvFile);
            Reader bomReader = FileUtil.getReader(csvFile, fileCharset);
            CsvReadConfig csvReadConfig = CsvReadConfig.defaultConfig();
            csvReadConfig.setHeaderLineNo(0);
            CsvReader reader = CsvUtil.getReader(bomReader, csvReadConfig);
            CsvData csvData;
            try {
                csvData = reader.read();
            } catch (Exception e) {
                log.error("解析 csv 异常", e);
                return new ApiResult<>(405, "解析文件异常," + e.getMessage());
            } finally {
                IoUtil.close(reader);
            }
            List<CsvRow> rows = csvData.getRows();
            Assert.notEmpty(rows, "没有任何数据");

            for (int i = 0; i < rows.size(); i++) {
                CsvRow csvRow = rows.get(i);
                String name = csvRow.getByName("name");
                int finalI = i;
                Assert.hasText(name, () -> String.format("第 %s 行 name 字段不能位空", finalI + 1));
                String groupName = csvRow.getByName("groupName");
                String host = csvRow.getByName("host");
                Assert.hasText(host, () -> String.format("第 %s 行 host 字段不能位空", finalI + 1));
                Integer port = ConvertUtil.toInt(csvRow.getByName("port"));
                Assert.state(port != null && NetUtil.isValidPort(port), () -> String.format("第 %s 行 port 字段不能位空或者不正确", finalI + 1));
                String user = csvRow.getByName("user");
                Assert.hasText(host, () -> String.format("第 %s 行 user 字段不能位空", finalI + 1));
                String password = csvRow.getByName("password");
                String charset = csvRow.getByName("charset");
                //
                String type = csvRow.getByName("connectType");
                type = (type == null || type.isEmpty() ? "" : type).toUpperCase();
                MachineSshModel.ConnectType connectType = EnumUtil.fromString(MachineSshModel.ConnectType.class, type, MachineSshModel.ConnectType.PASS);
                String privateKey = csvRow.getByName("privateKey");
                Integer timeout = ConvertUtil.toInt(csvRow.getByName("timeout"));
                //
                MachineSshModel where = new MachineSshModel();
                where.setHost(host);
                where.setUser(user);
                where.setPort(port);
                where.setConnectType(connectType.name());
                MachineSshModel machineSshModel = machineSshServer.queryByBean(where);
                if (machineSshModel == null) {
                    // 添加
                    where.setName(name);
                    where.setGroupName(groupName);
                    where.setPassword(password);
                    where.setPrivateKey(privateKey);
                    where.setTimeout(timeout);
                    where.setCharset(charset);
                    machineSshServer.insert(where);
                    addCount++;
                } else {
                    MachineSshModel update = new MachineSshModel();
                    update.setId(machineSshModel.getId());
                    update.setName(name);
                    update.setGroupName(groupName);
                    update.setPassword(password);
                    update.setPrivateKey(privateKey);
                    update.setTimeout(timeout);
                    update.setCharset(charset);
                    machineSshServer.updateById(update);
                    updateCount++;
                }
            }
        } finally {
            FileUtil.del(csvFile);
        }
        String fileCharsetStr = Optional.ofNullable(fileCharset).map(Charset::name).orElse("");
        return ApiResult.success("导入成功(编码格式：{}),添加 {} 条数据,修改 {} 条数据", fileCharsetStr, addCount, updateCount);
    }
}
