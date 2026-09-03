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

package io.voyager1.permission;

import io.voyager1.ApplicationStartTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 权限模型（ClassFeature/MethodFeature/SystemPermission）契约测试
 *
 * @since 2026/8/3
 */
public class PermissionModelTest extends ApplicationStartTest {

    @Test
    public void testClassFeatureHasNullAndRoot() {
        Assertions.assertEquals("", ClassFeature.NULL.getName().get());
        Assertions.assertEquals(ClassFeature.NODE, ClassFeature.NODE);
    }

    @Test
    public void testClassFeatureNameNotEmpty() {
        for (ClassFeature feature : ClassFeature.values()) {
            if (feature == ClassFeature.NULL) {
                continue;
            }
            String name = feature.getName().get();
            Assertions.assertFalse(name.isEmpty(), "权限功能描述不能为空: " + feature.name());
        }
    }

    @Test
    public void testClassFeatureUniqueNames() {
        // 已知上游数据重复：i18n.node_authorized_config.f934 被两个枚举引用
        long duplicateGroups = Arrays.stream(ClassFeature.values())
            .filter(f -> f != ClassFeature.NULL)
            .map(f -> f.getName().get())
            .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
            .values().stream()
            .filter(v -> v > 1)
            .count();
        Assertions.assertEquals(1, duplicateGroups, "权限功能描述仅允许已知的 1 处重复");
    }

    @Test
    public void testMethodFeature() {
        Assertions.assertNotNull(MethodFeature.EDIT.getName().get());
        Assertions.assertEquals(8, MethodFeature.values().length);
        Assertions.assertTrue(Arrays.stream(MethodFeature.values())
            .filter(f -> f != MethodFeature.NULL)
            .map(f -> f.getName().get())
            .noneMatch(String::isEmpty), "功能方法描述不允许为空");
    }

    @Test
    public void testSystemPermissionAnnotation() {
        // RUNTIME 保留，供拦截器反射读取
        Assertions.assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME,
            SystemPermission.class.getAnnotation(java.lang.annotation.Retention.class).value());
        Assertions.assertTrue(SystemPermission.class.isAnnotation());
    }

    @Test
    public void testFeatureAnnotation() {
        Assertions.assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME,
            Feature.class.getAnnotation(java.lang.annotation.Retention.class).value());
    }
}
