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

package io.voyager1;

import io.voyager1.core.AppType;
import io.voyager1.core.AppTypeBinding;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * AppType 枚举与 AppTypeBinding 注解契约测试
 *
 * @since 2026/8/3
 */
public class AppTypeTest {

    @Test
    public void testAgentEnum() {
        AppType agent = AppType.Agent;
        Assertions.assertEquals("io.voyager1.Voyager1AgentApplication", agent.getApplicationClass());
        Assertions.assertEquals("VOYAGER1_AGENT_APPLICATION", agent.getTag());
    }

    @Test
    public void testServerEnum() {
        AppType server = AppType.Server;
        Assertions.assertEquals("io.voyager1.Voyager1ServerApplication", server.getApplicationClass());
        Assertions.assertEquals("VOYAGER1_SERVER_APPLICATION", server.getTag());
    }

    @Test
    public void testAppTypeBindingAnnotation() throws NoSuchMethodException {
        // 注解元数据：必须可被反射读取
        AppTypeBinding annotation = AppTypeTest.class.getAnnotation(AppTypeBinding.class);
        Assertions.assertNull(annotation, "该测试类不携带注解");

        Assertions.assertNotNull(AppTypeBinding.class.getAnnotation(java.lang.annotation.Retention.class));
        Assertions.assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME,
            AppTypeBinding.class.getAnnotation(java.lang.annotation.Retention.class).value());
    }

    @Test
    public void testGetRemoteUrlWithoutAuth() {
        RemoteVersion remoteVersion = new RemoteVersion();
        remoteVersion.setAgentUrl("https://example.com/agent.zip");
        remoteVersion.setServerUrl("https://example.com/server.zip");
        // 未配置 VOYAGER1_REMOTE_VERSION_AUTH 时原样返回
        Assertions.assertEquals("https://example.com/agent.zip", AppType.Agent.getRemoteUrl(remoteVersion));
        Assertions.assertEquals("https://example.com/server.zip", AppType.Server.getRemoteUrl(remoteVersion));
    }
}
