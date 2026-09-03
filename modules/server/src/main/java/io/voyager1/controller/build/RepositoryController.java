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
import org.springframework.data.domain.Pageable;
import io.voyager1.util.CsvUtil;
import io.voyager1.util.CsvRow;
import io.voyager1.util.CsvData;

import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.DateTime;
import io.voyager1.util.BomReader;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.Tuple;
import io.voyager1.util.Validator;
import io.voyager1.util.CsvReadConfig;
import io.voyager1.util.CsvReader;
import io.voyager1.util.CsvWriter;
import io.voyager1.util.StrUtil;
import io.voyager1.util.Tuple;
import io.voyager1.util.EnumUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.URLUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.plugin.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.build.BuildUtil;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerConst;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.controller.build.repository.ImportRepoUtil;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.RepositoryModel;
import io.voyager1.model.enums.GitProtocolEnum;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.dblog.BuildInfoService;
import io.voyager1.service.dblog.RepositoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Repository controller
 *
 */
@RestController
@Feature(cls = ClassFeature.BUILD_REPOSITORY)
@Slf4j
public class RepositoryController extends BaseServerController {

    private final RepositoryService repositoryService;
    private final BuildInfoService buildInfoService;

    public RepositoryController(RepositoryService repositoryService,
                                BuildInfoService buildInfoService) {
        this.repositoryService = repositoryService;
        this.buildInfoService = buildInfoService;
    }

    /**
     * load repository list
     *
     * @return json
     */
    @PostMapping(value = "/build/repository/list")
    @Feature(method = MethodFeature.LIST)
    public Object loadRepositoryList(HttpServletRequest request) {
        PageResultDto<RepositoryModel> pageResult = repositoryService.listPage(request);
        return ApiResult.success("获取成功", pageResult);
    }

