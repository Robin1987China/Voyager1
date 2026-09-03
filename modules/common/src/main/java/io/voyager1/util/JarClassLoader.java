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
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Jar 类加载器，"" {@code io.voyager1.util.JarClassLoader}。
 */
public class JarClassLoader extends URLClassLoader {

    public JarClassLoader(File jarFileOrDir) {
        this(jarFileOrDir, JarClassLoader.class.getClassLoader());
    }

    public JarClassLoader(File jarFileOrDir, ClassLoader parent) {
        super(new URL[0], parent);
        addJar(jarFileOrDir);
    }

    /**
     * 从文件或目录加载。目录则加载其中所有 jar 文件。
     *
     * @param jarFileOrDir jar 文件或目录
     * @return JarClassLoader
     */
    public static JarClassLoader load(File jarFileOrDir) {
        return load(jarFileOrDir, JarClassLoader.class.getClassLoader());
    }

    /**
     * 从文件或目录加载。目录则加载其中所有 jar 文件。
     *
     * @param jarFileOrDir jar 文件或目录
     * @param classLoader  父类加载器
     * @return JarClassLoader
     */
    public static JarClassLoader load(File jarFileOrDir, ClassLoader classLoader) {
        return new JarClassLoader(jarFileOrDir, classLoader);
    }

    /**
     * 将 jar 文件或目录中的 jar 文件加入类加载器。
     *
     * @param jarFileOrDir jar 文件或目录
     * @return this
     */
    public JarClassLoader addJar(File jarFileOrDir) {
        try {
            if (jarFileOrDir != null && jarFileOrDir.isDirectory()) {
                File[] jars = jarFileOrDir.listFiles(
                        (dir, name) -> name != null && name.toLowerCase().endsWith(".jar"));
                if (jars != null) {
                    for (File jar : jars) {
                        addURL(jar.toURI().toURL());
                    }
                }
            } else if (jarFileOrDir != null) {
                addURL(jarFileOrDir.toURI().toURL());
            }
        } catch (MalformedURLException e) {
            throw new UtilException(e);
        }
        return this;
    }

    /**
     * 将 jar 文件加入给定的 URLClassLoader。
     *
     * @param classLoader 目标类加载器
     * @param jarFile     jar 文件
     */
    public static void loadJar(URLClassLoader classLoader, File jarFile) {
        try {
            Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);
            addURL.invoke(classLoader, jarFile.toURI().toURL());
        } catch (Exception e) {
            throw new UtilException(e);
        }
    }
}
