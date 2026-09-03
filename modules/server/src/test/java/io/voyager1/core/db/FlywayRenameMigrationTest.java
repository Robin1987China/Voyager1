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

package io.voyager1.core.db;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Flyway 表重命名迁移（V2）回归测试
 * <p>
 * 覆盖两种场景：
 * <ol>
 *   <li>存量库：旧表 {@code CERTIFICATE_INFO} 有数据 + InitDb 刚建的空白新表 {@code SYS_CERTIFICATE}，
 *       迁移后旧表消失、数据完整迁入新表。</li>
 *   <li>全新库：InitDb 已按新名建 {@code SYS_CERTIFICATE}，无旧表，迁移跳过且数据不受影响。</li>
 * </ol>
 */
public class FlywayRenameMigrationTest {

    private DataSource newDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:rename_" + System.nanoTime() + ";MODE=MYSQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private Flyway flyway(DataSource ds) {
        return Flyway.configure()
            .dataSource(ds)
            .baselineOnMigrate(true)
            .baselineVersion("1")
            .baselineDescription("existing-schema-baseline")
            // 该测试只验证 V2/V3/V4 表/列重命名迁移；V1（全量建表）与 V5/V6（增量列/索引）不在本测试场景内
            .target("4")
            .locations("classpath:db/migration")
            .load();
    }

    private boolean tableExists(Connection c, String tableName) throws Exception {
        try (ResultSet rs = c.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    @Test
    public void testExistingDbRenamePreservesData() throws Exception {
        DataSource ds = newDataSource();
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            // 模拟存量库：旧表 CERTIFICATE_INFO 有数据 + InitDb 刚按新名建的空白表 SYS_CERTIFICATE
            st.execute("CREATE TABLE CERTIFICATE_INFO (id VARCHAR(50) PRIMARY KEY, name VARCHAR(50))");
            st.execute("INSERT INTO CERTIFICATE_INFO VALUES ('c1', 'my-cert')");
            st.execute("CREATE TABLE SYS_CERTIFICATE (id VARCHAR(50) PRIMARY KEY)");
        }
        flyway(ds).migrate();
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            assertFalse(tableExists(c, "CERTIFICATE_INFO"), "旧表 CERTIFICATE_INFO 应已被重命名");
            try (ResultSet rs = st.executeQuery("SELECT id, name FROM SYS_CERTIFICATE")) {
                assertTrue(rs.next(), "新表应包含旧表数据");
                assertEquals("c1", rs.getString("id"));
                assertEquals("my-cert", rs.getString("name"));
            }
        }
    }

    @Test
    public void testV3BatchRenamePreservesData() throws Exception {
        DataSource ds = newDataSource();
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            // 模拟存量库：代表性旧表 USER_INFO 有数据 + InitDb 刚按新名建的空白表 SYS_USER
            st.execute("CREATE TABLE USER_INFO (id VARCHAR(50) PRIMARY KEY, name VARCHAR(50))");
            st.execute("INSERT INTO USER_INFO VALUES ('u1', 'admin-user')");
            st.execute("CREATE TABLE SYS_USER (id VARCHAR(50) PRIMARY KEY)");
        }
        flyway(ds).migrate();
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            assertFalse(tableExists(c, "USER_INFO"), "旧表 USER_INFO 应已被 V3 重命名");
            try (ResultSet rs = st.executeQuery("SELECT id, name FROM SYS_USER")) {
                assertTrue(rs.next(), "新表 SYS_USER 应包含旧表数据");
                assertEquals("u1", rs.getString("id"));
                assertEquals("admin-user", rs.getString("name"));
            }
        }
    }

    @Test
    public void testV4ColumnRenamePreservesData() throws Exception {
        DataSource ds = newDataSource();
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            // 模拟存量库：旧表 CLUSTER_INFO 带 PEGASUSVERSION 列 + 数据
            st.execute("CREATE TABLE CLUSTER_INFO (id VARCHAR(50) PRIMARY KEY, PEGASUSVERSION VARCHAR(50))");
            st.execute("INSERT INTO CLUSTER_INFO VALUES ('cl1', '2.11.12')");
        }
        flyway(ds).migrate();
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            // V3 已将表重命名为 INFRA_CLUSTER，V4 已将列重命名为 VOYAGER1VERSION
            assertFalse(columnExists(c, "INFRA_CLUSTER", "PEGASUSVERSION"), "PEGASUSVERSION 列应已被重命名");
            assertTrue(columnExists(c, "INFRA_CLUSTER", "VOYAGER1VERSION"), "VOYAGER1VERSION 列应存在");
            try (ResultSet rs = st.executeQuery("SELECT id, VOYAGER1VERSION FROM INFRA_CLUSTER")) {
                assertTrue(rs.next());
                assertEquals("cl1", rs.getString("id"));
                assertEquals("2.11.12", rs.getString("VOYAGER1VERSION"));
            }
        }
    }

    private boolean columnExists(Connection c, String table, String column) throws Exception {
        try (ResultSet rs = c.getMetaData().getColumns(null, null, table, column)) {
            return rs.next();
        }
    }

    @Test
    public void testFreshDbSkipsRename() throws Exception {
        DataSource ds = newDataSource();
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            // 模拟全新库：InitDb 已按新名建 SYS_CERTIFICATE，无旧表
            st.execute("CREATE TABLE SYS_CERTIFICATE (id VARCHAR(50) PRIMARY KEY, name VARCHAR(50))");
            st.execute("INSERT INTO SYS_CERTIFICATE VALUES ('c2', 'fresh-cert')");
        }
        flyway(ds).migrate();
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT id, name FROM SYS_CERTIFICATE")) {
                assertTrue(rs.next());
                assertEquals("c2", rs.getString("id"));
                assertEquals("fresh-cert", rs.getString("name"));
            }
        }
    }
}
