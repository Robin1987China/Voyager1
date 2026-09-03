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

package io.voyager1.func.user.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseUserModifyDbModel;

/**
 * @since 2023/3/9
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "SYS_USER_LOGIN_LOG",
    nameKey = "用户登录日志")
@Data
@NoArgsConstructor
public class UserLoginLogModel extends BaseUserModifyDbModel {

    /**
     * 操作ip
     */
    private String ip;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 浏览器标识
     */
    private String userAgent;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误原因
     * <p>
     * 0 正常登录
     * 1 密码错误
     * 2 被锁定
     * 3 续期
     * 4 账号被禁用
     * 6 oauth2 登录成功
     */
    private Integer operateCode;
}
