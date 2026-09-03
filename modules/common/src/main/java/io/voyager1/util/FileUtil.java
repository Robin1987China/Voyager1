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

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件工具{@code io.voyager1.util.FileUtil} 的常用方法。
 */
public class FileUtil {

    /**
     * 空 File 常量
     */
    public static final File FILE = new File("");
    /**
     * jar 扩展名
     */
    public static final String JAR = "jar";
    /**
     * jar 文件扩展名
     */
    public static final String JAR_FILE_EXT = ".jar";
    /**
     * 路径分隔符
     */
    public static final String PATH = File.pathSeparator;
    /**
     * 文件分隔符
     */
    public static final String FILE_SEPARATOR = File.separator;
    /**
     * 路径分隔符
     */
    public static final String PATH_SEPARATOR = File.pathSeparator;

    // ---- 构造 ----

    public static File file(String path) {
        return new File(path);
    }

    public static File file(String parent, String... child) {
        File f = new File(parent);
        for (String c : child) {
            f = new File(f, c);
        }
        return f;
    }

    public static File file(File parent, String... child) {
        File f = parent;
        for (String c : child) {
            f = new File(f, c);
        }
        return f;
    }

    // ---- 基本信息 ----

    public static boolean exist(File file) {
        return file != null && file.exists();
    }

    public static boolean isFile(File file) {
        return file != null && file.isFile();
    }

    public static boolean isDirectory(File file) {
        return file != null && file.isDirectory();
    }

    public static String getAbsolutePath(File file) {
        return file == null ? null : file.getAbsolutePath();
    }

    public static String getName(File file) {
        return file == null ? null : file.getName();
    }

    public static long size(File file) {
        return file == null || !file.exists() ? 0 : file.length();
    }

    public static String readableFileSize(long size) {
        if (size <= 0) {
            return "0";
        }
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int idx = (int) (Math.log10(size) / Math.log10(1024));
        idx = Math.min(idx, units.length - 1);
        double v = size / Math.pow(1024, idx);
        return String.format("%.1f%s", v, units[idx]);
    }

    public static File getTmpDir() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    public static File getUserHomeDir() {
        return new File(System.getProperty("user.home"));
    }

    public static String getUserHomePath() {
        return System.getProperty("user.home");
    }

    public static String getLineSeparator() {
        return System.lineSeparator();
    }

    public static boolean isEmpty(File file) {
        return file == null || !file.exists() || file.length() == 0;
    }

    public static boolean isNotEmpty(File file) {
        return !isEmpty(file);
    }

    public static boolean isDirEmpty(File file) {
        if (file == null || !file.isDirectory()) {
            return true;
        }
        String[] list = file.list();
        return list == null || list.length == 0;
    }

    public static boolean isAbsolutePath(String path) {
        return path != null && Paths.get(path).isAbsolute();
    }

    public static boolean isSub(File parent, File sub) {
        if (parent == null || sub == null) {
            return false;
        }
        Path p = parent.toPath().normalize();
        Path s = sub.toPath().normalize();
        return s.startsWith(p);
    }

    public static String mainName(String path) {
        return path == null ? null : mainName(new File(path));
    }

    public static String mainName(File file) {
        if (file == null) {
            return null;
        }
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    public static String extName(File file) {
        if (file == null) {
            return null;
        }
        return extName(file.getName());
    }

    public static String extName(String name) {
        if (name == null) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1);
    }

    public static String normalize(String path) {
        if (path == null) {
            return null;
        }
        return Paths.get(path).normalize().toString();
    }

    public static File normalize(File file) {
        if (file == null) {
            return null;
        }
        return file.toPath().normalize().toFile();
    }

    public static File getParent(File file, int level) {
        File result = file;
        for (int i = 0; i < level && result != null; i++) {
            result = result.getParentFile();
        }
        return result;
    }

    public static java.util.Date lastModifiedTime(File file) {
        return file == null ? null : new java.util.Date(file.lastModified());
    }

