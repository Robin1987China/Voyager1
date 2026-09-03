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

package io.voyager1.controller.system;

import io.voyager1.util.CollStreamUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.ClassUtil;
import io.voyager1.util.IdUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.DigestUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.util.ContentType;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerConst;
import io.voyager1.common.ServerOpenApi;
import io.voyager1.common.UrlRedirectUtil;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.db.DbExtConfig;
import io.voyager1.core.db.TableName;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.BackupInfoModel;
import io.voyager1.model.enums.BackupStatusEnum;
import io.voyager1.model.enums.BackupTypeEnum;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.dblog.BackupInfoService;
import io.voyager1.service.system.SystemParametersServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据库备份 controller
 *
 * @since 2021-11-18
 */
@RestController
@Feature(cls = ClassFeature.SYSTEM_BACKUP)
@SystemPermission
@ConditionalOnProperty(prefix = "voyager1.db", name = "mode", havingValue = "H2", matchIfMissing = true)
@Slf4j
public class BackupInfoController extends BaseServerController {


    private final BackupInfoService backupInfoService;
    private final SystemParametersServer systemParametersServer;

    public BackupInfoController(BackupInfoService backupInfoService,
                                SystemParametersServer systemParametersServer) {
        this.backupInfoService = backupInfoService;
        this.systemParametersServer = systemParametersServer;
    }

    /**
     * 分页加载备份列表数据
     *
     * @return json
     */
    @PostMapping(value = "/system/backup/list")
    @Feature(method = MethodFeature.LIST)
    public Object loadBackupList(HttpServletRequest request) {
        // 查询数据库
        PageResultDto<BackupInfoModel> pageResult = backupInfoService.listPage(request);
        pageResult.each(backupInfoModel -> backupInfoModel.setFileExist(FileUtil.exist(backupInfoModel.getFilePath())));
        return ApiResult.success("获取成功", pageResult);
    }

