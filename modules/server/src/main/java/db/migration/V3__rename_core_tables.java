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
 * V3：承继表批量重命名为原创新名（SYS_/INFRA_/CI_/OPS_/STORAGE_ 前缀）
 * <p>
 * 由 script/rename-tables.mjs 依据 table-rename-map.json 自动生成。
 * 条件重命名：旧表存在 → 删除 InitDb 刚建的空白新表（如有）→ 旧表重命名（保留数据）；
 * 旧表不存在（全新库已按新名建表）→ 跳过。
 */
public class V3__rename_core_tables extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        renameIfOldExists(conn, "BUILD_INFO", "CI_BUILD");
        renameIfOldExists(conn, "BUILDHISTORYLOG", "CI_BUILD_LOG");
        renameIfOldExists(conn, "CLUSTER_INFO", "INFRA_CLUSTER");
        renameIfOldExists(conn, "COMMAND_EXEC_LOG", "OPS_COMMAND_LOG");
        renameIfOldExists(conn, "COMMAND_INFO", "OPS_COMMAND");
        renameIfOldExists(conn, "DOCKER_INFO", "INFRA_DOCKER");
        renameIfOldExists(conn, "DOCKER_SWARM_INFO", "INFRA_DOCKER_SWARM");
        renameIfOldExists(conn, "FILE_RELEASE_TASK_LOG", "OPS_FILE_RELEASE_LOG");
        renameIfOldExists(conn, "FILE_RELEASE_TASK_TEMPLATE", "OPS_FILE_RELEASE_TEMPLATE");
        renameIfOldExists(conn, "FILE_STORAGE", "STORAGE_FILE");
        renameIfOldExists(conn, "LOG_READ", "OPS_LOG_FILE");
        renameIfOldExists(conn, "MACHINE_DOCKER_INFO", "INFRA_MACHINE_DOCKER");
        renameIfOldExists(conn, "MACHINE_NODE_INFO", "INFRA_MACHINE");
        renameIfOldExists(conn, "MACHINE_NODE_STAT_LOG", "INFRA_MACHINE_STAT_LOG");
        renameIfOldExists(conn, "MACHINE_SSH_INFO", "INFRA_MACHINE_SSH");
        renameIfOldExists(conn, "MONITOR_INFO", "OPS_MONITOR");
        renameIfOldExists(conn, "MONITOR_USER_OPT", "OPS_MONITOR_NOTIFY");
        renameIfOldExists(conn, "MONITORNOTIFYLOG", "OPS_MONITOR_NOTIFY_LOG");
        renameIfOldExists(conn, "NODE_INFO", "INFRA_NODE");
        renameIfOldExists(conn, "OUT_GIVING", "OPS_RELEASE");
        renameIfOldExists(conn, "OUTGIVINGLOG", "OPS_RELEASE_LOG");
        renameIfOldExists(conn, "PROJECT_INFO", "CI_PROJECT");
        renameIfOldExists(conn, "REPOSITORY", "CI_REPOSITORY");
        renameIfOldExists(conn, "SCRIPT_EXECUTE_LOG", "OPS_SCRIPT_LOG");
        renameIfOldExists(conn, "SCRIPT_INFO", "OPS_SCRIPT");
        renameIfOldExists(conn, "SCRIPT_LIBRARY", "OPS_SCRIPT_LIBRARY");
        renameIfOldExists(conn, "SERVER_SCRIPT_EXECUTE_LOG", "OPS_SERVER_SCRIPT_LOG");
        renameIfOldExists(conn, "SERVER_SCRIPT_INFO", "OPS_SERVER_SCRIPT");
        renameIfOldExists(conn, "SSH_INFO", "INFRA_SSH");
        renameIfOldExists(conn, "SSHTERMINALEXECUTELOG", "INFRA_SSH_SESSION_LOG");
        renameIfOldExists(conn, "STATIC_FILE_STORAGE", "STORAGE_STATIC_FILE");
        renameIfOldExists(conn, "SYSTEM_PARAMETERS", "SYS_PARAMETER");
        renameIfOldExists(conn, "USER_BIND_WORKSPACE", "SYS_USER_WORKSPACE");
        renameIfOldExists(conn, "USER_INFO", "SYS_USER");
        renameIfOldExists(conn, "USER_LOGIN_LOG", "SYS_USER_LOGIN_LOG");
        renameIfOldExists(conn, "USER_PERMISSION_GROUP", "SYS_PERMISSION_GROUP");
        renameIfOldExists(conn, "USEROPERATELOGV1", "SYS_OPERATION_LOG");
        renameIfOldExists(conn, "WORKSPACE", "SYS_WORKSPACE");
    }

    private void renameIfOldExists(Connection conn, String oldName, String newName) throws Exception {
        if (!tableExists(conn, oldName)) {
            return; // 全新库：InitDb 已按新名建表
        }
        try (Statement st = conn.createStatement()) {
            if (tableExists(conn, newName)) {
                st.execute("DROP TABLE " + newName);
            }
            st.execute("ALTER TABLE " + oldName + " RENAME TO " + newName);
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws Exception {
        try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }
}
