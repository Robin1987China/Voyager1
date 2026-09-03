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

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * 文件移动操作的 FileVisitor 实现，"" {@code io.voyager1.util.MoveVisitor}。
 * <p>用于递归遍历移动目录和文件，会自动创建目标目录中不存在的上级目录。</p>
 */
public class MoveVisitor extends SimpleFileVisitor<Path> {

    private final Path source;
    private final Path target;
    private boolean isTargetCreated;
    private final CopyOption[] copyOptions;

    /**
     * 构造。
     *
     * @param source      源 Path
     * @param target      目标 Path
     * @param copyOptions 拷贝（移动）选项
     */
    public MoveVisitor(Path source, Path target, CopyOption... copyOptions) {
        if (target != null && Files.exists(target) && !Files.isDirectory(target)) {
            throw new IllegalArgumentException("Target must be a directory");
        }
        this.source = source;
        this.target = target;
        this.copyOptions = copyOptions;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        initTarget();
        Path targetDir = target.resolve(source.relativize(dir));
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        } else if (!Files.isDirectory(targetDir)) {
            throw new FileAlreadyExistsException(targetDir.toString());
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        initTarget();
        Files.move(file, target.resolve(source.relativize(file)), copyOptions);
        return FileVisitResult.CONTINUE;
    }

    private void initTarget() throws IOException {
        if (!this.isTargetCreated) {
            Files.createDirectories(this.target);
            this.isTargetCreated = true;
        }
    }
}
