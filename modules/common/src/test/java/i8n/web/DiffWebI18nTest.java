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
import io.voyager1.util.DigestUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import lombok.Lombok;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

/**
 * @since 2024/6/18
 */
@Tag("manual")
public class DiffWebI18nTest {

    public static final Pattern pattern = Pattern.compile("t\\('(.*?)'.*?\\)");

    @Test
    public void test() throws Exception {
        File file = new File("");
        String rootPath = file.getAbsolutePath();
        File rootFile = new File(rootPath).getParentFile().getParentFile();
        rootFile = FileUtil.file(rootFile, "web-vue");
        JSONObject zhCn = loadJson(rootFile, "zh_cn2");
        Map<String, String> cacheKey = new HashMap<>();
        this.generateWaitMap(zhCn, "", cacheKey);
        Collection<String> values = cacheKey.values();
        HashSet<String> set = new HashSet<>(values);
        Map<String, String> chinese = new HashMap<>();
        for (String s : set) {
            int len = 8;
            String md5 = DigestUtil.md5(s);
            while (true) {
                String newKey = String.format("i18n.%s", StrUtil.sub(md5, 0, len += 2));
                String existsKey = chinese.get(s);
                if (existsKey == null) {
                    chinese.put(s, newKey);
                    break;
                }
                if (java.util.Objects.equals(newKey, existsKey)) {
                    break;
                }
                Assertions.assertTrue(md5.length() >= len, "截取中文 md5 key 超范围：" + s);
            }
        }
        //
        Map<String, String> newKeyMap = new HashMap<>(chinese.size());
        cacheKey.forEach((key, value) -> {
            String newKey = chinese.get(value);
            newKeyMap.put(key, newKey);
        });
        walkFile(rootFile, file1 -> {
            try {
                matchFile(file1, newKeyMap);
            } catch (Exception e) {
                throw Lombok.sneakyThrow(e);
            }
        });
        //
        Map<String, String> newMap = new TreeMap<>();
        chinese.forEach((key, value) -> {
            newMap.put(value, key);
        });
        File file1 = FileUtil.file(rootFile, "/src/i18n/locales/zh_cn.json");
        FileUtil.writeString(JSONArray.toJSONString(newMap, JSONWriter.Feature.PrettyFormat), file1, StandardCharsets.UTF_8);
    }


    private void generateWaitMap(JSONObject jsonObject, String rootPath, Map<String, String> cache) throws Exception {
        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            String keyPath = rootPath.isEmpty() ? key : rootPath + "." + key;
            Object value = entry.getValue();
            if (value instanceof JSONObject) {
                generateWaitMap((JSONObject) value, keyPath, cache);
            } else if (value instanceof String) {
                cache.put(keyPath, (String) value);
            } else {
                System.err.println("不支持的数据格式：" + keyPath);
            }
        }
    }

    public static JSONObject loadJson(File rootFile, String tag) {
        File file1 = FileUtil.file(rootFile, "/src/i18n/locales/" + tag + ".json");
        if (!file1.exists()) {
            return new JSONObject();
        }
        return JSONObject.parseObject(FileUtil.readUtf8String(file1));
    }


    /**
     * 扫描指定目录下所有 java 文件（忽略 test、i18n-temp 目录）
     *
     * @param file     目录
     * @param consumer java 文件
     */
    public static void walkFile(File file, Consumer<File> consumer) {
        FileUtil.walkFiles(file, file1 -> {
            if (FileUtil.isDirectory(file1)) {
                return;
            }
            String path = FileUtil.getAbsolutePath(file1);
            String normalize = FileUtil.normalize(path);
            if (StrUtil.containsAny(normalize, "/node_modules/")) {
                return;
            }
            if (StrUtil.equalsAny(FileUtil.extName(file1), "ts", "js", "vue")) {
                consumer.accept(file1);
            }
        });
    }

    private void matchFile(File file, Map<String, String> newKeyMap) throws Exception {
        StringWriter writer = new StringWriter();
        boolean modified = false;
        Charset charset = StandardCharsets.UTF_8;
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), charset)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = DiffWebI18nTest.pattern.matcher(line);
                String newLine = line;
                while (matcher.find()) {
                    String key = matcher.group(1);
                    int start = matcher.start(1);
                    String subPre = StrUtil.subPre(line, start);
                    if (StrUtil.endWithAny(subPre, "split('", "import('", "emit('", "onSubmit('", "mount('", "reject('", "component('", "recoverNet('", "executionRequest('", "commit('")) {
                        // 特殊方法
                        continue;
                    }
                    if ((key != null && key.startsWith("i18n."))) {
                        // 忽略 i18n.{}
                        continue;
                    }
                    String newKey = newKeyMap.get(key);
                    if (newKey == null) {
                        throw new IllegalStateException("没有找到对应的 key：\n" + key + "\n" + line);
                    }
                    newLine = line.replace(String.format("'%s'", key), String.format("'%s'", newKey));
                    System.out.println(key + "  " + newKey);
                }
                //
                writer.write(newLine);
                if (!modified) {
                    modified = !java.util.Objects.equals(line, newLine);
                }
                writer.write(FileUtil.getLineSeparator());
            }
        }
        if (modified) {
            // 移动到原路径
            FileUtil.writeString(writer.toString(), file, charset);
        }
    }

    //@Test
    public void test2() {
        String line = "<span>{{ text === 'GLOBAL' ? $t('i18n.8DBEBAAE.1') : $t('pages.system.workspace-env.919267cc') }}</span>";
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            String key = matcher.group(1);
            System.out.println(key + "  ");
            int start = matcher.start(1);
            String subPre = StrUtil.subPre(line, start);
            if (StrUtil.endWithAny(subPre, "split('", "import('", "emit('", "onSubmit('", "mount('", "reject('", "component('", "recoverNet('", "executionRequest('", "commit('")) {
                // 特殊方法
                continue;
            }
            if ((key != null && key.startsWith("i18n."))) {
                // 忽略 i18n.{}
                continue;
            }

        }
    }
}
