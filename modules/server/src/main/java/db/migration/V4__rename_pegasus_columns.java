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
 * V4：存量库残留 PEGASUS* 列名重命名为 VOYAGER1*（条件列重命名）
 * <p>
 * 背景：项目从 Pegasus 改名为 Voyager1 时，CSV 列名已更新为 {@code voyager1*}，
 * 但存量库仍保留 {@code PEGASUS*} 列名（H2 以大写存储），导致集群/节点心跳、版本上报等 SQL 报
 * {@code Column "VOYAGER1VERSION" not found}。
 * <p>
 * 全新库：CSV 已按 {@code voyager1*} 建列，无 {@code PEGASUS*} 列，全部跳过。
 */
public class V4__rename_pegasus_columns extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();

        // INFRA_CLUSTER（原 CLUSTER_INFO）
        renameColumnIfExists(conn, "INFRA_CLUSTER", "PEGASUSVERSION", "VOYAGER1VERSION");

        // INFRA_MACHINE（原 MACHINE_NODE_INFO）
        renameColumnIfExists(conn, "INFRA_MACHINE", "PEGASUSBUILDTIME", "VOYAGER1BUILDTIME");
        renameColumnIfExists(conn, "INFRA_MACHINE", "PEGASUSHTTPPROXY", "VOYAGER1HTTPPROXY");
        renameColumnIfExists(conn, "INFRA_MACHINE", "PEGASUSHTTPPROXYTYPE", "VOYAGER1HTTPPROXYTYPE");
        renameColumnIfExists(conn, "INFRA_MACHINE", "PEGASUSPASSWORD", "VOYAGER1PASSWORD");
        renameColumnIfExists(conn, "INFRA_MACHINE", "PEGASUSPROJECTCOUNT", "VOYAGER1PROJECTCOUNT");
        renameColumnIfExists(conn, "INFRA_MACHINE", "PEGASUSPROTOCOL", "VOYAGER1PROTOCOL");
        renameColumnIfExists(conn, "INFRA_MACHINE", "PEGASUSSCRIPTCOUNT", "VOYAGER1SCRIPTCOUNT");
        renameColumnIfExists(conn, "INFRA_MACHINE", "PEGASUSTIMEOUT", "VOYAGER1TIMEOUT");
        renameColumnIfExists(conn, "INFRA_MACHINE", "PEGASUSUPTIME", "VOYAGER1UPTIME");
        renameColumnIfExists(conn, "INFRA_MACHINE", "PEGASUSURL", "VOYAGER1URL");
        renameColumnIfExists(conn, "INFRA_MACHINE", "PEGASUSUSERNAME", "VOYAGER1USERNAME");
        renameColumnIfExists(conn, "INFRA_MACHINE", "PEGASUSVERSION", "VOYAGER1VERSION");

        // INFRA_MACHINE_SSH（原 MACHINE_SSH_INFO）
        renameColumnIfExists(conn, "INFRA_MACHINE_SSH", "PEGASUSAGENTPID", "VOYAGER1AGENTPID");

        // INFRA_NODE（原 NODE_INFO）
        renameColumnIfExists(conn, "INFRA_NODE", "PEGASUSPROJECTCOUNT", "VOYAGER1PROJECTCOUNT");
        renameColumnIfExists(conn, "INFRA_NODE", "PEGASUSSCRIPTCOUNT", "VOYAGER1SCRIPTCOUNT");
    }

    private void renameColumnIfExists(Connection conn, String table, String oldCol, String newCol) throws Exception {
        if (!columnExists(conn, table, oldCol)) {
            return; // 全新库：CSV 已按新列名建列
        }
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE " + table + " ALTER COLUMN " + oldCol + " RENAME TO " + newCol);
        }
    }

    private boolean columnExists(Connection conn, String table, String column) throws Exception {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, table, column)) {
            return rs.next();
        }
    }
}
