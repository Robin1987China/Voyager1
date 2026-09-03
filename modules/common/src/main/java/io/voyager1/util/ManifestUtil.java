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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Jar 包中 MANIFEST.MF 获取与解析工具 {@code io.voyager1.util.ManifestUtil}。
 */
public class ManifestUtil {

    private static final String[] MANIFEST_NAMES = {"Manifest.mf", "manifest.mf", "MANIFEST.MF"};

    private ManifestUtil() {
    }

    /**
     * 根据 class 获取其所在 jar 包文件的 Manifest，若该类不在 jar 包中则返回 {@code null}。
     *
     * @param cls 类
     * @return Manifest
     */
    public static Manifest getManifest(Class<?> cls) {
        if (cls == null) {
            return null;
        }
        URL url = cls.getResource(cls.getSimpleName() + ".class");
        if (url == null) {
            return null;
        }
        try {
            URLConnection connection = url.openConnection();
            if (connection instanceof JarURLConnection) {
                return getManifest(((JarURLConnection) connection).getJarFile());
            }
        } catch (IOException e) {
            throw new RuntimeException("读取 Manifest 失败", e);
        }
        return null;
    }

    /**
     * 获取 jar 包文件或目录下的 Manifest。
     *
     * @param classpathItem 文件路径
     * @return Manifest
     */
    public static Manifest getManifest(File classpathItem) {
        if (classpathItem == null || !classpathItem.exists()) {
            return null;
        }
        Manifest manifest = null;
        if (classpathItem.isFile()) {
            try (JarFile jarFile = new JarFile(classpathItem)) {
                manifest = getManifest(jarFile);
            } catch (IOException e) {
                throw new RuntimeException("读取 Manifest 失败", e);
            }
        } else {
            File metaDir = new File(classpathItem, "META-INF");
            File manifestFile = null;
            if (metaDir.isDirectory()) {
                for (String name : MANIFEST_NAMES) {
                    File mf = new File(metaDir, name);
                    if (mf.isFile()) {
                        manifestFile = mf;
                        break;
                    }
                }
            }
            if (manifestFile != null) {
                try (InputStream in = new FileInputStream(manifestFile)) {
                    manifest = new Manifest(in);
                } catch (IOException e) {
                    throw new RuntimeException("读取 Manifest 失败", e);
                }
            }
        }
        return manifest;
    }

    /**
     * 获取 jar 包的 Manifest。
     *
     * @param jarFile jar 文件
     * @return Manifest
     */
    public static Manifest getManifest(JarFile jarFile) {
        if (jarFile == null) {
            return null;
        }
        try {
            return jarFile.getManifest();
        } catch (IOException e) {
            throw new RuntimeException("读取 Manifest 失败", e);
        }
    }
}
