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

package io.voyager1.system;

import io.voyager1.Voyager1Application;
import io.voyager1.common.Const;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.common.Voyager1Manifest;
import io.voyager1.util.FileUtil;
import io.voyager1.util.FileUtils;
import io.voyager1.util.Opt;
import io.voyager1.util.SystemUtil;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.function.Function;

/**
 * 外部资源配置
 */
@Slf4j
public class ExtConfigBean {
    /**
     * 控制台日志编码
     */
    private static Charset consoleLogCharset;

    public static void setConsoleLogCharset(Charset consoleLogCharset) {
        ExtConfigBean.consoleLogCharset = consoleLogCharset;
    }

    public static Charset getConsoleLogCharset() {
        return (consoleLogCharset != null ? consoleLogCharset : java.nio.charset.Charset.defaultCharset());
    }

    /**
     * 项目运行存储路径
     */
    private static String path;

    public static void setPath(String path) {
        ExtConfigBean.path = path;
    }

    /**
     * 动态获取外部配置文件的 resource
     *
     * @return File
     */
    public static Resource getResource() {
        String property = SpringContextHolder.getApplicationContext().getEnvironment().getProperty("spring.config.location");
        Resource configResource = Opt.ofBlankAble(property)
            .map(FileSystemResource::new)
            .flatMap((Function<Resource, Opt<Resource>>) resource -> resource.exists() ? Opt.of(resource) : Opt.empty())
            .orElseGet(() -> {
                ClassPathResource classPathResource = new ClassPathResource(Const.FILE_NAME);
                return classPathResource.exists() ? classPathResource : new ClassPathResource("/config_default/" + Const.FILE_NAME);
            });
        Assert.state(configResource.exists(), "均未找到配置文件");
        return configResource;
    }

    /**
     * 判断是否存在对应的配置资源
     *
     * @param name 名称
     * @return true 存在
     */
    public static boolean existConfigResource(String name) {
        File configResourceFile = getConfigResourceFile(name);
        if (configResourceFile == null) {
            return false;
        }
        return FileUtil.exist(configResourceFile) && FileUtil.isFile(configResourceFile);
    }

    /**
     * 获取对应的配置资源 file 对象
     *
     * @param name 名称
     * @return true 存在
     */
    public static File getConfigResourceFile(String name) {
        FileUtils.checkSlip(name);
        File resourceDir = getConfigResourceDir();
        return Opt.ofBlankAble(resourceDir).map(file -> FileUtil.file(file, name)).orElse(null);
    }

    /**
     * 获取对应的配置资源目录 对象
     */
    public static File getConfigResourceDir() {
        String property = SpringContextHolder.getApplicationContext().getEnvironment().getProperty("spring.config.location");
        return Opt.ofBlankAble(property).map(s -> {
            File file = FileUtil.file(s);
            return FileUtil.getParent(file, 1);
        }).orElse(null);
    }

    /**
     * 动态获取外部配置文件的 resource
     *
     * @return File
     */
    public static InputStream getConfigResourceInputStream(String name) {
        InputStream inputStream = tryGetConfigResourceInputStream(name);
        Assert.notNull(inputStream, "未找到配置文件:" + name);
        return inputStream;
    }

    /**
     * 动态获取外部配置文件的 resource
     *
     * @return File
     */
    public static InputStream tryGetConfigResourceInputStream(String name) {
        FileUtils.checkSlip(name);
        File configResourceDir = getConfigResourceDir();
        return Opt.ofBlankAble(configResourceDir)
            .map((Function<File, InputStream>) configDir -> {
                File file = FileUtil.file(configDir, name);
                if (FileUtil.isFile(file)) {
                    return FileUtil.getInputStream(file);
                }
                return null;
            })
            .orElseGet(() -> {
                log.debug("外置配置不存在或者未配置：{},使用默认配置", name);
                return tryGetDefaultConfigResourceInputStream(name);
            });
    }

    /**
     * 动态获取外部配置文件的 resource
     *
     * @return File
     */
    public static InputStream tryGetDefaultConfigResourceInputStream(String name) {
        String normalize = FileUtil.normalize("/config_default/" + name);
        ClassPathResource classPathResource = new ClassPathResource(normalize);
        if (!classPathResource.exists()) {
            return null;
        }
        try {
            return classPathResource.getInputStream();
        } catch (IOException e) {
            throw Lombok.sneakyThrow(e);
        }
    }

    /**
     * 动态获取外部配置文件的 resource
     *
     * @return File
     */
    public static InputStream getDefaultConfigResourceInputStream(String name) {
        InputStream inputStream = tryGetDefaultConfigResourceInputStream(name);
        Assert.notNull(inputStream, name + "配置文件不存在");
        return inputStream;
    }

    /**
     * 模糊匹配获取配置文件资源
     *
     * @param matchStr 匹配关键词
     * @return 资源
     */
    public static Resource[] getConfigResources(String matchStr) {
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
        File configResourceDir = getConfigResourceDir();
        return Opt.ofBlankAble(configResourceDir)
            .map(file -> {
                try {
                    String format = String.format("%s%s/%s", ResourceUtils.FILE_URL_PREFIX, file.getAbsolutePath(), matchStr);
                    Resource[] resources = pathMatchingResourcePatternResolver.getResources(format);
                    if ((resources == null || resources.length == 0)) {
                        log.warn("配置文件不存在 {}", format);
                        return null;
                    }
                    return resources;
                } catch (IOException e) {
                    throw Lombok.sneakyThrow(e);
                }
            })
            .orElse(null);
    }

    /**
     * 模糊匹配获取配置文件资源
     *
     * @param matchStr 匹配关键词
     * @return 资源
     */
    public static Resource[] getDefaultConfigResources(String matchStr) {
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
        try {
            String format = String.format("%s/config_default/%s", ResourceUtils.CLASSPATH_URL_PREFIX, matchStr);
            return pathMatchingResourcePatternResolver.getResources(format);
        } catch (IOException e) {
            throw Lombok.sneakyThrow(e);
        }
    }


    public static String getPath() {
        if ((path == null || path.isEmpty())) {
            if (Voyager1Manifest.getInstance().isDebug()) {
                File newFile;
                String voyager1DevPath = SystemUtil.get("VOYAGER1_DEV_PATH");
                if ((voyager1DevPath != null && !voyager1DevPath.isEmpty())) {
                    newFile = FileUtil.file(voyager1DevPath, Voyager1Application.getAppType().name().toLowerCase());
                } else {
                    // 调试模式 为根路径的 voyager1文件
                    newFile = FileUtil.file(FileUtil.getUserHomeDir(), "voyager1", Voyager1Application.getAppType().name().toLowerCase());
                }
                path = FileUtil.getAbsolutePath(newFile);
            } else {
                // 获取当前项目运行路径的父级
                File file = Voyager1Manifest.getRunPath();
                if (!file.exists() && !file.isFile()) {
                    throw new Voyager1RuntimeException("请配置运行路径属性【voyager1.path】");
                }
                File parentFile = file.getParentFile().getParentFile();
                path = FileUtil.getAbsolutePath(parentFile);
            }
        }
        return FileUtil.normalize(path);
    }
}
