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

package io.voyager1.service.script;

import io.voyager1.util.FileUtil;
import io.voyager1.util.PatternPool;
import io.voyager1.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.data.ScriptLibraryModel;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @since 2024/6/14
 */
@Service
@Slf4j
@Getter
public class ScriptLibraryService {
    /**
     * 脚本库目录
     */
    private final File globalScriptDir;
    private final Pattern pattern = PatternPool.get("G@\\(\"(.*?)\"\\)", Pattern.DOTALL);

    public ScriptLibraryService(Voyager1Application voyager1Application) {
        this.globalScriptDir = FileUtil.file(voyager1Application.getDataPath(), "global-script");
    }

    /**
     * 引用替换
     *
     * @param script 脚本
     * @return 替换后的脚本
     */
    public String referenceReplace(String script) {
        if ((script == null || script.isEmpty())) {
            return script;
        }
        Map<String, ScriptLibraryModel> map = new HashMap<>(3);
        Matcher matcher = pattern.matcher(script);
        StringBuffer modified = new StringBuffer();
        while (matcher.find()) {
            String tag = matcher.group(1);
            ScriptLibraryModel scriptLibraryModel = map.get(tag);
            if (scriptLibraryModel == null) {
                scriptLibraryModel = this.get(tag);
                if (scriptLibraryModel != null) {
                    map.put(tag, scriptLibraryModel);
                }
            }
            Assert.notNull(scriptLibraryModel, String.format("未找到脚本库信息:{}, 请检查引用标记是否正确或者脚本是否被删除", tag));
            matcher.appendReplacement(modified, scriptLibraryModel.getScript());
        }
        matcher.appendTail(modified);
        return modified.toString();
    }

    /**
     * 获取脚本库列表
     *
     * @return list
     */
    public List<ScriptLibraryModel> list() {
        List<File> list = FileUtil.loopFiles(globalScriptDir, 1, pathname -> java.util.Objects.equals("json", FileUtil.extName(pathname)));
        return list.stream()
            .map(file -> {
                try {
                    String string = FileUtil.readUtf8String(file);
                    ScriptLibraryModel scriptModel = JSONObject.parseObject(string, ScriptLibraryModel.class);
                    // 以文件名为脚本标签
                    scriptModel.setTag(FileUtil.mainName(file));
                    // 脚本内容不返回
                    scriptModel.setScript(null);
                    return scriptModel;
                } catch (Exception e) {
                    log.error("读取全局脚本文件失败", e);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 获取脚本库
     *
     * @param id id
     * @return model
     */
    public ScriptLibraryModel get(String id) {
        if ((id == null || id.isEmpty())) {
            return null;
        }
        File file = FileUtil.file(globalScriptDir, id + ".json");
        if (FileUtil.exist(file)) {
            String string = FileUtil.readUtf8String(file);
            ScriptLibraryModel scriptModel = JSONObject.parseObject(string, ScriptLibraryModel.class);
            // 以文件名为脚本标签
            scriptModel.setTag(FileUtil.mainName(file));
            return scriptModel;
        }
        return null;
    }
}
