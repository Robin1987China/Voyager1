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

import io.voyager1.util.CollUtil;
import io.voyager1.util.CharsetUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseVoyager1Controller;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.data.AgentWhitelist;
import io.voyager1.service.WhitelistDirectoryService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @since 2019/4/16
 */
@RestController
@RequestMapping(value = "/system")
public class WhitelistDirectoryController extends BaseVoyager1Controller {

    private final WhitelistDirectoryService whitelistDirectoryService;

    public WhitelistDirectoryController(WhitelistDirectoryService whitelistDirectoryService) {
        this.whitelistDirectoryService = whitelistDirectoryService;
    }

    @RequestMapping(value = "whitelistDirectory_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<AgentWhitelist> whiteListDirectoryData() {
        AgentWhitelist agentWhitelist = whitelistDirectoryService.getWhitelist();
        return ApiResult.success("", agentWhitelist);
    }


    @PostMapping(value = "whitelistDirectory_submit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> whitelistDirectorySubmit(String project,


                                                         String allowEditSuffix) {
        List<String> list = AgentWhitelist.parseToList(project, true, "项目路径授权不能为空");
        //
        List<String> allowEditSuffixList = AgentWhitelist.parseToList(allowEditSuffix, "允许编辑的文件后缀不能为空");
        return save(list, allowEditSuffixList);
    }


    private ApiResult<String> save(List<String> projects,

                                     List<String> allowEditSuffixList) {
        List<String> projectArray;
        {
            projectArray = AgentWhitelist.covertToArray(projects, "项目路径授权不能位于Voyager1目录下");
            String error = findStartsWith(projectArray);
            Assert.isNull(error, "授权目录中不能存在包含关系：" + error);
        }

        //
        if ((allowEditSuffixList != null && !allowEditSuffixList.isEmpty())) {
            for (String s : allowEditSuffixList) {
                List<String> split = java.util.Arrays.asList(s.split(java.util.regex.Pattern.quote("@")));
                if (split.size() > 1) {
                    String last = (split == null || split.isEmpty() ? null : split.get(split.size() - 1));
                    try {
                        CharsetUtil.charset(last);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("配置的字符编码格式不合法：" + s);
                    }
                }
            }
        }

        AgentWhitelist agentWhitelist = whitelistDirectoryService.getWhitelist();

        agentWhitelist.setProject(projectArray);
        agentWhitelist.setAllowEditSuffix(allowEditSuffixList);
        whitelistDirectoryService.saveWhitelistDirectory(agentWhitelist);
        return new ApiResult<>(200, "保存成功");
    }

    /**
     * 检查授权包含关系
     *
     * @param jsonArray 要检查的对象
     * @return null 正常
     */
    private String findStartsWith(List<String> jsonArray) {
        return AgentWhitelist.findStartsWith(jsonArray);
    }
}
