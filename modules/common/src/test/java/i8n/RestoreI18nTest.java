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

package i8n;

import io.voyager1.util.FileUtil;
import io.voyager1.util.CharsetUtil;
import io.voyager1.util.StrUtil;
import lombok.Lombok;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

/**
 * 还原 i18n
 *
 * @since 2024/6/13
 */
public class RestoreI18nTest {
    private final Charset charset = StandardCharsets.UTF_8;
    private File rootFile;
    private File zhPropertiesFile;

    @BeforeEach
    public void before() throws Exception {
        File file = new File("");
        String rootPath = file.getAbsolutePath();
        rootFile = new File(rootPath).getParentFile();
        //
        zhPropertiesFile = FileUtil.file(rootFile, "common/src/main/resources/i18n/messages_zh_CN.properties");
    }

    @Test
    public void test() throws IOException {
        Properties zhProperties = new Properties();
        try (BufferedReader inputStream = FileUtil.getReader(zhPropertiesFile, charset)) {
            zhProperties.load(inputStream);
        }
        // 提取中文
        ExtractI18nTest.walkFile(rootFile, file1 -> {
            try {
                for (Pattern pattern : ExtractI18nTest.messageKeyPatterns) {
                    restoreChineseInFile(file1, pattern, zhProperties);
                }
            } catch (Exception e) {
                throw Lombok.sneakyThrow(e);
            }
        });
    }

    private void restoreChineseInFile(File file, Pattern pattern, Properties zhProperties) throws Exception {
        StringWriter writer = new StringWriter();
        boolean modified = false;
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), charset)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (ExtractI18nTest.canIgnore(line)) {
                    writer.write(line);
                } else {
                    // 将 i18n key 替换为中文
                    StringBuffer modifiedLine = new StringBuffer();
                    Matcher matcher = pattern.matcher(line);
                    if (StrUtil.containsAny(line, ExtractI18nTest.Voyager1Annotation)) {
                        if (matcher.find()) {
                            String key = matcher.group(1);
                            if (ExtractI18nTest.needIgnoreCase(key, line)) {
                                String chineseText = (String) zhProperties.get(key);
                                if (chineseText == null) {
                                    throw new IllegalArgumentException("找不到对应的中文:" + key);
                                }
                                // 完整替换
                                modifiedLine.append(line.replace(String.format("\"%s\"", key), String.format("\"%s\"", chineseText)));
                            } else {
                                modifiedLine.append(line);
                            }
                        } else {
                            modifiedLine.append(line);
                        }
                    } else {
                        while (matcher.find()) {
                            String key = matcher.group(1);
                            if (!ExtractI18nTest.needIgnoreCase(key, line)) {
                                continue;
                            }
                            String chineseText = (String) zhProperties.get(key);
                            if (chineseText == null) {
                                throw new IllegalArgumentException("找不到对应的中文:" + key);
                            }
                            // 正则关键词替换
                            matcher.appendReplacement(modifiedLine, String.format("\"%s\"", chineseText));
                        }
                        matcher.appendTail(modifiedLine);
                    }
                    String lineString = modifiedLine.toString();
                    writer.write(lineString);
                    if (!modified) {
                        modified = !java.util.Objects.equals(line, lineString);
                    }
                }
                writer.write(FileUtil.getLineSeparator());
            }
        }
        if (modified) {
            // 移动到原路径
            FileUtil.writeString(writer.toString(), file, charset);
        }
    }
}
