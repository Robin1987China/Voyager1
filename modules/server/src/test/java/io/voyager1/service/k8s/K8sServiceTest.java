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

package io.voyager1.service.k8s;

import io.voyager1.ApplicationStartTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * K8s 命令注入校验测试
 *
 * @since 2026/8/14
 */
public class K8sServiceTest extends ApplicationStartTest {

    @Autowired
    private K8sService k8sService;

    @Test
    public void testValidateResourceTypeAllowed() {
        Assertions.assertDoesNotThrow(() -> k8sService.validateResourceType("pods"));
        Assertions.assertDoesNotThrow(() -> k8sService.validateResourceType("deployments"));
        Assertions.assertDoesNotThrow(() -> k8sService.validateResourceType("services"));
    }

    @Test
    public void testValidateResourceTypeRejectsInjection() {
        Assertions.assertThrows(IllegalStateException.class, () -> k8sService.validateResourceType("pods; id"));
        Assertions.assertThrows(IllegalStateException.class, () -> k8sService.validateResourceType("pods | id"));
        Assertions.assertThrows(IllegalStateException.class, () -> k8sService.validateResourceType("pods$(id)"));
        Assertions.assertThrows(IllegalStateException.class, () -> k8sService.validateResourceType("pods && id"));
    }

    @Test
    public void testValidateResourceTypeRejectsEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> k8sService.validateResourceType(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> k8sService.validateResourceType(null));
    }

    @Test
    public void testValidateNamespace() {
        Assertions.assertNull(k8sService.validateNamespace("", "default"));
        Assertions.assertNull(k8sService.validateNamespace("all", "default"));
        Assertions.assertEquals("dev", k8sService.validateNamespace("dev", "default"));
        Assertions.assertThrows(IllegalStateException.class, () -> k8sService.validateNamespace("dev; rm -rf /", "default"));
        Assertions.assertThrows(IllegalStateException.class, () -> k8sService.validateNamespace("Dev_Underscore", "default"));
    }
}
