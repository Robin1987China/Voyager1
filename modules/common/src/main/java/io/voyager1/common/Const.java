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

import java.util.function.Supplier;

/**
 * Const class
 */
public class Const {
    /**
     * 升级提示语
     */
    public static final Supplier<String> UPGRADE_MSG = () -> "升级(重启)中大约需要30秒～2分钟左右";

    /**
     * 请求 header
     */
    public static final String WORKSPACE_ID_REQ_HEADER = "workspaceId";
    /**
     * 默认的工作空间
     */
    public static final String WORKSPACE_DEFAULT_ID = "DEFAULT";
    /**
     * 工作空间全局
     */
    public static final String WORKSPACE_GLOBAL = "GLOBAL";
    /**
     * 默认的分组名
     */
    public static final Supplier<String> DEFAULT_GROUP_NAME = () -> "默认";
//    /**
//     * websocket 传输 agent 包 buffer size
//     */
//    public static final int DEFAULT_BUFFER_SIZE = 1024 * 1024;
    /**
     * id 最大长度
     */
    public static final int ID_MAX_LEN = 50;

    /**
     * 用户名header
     */
    public static final String VOYAGER1_SERVER_USER_NAME = "Voyager1-Server-UserName";

    public static final String VOYAGER1_AGENT_AUTHORIZE = "Voyager1-Agent-Authorize";

    public static final String DATA = "data";

    public static final int AUTHORIZE_ERROR = 900;
    /**
     * 脚本模板存放路径
     */
    public static final String SCRIPT_DIRECTORY = "script";
    /**
     * 脚本默认运行缓存执行文件路径，考虑 windows 文件被占用情况
     */
    public static final String SCRIPT_RUN_CACHE_DIRECTORY = "script_run_cache";
    /**
     * 授权信息
     */
    public static final String AUTHORIZE = "agent_authorize.json";

    /**
     * 程序升级信息文件
     */
    public static final String UPGRADE = "upgrade.json";
    public static final String RUN_JAR = "run.bin";
    /**
     * 远程版本信息
     */
    public static final String REMOTE_VERSION = "remote_version.json";

    public static final String FILE_NAME = "application.yml";

    /**
     *
     */
    public static final String SYSTEM_ID = "system";

    public static final String SOCKET_MSG_TAG = "VOYAGER1_MSG";

    /**
     * 第一次服务端安装信息
     */
    public static final String INSTALL = "INSTALL.json";
    /**
     * 隐私变量占位符
     */
    public static final String PRIVACY_PLACEHOLDER = "******";
    /**
     * 隐私变量关键词（避免在日志中出现）
     */
    public static final String[] PRIVACY_VARIABLE_KEYWORDS = new String[]{"pwd", "pass", "password", "token"};
}
