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

package io.voyager1.core;

import io.voyager1.ApplicationStartTest;
import io.voyager1.core.entity.NodeEntity;
import io.voyager1.core.entity.WorkspaceInfoEntity;
import io.voyager1.core.repository.NodeRepository;
import io.voyager1.core.repository.WorkspaceInfoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * group 保留字列回归测试（部署事故回归）
 * <p>
 * INFRA_NODE 等表含 {@code group} 列（H2 保留字）。此前 H2 URL 的 NON_KEYWORDS=GROUP
 * 破坏了 {@code GROUP BY} 语法；改为引用标识符 {@code "GROUP"} 后，JPA 必须能用
 * {@code @Column(name="\"GROUP\"")} 正常读写该列。本测试在真实 Spring 上下文 + H2 + Flyway
 * 上验证，杜绝再次出现"测试全绿、部署即崩"。
 */
public class GroupColumnJpaTest extends ApplicationStartTest {

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private WorkspaceInfoRepository workspaceInfoRepository;

    @Autowired
    private javax.sql.DataSource dataSource;

    @Test
    public void testNodeGroupColumnWriteRead() {
        String id = "group-test-node-" + System.currentTimeMillis();
        NodeEntity entity = new NodeEntity();
        entity.setId(id);
        entity.setName("分组测试节点");
        entity.setGroup("生产分组");
        entity.setWorkspaceId("DEFAULT");
        entity.setUrl("http://127.0.0.1:1");
        entity.setLoginName("admin");
        entity.setLoginPwd("pwd");
        entity.setProtocol("http");
        nodeRepository.save(entity);

        // JPA 读回 group 列
        NodeEntity loaded = nodeRepository.findById(id).orElse(null);
        assertNotNull(loaded, "节点应能保存并读回");
        assertEquals("生产分组", loaded.getGroup(), "group 列（引用标识符）读写应一致");

        // 原生 SQL 验证列真实落库为 GROUP
        org.springframework.jdbc.core.JdbcTemplate jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        String group = jdbc.queryForObject("SELECT \"GROUP\" FROM INFRA_NODE WHERE id = ?", String.class, id);
        assertEquals("生产分组", group);

        // 原生 SQL 含 GROUP BY 也应正常（回归：NON_KEYWORDS=GROUP 曾破坏该语法）
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM (SELECT workspaceId, COUNT(1) AS c FROM INFRA_NODE GROUP BY workspaceId) t", Integer.class);
        assertNotNull(count);

        nodeRepository.deleteById(id);
    }

    @Test
    public void testWorkspaceGroupColumnWriteRead() {
        String id = "group-test-ws-" + System.currentTimeMillis();
        WorkspaceInfoEntity entity = new WorkspaceInfoEntity();
        entity.setId(id);
        entity.setName("分组测试工作空间");
        entity.setGroup("默认");
        entity.setDescription("test");
        workspaceInfoRepository.save(entity);

        WorkspaceInfoEntity loaded = workspaceInfoRepository.findById(id).orElse(null);
        assertNotNull(loaded, "工作空间应能保存并读回");
        assertEquals("默认", loaded.getGroup(), "工作空间 group 列读写应一致");

        workspaceInfoRepository.deleteById(id);
    }
}
