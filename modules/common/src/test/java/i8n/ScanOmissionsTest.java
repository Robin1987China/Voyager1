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

import lombok.Lombok;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @since 2024/6/14
 */
public class ScanOmissionsTest {

    @Test
    public void test() {
        File file = new File("");
        String rootPath = file.getAbsolutePath();
        File rootFile = new File(rootPath).getParentFile();

        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]");

        ExtractI18nTest.walkFile(rootFile, file1 -> {
            try {
                omissions(file1, pattern);
            } catch (Exception e) {
                throw Lombok.sneakyThrow(e);
            }
        });
    }

    private void omissions(File file, Pattern pattern) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (ExtractI18nTest.canIgnore(line)) {
                    continue;
                }
                //
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    System.err.println(line);
                }
            }
        }
    }
}