    /**
     * load build list with params
     *
     * @return json
     */
    @GetMapping(value = "/build/repository/list-group", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<String>> getBuildGroupAll() {
        // load list with page
        List<String> group = repositoryService.listGroup();
        return ApiResult.success("", group);
    }

    /**
     * 下载导入模板
     */
    @GetMapping(value = "/build/repository/import-template", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = "仓库信息导入模板.csv";
        this.setApplicationHeader(response, fileName);
        //
        CsvWriter writer = CsvUtil.getWriter(response.getWriter());
        writer.writeLine("name", "address", "type", "protocol", "share", "private rsa", "username", "password", "timeout(s)");
        writer.flush();
    }

    /**
     * export repository by csv
     */
    @GetMapping(value = "/build/repository/export")
    @Feature(method = MethodFeature.DOWNLOAD)
    @SystemPermission
    public void exportRepositoryList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String prex  = "导出的 仓库信息 数据 ";
        String fileName = prex + DateTime.now().toString("yyyy-MM-dd") + ".csv";
        this.setApplicationHeader(response, fileName);
        CsvWriter writer = CsvUtil.getWriter(response.getWriter());
        int pageInt = 0;
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        writer.writeLine("name", "group", "address", "type", "protocol", "private rsa", "username", "password", "timeout(s)");
        while (true) {
            // 下一页
            paramMap.put("page", String.valueOf(++pageInt));
            PageResultDto<RepositoryModel> listPage = repositoryService.listPage(paramMap, false);
            if (listPage.isEmpty()) {
                break;
            }
            listPage.getResult()
                .stream()
                .map((Function<RepositoryModel, List<Object>>) repositoryModel -> new java.util.ArrayList<>(java.util.Arrays.asList(repositoryModel.getName())))
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
    @PostMapping(value = "/build/repository/import-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    @SystemPermission
    public ApiResult<String> importData(MultipartFile file, HttpServletRequest request) throws IOException {
        Assert.notNull(file, "没有上传文件");
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);
        boolean csv = StrUtil.endWithIgnoreCase(extName, "csv");
        Assert.state(csv, "不允许的文件格式");
        java.io.Reader bomReader = IoUtil.getBomReader(file.getInputStream());
        CsvReadConfig csvReadConfig = CsvReadConfig.defaultConfig();
        csvReadConfig.setHeaderLineNo(0);
        CsvReader reader = CsvUtil.getReader(bomReader, csvReadConfig);
        CsvData csvData;
        try {
            csvData = reader.read();
        } catch (Exception e) {
            log.error("解析 csv 异常", e);
            return new ApiResult<>(405, "解析文件异常," + e.getMessage());
        }
        List<CsvRow> rows = csvData.getRows();
        Assert.notEmpty(rows, "没有任何数据");
        int addCount = 0, updateCount = 0;
        for (int i = 0; i < rows.size(); i++) {
            int finalI = i;
            CsvRow csvRow = rows.get(i);
            String name = csvRow.getByName("name");
            Assert.hasText(name, () -> String.format("第 %s 行 name 字段不能位空", finalI + 1));
            String group = csvRow.getByName("group");
            String address = csvRow.getByName("address");
            Assert.hasText(address, () -> String.format("第 %s 行 address 字段不能位空", finalI + 1));
            String type = csvRow.getByName("type");
            Assert.hasText(type, () -> String.format("第 %s 行 type 字段不能位空", finalI + 1));
            RepositoryModel.RepoType repoType = null;
            if ("Git".equalsIgnoreCase(type)) {
                repoType = RepositoryModel.RepoType.Git;
            } else if ("Svn".equalsIgnoreCase(type)) {
                repoType = RepositoryModel.RepoType.Svn;
            }
            Assert.notNull(repoType, () -> String.format("第 %s 行 type 字段值错误（Git/Svn）", finalI + 1));
            String protocol = csvRow.getByName("protocol");
            Assert.hasText(protocol, () -> String.format("第 %s 行 protocol 字段不能位空", finalI + 1));
            GitProtocolEnum gitProtocolEnum = null;
            if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
                gitProtocolEnum = GitProtocolEnum.HTTP;
            } else if ("ssh".equalsIgnoreCase(protocol)) {
                gitProtocolEnum = GitProtocolEnum.SSH;
            }
            Assert.notNull(gitProtocolEnum, () -> String.format("第 %s 行 protocol 字段值错误（http/http/ssh）", finalI + 1));
            String privateRsa = csvRow.getByName("private rsa");
            String username = csvRow.getByName("username");
            String password = csvRow.getByName("password");
            Integer timeout = ConvertUtil.toInt(csvRow.getByName("timeout(s)"));
            //
            String optWorkspaceId = repositoryService.covertGlobalWorkspace(request);
            RepositoryModel where = new RepositoryModel();
            where.setProtocol(gitProtocolEnum.getCode());
            where.setGitUrl(address);
            // 工作空间
            where.setWorkspaceId(optWorkspaceId);
            // 查询是否存在
            RepositoryModel repositoryModel = repositoryService.queryByBean(where);
            //
            where.setName(name);
            where.setGroup(group);
            where.setTimeout(timeout);
            where.setPassword(password);
            where.setRsaPrv(privateRsa);
            where.setRepoType(repoType.getCode());
            where.setUserName(username);
            // 检查 rsa 私钥
            boolean andUpdateSshKey = this.checkAndUpdateSshKey(where);
            Assert.state(andUpdateSshKey, String.format("第 %s 行 rsa 私钥文件不存在或者有误", finalI + 1));
            if (where.getRepoType() == RepositoryModel.RepoType.Git.getCode()) {
                // 验证 git 仓库信息
                try {
                    IPlugin plugin = PluginFactory.getPlugin("git-clone");
                    Map<String, Object> map = where.toMap();
                    Tuple branchAndTagList = (Tuple) plugin.execute("branchAndTagList", map);
                    //Tuple tuple = GitUtil.getBranchAndTagList(repositoryModelReq);
                } catch (Exception e) {
                    log.warn("获取仓库分支失败", e);
                    throw new IllegalStateException(String.format("第 %s 行 仓库信息有误", finalI + 1));
                }
            }
            if (repositoryModel == null) {
                // 添加
                repositoryService.insert(where);
                addCount++;
            } else {
                where.setId(repositoryModel.getId());
                repositoryService.updateById(where);
                updateCount++;
            }
        }
        return ApiResult.success("导入成功,添加 {} 条数据,修改 {} 条数据", addCount, updateCount);
    }

    /**
     * load repository list
     *
     * @return json
     */
    @GetMapping(value = "/build/repository/get")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<RepositoryModel> loadRepositoryGet(String id, HttpServletRequest request) {
        RepositoryModel repositoryModel = repositoryService.getByKey(id, request);
        Assert.notNull(repositoryModel, "没有对应的仓库");
        return ApiResult.success("", repositoryModel);
    }

    /**
     * 过滤前端多余避免核心字段被更新
     *
     * @param repositoryModelReq 仓库对象
     * @return 可以更新的对象
     */
    private RepositoryModel convertRequest(RepositoryModel repositoryModelReq) {
        RepositoryModel repositoryModel = new RepositoryModel();
        repositoryModel.setName(repositoryModelReq.getName());
        repositoryModel.setGroup(repositoryModelReq.getGroup());
        repositoryModel.setUserName(repositoryModelReq.getUserName());
        repositoryModel.setId(repositoryModelReq.getId());
        repositoryModel.setProtocol(repositoryModelReq.getProtocol());
        repositoryModel.setTimeout(repositoryModelReq.getTimeout());
        repositoryModel.setGitUrl(repositoryModelReq.getGitUrl());
        repositoryModel.setPassword(repositoryModelReq.getPassword());
        repositoryModel.setRepoType(repositoryModelReq.getRepoType());
        repositoryModel.setSortValue(repositoryModelReq.getSortValue());
        repositoryModel.setRsaPrv(repositoryModelReq.getRsaPrv());
        return repositoryModel;
    }

    /**
     * edit
     *
     * @param req 仓库实体
     * @return json
     */
    @PostMapping(value = "/build/repository/edit")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> editRepository(RepositoryModel req, HttpServletRequest request) {
        RepositoryModel repositoryModelReq = this.convertRequest(req);
        repositoryModelReq.setWorkspaceId(repositoryService.covertGlobalWorkspace(request));
        this.checkInfo(repositoryModelReq, request);
        // 检查 rsa 私钥
        boolean andUpdateSshKey = this.checkAndUpdateSshKey(repositoryModelReq);
        Assert.state(andUpdateSshKey, "rsa 私钥文件不存在或者有误");

        if (repositoryModelReq.getRepoType() == RepositoryModel.RepoType.Git.getCode()) {
            RepositoryModel repositoryModel = repositoryService.getByKey(repositoryModelReq.getId(), false);
            if (repositoryModel != null) {
                repositoryModelReq.setRsaPrv((repositoryModelReq.getRsaPrv() == null || repositoryModelReq.getRsaPrv().isEmpty() ? repositoryModel.getRsaPrv() : repositoryModelReq.getRsaPrv()));
                repositoryModelReq.setPassword((repositoryModelReq.getPassword() == null || repositoryModelReq.getPassword().isEmpty() ? repositoryModel.getPassword() : repositoryModelReq.getPassword()));
            }
            // 验证 git 仓库信息
            try {
                IPlugin plugin = PluginFactory.getPlugin("git-clone");
                Map<String, Object> map = repositoryModelReq.toMap();
                Tuple branchAndTagList = (Tuple) plugin.execute("branchAndTagList", map);
                //Tuple tuple = GitUtil.getBranchAndTagList(repositoryModelReq);
            } catch (Exception e) {
                log.warn("获取仓库分支失败", e);
                return new ApiResult<>(500, "无法连接此仓库，" + e.getMessage());
            }
        }
        if ((repositoryModelReq.getId() == null || repositoryModelReq.getId().isEmpty())) {
            // insert data
            repositoryService.insert(repositoryModelReq);
        } else {
            // update data
            repositoryService.getByKeyAndGlobal(repositoryModelReq.getId(), request);
            //repositoryModelReq.setWorkspaceId(repositoryService.getCheckUserWorkspace(getRequest()));
            repositoryService.updateById(repositoryModelReq);
        }

        return new ApiResult<>(200, "操作成功");
    }

    /**
     * edit
     *
     * @param id 仓库信息
     * @return json
     */
    @PostMapping(value = "/build/repository/rest_hide_field")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> restHideField(@ValidatorItem String id, HttpServletRequest request) {
        RepositoryModel byKeyAndGlobal = repositoryService.getByKeyAndGlobal(id, request);
        RepositoryModel repositoryModel = new RepositoryModel();
        repositoryModel.setId(byKeyAndGlobal.getId());
        repositoryModel.setPassword("");
        repositoryModel.setRsaPrv("");
        repositoryModel.setRsaPub("");
        repositoryService.updateById(repositoryModel, request);
        return new ApiResult<>(200, "操作成功");
    }

    @GetMapping(value = "/build/repository/provider_info")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<Map<String, Map<String, Object>>> providerInfo() {
        Map<String, Map<String, Object>> providerList = ImportRepoUtil.getProviderList();
        return ApiResult.success(HttpStatus.OK.name(), providerList);
    }

    @GetMapping(value = "/build/repository/authorize_repos")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<JSONObject>> authorizeRepos(HttpServletRequest request,
                                                                  @ValidatorItem String token,
                                                                  String address,
                                                                  @ValidatorItem String type,
                                                                  String condition) {
        // 获取分页信息
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        Pageable page = repositoryService.parsePage(paramMap);
        Assert.hasText(token, "请填写个人令牌");
        // 搜索条件
        // 远程仓库
        //ImportRepoUtil.getProviderConfig(type);

        String userName = ImportRepoUtil.getCurrentUserName(type, token, address);
        io.voyager1.util.JSONObject repoList = ImportRepoUtil.getRepoList(type, condition, page, token, userName, address);
        PageResultDto<JSONObject> pageResultDto = new PageResultDto<>(page.getPageNumber(), page.getPageSize(), repoList.getLong("total").intValue());
        List<JSONObject> objects = repoList.getJSONArray("data")
            .stream()
            .map(o -> {
                io.voyager1.util.JSONObject obj = (io.voyager1.util.JSONObject) o;
                JSONObject jsonObject = new JSONObject();
                jsonObject.putAll(obj.toMap());
                jsonObject.put("exists", RepositoryController.this.checkRepositoryUrl(obj.getStr("url"), request));
                return jsonObject;
            })
            .collect(Collectors.toList());
        pageResultDto.setResult(objects);
        return ApiResult.success(HttpStatus.OK.name(), pageResultDto);
    }

    /**
     * 检查信息
     *
     * @param request            请求信息
     * @param repositoryModelReq 仓库信息
     */
    private void checkInfo(RepositoryModel repositoryModelReq, HttpServletRequest request) {
        Assert.notNull(repositoryModelReq, "请输入正确的信息");
        Assert.hasText(repositoryModelReq.getName(), "请填写仓库名称");
        Integer repoType = repositoryModelReq.getRepoType();
        Assert.state(repoType != null && (repoType == RepositoryModel.RepoType.Git.getCode() || repoType == RepositoryModel.RepoType.Svn.getCode()), "请选择仓库类型");
        Assert.hasText(repositoryModelReq.getGitUrl(), "请填写仓库地址");
        //
        Integer protocol = repositoryModelReq.getProtocol();
        Assert.state(protocol != null && (protocol == GitProtocolEnum.HTTP.getCode() || protocol == GitProtocolEnum.SSH.getCode()), "请选择拉取代码的协议");
        // 修正字段
        if (protocol == GitProtocolEnum.HTTP.getCode()) {
            //  http
            repositoryModelReq.setRsaPub("");
            repositoryModelReq.setRsaPrv("");
        } else if (protocol == GitProtocolEnum.SSH.getCode()) {
            // ssh
            repositoryModelReq.setPassword((repositoryModelReq.getPassword() == null || repositoryModelReq.getPassword().isEmpty() ? "" : repositoryModelReq.getPassword()));
        }
        String workspaceId = repositoryService.getCheckUserWorkspace(request);
        //
        boolean repositoryUrl = this.checkRepositoryUrl(workspaceId, repositoryModelReq.getId(), repositoryModelReq.getGitUrl());
        Assert.state(!repositoryUrl, "已经存在对应的仓库信息啦");
    }

    /**
     * 判断仓库地址是否存在
     *
     * @param workspaceId 工作空间ID
     * @param id          仓库ID
     * @param url         仓库 url
     * @return true 在当前工作空间已经存在拉
     */
    private boolean checkRepositoryUrl(String workspaceId, String id, String url) {
        // 判断仓库是否重复
        if ((id != null && !id.isEmpty())) {
            Validator.validateGeneral(id, "错误的ID");
        }
        return repositoryService.existsByGitUrl(workspaceId, id, url);
    }

    /**
     * 判断仓库地址是否存在
     *
     * @param url 仓库 url
     * @return true 在当前工作空间已经存在拉
     */
    private boolean checkRepositoryUrl(String url, HttpServletRequest request) {
        String workspaceId = repositoryService.getCheckUserWorkspace(request);
        return this.checkRepositoryUrl(workspaceId, null, url);
    }

    /**
     * check and update ssh key
     *
     * @param repositoryModelReq 仓库
     */
    private boolean checkAndUpdateSshKey(RepositoryModel repositoryModelReq) {
        if (repositoryModelReq.getProtocol() == GitProtocolEnum.SSH.getCode()) {
            // if rsa key is not empty
            if ((repositoryModelReq.getRsaPrv() != null && !repositoryModelReq.getRsaPrv().isEmpty())) {
                /**
                 * if rsa key is start with "file:"
                 * copy this file
                 */
                if (StrUtil.startWith(repositoryModelReq.getRsaPrv(), URLUtil.FILE_URL_PREFIX)) {
                    String rsaPath = StrUtil.removePrefix(repositoryModelReq.getRsaPrv(), URLUtil.FILE_URL_PREFIX);
                    if (!FileUtil.exist(rsaPath)) {
                        log.warn("there is no rsa file... {}", rsaPath);
                        return false;
                    }
                } else {
                    //File rsaFile = BuildUtil.getRepositoryRsaFile(repositoryModelReq.getId() + Const.ID_RSA);
                    //  or else put into file
                    //FileUtil.writeUtf8String(repositoryModelReq.getRsaPrv(), rsaFile);
                }
            }
        }
        return true;
    }

    /**
     * delete
     *
     * @param id 仓库ID
     * @return json
     */
    @PostMapping(value = "/build/repository/delete")
    @Feature(method = MethodFeature.DEL)
    public Object delRepository(@ValidatorItem String id, HttpServletRequest request) {
        // 判断仓库是否被关联
        Entity entity = Entity.create();
        entity.set("repositoryId", id);
        boolean exists = buildInfoService.exists(entity);
        Assert.state(!exists, "当前仓库被构建关联，不能直接删除");
        RepositoryModel keyAndGlobal = repositoryService.getByKeyAndGlobal(id, request);
        repositoryService.delByKey(keyAndGlobal.getId());
        File rsaFile = BuildUtil.getRepositoryRsaFile(id + ServerConst.ID_RSA);
        FileUtil.del(rsaFile);
        return ApiResult.success("删除成功");
    }

    /**
     * 排序
     *
     * @param id        节点ID
     * @param method    方法
     * @param compareId 比较的ID
     * @return msg
     */
    @GetMapping(value = "/build/repository/sort-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> sortItem(@ValidatorItem String id,
                                         @ValidatorItem String method,
                                         String compareId, HttpServletRequest request) {
        if ((method != null && method.equalsIgnoreCase("top"))) {
            repositoryService.sortToTop(id, request);
        } else if ((method != null && method.equalsIgnoreCase("up"))) {
            repositoryService.sortMoveUp(id, compareId, request);
        } else if ((method != null && method.equalsIgnoreCase("down"))) {
            repositoryService.sortMoveDown(id, compareId, request);
        } else {
            return new ApiResult<>(400, "不支持的方式" + method);
        }
        return new ApiResult<>(200, "操作成功");
    }

}
