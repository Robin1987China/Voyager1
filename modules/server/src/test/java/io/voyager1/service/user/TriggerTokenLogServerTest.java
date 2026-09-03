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

package io.voyager1.service.user;

import io.voyager1.util.StrUtil;
import io.voyager1.ApplicationStartTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 触发器 token 生成/校验测试（真实 H2 上下文）
 *
 * @since 2026/8/3
 */
public class TriggerTokenLogServerTest extends ApplicationStartTest {

    @Autowired
    private TriggerTokenLogServer triggerTokenLogServer;

    @Test
    public void testRestTokenGenerate() {
        String token = triggerTokenLogServer.restToken(null, "build", "data-001", "user-001");
        Assertions.assertFalse((token == null || token.isEmpty()));
        Assertions.assertEquals(32, token.length());
    }

    @Test
    public void testRestTokenDifferentEachTime() {
        String token1 = triggerTokenLogServer.restToken(null, "build", "data-001", "user-001");
        String token2 = triggerTokenLogServer.restToken(null, "build", "data-001", "user-001");
        Assertions.assertNotEquals(token1, token2);
    }

    @Test
    public void testGetUserByTokenInvalid() {
        Assertions.assertNull(triggerTokenLogServer.getUserByToken("not-exist-token", "build"));
    }

    @Test
    public void testGetUserByTokenNull() {
        Assertions.assertNull(triggerTokenLogServer.getUserByToken(null, "build"));
    }
}
