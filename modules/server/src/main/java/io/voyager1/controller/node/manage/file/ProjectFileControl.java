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
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.controller.outgiving.OutGivingWhitelistService;
import io.voyager1.model.data.ServerWhitelist;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.NodeDataPermission;
import io.voyager1.service.node.ProjectInfoCacheService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 文件管理
 *
 */
@RestController
@RequestMapping(value = "/node/manage/file/")
@Feature(cls = ClassFeature.PROJECT_FILE)
@NodeDataPermission(cls = ProjectInfoCacheService.class)
public class ProjectFileControl extends BaseServerController {
    private final OutGivingWhitelistService outGivingWhitelistService;

    public ProjectFileControl(OutGivingWhitelistService outGivingWhitelistService) {
        this.outGivingWhitelistService = outGivingWhitelistService;
    }


    /**
     * 列出目录下的文件
     *
     * @return json
     */
    @RequestMapping(value = "getFileList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.LIST)
    public ApiResult<Object> getFileList(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_GetFileList);
    }

    /**
     * 上传文件
     *
     * @return json
     */
    @RequestMapping(value = "upload-sharding", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.UPLOAD, log = false)
    public ApiResult<String> uploadSharding(String sliceId, MultipartHttpServletRequest request) {
        Assert.state(BaseServerController.SHARDING_IDS.containsKey(sliceId), "不合法的分片id");
        return NodeForward.requestMultipart(getNode(), request, NodeUrl.Manage_File_Upload_Sharding);
    }

    /**
     * 合并分片
     *
     * @return json
     */
    @RequestMapping(value = "sharding-merge", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.UPLOAD)
    public ApiResult<String> shardingMerge(String sliceId, HttpServletRequest request) {
        Assert.state(BaseServerController.SHARDING_IDS.containsKey(sliceId), "不合法的分片id");
        ApiResult<String> message = NodeForward.request(getNode(), request, NodeUrl.Manage_File_Sharding_Merge);
        // 判断-删除分片id
        BaseServerController.SHARDING_IDS.remove(sliceId);
        return message;
    }

    /**
     * 下载文件
     */
    @RequestMapping(value = "download", method = RequestMethod.GET)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.DOWNLOAD)
    public void download(HttpServletRequest request, HttpServletResponse response) {
        NodeForward.requestDownload(getNode(), request, response, NodeUrl.Manage_File_Download);
    }

    /**
     * 删除文件
     *
     * @return json
     */
    @RequestMapping(value = "deleteFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.DEL)
    public ApiResult<Object> deleteFile(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_DeleteFile);
    }


    /**
     * 更新配置文件
     *
     * @return json
     */
    @PostMapping(value = "update_config_file", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.EDIT)
    public ApiResult<Object> updateConfigFile(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_UpdateConfigFile);
    }

    /**
     * 删除文件
     *
     * @return json
     */
    @GetMapping(value = "read_file", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.LIST)
    public ApiResult<Object> readFile(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_ReadFile);
    }

    /**
     * 下载远程文件
     *
     * @return json
     */
    @GetMapping(value = "remote_download", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.REMOTE_DOWNLOAD)
    public ApiResult<Object> remoteDownload(@ValidatorItem String url, HttpServletRequest request) {
        // 验证远程 地址
        ServerWhitelist whitelist = outGivingWhitelistService.getServerWhitelistData(request);
        whitelist.checkAllowRemoteDownloadHost(url);
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_Remote_Download);
    }

    /**
     * 创建文件
     *
     * @return json
     */
    @GetMapping(value = "new_file_folder", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.EDIT)
    public ApiResult<Object> newFileFolder(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.MANAGE_FILE_NEW_FILE_FOLDER);
    }


    /**
     * 修改文件名
     *
     * @return json
     */
    @GetMapping(value = "rename_file_folder", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.EDIT)
    public ApiResult<Object> renameFileFolder(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.MANAGE_FILE_RENAME_FILE_FOLDER);
    }

    /**
     * 复制文件
     *
     * @return json
     */
    @PostMapping(value = "copy", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.EDIT)
    public ApiResult<Object> copy(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.MANAGE_FILE_COPY);
    }

    /**
     * 压缩文件
     *
     * @return json
     */
    @PostMapping(value = "compress", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.EDIT)
    public ApiResult<Object> compress(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.MANAGE_FILE_COMPRESS);
    }

}
