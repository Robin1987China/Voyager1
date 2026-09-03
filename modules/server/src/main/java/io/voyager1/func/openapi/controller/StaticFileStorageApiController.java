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

package io.voyager1.func.openapi.controller;

import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.ServerOpenApi;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.interceptor.NotLogin;
import io.voyager1.func.files.model.StaticFileStorageModel;
import io.voyager1.func.files.service.StaticFileStorageService;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.user.TriggerTokenLogServer;
import io.voyager1.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @since 2023/12/28
 */
@RestController
@NotLogin
@Slf4j
public class StaticFileStorageApiController extends BaseDownloadApiController {

    private final TriggerTokenLogServer triggerTokenLogServer;
    private final StaticFileStorageService staticFileStorageService;

    public StaticFileStorageApiController(TriggerTokenLogServer triggerTokenLogServer,
                                          StaticFileStorageService staticFileStorageService) {
        this.triggerTokenLogServer = triggerTokenLogServer;
        this.staticFileStorageService = staticFileStorageService;
    }


    @GetMapping(value = ServerOpenApi.STATIC_FILE_STORAGE_DOWNLOAD, produces = MediaType.APPLICATION_JSON_VALUE)
    public void download(@PathVariable String id,
                         @PathVariable String token,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        StaticFileStorageModel storageModel = staticFileStorageService.getByKey(id);
        Assert.notNull(storageModel, "文件不存在");

        Assert.state(java.util.Objects.equals(token, storageModel.getTriggerToken()), "token错误,或者已经失效");
        //
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, staticFileStorageService.typeName());
        //
        Assert.notNull(userModel, "token错误,或者已经失效:-1");
        File file = FileUtil.file(storageModel.getAbsolutePath());
        // 需要考虑文件名中存在非法字符
        String name = FileUtils.safeFileName(storageModel.getName(), storageModel.getExtName(), file.getName());
        //    解析断点续传相关信息
        long fileSize = FileUtil.size(file);
        long[] resolveRange = this.resolveRange(request, fileSize, storageModel.getId(), storageModel.getName(), response);
        this.download(file, fileSize, name, resolveRange, response);
    }
}
