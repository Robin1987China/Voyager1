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

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.ReflectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.JakartaServletUtil;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.ServerOpenApi;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.interceptor.NotLogin;
import io.voyager1.func.files.model.FileStorageModel;
import io.voyager1.func.files.service.FileStorageService;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.user.TriggerTokenLogServer;
import io.voyager1.system.ServerConfig;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 2023/3/17
 */
@RestController
@NotLogin
@Slf4j
public class FileStorageApiController extends BaseDownloadApiController {

    private final TriggerTokenLogServer triggerTokenLogServer;
    private final FileStorageService fileStorageService;
    private final ServerConfig serverConfig;

    public FileStorageApiController(TriggerTokenLogServer triggerTokenLogServer,
                                    FileStorageService fileStorageService,
                                    ServerConfig serverConfig) {
        this.triggerTokenLogServer = triggerTokenLogServer;
        this.fileStorageService = fileStorageService;
        this.serverConfig = serverConfig;
    }


    /**
     * 解析别名参数
     *
     * @param id      别名
     * @param token   token
     * @param sort    排序
     * @param request 请求
     * @return 数据
     */
    private FileStorageModel queryByAliasCode(String id, String token, String sort, HttpServletRequest request) {
        // 先验证 token 和 id 是否都存在
        {
            FileStorageModel data = new FileStorageModel();
            data.setAliasCode(id);
            data.setTriggerToken(token);
            Assert.state(fileStorageService.exists(data), "别名或者token错误,或者已经失效");
        }
        org.springframework.data.domain.Sort sortOrder = Opt.ofBlankAble(sort)
            .map(s -> io.voyager1.util.ConvertUtil.splitTrim(s, ","))
            .map(strings ->
                strings.stream()
                    .map(s -> {
                        List<String> list = io.voyager1.util.ConvertUtil.splitTrim(s, ":");
                        String field = (list == null || list.isEmpty() ? null : list.get(0));
                        String s1 = (1 < list.size() ? list.get(1) : null);
                        org.springframework.data.domain.Sort.Direction dir = "asc".equalsIgnoreCase(s1)
                            ? org.springframework.data.domain.Sort.Direction.ASC : org.springframework.data.domain.Sort.Direction.DESC;
                        return new org.springframework.data.domain.Sort.Order(dir, field);
                    })
                    .collect(Collectors.toList()))
            .map(org.springframework.data.domain.Sort::by)
            .orElse(org.springframework.data.domain.Sort.unsorted());
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        FileStorageModel where = new FileStorageModel();
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            String key = entry.getKey();
            if ((key != null && key.startsWith("filter_"))) {
                key = (key != null && key.startsWith("filter_") ? key.substring("filter_".length()) : key);
                ReflectUtil.setFieldValue(where, key, entry.getValue());
            }
        }
        where.setAliasCode(id);
        List<FileStorageModel> fileStorageModels = fileStorageService.queryList(where, 1, sortOrder);
        return (fileStorageModels == null || fileStorageModels.isEmpty() ? null : fileStorageModels.get(0));
    }

    @GetMapping(value = ServerOpenApi.FILE_STORAGE_DOWNLOAD, produces = MediaType.APPLICATION_JSON_VALUE)
    public void download(@PathVariable String id,
                         @PathVariable String token,
                         String sort,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        FileStorageModel storageModel = fileStorageService.getByKey(id);
        if (storageModel == null) {
            // 根据别名查询
            storageModel = this.queryByAliasCode(id, token, sort, request);
            Assert.notNull(storageModel, "没有对应数据");
        } else {
            Assert.state(java.util.Objects.equals(token, storageModel.getTriggerToken()), "token错误,或者已经失效");
        }
        //
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, fileStorageService.typeName());
        //
        Assert.notNull(userModel, "token错误,或者已经失效:-1");
        //
        File storageSavePath = serverConfig.fileStorageSavePath();
        File fileStorageFile = FileUtil.file(storageSavePath, storageModel.getPath());
        // 需要考虑文件名中存在非法字符
        String name = FileUtils.safeFileName(storageModel.getName(), storageModel.getExtName(), fileStorageFile.getName());
        //    解析断点续传相关信息
        long fileSize = FileUtil.size(fileStorageFile);
        long[] resolveRange = this.resolveRange(request, fileSize, storageModel.getId(), storageModel.getName(), response);
        this.download(fileStorageFile, fileSize, name, resolveRange, response);
    }
}