    /**
     * 删除备份数据
     *
     * @param id 备份 ID
     * @return json
     */
    @PostMapping(value = "/system/backup/delete")
    @Feature(method = MethodFeature.DEL)
    @SystemPermission(superUser = true)
    public ApiResult<String> deleteBackup(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "数据 id 不能为空") String id) {
        // 删除备份信息
        backupInfoService.delByKey(id);
        return new ApiResult<>(200, "删除成功");
    }

    /**
     * 还原备份数据
     * 还原的时候不能异步了，只能等待备份还原成功或者失败
     *
     * @param id 备份 ID
     * @return json
     */
    @PostMapping(value = "/system/backup/restore")
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> restoreBackup(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "数据 id 不能为空") String id) {
        // 根据 id 查询备份信息
        BackupInfoModel backupInfoModel = backupInfoService.getByKey(id);
        Objects.requireNonNull(backupInfoModel, "备份数据不存在");

        // 检查备份文件是否存在
        String filePath = backupInfoModel.getFilePath();
        File file = new File(filePath);
        if (!FileUtil.exist(file)) {
            return new ApiResult<>(400, "备份文件不存在");
        }
        // 还原备份文件
        boolean flag = backupInfoService.restoreWithSql(filePath);
        if (flag) {
            // 还原备份数据成功之后需要修改当前备份信息的状态（因为备份的时候该备份信息状态是备份中）
            this.fuzzyUpdate(DigestUtil.sha1(file));
            return new ApiResult<>(200, "还原备份数据成功");
        }
        return new ApiResult<>(400, "还原备份数据失败");
    }

    /**
     * 模糊更新
     *
     * @param sha1 文件签名
     */
    private void fuzzyUpdate(String sha1) {
        BackupInfoModel where = new BackupInfoModel();
        where.setStatus(BackupStatusEnum.DEFAULT.getCode());
        List<BackupInfoModel> list = backupInfoService.listByBean(where);
        Optional.ofNullable(list).ifPresent(backupInfoModels -> {
            for (BackupInfoModel backupInfoModel : backupInfoModels) {
                String filePath = backupInfoModel.getFilePath();
                if (!FileUtil.exist(filePath)) {
                    continue;
                }
                File file = FileUtil.file(filePath);
                if (java.util.Objects.equals(DigestUtil.sha1(file), sha1)) {
                    // 是同一个文件
                    BackupInfoModel update = new BackupInfoModel();
                    update.setId(backupInfoModel.getId());
                    update.setFileSize(FileUtil.size(file));
                    update.setStatus(BackupStatusEnum.SUCCESS.getCode());
                    update.setSha1Sum(sha1);
                    int updateCount = backupInfoService.updateById(update);
                    log.debug("更新还原数据：{}", updateCount);
                }
            }
        });
    }

    /**
     * 创建备份任务
     *
     * @param map 参数 map.tableNameList 选中备份的表名称
     * @return json
     */
    @PostMapping(value = "/system/backup/create")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> backup(@RequestBody Map<String, Object> map) {
        List<String> tableNameList = JSON.parseArray(JSON.toJSONString(map.get("tableNameList")), String.class);
        backupInfoService.backupToSql(tableNameList);
        return new ApiResult<>(200, "操作成功，请稍后刷新查看备份状态");
    }

    /**
     * 导入备份数据
     *
     * @return json
     */
    @PostMapping(value = "/system/backup/upload")
    @Feature(method = MethodFeature.UPLOAD)
    @SystemPermission(superUser = true)
    public ApiResult<String> uploadBackupFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);
        Assert.state(StrUtil.containsAnyIgnoreCase(extName, "sql"), "不支持的文件类型：" + extName);
        String saveFileName = originalFilename;
        saveFileName = saveFileName.replace("\\", "_");
        // 存储目录
        File directory = FileUtil.file(new File(io.voyager1.system.ExtConfigBean.getPath(), "db"), DbExtConfig.BACKUP_DIRECTORY_NAME);
        // 生成唯一id
        String format = String.format("%s_%s", java.util.UUID.randomUUID().toString().replace("-", ""), saveFileName);
        format = (format == null ? null : (format.length() <= 40 ? format : format.substring(0, 40)));
        File backupSqlFile = FileUtil.file(directory, format + "." + extName);
        FileUtil.mkParentDirs(backupSqlFile);
        file.transferTo(backupSqlFile);
        // 记录到数据库
        String sha1Sum = DigestUtil.sha1(backupSqlFile);
        BackupInfoModel backupInfoModel = new BackupInfoModel();
        backupInfoModel.setSha1Sum(sha1Sum);
        boolean exists = backupInfoService.exists(backupInfoModel);
        if (exists) {
            FileUtil.del(backupSqlFile);
            return new ApiResult<>(400, "导入的数据已经存在啦");
        }

        backupInfoModel.setName(backupSqlFile.getName());
        backupInfoModel.setBackupType(BackupTypeEnum.IMPORT.getCode());
        backupInfoModel.setStatus(BackupStatusEnum.SUCCESS.getCode());
        backupInfoModel.setFileSize(FileUtil.size(backupSqlFile));

        backupInfoModel.setSha1Sum(sha1Sum);
        backupInfoModel.setFilePath(FileUtil.getAbsolutePath(backupSqlFile));
        backupInfoService.insert(backupInfoModel);

        return new ApiResult<>(200, "导入成功");
    }

    /**
     * 下载备份数据
     *
     * @param id 备份 ID
     */
    @GetMapping(value = "/system/backup/download")
    @Feature(method = MethodFeature.DOWNLOAD)
    public void downloadBackup(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "数据 id 不能为空") String id, HttpServletResponse response) {
        // 根据 id 查询备份信息
        BackupInfoModel backupInfoModel = backupInfoService.getByKey(id);
        Objects.requireNonNull(backupInfoModel, "备份数据不存在");

        // 检查备份文件是否存在
        File file = new File(backupInfoModel.getFilePath());
        if (!FileUtil.exist(file)) {
            //log.error("文件不存在，无法下载...backupId: {}", id);
            JakartaServletUtil.write(response, ApiResult.getString(404, "文件不存在，无法下载"), ContentType.JSON.toString());
            return;
        }

        // 下载文件
        JakartaServletUtil.write(response, file);
    }

    /**
     * 读取数据库表名称列表
     *
     * @return json
     */
    @PostMapping(value = "/system/backup/table-name-list")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<JSONObject>> loadTableNameList() {
        // 从数据库加载表名称列表
        List<String> tableNameList = backupInfoService.h2TableNameList();
        // 扫描程序，拿到表名称和别名

        Set<Class<?>> classes = ClassUtil.scanPackageByAnnotation("io.voyager1", TableName.class);
        Map<String, String> TABLE_NAME_MAP = CollStreamUtil.toMap(classes, aClass -> {
            TableName tableName = aClass.getAnnotation(TableName.class);
            return tableName.value();
        }, aClass -> {
            TableName tableName = aClass.getAnnotation(TableName.class);
            return I18nMessageUtil.get(tableName.nameKey());
        });

        List<JSONObject> list = tableNameList.stream().map(s -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("tableName", s);
            jsonObject.put("tableDesc", (TABLE_NAME_MAP.get(s) == null || TABLE_NAME_MAP.get(s).isEmpty() ? s : TABLE_NAME_MAP.get(s)));
            return jsonObject;
        }).collect(Collectors.toList());
        return new ApiResult<>(200, "", list);
    }


    /**
     * get a trigger url
     *
     * @param rest rest
     * @return json
     */
    @RequestMapping(value = "/system/backup/trigger-url", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Map<String, String>> getTriggerUrl(String rest, HttpServletRequest request) {
        String configToken = systemParametersServer.getConfig("backup-db-token", String.class);
        if ((configToken == null || configToken.isEmpty()) || (rest != null && !rest.isEmpty())) {
            configToken = java.util.UUID.randomUUID().toString();
            String desc = "备份数据触发器";
            systemParametersServer.upsert("backup-db-token", configToken, desc);
        }
        Map<String, String> map = this.getToken(configToken, request);
        String string = "重置成功";
        return ApiResult.success((rest == null || rest.isEmpty()) ? "ok" : string, map);
    }

    private Map<String, String> getToken(String token, HttpServletRequest request) {
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
        String url = ServerOpenApi.BACKUP_TRIGGER_URL.
            replace("{token}", token);
        String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
        Map<String, String> map = new HashMap<>(10);
        map.put("triggerUrl", FileUtil.normalize(triggerBuildUrl));
        map.put("token", token);
        return map;
    }
}
