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

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.core.api.ApiResult;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseAgentController;
import io.voyager1.common.commander.CommandOpResult;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.configuration.AgentConfig;
import io.voyager1.util.CompressionFileUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * @since 2023/3/28
 */
@RestController
@RequestMapping(value = "/manage/file2/")
@Slf4j
public class FileManageController extends BaseAgentController {

    private final AgentConfig agentConfig;

    public FileManageController(AgentConfig agentConfig) {
        this.agentConfig = agentConfig;
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
                                                       @ValidatorItem(msg = "文件目录缺失") String path,
                                                       Integer stripComponents,
                                                       String sliceId,
                                                       Integer totalSlice,
                                                       String fileSumMd5) throws Exception {
        String tempPathName = agentConfig.getFixedTempPathName();
        File successFile = this.shardingTryMerge(tempPathName, sliceId, totalSlice, fileSumMd5);
        File lib = FileUtil.file(path);
        // 处理上传文件
        if ("unzip".equals(type)) {
            // 解压
            try {
                int stripComponentsValue = ConvertUtil.toInt(stripComponents, 0);
                CompressionFileUtil.unCompress(successFile, lib, stripComponentsValue);
            } finally {
                if (!FileUtil.del(successFile)) {
                    log.error("删除文件失败：" + successFile.getPath());
                }
            }
        } else {
            // 移动文件到对应目录
            FileUtil.mkdir(lib);
            FileUtil.move(successFile, lib, true);
        }
        return ApiResult.success("上传成功");
    }
}
