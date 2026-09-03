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

package io.voyager1.controller.node.manage.file;


import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.NodeDataPermission;
import io.voyager1.service.node.ProjectInfoCacheService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @since 2022/5/11
 */
@RestController
@RequestMapping(value = "/node/manage/file/")
@Feature(cls = ClassFeature.PROJECT_FILE)
@NodeDataPermission(cls = ProjectInfoCacheService.class)
public class ProjectFileBackupController extends BaseServerController {

    /**
     * 查询备份列表
     *
     * @param id 项目ID
     * @return list
     */
    @RequestMapping(value = "list-backup", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Object> listBackup(String id, HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.MANAGE_FILE_BACKUP_LIST_BACKUP);
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
    public ApiResult<Object> backupItemFiles(String id, String path, @ValidatorItem String backupId, HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.MANAGE_FILE_BACKUP_LIST_ITEM_FILES);
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
    public void download(String id, @ValidatorItem String backupId, @ValidatorItem String filename, String levelName,
                         HttpServletResponse response,
                         HttpServletRequest request) {
        NodeForward.requestDownload(getNode(), request, response, NodeUrl.MANAGE_FILE_BACKUP_DOWNLOAD);
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
    public ApiResult<Object> deleteFile(String id, @ValidatorItem String backupId, @ValidatorItem String filename, String levelName, HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.MANAGE_FILE_BACKUP_DELETE);
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
    public ApiResult<Object> recoverFile(String id, @ValidatorItem String backupId, String type, String filename, String levelName, HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.MANAGE_FILE_BACKUP_RECOVER);
    }
}
