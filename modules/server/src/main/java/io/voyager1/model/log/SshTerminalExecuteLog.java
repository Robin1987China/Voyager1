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

package io.voyager1.model.log;

import io.voyager1.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseWorkspaceModel;
import io.voyager1.model.data.SshModel;

/**
 * ssh 终端执行日志
 *
 * @since 2021/08/04
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "INFRA_SSH_SESSION_LOG",
    nameKey = "ssh 终端执行日志", parents = SshModel.class)
@Data
@NoArgsConstructor
public class SshTerminalExecuteLog extends BaseWorkspaceModel {
    /**
     * 操作ip
     */
    private String ip;
    /**
     * 用户ip
     */
    private String userId;
    /**
     * sshid
     */
    private String sshId;
    /**
     * 名称
     */
    private String sshName;
    /**
     * 执行的命令
     */
    private String commands;
    /**
     * 浏览器标识
     */
    private String userAgent;

    /**
     * 是否拒绝执行,true 运行执行，false 拒绝执行
     */
    private Boolean refuse;

    private String machineSshId;

    private String machineSshName;

    public void setUserAgent(String userAgent) {
        this.userAgent = (userAgent == null ? null : (userAgent.length() <= 280 ? userAgent : userAgent.substring(0, 280)));
    }
}
