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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * {@link FileUtil#move} 语义测试：目标为目录时移动进目录（保留原文件名）。
 *
 * @since 2026/9/7
 */
public class FileUtilMoveTest {

    @TempDir
    File tempDir;

    private File writeFile(String name, String content) throws Exception {
        File file = new File(tempDir, name);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    @Test
    public void testMoveIntoNonEmptyDirectory() throws Exception {
        // 非空目标目录：修复前会抛 DirectoryNotEmptyException
        File dir = new File(tempDir, "lib");
        Assertions.assertTrue(dir.mkdirs());
        writeFile("lib/existing.txt", "keep");

        File src = writeFile("upload.txt", "content");

        File moved = FileUtil.move(src, dir, true);

        Assertions.assertEquals(new File(dir, "upload.txt"), moved, "应移动进目录并保留原文件名");
        Assertions.assertFalse(src.exists(), "源文件应已移走");
        Assertions.assertEquals("content", new String(Files.readAllBytes(moved.toPath()), StandardCharsets.UTF_8));
        Assertions.assertTrue(new File(dir, "existing.txt").exists(), "目录内原有文件不受影响");
    }

    @Test
    public void testMoveToFileOverride() throws Exception {
        File src = writeFile("a.txt", "new");
        File target = writeFile("b.txt", "old");

        File moved = FileUtil.move(src, target, true);

        Assertions.assertEquals(target, moved);
        Assertions.assertEquals("new", new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void testMoveToNewFile() throws Exception {
        File src = writeFile("a.txt", "new");
        File target = new File(tempDir, "c.txt");

        File moved = FileUtil.move(src, target, false);

        Assertions.assertEquals(target, moved);
        Assertions.assertTrue(target.exists());
        Assertions.assertFalse(src.exists());
    }
}
