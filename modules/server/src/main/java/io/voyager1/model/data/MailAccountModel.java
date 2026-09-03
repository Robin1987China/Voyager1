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

package io.voyager1.model.data;

import io.voyager1.util.ObjectUtil;
import io.voyager1.model.BaseJsonModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统邮箱配置
 *
 * @since 2019/7/16
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class MailAccountModel extends BaseJsonModel {

    public static final String ID = "MAIL_CONFIG";

    /**
     * SMTP服务器域名
     */
    private String host;
    /**
     * SMTP服务端口
     */
    private Integer port;
    /**
     * 用户名
     */
    private String user;
    /**
     * 密码
     */
    private String pass;
    /**
     * 发送方，遵循RFC-822标准
     */
    private String from;
    /**
     * 使用 SSL安全连接
     */
    private Boolean sslEnable;
    /**
     * 指定的端口连接到在使用指定的套接字工厂。如果没有设置,将使用默认端口
     */
    @Deprecated
    private Integer socketFactoryPort;

    /**
     * 超时时间
     */
    private Integer timeout;

    /**
     * 兼容端口
     *
     * @return port
     */
    public Integer getPort() {
        if (sslEnable != null && sslEnable) {
            if (socketFactoryPort != null) {
                return socketFactoryPort;
            }
        }
        return (port != null ? port : socketFactoryPort);
    }

}
