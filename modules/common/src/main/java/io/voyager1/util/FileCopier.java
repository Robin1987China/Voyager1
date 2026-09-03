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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件拷贝器，"" {@code io.voyager1.util.FileCopier}。
 * <p>支持文件到文件、文件到目录、目录到目录以及目录内容拷贝。</p>
 */
public class FileCopier {

    private final File src;
    private final File dest;
    private boolean isOverride;
    private boolean isCopyAttributes;
    private boolean isCopyContentIfDir;
    private boolean isOnlyCopyFile;
    private FileFilter copyFilter;

    /**
     * 新建一个文件复制器。
     *
     * @param src  源文件
     * @param dest 目标文件
     * @return this
     */
    public static FileCopier create(File src, File dest) {
        return new FileCopier(src, dest);
    }

    /**
     * 新建一个文件复制器。
     *
     * @param srcPath  源路径
     * @param destPath 目标路径
     * @return this
     */
    public static FileCopier create(String srcPath, String destPath) {
        return new FileCopier(new File(srcPath), new File(destPath));
    }

    public FileCopier(File src, File dest) {
        this.src = src;
        this.dest = dest;
    }

    public boolean isOverride() {
        return isOverride;
    }

    public FileCopier setOverride(boolean isOverride) {
        this.isOverride = isOverride;
        return this;
    }

    public boolean isCopyAttributes() {
        return isCopyAttributes;
    }

    public FileCopier setCopyAttributes(boolean isCopyAttributes) {
        this.isCopyAttributes = isCopyAttributes;
        return this;
    }

    public boolean isCopyContentIfDir() {
        return isCopyContentIfDir;
    }

    public FileCopier setCopyContentIfDir(boolean isCopyContentIfDir) {
        this.isCopyContentIfDir = isCopyContentIfDir;
        return this;
    }

    public boolean isOnlyCopyFile() {
        return isOnlyCopyFile;
    }

    public FileCopier setOnlyCopyFile(boolean isOnlyCopyFile) {
        this.isOnlyCopyFile = isOnlyCopyFile;
        return this;
    }

    public FileFilter getCopyFilter() {
        return copyFilter;
    }

    public FileCopier setCopyFilter(FileFilter copyFilter) {
        this.copyFilter = copyFilter;
        return this;
    }

    /**
     * 执行拷贝。
     *
     * @return 拷贝后目标文件或目录
     */
    public File copy() {
        final File src = this.src;
        File dest = this.dest;
        if (src == null) {
            throw new IllegalArgumentException("Source File is null !");
        }
        if (!src.exists()) {
            throw new IllegalArgumentException("File not exist: " + src);
        }
        if (dest == null) {
            throw new IllegalArgumentException("Destination File or directory is null !");
        }
        if (equals(src, dest)) {
            throw new IllegalArgumentException("Source and dest are equal: " + src);
        }

        if (src.isDirectory()) {
            if (dest.exists() && !dest.isDirectory()) {
                throw new IllegalArgumentException("Src is a directory but dest is a file!");
            }
            File subTarget = isCopyContentIfDir ? dest : new File(dest, src.getName());
            internalCopyDirContent(src, subTarget);
        } else {
            dest = internalCopyFile(src, dest);
        }
        return dest;
    }

    private void internalCopyDirContent(File src, File dest) {
        if (copyFilter != null && !copyFilter.accept(src)) {
            return;
        }
        if (!dest.exists()) {
            dest.mkdirs();
        } else if (!dest.isDirectory()) {
            throw new IllegalArgumentException("Src [" + src.getPath() + "] is a directory but dest [" + dest.getPath() + "] is a file!");
        }

        String[] files = src.list();
        if (files != null) {
            for (String file : files) {
                File srcFile = new File(src, file);
                File destFile = this.isOnlyCopyFile ? dest : new File(dest, file);
                if (srcFile.isDirectory()) {
                    internalCopyDirContent(srcFile, destFile);
                } else {
                    internalCopyFile(srcFile, destFile);
                }
            }
        }
    }

    private File internalCopyFile(File src, File dest) {
        if (copyFilter != null && !copyFilter.accept(src)) {
            return src;
        }
        if (dest.exists()) {
            if (dest.isDirectory()) {
                dest = new File(dest, src.getName());
            }
            if (dest.exists() && !isOverride) {
                return src;
            }
        } else {
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
        }

        List<CopyOption> options = new ArrayList<>(2);
        if (isOverride) {
            options.add(StandardCopyOption.REPLACE_EXISTING);
        }
        if (isCopyAttributes) {
            options.add(StandardCopyOption.COPY_ATTRIBUTES);
        }
        try {
            Files.copy(src.toPath(), dest.toPath(), options.toArray(new CopyOption[0]));
        } catch (IOException e) {
            throw new RuntimeException("复制文件失败: " + src + " -> " + dest, e);
        }
        return dest;
    }

    private static boolean equals(File a, File b) {
        try {
            return a.getCanonicalPath().equals(b.getCanonicalPath());
        } catch (IOException e) {
            return a.getAbsolutePath().equals(b.getAbsolutePath());
        }
    }
}
