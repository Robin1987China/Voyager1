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

package io.voyager1.service;

import io.voyager1.util.CollStreamUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.DateTime;
import io.voyager1.util.FileUtil;
import io.voyager1.util.ArrayUtil;
import io.voyager1.util.ReUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.DigestUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.i18n.I18nThreadUtil;
import io.voyager1.configuration.AgentConfig;
import io.voyager1.configuration.ProjectConfig;
import io.voyager1.model.data.DslYmlDto;
import io.voyager1.model.data.NodeProjectInfoModel;
import io.voyager1.service.manage.ProjectInfoService;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 项目文件备份工具
 *
 * @since 2022/5/10
 */
@Slf4j
@Service
public class ProjectFileBackupService {

    private final ProjectConfig projectConfig;
    private final ProjectInfoService projectInfoService;

    public ProjectFileBackupService(AgentConfig agentConfig,
                                    ProjectInfoService projectInfoService) {
        this.projectConfig = agentConfig.getProject();
        this.projectInfoService = projectInfoService;
    }

    /**
     * 整个项目的备份目录
     *
     * @param projectInfoModel 项目
     * @return file
     */
    public File pathProject(NodeProjectInfoModel projectInfoModel) {
        NodeProjectInfoModel infoModel = projectInfoService.resolveModel(projectInfoModel);
        DslYmlDto dslYmlDto = infoModel.dslConfig();
        String backupPath = resolveBackupPath(dslYmlDto);
        return pathProject(backupPath, infoModel.getId());
    }

    /**
     * 整个项目的备份目录
     *
     * @param pathId     项目ID
     * @param backupPath 备份路径
     * @return file
     */
    private File pathProject(String backupPath, String pathId) {
        if ((backupPath == null || backupPath.isEmpty())) {
            String dataPath = Voyager1Application.getInstance().getDataPath();
            return FileUtil.file(dataPath, "project_file_backup", pathId);
        }
        return FileUtil.file(backupPath, pathId);
    }

    /**
     * 获取项目的单次备份目录，备份ID
     *
     * @param projectInfoModel 项目
     * @param backupId         备份ID
     * @return file
     */
    public File pathProjectBackup(NodeProjectInfoModel projectInfoModel, String backupId) {
        File fileBackup = pathProject(projectInfoModel);
        return FileUtil.file(fileBackup, backupId);
    }

    /**
     * 备份项目文件
     *
     * @param projectInfoModel 项目
     */
    public String backup(NodeProjectInfoModel projectInfoModel) {
        NodeProjectInfoModel infoModel = projectInfoService.resolveModel(projectInfoModel);
        int backupCount = resolveBackupCount(infoModel.dslConfig());
        if (backupCount <= 0) {
            // 未开启备份
            return null;
        }
        File file = projectInfoService.resolveLibFile(infoModel);
        //
        if (!FileUtil.exist(file)) {
            return null;
        }
        String backupId = DateTime.now().toString("yyyyMMddHHmmssSSS");
        log.debug("开始准备备份项目文件：{} {}", projectInfoModel.getId(), backupId);
        File projectFileBackup = this.pathProjectBackup(infoModel, backupId);
        Assert.state(!FileUtil.exist(projectFileBackup), "备份目录冲突：" + projectFileBackup.getName());
        FileUtil.copyContent(file, projectFileBackup, true);
        //
        return backupId;
    }

    /**
     * 检查备份保留个数
     *
     * @param backupPath 目录
     */
    private void clearOldBackup(File backupPath, DslYmlDto dslYmlDto) {
        int backupCount = resolveBackupCount(dslYmlDto);
        //
        if (!FileUtil.isDirectory(backupPath)) {
            return;
        }
        File[] files = backupPath.listFiles();
        if (files == null) {
            return;
        }
        List<File> collect = Arrays.stream(files)
            .filter(FileUtil::isDirectory)
            .sorted(Comparator.comparing(FileUtil::lastModifiedTime))
            .collect(Collectors.toList());
        // 截取
        int max = Math.max(collect.size() - backupCount, 0);
        if (max > 0) {
            collect = collect.subList(0, max);
            // 删除
            collect.forEach(CommandUtil::systemFastDel);
        }
    }

    /**
     * 解析项目的备份路径
     *
     * @param dslYmlDto dsl 配置
     * @return path
     */
    public String resolveBackupPath(DslYmlDto dslYmlDto) {
        return Optional.ofNullable(dslYmlDto)
            .map(DslYmlDto::getFile)
            .map(DslYmlDto.FileConfig::getBackupPath)
            .filter(s -> !(s == null || s.isEmpty()))
            .orElse(null);
    }

    public int resolveBackupCount(DslYmlDto dslYmlDto) {
        return Optional.ofNullable(dslYmlDto)
            .map(DslYmlDto::getFile)
            .map(DslYmlDto.FileConfig::getBackupCount)
            .orElseGet(projectConfig::getFileBackupCount);
    }

