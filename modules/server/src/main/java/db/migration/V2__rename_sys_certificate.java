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

package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * V2：CERTIFICATE_INFO → SYS_CERTIFICATE（条件重命名）
 * <p>
 * 采用 Java 迁移以兼容三种场景：
 * <ol>
 *   <li>存量库（旧表 {@code CERTIFICATE_INFO} 有数据，CSV 已改新名，InitDb 刚建空表 {@code SYS_CERTIFICATE}）：
 *       删除空新表 → 旧表重命名为新表。</li>
 *   <li>存量库（旧表存在、新表不存在）：直接重命名。</li>
 *   <li>全新库（InitDb 已按新名建 {@code SYS_CERTIFICATE}，无旧表）：跳过。</li>
 * </ol>
 */
public class V2__rename_sys_certificate extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        if (!tableExists(conn, "CERTIFICATE_INFO")) {
            return; // 全新库：CSV 已按新名建表，无需重命名
        }
        try (Statement st = conn.createStatement()) {
            if (tableExists(conn, "SYS_CERTIFICATE")) {
                // InitDb 刚按新名建的空白表，删除以让位给旧表改名（保留旧表数据）
                st.execute("DROP TABLE SYS_CERTIFICATE");
            }
            st.execute("ALTER TABLE CERTIFICATE_INFO RENAME TO SYS_CERTIFICATE");
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws Exception {
        try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }
}