    public static java.util.Date lastModifiedTime(String path) {
        return path == null ? null : new java.util.Date(new File(path).lastModified());
    }

    public static boolean equals(File f1, File f2) {
        if (f1 == null || f2 == null) {
            return false;
        }
        try {
            return f1.getCanonicalPath().equals(f2.getCanonicalPath());
        } catch (IOException e) {
            return f1.getAbsolutePath().equals(f2.getAbsolutePath());
        }
    }

    public static boolean pathEquals(String p1, String p2) {
        return p1 != null && p2 != null && Paths.get(p1).normalize().equals(Paths.get(p2).normalize());
    }

    public static boolean pathEquals(File f1, File f2) {
        if (f1 == null || f2 == null) {
            return false;
        }
        return f1.toPath().normalize().equals(f2.toPath().normalize());
    }

    public static boolean pathEquals(File f1, String p2) {
        return f1 != null && p2 != null && f1.toPath().normalize().equals(Paths.get(p2).normalize());
    }

    public static boolean pathEndsWith(File path, String suffix) {
        return path != null && path.getAbsolutePath().endsWith(suffix);
    }

    public static boolean pathEndsWith(String path, String suffix) {
        return path != null && suffix != null && path.endsWith(suffix);
    }

    public static String subPath(String root, File path) {
        return path == null ? null : subPath(root, path.getAbsolutePath());
    }

    public static String subPath(File root, File path) {
        if (root == null || path == null) {
            return null;
        }
        return subPath(root.getAbsolutePath(), path.getAbsolutePath());
    }

    public static String subPath(String root, String path) {
        if (root == null || path == null) {
            return null;
        }
        Path r = Paths.get(root).normalize();
        Path p = Paths.get(path).normalize();
        if (!p.startsWith(r)) {
            return null;
        }
        return r.relativize(p).toString();
    }

    // ---- 读写 ----

