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

package i8n.web;

import io.voyager1.util.FileUtil;
import io.voyager1.util.CharsetUtil;
import io.voyager1.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.Lombok;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

/**
 * @since 2024/6/18
 */
public class CheckWebI18nKeyTest {

    private File rootFile;

    @BeforeEach
    public void beforeTest() {
        File file = new File("");
        String rootPath = file.getAbsolutePath();
        rootFile = new File(rootPath).getParentFile().getParentFile();
        rootFile = FileUtil.file(rootFile, "web-vue");
    }

    @Test
    public void test() {
        JSONObject zhCn = DiffWebI18nTest.loadJson(rootFile, "zh_cn");
        for (Map.Entry<String, Object> entry : zhCn.entrySet()) {
            Object value = entry.getValue();
            if (value.toString().length() == 1) {
                System.out.println(String.format("错误的值：%s=%s", entry.getKey(), value));
            }
        }
    }

    @Test
    public void test2() {
        Set<String> keys = new HashSet<>();
        DiffWebI18nTest.walkFile(rootFile, file1 -> {
            try {
                keys.addAll(matchFile(file1, DiffWebI18nTest.pattern));
            } catch (Exception e) {
                throw Lombok.sneakyThrow(e);
            }
        });
        JSONObject zhCn = DiffWebI18nTest.loadJson(rootFile, "zh_cn");
        JSONObject zhCn2 = DiffWebI18nTest.loadJson(rootFile, "zh_cn2");
        for (String key : keys) {
            if (zhCn.containsKey(key)) {
                continue;
            }
            if (zhCn2.containsKey(key)) {
                zhCn.put(key, zhCn2.get(key));
                continue;
            }
            System.err.println(String.format("缺少：%s", key));
        }
        //File file1 = FileUtil.file(rootFile, "/src/i18n/locales/zh_cn.json");
        //FileUtil.writeString(JSONArray.toJSONString(zhCn, JSONWriter.Feature.PrettyFormat), file1, StandardCharsets.UTF_8);
    }

    private Set<String> matchFile(File file, Pattern pattern) throws Exception {
        Charset charset = StandardCharsets.UTF_8;
        Set<String> keys = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), charset)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    String key = matcher.group(1);
                    int start = matcher.start(1);
                    String subPre = StrUtil.subPre(line, start);
                    if (StrUtil.endWithAny(subPre, "split('", "import('", "emit('", "onSubmit('", "mount('", "reject('", "component('", "recoverNet('", "executionRequest('", "commit('")) {
                        // 特殊方法
                        continue;
                    }
                    keys.add(key);
                }
            }
        }
        return keys;
    }

}
