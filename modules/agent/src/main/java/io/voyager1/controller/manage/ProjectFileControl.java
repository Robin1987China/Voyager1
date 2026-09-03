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

package io.voyager1.controller.manage;

import io.voyager1.util.CollStreamUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.ArrayUtil;
import io.voyager1.util.BooleanUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.DigestUtil;
import io.voyager1.util.CompressUtil;
import io.voyager1.util.Archiver;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.util.HttpUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseAgentController;
import io.voyager1.common.commander.CommandOpResult;
import io.voyager1.common.commander.ProjectCommander;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.configuration.AgentConfig;
import io.voyager1.controller.manage.vo.DiffFileVo;
import io.voyager1.model.AfterOpt;
import io.voyager1.model.BaseEnum;
import io.voyager1.model.data.AgentWhitelist;
import io.voyager1.model.data.NodeProjectInfoModel;
import io.voyager1.service.ProjectFileBackupService;
import io.voyager1.service.WhitelistDirectoryService;
import io.voyager1.socket.ConsoleCommandOp;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.CompressionFileUtil;
import io.voyager1.util.FileUtils;
import io.voyager1.util.StringUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目文件管理
 *
 * @since 2019/4/17
 */
@RestController
@RequestMapping(value = "/manage/file/")
@Slf4j
public class ProjectFileControl extends BaseAgentController {

    private final WhitelistDirectoryService whitelistDirectoryService;
    private final AgentConfig agentConfig;
    private final ProjectFileBackupService projectFileBackupService;
    private final ProjectCommander projectCommander;

    public ProjectFileControl(WhitelistDirectoryService whitelistDirectoryService,
                              AgentConfig agentConfig,
                              ProjectFileBackupService projectFileBackupService,
                              ProjectCommander projectCommander) {
        this.whitelistDirectoryService = whitelistDirectoryService;
        this.agentConfig = agentConfig;
        this.projectFileBackupService = projectFileBackupService;
        this.projectCommander = projectCommander;
    }

    @RequestMapping(value = "getFileList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<JSONObject>> getFileList(String id, String path) {
        // 查询项目路径
        NodeProjectInfoModel pim = getProjectInfoModel();
        String lib = projectInfoService.resolveLibPath(pim);
        File fileDir = FileUtil.file(lib, (path == null || path.isEmpty() ? FileUtil.FILE_SEPARATOR : path));
        boolean exist = FileUtil.exist(fileDir);
        if (!exist) {
            return ApiResult.success("查询成功", Collections.emptyList());
        }
        //
        File[] filesAll = fileDir.listFiles();
        if ((filesAll == null || filesAll.length == 0)) {
            return ApiResult.success("查询成功", Collections.emptyList());
        }
        boolean disableScanDir = pim.isDisableScanDir();
        List<JSONObject> arrayFile = FileUtils.parseInfo(filesAll, false, lib, disableScanDir);
        AgentWhitelist whitelist = whitelistDirectoryService.getWhitelist();
        for (JSONObject jsonObject : arrayFile) {
            String filename = jsonObject.getString("filename");
            jsonObject.put("textFileEdit", AgentWhitelist.checkSilentFileSuffix(whitelist.getAllowEditSuffix(), filename));
        }
        return ApiResult.success("查询成功", arrayFile);
    }

