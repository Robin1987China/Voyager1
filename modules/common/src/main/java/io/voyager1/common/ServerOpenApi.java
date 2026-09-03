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

package io.voyager1.common;

/**
 * Server 开发接口api 列表
 *
 * @since 2019/8/5
 */
public class ServerOpenApi {
    /**
     * 用户的token
     */
    public static final String USER_TOKEN_HEAD = "VOYAGER1-USER-TOKEN";

    /**
     * 存放token的http head
     */
    public static final String HTTP_HEAD_AUTHORIZATION = "Authorization";

    public static final String API = "/api/";

    /**
     * 接收推送
     */
    public static final String RECEIVE_PUSH = API + "node/receive_push";

    public static final String PUSH_NODE_KEY = "--auto-push-to-server";
    /**
     * 触发构建(新), 第一级构建id,第二级token
     */
    public static final String BUILD_TRIGGER_BUILD2 = API + "build2/{id}/{token}";

    /**
     * 触发构建 批量触发
     */
    public static final String BUILD_TRIGGER_BUILD_BATCH = API + "build_batch";

    /**
     * 文件下载
     */
    public static final String FILE_STORAGE_DOWNLOAD = API + "file-storage/download/{id}/{token}";
    /**
     * 静态文件下载
     */
    public static final String STATIC_FILE_STORAGE_DOWNLOAD = API + "file-storage/static/download/{id}/{token}";
    /**
     * 获取当前构建状态
     */
    public static final String BUILD_TRIGGER_STATUS = API + "build_status";

    /**
     * 获取当前构建日志
     */
    public static final String BUILD_TRIGGER_LOG = API + "build_log";

    /**
     * SSH 脚本执行, 第一级脚本id,第二级token
     */
    public static final String SSH_COMMAND_TRIGGER_URL = API + "ssh_command/{id}/{token}";

    /**
     * SSH 脚本执行 批量触发
     */
    public static final String SSH_COMMAND_TRIGGER_BATCH = API + "ssh_command_batch";

    /**
     * 服务端脚本执行, 第一级脚本id,第二级token
     */
    public static final String SERVER_SCRIPT_TRIGGER_URL = API + "server_script/{id}/{token}";

    /**
     * 服务端脚本执行 批量触发
     */
    public static final String SERVER_SCRIPT_TRIGGER_BATCH = API + "server_script_batch";

    /**
     * 插件端脚本执行, 第一级脚本id,第二级token
     */
    public static final String NODE_SCRIPT_TRIGGER_URL = API + "node_script/{id}/{token}";

    /**
     * 插件端脚本执行 批量触发
     */
    public static final String NODE_SCRIPT_TRIGGER_BATCH = API + "node_script_batch";

    /**
     * 项目触发器, 第一级项目id（服务端存储）,第二级token
     */
    public static final String SERVER_PROJECT_TRIGGER_URL = API + "project/{id}/{token}";

    /**
     * 项目触发器,批量触发
     */
    public static final String SERVER_PROJECT_TRIGGER_BATCH = API + "project_batch";

    /**
     * 环境变量, 第一级脚本id,第二级token
     */
    public static final String SERVER_ENV_VAR_TRIGGER_URL = API + "env-var/{id}/{token}";

    /**
     * 备份数据库，触发器
     */
    public static final String BACKUP_TRIGGER_URL = API + "backup-db/{token}";
}
