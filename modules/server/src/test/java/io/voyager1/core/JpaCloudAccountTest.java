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
import io.voyager1.core.entity.CloudAccountEntity;
import io.voyager1.core.repository.CloudAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 首个 JPA 实体 PoC 测试（Phase 1）
 * <p>
 * 证明新持久层（JPA）与旧存储层（JdbcTemplate）对同一张 CLOUD_ACCOUNT 表读写一致。
 */
public class JpaCloudAccountTest extends ApplicationStartTest {

    @Autowired
    private CloudAccountRepository repository;

    @Autowired
    private DataSource dataSource;

    @Test
    public void testJpaWriteOldRead() {
        String id = "jpa-" + System.currentTimeMillis();
        CloudAccountEntity entity = new CloudAccountEntity(
            id, System.currentTimeMillis(), null, null, "JPA写入", "aliyun", "ak", "sk", null, "cn-hangzhou", "poctest");
        repository.save(entity);

        // 旧层 JdbcTemplate 读取，证明数据真实落库且两套存储一致
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String name = jdbc.queryForObject("SELECT name FROM CLOUD_ACCOUNT WHERE id = ?", String.class, id);
        assertEquals("JPA写入", name);

        repository.deleteById(id);
    }

    @Test
    public void testOldWriteJpaRead() {
        String id = "old-" + System.currentTimeMillis();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO CLOUD_ACCOUNT (id, name, vendor, region) VALUES (?, ?, ?, ?)", id, "旧层写入", "aws", "us-east-1");

        CloudAccountEntity entity = repository.findById(id).orElse(null);
        assertNotNull(entity);
        assertEquals("旧层写入", entity.getName());

        jdbc.update("DELETE FROM CLOUD_ACCOUNT WHERE id = ?", id);
    }
}