    /**
     * 检查文件变动
     *
     * @param projectInfoModel 项目
     * @param backupId         要对比的备份ID
     */
    public void checkDiff(NodeProjectInfoModel projectInfoModel, String backupId) {
        if ((backupId == null || backupId.isEmpty())) {
            // 备份ID 不存在
            return;
        }
        log.debug("开始检查备份项目文件：{} {}", projectInfoModel.getId(), backupId);
        NodeProjectInfoModel infoModel = projectInfoService.resolveModel(projectInfoModel);
        File projectPath = projectInfoService.resolveLibFile(infoModel);
        DslYmlDto dslYmlDto = infoModel.dslConfig();
        // 考虑到大文件对比，比较耗时。需要异步对比文件
        I18nThreadUtil.execute(() -> {
            try {
                //String useBackupPath = resolveBackupPath(dslYmlDto);
                File backupItemPath = this.pathProjectBackup(infoModel, backupId);
                File backupPath = this.pathProject(infoModel);
                // 获取文件列表
                Map<String, File> backupFiles = this.listFiles(backupItemPath);
                // 差异备份
                boolean diffBackup = Optional.ofNullable(dslYmlDto)
                    .map(DslYmlDto::getFile)
                    .map(DslYmlDto.FileConfig::getDiffBackup)
                    .orElse(true);
                if (diffBackup) {
                    Map<String, File> nowFiles = this.listFiles(projectPath);
                    nowFiles.forEach((fileSha1, file) -> {
                        // 当前目录存在的，但是备份目录也存在的相同文件则删除
                        File backupFile = backupFiles.get(fileSha1);
                        if (backupFile != null) {
                            CommandUtil.systemFastDel(backupFile);
                            backupFiles.remove(fileSha1);
                        }
                    });
                }
                // 判断保存指定后缀
                String[] backupSuffix = Optional.ofNullable(dslYmlDto)
                    .map(DslYmlDto::getFile)
                    .map(DslYmlDto.FileConfig::getBackupSuffix)
                    .orElseGet(projectConfig::getFileBackupSuffix);
                if ((backupSuffix != null && backupSuffix.length > 0)) {
                    backupFiles.values()
                        .stream()
                        .filter(file -> {
                            String name = FileUtil.getName(file);
                            for (String reg : backupSuffix) {
                                if (ReUtil.isMatch(reg, name)) {
                                    // 满足正则条件
                                    return false;
                                }
                            }
                            return !StrUtil.endWithAny(name, backupSuffix);
                        })
                        .forEach(CommandUtil::systemFastDel);
                }
                // 删除空文件夹
                loopClean(backupItemPath);
                // 检查备份保留个数
                clearOldBackup(backupPath, dslYmlDto);
                // 合并之前备份目录
                margeBackupPath(infoModel);
            } catch (Exception e) {
                log.warn("对比清空项目文件备份失败", e);
            }
        });
    }

    /**
     * 合并备份路径
     *
     * @param projectInfoModel 项目
     */
    public void margeBackupPath(NodeProjectInfoModel projectInfoModel) {
        File backupPath = this.pathProject(projectInfoModel);
        File backupPathBefore = this.pathProject(null, projectInfoModel.getId());
        if (FileUtil.isDirectory(backupPathBefore) && !FileUtil.equals(backupPathBefore, backupPath)) {
            // 默认的备份路径存在，并且现在的路径和默认的不一致
            FileUtil.moveContent(backupPathBefore, backupPath, true);
            FileUtil.del(backupPathBefore);
        }
    }

    private void loopClean(File backupPath) {
        if (FileUtil.isFile(backupPath)) {
            return;
        }
        //
        Optional.ofNullable(backupPath.listFiles()).ifPresent(files1 -> {
            for (File file : files1) {
                this.loopClean(file);
            }
        });
        // 检查目录是否为空
        if (FileUtil.isDirEmpty(backupPath)) {
            FileUtil.del(backupPath);
        }
    }

    /**
     * 获取文件列表信息
     *
     * @param path 路径
     * @return 文件列表信息
     */
    private Map<String, File> listFiles(File path) {
        // 将所有的文件信息组装并签名
        List<File> files = FileUtil.loopFiles(path);
        List<JSONObject> collect = files.stream().map(file -> {
            //
            JSONObject item = new JSONObject();
            item.put("file", file);
            item.put("sha1", DigestUtil.sha1(file) + "-" + StringUtil.delStartPath(file, path, true));
            return item;
        }).collect(Collectors.toList());
        return CollStreamUtil.toMap(collect,
            jsonObject12 -> jsonObject12.getString("sha1"),
            jsonObject1 -> (File) jsonObject1.get("file"));
    }
}
