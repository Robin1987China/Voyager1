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

package io.voyager1.cloud;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * ICloudProvider 状态规范化测试（纯函数，无外部依赖）
 *
 * @since 2026/8/31
 */
public class CloudProviderNormalizeTest {

    private final ICloudProvider provider = new ICloudProvider() {
        @Override
        public String vendor() {
            return "test";
        }

        @Override
        public boolean testConnectivity(CloudCredential credential) {
            return true;
        }

        @Override
        public List<CloudInstanceInfo> listInstances(CloudCredential credential, String region) {
            return Collections.emptyList();
        }

        @Override
        public void startInstance(CloudCredential credential, String region, String instanceId) {
        }

        @Override
        public void stopInstance(CloudCredential credential, String region, String instanceId) {
        }

        @Override
        public void rebootInstance(CloudCredential credential, String region, String instanceId) {
        }
    };

    @Test
    public void testUpperToTitleCase() {
        assertEquals("Running", provider.normalizeStatus("RUNNING"));
        assertEquals("Stopped", provider.normalizeStatus("STOPPED"));
    }

    @Test
    public void testLowerToTitleCase() {
        assertEquals("Running", provider.normalizeStatus("running"));
        assertEquals("Pending", provider.normalizeStatus("pending"));
    }

    @Test
    public void testAlreadyTitleCaseUnchanged() {
        assertEquals("Running", provider.normalizeStatus("Running"));
    }

    @Test
    public void testNullPassthrough() {
        assertNull(provider.normalizeStatus(null));
    }

    @Test
    public void testEmptyPassthrough() {
        assertEquals("", provider.normalizeStatus(""));
    }

    @Test
    public void testSingleChar() {
        assertEquals("A", provider.normalizeStatus("a"));
    }
}
