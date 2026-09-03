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

package io.voyager1.service.cloud;

import io.voyager1.ApplicationStartTest;
import io.voyager1.cloud.CloudCredential;
import io.voyager1.model.data.CloudAccountModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * CloudService 凭证加密/解密/脱敏测试
 *
 * @since 2026/8/31
 */
public class CloudServiceTest extends ApplicationStartTest {

    @Autowired
    private CloudService cloudService;

    @BeforeEach
    public void clean() {
        cloudService.execute("delete from CLOUD_ACCOUNT");
    }

    @Test
    public void testSaveAccountEncrypted() {
        String id = cloudService.saveAccount(null, "测试", "aliyun", "AK", "SK", null, "cn-hangzhou", null);
        // 直接查库（不走脱敏），应存密文
        CloudAccountModel raw = cloudService.getByKey(id);
        Assertions.assertTrue(raw.getAccessKey().startsWith("ENC:"), "accessKey 应加密存储");
        Assertions.assertTrue(raw.getSecretKey().startsWith("ENC:"), "secretKey 应加密存储");
        Assertions.assertNotEquals("AK", raw.getAccessKey());
        Assertions.assertNotEquals("SK", raw.getSecretKey());
    }

    @Test
    public void testListAccountsMasked() {
        cloudService.saveAccount(null, "测试", "aliyun", "AKID1234567890", "SECRET1234567890", null, "cn-hangzhou", null);
        List<CloudAccountModel> list = cloudService.listAccounts();
        Assertions.assertEquals(1, list.size());
        CloudAccountModel account = list.get(0);
        // secretKey 完全脱敏
        Assertions.assertEquals("******", account.getSecretKey());
        // accessKey 首尾 4 位脱敏
        Assertions.assertTrue(account.getAccessKey().startsWith("AKID"), "accessKey 应保留首 4 位");
        Assertions.assertTrue(account.getAccessKey().endsWith("7890"), "accessKey 应保留尾 4 位");
    }

    @Test
    public void testDecryptCredentialWithExtraKey() {
        String id = cloudService.saveAccount(null, "测试", "azure", "client-id", "client-secret", "tenant-id", "eastus", null);
        CloudCredential credential = cloudService.decryptCredential(id);
        Assertions.assertEquals("azure", credential.getVendor());
        Assertions.assertEquals("client-id", credential.getAccessKey());
        Assertions.assertEquals("client-secret", credential.getSecretKey());
        Assertions.assertEquals("tenant-id", credential.getExtraKey(), "extraKey 应解密传递（Azure tenantId）");
        Assertions.assertEquals("eastus", credential.getRegion());
    }

    @Test
    public void testDecryptCredentialNullExtraKey() {
        String id = cloudService.saveAccount(null, "测试", "aliyun", "AK", "SK", null, "cn-hangzhou", null);
        CloudCredential credential = cloudService.decryptCredential(id);
        Assertions.assertNull(credential.getExtraKey(), "未填 extraKey 时应为 null");
    }
}