    /**
     * 对比文件
     *
     * @param diffFileVo 参数
     * @return json
     */
    @PostMapping(value = "diff_file", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> diffFile(@RequestBody DiffFileVo diffFileVo) {
        String id = diffFileVo.getId();
        NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel(id);
        //
        List<DiffFileVo.DiffItem> data = diffFileVo.getData();
        Assert.notEmpty(data, "没有要对比的数据");
        // 扫描项目目录下面的所有文件
        File lib = projectInfoService.resolveLibFile(projectInfoModel);
        String path = FileUtil.file(lib, Opt.ofBlankAble(diffFileVo.getDir()).orElse("/")).getAbsolutePath();
        List<File> files = FileUtil.loopFiles(path);
        // 将所有的文件信息组装并签名
        List<JSONObject> collect = files.stream().map(file -> {
            //
            JSONObject item = new JSONObject();
            item.put("name", StringUtil.delStartPath(file, path, true));
            item.put("sha1", DigestUtil.sha1(file));
            return item;
        }).collect(Collectors.toList());
        // 得到 当前下面文件夹下面所有的文件信息 map
        Map<String, String> nowMap = CollStreamUtil.toMap(collect,
            jsonObject12 -> jsonObject12.getString("name"),
            jsonObject1 -> jsonObject1.getString("sha1"));
        // 将需要对应的信息转为 map
        Map<String, String> tryMap = CollStreamUtil.toMap(data, DiffFileVo.DiffItem::getName, DiffFileVo.DiffItem::getSha1);
        // 对应需要 当前项目文件夹下没有的和文件内容有变化的
        List<JSONObject> canSync = tryMap.entrySet()
            .stream()
            .filter(stringStringEntry -> {
                String nowSha1 = nowMap.get(stringStringEntry.getKey());
                if ((nowSha1 == null || nowSha1.isEmpty())) {
                    // 不存在
                    return true;
                }
                // 如果 文件信息一致 则过滤
                return !java.util.Objects.equals(stringStringEntry.getValue(), nowSha1);
            })
            .map(stringStringEntry -> {
                //
                JSONObject item = new JSONObject();
                item.put("name", stringStringEntry.getKey());
                item.put("sha1", stringStringEntry.getValue());
                return item;
            })
            .collect(Collectors.toList());
        // 对比项目文件夹下有对，但是需要对应对信息里面没有对。此类文件需要删除
        List<JSONObject> delArray = nowMap.entrySet()
            .stream()
            .filter(stringStringEntry -> !tryMap.containsKey(stringStringEntry.getKey()))
            .map(stringStringEntry -> {
                //
                JSONObject item = new JSONObject();
                item.put("name", stringStringEntry.getKey());
                item.put("sha1", stringStringEntry.getValue());
                return item;
            })
            .collect(Collectors.toList());
        //
        JSONObject result = new JSONObject();
        result.put("diff", canSync);
        result.put("del", delArray);
        return ApiResult.success("", result);
    }


    private void saveProjectFileBefore(File lib, NodeProjectInfoModel projectInfoModel) throws Exception {
        String closeFirstStr = getParameter("closeFirst");
        // 判断是否需要先关闭项目
        boolean closeFirst = Boolean.parseBoolean(closeFirstStr);
        if (closeFirst) {
            CommandOpResult result = projectCommander.execCommand(ConsoleCommandOp.stop, projectInfoModel);
            Assert.state(result.isSuccess(), "关闭项目失败：" + result.msgStr());
        }
        String clearType = getParameter("clearType");
        // 判断是否需要清空
        if ("clear".equalsIgnoreCase(clearType)) {
            CommandUtil.systemFastDel(lib);
        }
    }

    @RequestMapping(value = "upload-sharding", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<CommandOpResult> uploadSharding(MultipartFile file,
                                                        String sliceId,
                                                        Integer totalSlice,
                                                        Integer nowSlice,
                                                        String fileSumMd5) throws Exception {
        String tempPathName = agentConfig.getFixedTempPathName();
        this.uploadSharding(file, tempPathName, sliceId, totalSlice, nowSlice, fileSumMd5);

        return ApiResult.success("上传成功");
    }

    @RequestMapping(value = "sharding-merge", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<CommandOpResult> shardingMerge(String type,
                                                       String levelName,
                                                       Integer stripComponents,
                                                       String sliceId,
                                                       Integer totalSlice,
                                                       String fileSumMd5,
                                                       String after) throws Exception {
        String tempPathName = agentConfig.getFixedTempPathName();
        File successFile = this.shardingTryMerge(tempPathName, sliceId, totalSlice, fileSumMd5);
        // 处理上传文件
        return this.upload(successFile, type, levelName, stripComponents, after);
    }

    /**
     * 处理上传文件
     *
     * @param file            上传的文件
     * @param type            上传类型
     * @param levelName       文件夹
     * @param stripComponents 剔除文件夹
     * @param after           上传之后
     * @return 结果
     * @throws Exception 异常
     */
    private ApiResult<CommandOpResult> upload(File file, String type, String levelName, Integer stripComponents, String after) throws Exception {
        NodeProjectInfoModel pim = getProjectInfoModel();
        File libFile = projectInfoService.resolveLibFile(pim);
        File lib = (levelName == null || levelName.isEmpty()) ? libFile : FileUtil.file(libFile, levelName);
        // 备份文件
        String backupId = projectFileBackupService.backup(pim);
        try {
            //
            this.saveProjectFileBefore(lib, pim);
            if ("unzip".equals(type)) {
                // 解压
                try {
                    int stripComponentsValue = ConvertUtil.toInt(stripComponents, 0);
                    CompressionFileUtil.unCompress(file, lib, stripComponentsValue);
                } finally {
                    if (!FileUtil.del(file)) {
                        log.error("{}{}", "删除文件失败：", file.getPath());
                    }
                }
            } else {
                // 移动文件到对应目录
                FileUtil.mkdir(lib);
                FileUtil.move(file, lib, true);
            }
            projectCommander.asyncWebHooks(pim, "fileChange", "changeEvent", "upload", "levelName", levelName, "fileType", type, "fileName", file.getName());
            //
            ApiResult<CommandOpResult> resultJsonMessage = this.saveProjectFileAfter(after, pim);
            if (resultJsonMessage != null) {
                return resultJsonMessage;
            }
        } finally {
            projectFileBackupService.checkDiff(pim, backupId);
        }
        return ApiResult.success("上传成功");
    }

    private ApiResult<CommandOpResult> saveProjectFileAfter(String after, NodeProjectInfoModel pim) throws Exception {
        if ((after == null || after.isEmpty())) {
            return null;
        }
        log.debug("开始准备项目重启：{} {}", pim.getId(), after);
        //
        AfterOpt afterOpt = BaseEnum.getEnum(AfterOpt.class, ConvertUtil.toInt(after, AfterOpt.No.getCode()));
        if ("restart".equalsIgnoreCase(after) || afterOpt == AfterOpt.Restart) {
            CommandOpResult result = projectCommander.execCommand(ConsoleCommandOp.restart, pim);

            return new ApiResult<>(result.isSuccess() ? 200 : 405, "上传成功并重启", result);
        } else if (afterOpt == AfterOpt.Order_Restart || afterOpt == AfterOpt.Order_Must_Restart) {
            CommandOpResult result = projectCommander.execCommand(ConsoleCommandOp.restart, pim);

            return new ApiResult<>(result.isSuccess() ? 200 : 405, "上传成功并重启", result);
        }
        return null;
    }

    @RequestMapping(value = "deleteFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> deleteFile(String filename, String type, String levelName) {
        NodeProjectInfoModel pim = getProjectInfoModel();
        File libFile = projectInfoService.resolveLibFile(pim);
        File file = FileUtil.file(libFile, (levelName == null || levelName.isEmpty() ? "/" : levelName));
        // 备份文件
        String backupId = projectFileBackupService.backup(pim);
        try {
            if ("clear".equalsIgnoreCase(type)) {
                // 清空文件
                if (FileUtil.clean(file)) {
                    projectCommander.asyncWebHooks(pim, "fileChange", "changeEvent", "delete", "levelName", levelName, "deleteType", type, "fileName", filename);
                    return ApiResult.success("清除成功");
                }
                boolean run = projectCommander.isRun(pim);
                Assert.state(!run, "文件被占用，请先停止项目");
                return new ApiResult<>(500, "删除失败：" + file.getAbsolutePath());
            } else {
                // 删除文件
                Assert.hasText(filename, "请选择要删除的文件");
                file = FileUtil.file(file, filename);
                if (FileUtil.del(file)) {
                    projectCommander.asyncWebHooks(pim, "fileChange", "changeEvent", "delete", "levelName", levelName, "deleteType", type, "fileName", filename);
                    return ApiResult.success("删除成功");
                }
                return new ApiResult<>(500, "删除失败");
            }
        } finally {
            projectFileBackupService.checkDiff(pim, backupId);
        }
    }


    @RequestMapping(value = "batch_delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> batchDelete(@RequestBody DiffFileVo diffFileVo) {
        String id = diffFileVo.getId();
        String dir = diffFileVo.getDir();
        NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel(id);
        // 备份文件
        String backupId = projectFileBackupService.backup(projectInfoModel);
        try {
            //
            List<DiffFileVo.DiffItem> data = diffFileVo.getData();
            Assert.notEmpty(data, "没有要对比的数据");
            File libFile = projectInfoService.resolveLibFile(projectInfoModel);
            //
            File path = FileUtil.file(libFile, (dir == null || dir.isEmpty() ? "/" : dir));
            for (DiffFileVo.DiffItem datum : data) {
                File file = FileUtil.file(path, datum.getName());
                if (FileUtil.del(file)) {
                    continue;
                }
                return new ApiResult<>(500, "删除失败：" + file.getAbsolutePath());
            }
            projectCommander.asyncWebHooks(projectInfoModel, "fileChange", "changeEvent", "batch-delete", "levelName", dir);
            return ApiResult.success("删除成功");
        } finally {
            projectFileBackupService.checkDiff(projectInfoModel, backupId);
        }

    }

    /**
     * 读取文件内容 （只能处理文本文件）
     *
     * @param filePath 相对项目文件的文件夹
     * @param filename 读取的文件名
     * @return json
     */
    @PostMapping(value = "read_file", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> readFile(String filePath, String filename) {
        NodeProjectInfoModel pim = getProjectInfoModel();
        filePath = (filePath == null || filePath.isEmpty() ? File.separator : filePath);
        // 判断文件后缀
        AgentWhitelist whitelist = whitelistDirectoryService.getWhitelist();
        Charset charset = AgentWhitelist.checkFileSuffix(whitelist.getAllowEditSuffix(), filename);
        File libFile = projectInfoService.resolveLibFile(pim);
        File file = FileUtil.file(libFile, filePath, filename);
        String ymlString = FileUtil.readString(file, charset);
        return ApiResult.success("", ymlString);
    }

    /**
     * copy
     *
     * @param filePath 相对项目文件的文件夹
     * @param filename 文件名
     * @return json
     */
    @PostMapping(value = "copy", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> copy(String filePath, String filename) {
        NodeProjectInfoModel pim = getProjectInfoModel();
        filePath = (filePath == null || filePath.isEmpty() ? File.separator : filePath);
        File libFile = projectInfoService.resolveLibFile(pim);
        File file = FileUtil.file(libFile, filePath, filename);
        int counter = 1;
        String baseName = FileUtil.mainName(file);
        String extension = FileUtil.extName(file);
        if ((extension != null && !extension.isEmpty())) {
            extension = "." + extension;
        } else {
            extension = "";
        }
        String newName;
        File targetFile;
        // 生成不冲突的新文件名
        do {
            newName = String.format("%s(%s)%s", baseName, counter, extension);
            targetFile = FileUtil.file(libFile, filePath, newName);
            counter++;
        } while (FileUtil.exist(targetFile));
        if (FileUtil.isDirectory(file)) {
            FileUtil.copyContent(file, targetFile, false);
        } else {
            FileUtil.copy(file, targetFile, false);
        }
        return ApiResult.success("复制成功");
    }

    /**
     * compress
     *
     * @param filePath 相对项目文件的文件夹
     * @param filename 文件名
     * @return json
     */
    @PostMapping(value = "compress", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> compress(String filePath, String filename, String type) {
        NodeProjectInfoModel pim = getProjectInfoModel();
        filePath = (filePath == null || filePath.isEmpty() ? File.separator : filePath);
        File libFile = projectInfoService.resolveLibFile(pim);
        File file = FileUtil.file(libFile, filePath, filename);
        Assert.state(FileUtil.isDirectory(file), "请选择文件夹进行压缩");
        String ext;
        if (java.util.Objects.equals(type, "zip")) {
            ext = ".zip";
        } else if (java.util.Objects.equals(type, "tar")) {
            ext = ".tar";
        } else if (java.util.Objects.equals(type, "tar.gz")) {
            ext = ".tar.gz";
        } else {
            return ApiResult.fail("不支持的压缩类型," + type);
        }
        int counter = 0;
        String baseName = FileUtil.mainName(file);
        String newName;
        File targetFile;
        // 生成不冲突的新文件名
        do {
            if (counter == 0) {
                newName = String.format("%s%s", baseName, ext);
            } else {
                newName = String.format("%s(%s)%s", baseName, counter, ext);
            }
            targetFile = FileUtil.file(libFile, filePath, newName);
            counter++;
        } while (FileUtil.exist(targetFile));
        //
        try (Archiver archiver = CompressUtil.createArchiver(Charset.defaultCharset(), FileUtil.extName(targetFile), targetFile)) {
            archiver.add(file);
        }
        return ApiResult.success("压缩成功");
    }

    /**
     * 保存文件内容 （只能处理文本文件）
     *
     * @param filePath 相对项目文件的文件夹
     * @param filename 读取的文件名
     * @param fileText 文件内容
     * @return json
     */
    @PostMapping(value = "update_config_file", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Object> updateConfigFile(String filePath, String filename, String fileText) {
        NodeProjectInfoModel pim = getProjectInfoModel();
        filePath = (filePath == null || filePath.isEmpty() ? File.separator : filePath);
        // 判断文件后缀
        AgentWhitelist whitelist = whitelistDirectoryService.getWhitelist();
        Charset charset = AgentWhitelist.checkFileSuffix(whitelist.getAllowEditSuffix(), filename);
        // 备份文件
        String backupId = projectFileBackupService.backup(pim);
        File libFile = projectInfoService.resolveLibFile(pim);
        try {
            FileUtil.writeString(fileText, FileUtil.file(libFile, filePath, filename), charset);
            projectCommander.asyncWebHooks(pim, "fileChange", "changeEvent", "edit", "levelName", filePath, "fileName", filename);
            return ApiResult.success("文件写入成功");
        } finally {
            projectFileBackupService.checkDiff(pim, backupId);
        }
    }


    /**
     * 将执行文件下载到客户端 本地
     *
     * @param id        项目id
     * @param filename  文件名
     * @param levelName 文件夹名
     */
    @GetMapping(value = "download", produces = MediaType.APPLICATION_JSON_VALUE)
    public void download(String id, String filename, String levelName, HttpServletResponse response) {
        Assert.hasText(filename, "请选择文件");
//		String safeFileName = pathSafe(filename);
//		if ((safeFileName == null || safeFileName.isEmpty())) {
//			return ApiResult.getString(405, "非法操作");
//		}
        NodeProjectInfoModel pim = getProjectInfoModel();
        File libFile = projectInfoService.resolveLibFile(pim);
        try {
            File file = FileUtil.file(libFile, (levelName == null || levelName.isEmpty() ? FileUtil.FILE_SEPARATOR : levelName), filename);
            if (file.isDirectory()) {
                JakartaServletUtil.write(response, ApiResult.getString(400, "暂不支持下载文件夹"), MediaType.APPLICATION_JSON_VALUE);
                return;
            }
            JakartaServletUtil.write(response, file);
        } catch (Exception e) {
            log.error("下载文件异常", e);
            JakartaServletUtil.write(response, ApiResult.getString(400, "下载失败。请刷新页面后重试", e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
        }
    }

    /**
     * 下载远程文件
     *
     * @param id              项目id
     * @param url             远程 url 地址
     * @param levelName       保存的文件夹
     * @param unzip           是否为压缩包、true 将自动解压
     * @param stripComponents 剔除层级
     * @return json
     */
    @PostMapping(value = "remote_download", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> remoteDownload(String id, String url, String levelName, String unzip, Integer stripComponents) {
        Assert.hasText(url, "请输入正确的远程地址");
        NodeProjectInfoModel pim = getProjectInfoModel();
        File libFile = projectInfoService.resolveLibFile(pim);
        String tempPathName = agentConfig.getTempPathName();
        //
        String backupId = null;
        try {
            File downloadFile = HttpUtil.downloadFileFromUrl(url, tempPathName);
            String fileSize = FileUtil.readableFileSize(downloadFile);
            // 备份文件
            backupId = projectFileBackupService.backup(pim);
            File file = FileUtil.file(libFile, (levelName == null || levelName.isEmpty() ? FileUtil.FILE_SEPARATOR : levelName));
            FileUtil.mkdir(file);
            if (Boolean.parseBoolean(unzip)) {
                // 需要解压文件
                try {
                    int stripComponentsValue = ConvertUtil.toInt(stripComponents, 0);
                    CompressionFileUtil.unCompress(downloadFile, file, stripComponentsValue);
                } finally {
                    if (!FileUtil.del(downloadFile)) {
                        log.error("删除文件失败：" + file.getPath());
                    }
                }
            } else {
                // 移动文件到对应目录
                FileUtil.move(downloadFile, file, true);
            }
            projectCommander.asyncWebHooks(pim, "fileChange", "changeEvent", "remoteDownload", "levelName", levelName, "fileName", file.getName(), "url", url);
            return ApiResult.success("下载成功文件大小：" + fileSize);
        } catch (Exception e) {
            log.error("下载远程文件异常", e);
            return new ApiResult<>(500, "下载远程文件失败:" + e.getMessage());
        } finally {
            projectFileBackupService.checkDiff(pim, backupId);
        }
    }

    /**
     * 创建文件夹/文件
     *
     * @param id        项目ID
     * @param levelName 二级文件夹名
     * @param filename  文件名
     * @param unFolder  true/1 为文件夹，false/2 为文件
     * @return json
     */
    @PostMapping(value = "new_file_folder.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Object> newFileFolder(String id, String levelName, @ValidatorItem String filename, String unFolder) {
        NodeProjectInfoModel projectInfoModel = getProjectInfoModel();
        File libFile = projectInfoService.resolveLibFile(projectInfoModel);
        File file = FileUtil.file(libFile, (levelName == null || levelName.isEmpty() ? FileUtil.FILE_SEPARATOR : levelName), filename);
        //
        Assert.state(!FileUtil.exist(file), "文件夹或者文件已存在");
        boolean folder = !ConvertUtil.toBool(unFolder, false);
        if (folder) {
            FileUtil.mkdir(file);
        } else {
            FileUtil.touch(file);
        }
        projectCommander.asyncWebHooks(projectInfoModel, "fileChange", "changeEvent", "newFileOrFolder", "levelName", levelName, "fileName", filename, "folder", folder);
        return ApiResult.success("操作成功");
    }

    /**
     * 修改文件夹/文件
     *
     * @param id        项目ID
     * @param levelName 二级文件夹名
     * @param filename  文件名
     * @param newname   新文件名
     * @return json
     */
    @PostMapping(value = "rename.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Object> rename(String id, String levelName, @ValidatorItem String filename, String newname) {
        NodeProjectInfoModel projectInfoModel = getProjectInfoModel();
        File libFile = projectInfoService.resolveLibFile(projectInfoModel);
        File file = FileUtil.file(libFile, (levelName == null || levelName.isEmpty() ? FileUtil.FILE_SEPARATOR : levelName), filename);
        File newFile = FileUtil.file(libFile, (levelName == null || levelName.isEmpty() ? FileUtil.FILE_SEPARATOR : levelName), newname);

        Assert.state(FileUtil.exist(file), "文件不存在");
        Assert.state(!FileUtil.exist(newFile), "文件名已经存在拉");

        FileUtil.rename(file, newname, false);
        projectCommander.asyncWebHooks(projectInfoModel, "fileChange", "changeEvent", "rename", "levelName", levelName, "fileName", filename, "newname", newname);
        return ApiResult.success("操作成功");
    }

}
