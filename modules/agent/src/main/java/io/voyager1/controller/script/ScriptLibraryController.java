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

package io.voyager1.controller.script;

import io.voyager1.util.FileUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseAgentController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.model.data.ScriptLibraryModel;
import io.voyager1.service.script.ScriptLibraryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

/**
 * @since 2024/6/1
 */
@RestController
@RequestMapping(value = "/script-library")
@Slf4j
public class ScriptLibraryController extends BaseAgentController {

    private final ScriptLibraryService scriptLibraryService;

    public ScriptLibraryController(ScriptLibraryService scriptLibraryService) {
        this.scriptLibraryService = scriptLibraryService;
    }

    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<ScriptLibraryModel>> list() {
        List<ScriptLibraryModel> modelList = scriptLibraryService.list();
        return ApiResult.success("", modelList);
    }

    @RequestMapping(value = "get", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<ScriptLibraryModel> get(@ValidatorItem String id) {
        ScriptLibraryModel scriptModel = scriptLibraryService.get(id);
        if (scriptModel != null) {
            return ApiResult.success("", scriptModel);
        }
        return ApiResult.fail("找不到对应的脚本");
    }

    @RequestMapping(value = "save", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> save(@ValidatorItem String id,
                                     @ValidatorItem(msg = "脚本内容不能为空") String script,
                                     String description,
                                     String version) {
        File file = FileUtil.file(scriptLibraryService.getGlobalScriptDir(), id + ".json");
        ScriptLibraryModel scriptModel = new ScriptLibraryModel();
        scriptModel.setId(id);
        scriptModel.setScript(script);
        scriptModel.setDescription(description);
        scriptModel.setVersion(version);
        FileUtil.writeUtf8String(JSONObject.toJSONString(scriptModel), file);
        return ApiResult.success("保存成功");
    }

    @RequestMapping(value = "del", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> del(@ValidatorItem String id) {
        File file = FileUtil.file(scriptLibraryService.getGlobalScriptDir(), id + ".json");
        FileUtil.del(file);
        return ApiResult.success("删除成功");
    }
}
