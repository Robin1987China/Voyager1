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

package io.voyager1.service.node.ssh;

import com.jcraft.jsch.Session;
import io.voyager1.ApplicationStartTest;
import io.voyager1.func.assets.model.MachineSshModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * SSH 链路冒烟：用 Voyager1 自身的 JSch 封装（{@link SshService}）连真实 SSH 目标，验证发布链路
 * （环境 SSH 目标 → {@code ReleaseManage.doSsh}）所依赖的 SSH 传输层可用。
 * <p>
 * <b>默认跳过</b>：只有设置了环境变量 {@code V1_SSH_TEST_HOST} 才执行（避免 CI 无目标时失败）。
 * 注意：pom 里 {@code excludedGroups} 是字面量（插件配置优先于用户属性），{@code @Tag("external")}
 * 无法用 {@code -DexcludedGroups=} 覆盖，故这里用环境变量开关而不是标签。
 * <p>
 * 本地起目标并执行（Docker 镜像已缓存时无需联网）：
 * <pre>
 *   docker run -d --name voyager1-ssh-target -p 22222:22 rastasheep/ubuntu-sshd:latest
 *   docker exec voyager1-ssh-target sh -c "echo 'root:Voyager1Test' | chpasswd"
 *   V1_SSH_TEST_HOST=127.0.0.1 mvn -pl modules/server test -Dtest=SshLinkSmokeTest
 * </pre>
 * 可用 V1_SSH_TEST_HOST / PORT / USER / PWD 覆盖目标。
 *
 * @since 2026/9/10
 */
@EnabledIfEnvironmentVariable(named = "V1_SSH_TEST_HOST", matches = ".+")
public class SshLinkSmokeTest extends ApplicationStartTest {

    @Autowired
    private SshService sshService;

    @Test
    public void testConnectRealSshTarget() throws Exception {
        String host = System.getenv().getOrDefault("V1_SSH_TEST_HOST", "127.0.0.1");
        int port = Integer.parseInt(System.getenv().getOrDefault("V1_SSH_TEST_PORT", "22222"));
        String user = System.getenv().getOrDefault("V1_SSH_TEST_USER", "root");
        String pwd = System.getenv().getOrDefault("V1_SSH_TEST_PWD", "Voyager1Test");

        MachineSshModel machineSsh = new MachineSshModel();
        machineSsh.setHost(host);
        machineSsh.setPort(port);
        machineSsh.setUser(user);
        machineSsh.setPassword(pwd);

        Session session = sshService.getSessionByModel(machineSsh);
        Assertions.assertNotNull(session, "SSH 会话未建立（目标 " + host + ":" + port + "）");
        Assertions.assertTrue(session.isConnected(), "SSH 会话未连接（目标 " + host + ":" + port + "）");
        session.disconnect();
    }
}