    public static String readString(File file, Charset charset) {
        try {
            return FileUtils.readFileToString(file, charset);
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + file, e);
        }
    }

    public static String readUtf(File file) {
        return readString(file, StandardCharsets.UTF_8);
    }

    public static String readUtfString(File file) {
        return readUtf(file);
    }

    public static List<String> readLines(File file, Charset charset) {
        try {
            return FileUtils.readLines(file, charset);
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + file, e);
        }
    }

    public static List<String> readLines(File file) {
        return readLines(file, StandardCharsets.UTF_8);
    }

    public static File writeString(File file, String content, Charset charset) {
        try {
            FileUtils.writeStringToFile(file, content, charset);
            return file;
        } catch (IOException e) {
            throw new RuntimeException("写入文件失败: " + file, e);
        }
    }

    public static File writeUtf(File file, String content) {
        return writeString(file, content, StandardCharsets.UTF_8);
    }

    public static File writeUtfString(File file, String content) {
        return writeUtf(file, content);
    }

    public static File writeUtf8String(String content, File file) {
        return writeUtf(file, content);
    }

    public static String readUtf8String(File file) {
        return readUtf(file);
    }

    /**
     * 根据文件名扩展名获取 MIME 类型
     */
    public static String getMimeType(java.nio.file.Path path) {
        return path == null ? null : getMimeType(path.getFileName() == null ? "" : path.getFileName().toString());
    }

    public static String getMimeType(String fileName) {
        if (fileName == null) {
            return null;
        }
        String ext = extName(fileName).toLowerCase();
        switch (ext) {
            case "html":
            case "htm":
                return "text/html";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            case "txt":
                return "text/plain";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "svg":
                return "image/svg+xml";
            case "ico":
                return "image/x-icon";
            case "pdf":
                return "application/pdf";
            case "zip":
                return "application/zip";
            case "jar":
                return "application/java-archive";
            case "csv":
                return "text/csv";
            case "yml":
            case "yaml":
                return "application/yaml";
            case "woff":
                return "font/woff";
            case "woff2":
                return "font/woff2";
            case "ttf":
                return "font/ttf";
            default:
                return "application/octet-stream";
        }
    }

    public static File writeLines(List<String> lines, File file, Charset charset) {
        try {
            FileUtils.writeLines(file, charset.name(), lines);
            return file;
        } catch (IOException e) {
            throw new RuntimeException("写入文件失败: " + file, e);
        }
    }

    public static File appendLines(List<String> lines, File file, Charset charset) {
        try {
            for (String line : lines) {
                FileUtils.writeStringToFile(file, line + System.lineSeparator(), charset, true);
            }
            return file;
        } catch (IOException e) {
            throw new RuntimeException("追加写入失败: " + file, e);
        }
    }

    public static java.io.BufferedReader getReader(File file, Charset charset) {
        try {
            return Files.newBufferedReader(file.toPath(), charset);
        } catch (IOException e) {
            throw new RuntimeException("打开 Reader 失败: " + file, e);
        }
    }

    public static java.io.BufferedReader getReader(File file) {
        return getReader(file, StandardCharsets.UTF_8);
    }

    public static java.io.BufferedWriter getWriter(File file, Charset charset) {
        try {
            return Files.newBufferedWriter(file.toPath(), charset);
        } catch (IOException e) {
            throw new RuntimeException("打开 Writer 失败: " + file, e);
        }
    }

    public static java.io.BufferedWriter getWriter(File file, Charset charset, boolean append) {
        try {
            return Files.newBufferedWriter(file.toPath(), charset,
                append ? new java.nio.file.OpenOption[]{java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND}
                    : new java.nio.file.OpenOption[]{java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING});
        } catch (IOException e) {
            throw new RuntimeException("打开 Writer 失败: " + file, e);
        }
    }

    public static String getMimeType(File file) {
        return file == null ? null : getMimeType(file.getName());
    }

    public static java.io.BufferedInputStream getInputStream(File file) {
        try {
            return new java.io.BufferedInputStream(Files.newInputStream(file.toPath()));
        } catch (IOException e) {
            throw new RuntimeException("打开 InputStream 失败: " + file, e);
        }
    }

    public static File writeFromStream(InputStream in, File file) {
        return writeFromStream(in, file, false);
    }

    public static File writeFromStream(InputStream in, File file, boolean append) {
        try {
            if (append) {
                try (java.io.OutputStream out = new java.io.FileOutputStream(file, true)) {
                    in.transferTo(out);
                }
            } else {
                FileUtils.copyToFile(in, file);
            }
            return file;
        } catch (IOException e) {
            throw new RuntimeException("写入文件失败: " + file, e);
        }
    }

    public static File saveJson(String content, File file) {
        return writeUtf(file, content);
    }

    public static String readJson(File file) {
        return readUtf(file);
    }

    public static String[] formatToArray(String content) {
        return content == null ? new String[0] : content.split(System.lineSeparator());
    }

    // ---- 目录/文件操作 ----

    public static boolean mkdir(File dir) {
        return dir != null && dir.mkdirs();
    }

    public static File mkParentDirs(File file) {
        if (file != null && file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        return file;
    }

    public static File touch(File file) {
        mkParentDirs(file);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            return file;
        } catch (IOException e) {
            throw new RuntimeException("创建文件失败: " + file, e);
        }
    }

    public static boolean del(File file) {
        return file != null && FileUtils.deleteQuietly(file);
    }

    public static boolean clean(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        try {
            FileUtils.cleanDirectory(dir);
            return true;
        } catch (IOException e) {
            throw new RuntimeException("清理目录失败: " + dir, e);
        }
    }

    public static void cleanEmpty(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                cleanEmpty(f);
                if (f.isDirectory() && isDirEmpty(f)) {
                    f.delete();
                }
            }
        }
    }

    public static File rename(File src, String newName) {
        File target = new File(src.getParentFile(), newName);
        src.renameTo(target);
        return target;
    }

    public static File move(File src, File target, boolean isOverride) {
        try {
            mkParentDirs(target);
            if (isOverride) {
                Files.move(src.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(src.toPath(), target.toPath());
            }
            return target;
        } catch (IOException e) {
            throw new RuntimeException("移动文件失败: " + src + " -> " + target, e);
        }
    }

    public static File moveContent(File src, File target) {
        clean(target);
        try {
            if (src.isDirectory()) {
                FileUtils.copyDirectory(src, target);
            } else {
                FileUtils.copyFile(src, target);
            }
            del(src);
            return target;
        } catch (IOException e) {
            throw new RuntimeException("移动内容失败", e);
        }
    }

    public static File copy(File src, File target, boolean isOverride) {
        try {
            mkParentDirs(target);
            if (src.isDirectory()) {
                FileUtils.copyDirectory(src, target);
            } else if (isOverride) {
                FileUtils.copyFile(src, target);
            } else if (!target.exists()) {
                FileUtils.copyFile(src, target);
            }
            return target;
        } catch (IOException e) {
            throw new RuntimeException("复制文件失败: " + src + " -> " + target, e);
        }
    }

    public static File copyFile(File src, File target) {
        try {
            FileUtils.copyFile(src, target);
            return target;
        } catch (IOException e) {
            throw new RuntimeException("复制文件失败: " + src, e);
        }
    }

    public static File copyFile(File src, File target, java.nio.file.CopyOption... options) {
        try {
            Files.copy(src.toPath(), target.toPath(), options);
            return target;
        } catch (IOException e) {
            throw new RuntimeException("复制文件失败: " + src, e);
        }
    }

    public static File copyContent(File src, File target) {
        return copy(src, target, true);
    }

    public static File createTempFile(String prefix, String suffix, File dir) {
        try {
            return File.createTempFile(prefix, suffix, dir);
        } catch (IOException e) {
            throw new RuntimeException("创建临时文件失败", e);
        }
    }

    // ---- 遍历 ----

    public static List<File> loopFiles(File dir) {
        List<File> result = new ArrayList<>();
        if (dir == null || !dir.isDirectory()) {
            return result;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return result;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                result.addAll(loopFiles(f));
            } else {
                result.add(f);
            }
        }
        return result;
    }

    public static List<File> walkFiles(File dir) {
        return loopFiles(dir);
    }

    public static List<File> walkFiles(File dir, java.util.function.Consumer<File> consumer) {
        List<File> result = new ArrayList<>();
        walkFilesRecursive(dir, consumer, result);
        return result;
    }

    private static void walkFilesRecursive(File file, java.util.function.Consumer<File> consumer, List<File> result) {
        if (file == null || !file.exists()) {
            return;
        }
        if (consumer != null) {
            consumer.accept(file);
        }
        result.add(file);
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    walkFilesRecursive(child, consumer, result);
                }
            }
        }
    }

    public static java.nio.file.Path walkFiles(java.nio.file.Path start, java.nio.file.FileVisitor<? super java.nio.file.Path> visitor) {
        try {
            return Files.walkFileTree(start, visitor);
        } catch (IOException e) {
            throw new RuntimeException("遍历文件失败: " + start, e);
        }
    }

    public static List<String> listFileNames(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return new ArrayList<>();
        }
        String[] names = dir.list();
        return names == null ? new ArrayList<>() : new ArrayList<>(java.util.Arrays.asList(names));
    }

    public static String getAdminDirectoryName() {
        return "admin";
    }

    public static boolean checkSlip(File root, File file) {
        return isSub(root, file);
    }

    // ---- String / Path 重载 ----

    public static boolean mkdir(String dir) {
        return dir != null && new File(dir).mkdirs();
    }

    public static boolean isFile(String path) {
        return path != null && new File(path).isFile();
    }

    public static boolean isDirectory(String path) {
        return path != null && new File(path).isDirectory();
    }

    public static boolean exist(String path) {
        return path != null && new File(path).exists();
    }

    public static boolean del(String path) {
        return path != null && FileUtils.deleteQuietly(new File(path));
    }

    public static boolean del(java.nio.file.Path path) {
        return path != null && FileUtils.deleteQuietly(path.toFile());
    }

    public static String getAbsolutePath(String path) {
        return path == null ? null : new File(path).getAbsolutePath();
    }

    public static String getName(String path) {
        return path == null ? null : new File(path).getName();
    }

    public static String getParent(String path, int level) {
        File f = path == null ? null : new File(path);
        for (int i = 0; i < level && f != null; i++) {
            f = f.getParentFile();
        }
        return f == null ? null : f.getAbsolutePath();
    }

    public static String readUtf8String(String path) {
        return readUtf(new File(path));
    }

    public static File writeString(String content, File file, Charset charset) {
        return writeString(file, content, charset);
    }

    public static File writeString(String content, String path, Charset charset) {
        return writeString(new File(path), content, charset);
    }

    public static String readString(java.net.URL url, Charset charset) {
        try (java.io.InputStream in = url.openStream()) {
            return new String(in.readAllBytes(), charset);
        } catch (java.io.IOException e) {
            throw new RuntimeException("读取 URL 失败: " + url, e);
        }
    }

    public static File createTempFile(String prefix, String suffix, File dir, boolean deleteOnExit) {
        try {
            File file = File.createTempFile(prefix, suffix, dir);
            if (deleteOnExit) {
                file.deleteOnExit();
            }
            return file;
        } catch (java.io.IOException e) {
            throw new RuntimeException("创建临时文件失败", e);
        }
    }

    public static List<File> loopFiles(File dir, int maxDepth, java.io.FileFilter filter) {
        List<File> result = new ArrayList<>();
        if (dir == null || !dir.isDirectory() || maxDepth < 0) {
            return result;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return result;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                if (maxDepth > 0) {
                    result.addAll(loopFiles(f, maxDepth - 1, filter));
                }
            } else if (filter == null || filter.accept(f)) {
                result.add(f);
            }
        }
        return result;
    }

    public static File moveContent(File src, File target, boolean isOverride) {
        if (isOverride) {
            clean(target);
        }
        return moveContent(src, target);
    }

    public static void readLines(File file, Charset charset, LineHandler handler) {
        for (String line : readLines(file, charset)) {
            handler.handle(line);
        }
    }

    public static List<File> loopFiles(File dir, java.io.FileFilter filter) {
        return loopFiles(dir, Integer.MAX_VALUE, filter);
    }

    public static List<File> loopFiles(String dir, java.io.FileFilter filter) {
        return loopFiles(new File(dir), filter);
    }

    public static List<File> loopFiles(String dir) {
        return loopFiles(new File(dir));
    }

    public static File copyContent(File src, File target, boolean isOverride) {
        if (isOverride) {
            clean(target);
        }
        return copyContent(src, target);
    }

    public static File rename(File src, String newName, boolean isOverride) {
        File target = new File(src.getParentFile(), newName);
        if (isOverride && target.exists()) {
            del(target);
        }
        src.renameTo(target);
        return target;
    }

    public static String readableFileSize(File file) {
        return readableFileSize(size(file));
    }

    public static File unCompress(File archive, File targetDir) {
        try {
            org.apache.commons.compress.archivers.ArchiveStreamFactory factory = new org.apache.commons.compress.archivers.ArchiveStreamFactory();
            String name = archive.getName().toLowerCase();
            try (InputStream in = Files.newInputStream(archive.toPath())) {
                if (name.endsWith(".zip")) {
                    org.apache.commons.compress.archivers.zip.ZipArchiveInputStream zis = new org.apache.commons.compress.archivers.zip.ZipArchiveInputStream(in);
                    org.apache.commons.compress.archivers.ArchiveEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (!zis.canReadEntryData(entry)) {
                            continue;
                        }
                        File out = new File(targetDir, entry.getName());
                        if (entry.isDirectory()) {
                            out.mkdirs();
                        } else {
                            FileUtils.copyToFile(zis, out);
                        }
                    }
                } else {
                    throw new RuntimeException("不支持的压缩格式: " + name);
                }
            }
            return targetDir;
        } catch (Exception e) {
            throw new RuntimeException("解压失败: " + archive, e);
        }
    }
}
