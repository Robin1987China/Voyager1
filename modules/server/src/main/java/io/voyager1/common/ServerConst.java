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

import io.voyager1.common.i18n.I18nMessageUtil;

import java.util.function.Supplier;

/**
 * @since 2022/8/30
 */
public class ServerConst extends Const {

    /**
     * h2 数据库表名字段
     */
    public static final String TABLE_NAME = "TABLE_NAME";

    /**
     * id_rsa
     */
    public static final String ID_RSA = "_id_rsa";
    /**
     * sshkey
     */
    public static final String SSH_KEY = "sshkey";
    /**
     * 引用工作空间环境变量的前缀
     */
    public static final String REF_WORKSPACE_ENV = "$ref.wEnv.";
    /**
     * 引用工作脚本模板的前缀
     */
    public static final String REF_SCRIPT = "$ref.script.";

    public static final String PROXY_PATH = "Voyager1-ProxyPath";

    /**
     * 分发包存储路径
     */
    public static final String OUTGIVING_FILE = "outgiving";
    /**
     * token自动续签状态码
     */
    public static final int RENEWAL_AUTHORIZE_CODE = 801;

    /**
     * token 失效
     */
    public static final int AUTHORIZE_TIME_OUT_CODE = 800;

    /**
     * 账号被锁定
     */
    public static final int ACCOUNT_LOCKED = 802;
    public static final Supplier<String> LOGIN_TIP = () -> "登录信息已失效,重新登录";
    public static final Supplier<String> ACCOUNT_LOCKED_TIP = () -> "账号已经被禁用,不能使用";

    public static final String CHECK_SYSTEM = "check-system";

    public static final String RSA = "RSA";

    public static final String EC = "EC";
}
