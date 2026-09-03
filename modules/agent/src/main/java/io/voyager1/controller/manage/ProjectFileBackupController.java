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

import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.ArrayUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseAgentController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.model.data.NodeProjectInfoModel;
import io.voyager1.service.ProjectFileBackupService;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 项目备份文件管理
 *
 * @since 2022/5/11
 */
@RestController
@RequestMapping(value = "/manage/file/")
@Slf4j
public class ProjectFileBackupController extends BaseAgentController {

    private final ProjectFileBackupService projectFileBackupService;

    public ProjectFileBackupController(ProjectFileBackupService projectFileBackupService) {
        this.projectFileBackupService = projectFileBackupService;
    }

    /**
     * 查询备份列表
     *
     * @param id 项目ID
     * @return list
     */
    @RequestMapping(value = "list-backup", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> listBackup(String id) {
        //
        NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel(id);
        // 合并
        projectFileBackupService.margeBackupPath(projectInfoModel);
        //
        File path = projectFileBackupService.pathProject(projectInfoModel);
        //
        List<File> collect = Arrays.stream(Optional.ofNullable(path.listFiles()).orElse(new File[0]))
            .filter(FileUtil::isDirectory)
            .collect(Collectors.toList());
        if ((collect == null || collect.isEmpty())) {
            return ApiResult.success("查询成功");
        }
        List<JSONObject> arrayFile = FileUtils.parseInfo(collect, true, path.getAbsolutePath(), projectInfoModel.isDisableScanDir());
        //
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("path", FileUtil.getAbsolutePath(path));
        jsonObject.put("list", arrayFile);
        return ApiResult.success("查询成功", jsonObject);
    }

    /**
     * 获取指定备份的文件列表
     *
     * @param id       项目
     * @param path     读取的二级目录
     * @param backupId 备份id
     * @return list
     */
    @RequestMapping(value = "backup-item-files", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<JSONObject>> backupItemFiles(String id, String path, @ValidatorItem(msg = "备份id缺失") String backupId) {
        // 查询项目路径
        NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel();
        File lib = projectFileBackupService.pathProjectBackup(projectInfoModel, backupId);
        File fileDir = FileUtil.file(lib, (path == null || path.isEmpty() ? FileUtil.FILE_SEPARATOR : path));
        //
        File[] filesAll = FileUtil.exist(fileDir) ? fileDir.listFiles() : new File[]{};
        if ((filesAll == null || filesAll.length == 0)) {
            return ApiResult.success("查询成功", Collections.emptyList());
        }
        List<JSONObject> arrayFile = FileUtils.parseInfo(filesAll, false, lib.getAbsolutePath(), projectInfoModel.isDisableScanDir());
        return ApiResult.success("查询成功", arrayFile);
    }

    /**
     * 将执行文件下载到客户端 本地
     *
     * @param id        项目id
     * @param filename  文件名
     * @param levelName 文件夹名
     * @param backupId  备份id
     */
    @GetMapping(value = "backup-download", produces = MediaType.APPLICATION_JSON_VALUE)
    public void download(String id, @ValidatorItem(msg = "备份id缺失") String backupId, @ValidatorItem String filename, String levelName, HttpServletResponse response) {
        try {
            NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel();
            File lib = projectFileBackupService.pathProjectBackup(projectInfoModel, backupId);
            File file = FileUtil.file(lib, (levelName == null || levelName.isEmpty() ? FileUtil.FILE_SEPARATOR : levelName), filename);
            if (file.isDirectory()) {
                JakartaServletUtil.write(response, "暂不支持下载文件夹", MediaType.TEXT_HTML_VALUE);
                return;
            }
            JakartaServletUtil.write(response, file);
        } catch (Exception e) {
            log.error("下载文件异常", e);
            JakartaServletUtil.write(response, "下载文件异常:" + e.getMessage(), MediaType.TEXT_HTML_VALUE);
        }
    }

    /**
     * 删除文件
     *
     * @param id        项目ID
     * @param backupId  备份ID
     * @param filename  文件名
     * @param levelName 层级目录
     * @return msg
     */
    @RequestMapping(value = "backup-delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Object> deleteFile(String id, @ValidatorItem(msg = "备份id缺失") String backupId, @ValidatorItem String filename, String levelName) {
        NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel();
        File lib = projectFileBackupService.pathProjectBackup(projectInfoModel, backupId);
        File file = FileUtil.file(lib, (levelName == null || levelName.isEmpty() ? FileUtil.FILE_SEPARATOR : levelName), filename);
        CommandUtil.systemFastDel(file);
        return ApiResult.success("删除成功");
    }

    /**
     * 还原项目文件
     *
     * @param id        项目ID
     * @param backupId  备份ID
     * @param type      类型 clear 清空还原
     * @param filename  文件名
     * @param levelName 目录
     * @return msg
     */
    @RequestMapping(value = "backup-recover", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Object> recoverFile(String id, @ValidatorItem String backupId, String type, String filename, String levelName) {
        NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel();
        File backupPath = projectFileBackupService.pathProjectBackup(projectInfoModel, backupId);
        File projectPath = projectInfoService.resolveLibFile(projectInfoModel);
        //
        File backupFile;
        File projectFile;
        if ((filename == null || filename.isEmpty())) {
            // 目录
            backupFile = FileUtil.file(backupPath, (levelName == null || levelName.isEmpty() ? FileUtil.FILE_SEPARATOR : levelName));
            Assert.state(FileUtil.exist(backupFile), "对应的文件不存在");
            projectFile = FileUtil.file(projectPath, (levelName == null || levelName.isEmpty() ? FileUtil.FILE_SEPARATOR : levelName));
            // 创建文件
            FileUtil.mkdir(projectFile);
            // 清空
            if ((type != null && type.equalsIgnoreCase("clear"))) {
                FileUtil.clean(projectFile);
            }
            //
            FileUtil.copyContent(backupFile, projectFile, true);
        } else {
            // 文件
            backupFile = FileUtil.file(backupPath, (levelName == null || levelName.isEmpty() ? FileUtil.FILE_SEPARATOR : levelName), filename);
            Assert.state(FileUtil.exist(backupFile), "对应的文件不存在");
            projectFile = FileUtil.file(projectPath, (levelName == null || levelName.isEmpty() ? FileUtil.FILE_SEPARATOR : levelName), filename);
            FileUtil.copy(backupFile, projectFile, true);
        }
        return ApiResult.success("还原成功");
    }


}
