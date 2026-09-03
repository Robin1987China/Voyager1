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

package io.voyager1.util;

import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 2023/2/9
 */
public class AntPathUtil {


    public static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    public static List<String> antPathMatcher(File rootFile, String match) {
        // 格式化，并处理 Slip 问题
        String matchStr = FileUtil.normalize("/" + match);
        List<String> paths = new ArrayList<>();
        //
        FileUtil.walkFiles(rootFile.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                return this.test(file);
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes exc) throws IOException {
                return this.test(dir);
            }

            private FileVisitResult test(Path path) {
                String subPath = FileUtil.subPath(FileUtil.getAbsolutePath(rootFile), path.toFile());
                subPath = FileUtil.normalize("/" + subPath);
                if (ANT_PATH_MATCHER.match(matchStr, subPath)) {
                    paths.add(subPath);
                    //return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return paths;
    }
}
