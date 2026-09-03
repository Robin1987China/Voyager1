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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 类工具，"" {@code io.voyager1.util.ClassUtil}。
 */
public class ClassUtil {

    public static boolean isSimpleValueType(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return clazz.isPrimitive()
            || clazz == String.class
            || Number.class.isAssignableFrom(clazz)
            || clazz == Boolean.class
            || clazz == Character.class;
    }

    public static boolean isPrimitiveWrapper(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return clazz == Integer.class || clazz == Long.class || clazz == Double.class
            || clazz == Float.class || clazz == Short.class || clazz == Byte.class
            || clazz == Boolean.class || clazz == Character.class;
    }

    public static boolean isNormalClass(Class<?> clazz) {
        return clazz != null && !clazz.isInterface() && !clazz.isEnum() && !clazz.isAnnotation() && !clazz.isAnonymousClass();
    }

    public static boolean isAbstract(Class<?> clazz) {
        return clazz != null && java.lang.reflect.Modifier.isAbstract(clazz.getModifiers());
    }

    public static String getLocationPath(Class<?> clazz) {
        try {
            java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
            return url == null ? null : java.net.URLDecoder.decode(url.getPath(), "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    public static java.net.URL getLocation(Class<?> clazz) {
        try {
            return clazz.getProtectionDomain().getCodeSource().getLocation();
        } catch (Exception e) {
            return null;
        }
    }

    public static Class<?> getTypeArgument(Class<?> clazz) {
        java.lang.reflect.Type superType = clazz.getGenericSuperclass();
        if (superType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.Type[] args = ((java.lang.reflect.ParameterizedType) superType).getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class) {
                return (Class<?>) args[0];
            }
        }
        return null;
    }

    public static Set<Class<?>> scanPackage(String packageName) {
        return scanPackageByAnnotation(packageName, null);
    }

    public static Set<Class<?>> scanPackage(String packageName, java.util.function.Predicate<Class<?>> classFilter) {
        Set<Class<?>> result = new LinkedHashSet<>();
        org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider provider =
            new org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
        for (org.springframework.beans.factory.config.BeanDefinition bd : provider.findCandidateComponents(packageName)) {
            try {
                Class<?> c = Class.forName(bd.getBeanClassName(), false, Thread.currentThread().getContextClassLoader());
                if (classFilter == null || classFilter.test(c)) {
                    result.add(c);
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    public static Set<Class<?>> scanPackageBySuper(String packageName, Class<?> superClass) {
        Set<Class<?>> result = new LinkedHashSet<>();
        org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider provider =
            new org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
        for (org.springframework.beans.factory.config.BeanDefinition bd : provider.findCandidateComponents(packageName)) {
            try {
                Class<?> c = Class.forName(bd.getBeanClassName(), false, Thread.currentThread().getContextClassLoader());
                if (superClass == null || superClass.isAssignableFrom(c)) {
                    result.add(c);
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    public static Set<Class<?>> scanPackageByAnnotation(String packageName, Class<? extends java.lang.annotation.Annotation> annotationClass) {
        Set<Class<?>> result = new LinkedHashSet<>();
        org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider provider =
            new org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider(false);
        if (annotationClass != null) {
            provider.addIncludeFilter(new org.springframework.core.type.filter.AnnotationTypeFilter(annotationClass));
        } else {
            provider.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
        }
        for (org.springframework.beans.factory.config.BeanDefinition bd : provider.findCandidateComponents(packageName)) {
            try {
                result.add(Class.forName(bd.getBeanClassName(), false, Thread.currentThread().getContextClassLoader()));
            } catch (Exception ignored) {
            }
        }
        return result;
    }
}
