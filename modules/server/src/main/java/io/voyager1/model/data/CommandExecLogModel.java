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

import io.voyager1.util.PropIgnore;
import io.voyager1.util.FileUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import io.voyager1.Voyager1Application;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseEnum;
import io.voyager1.model.BaseWorkspaceModel;

import java.io.File;

/**
 * @since 2021/12/22
 */
@TableName(value = "OPS_COMMAND_LOG",
    nameKey = "命令执行记录", parents = CommandModel.class)
@Data
@EqualsAndHashCode(callSuper = true)
public class CommandExecLogModel extends BaseWorkspaceModel {

    /**
     * 命令ID
     */
    private String commandId;

    /**
     * 批次ID
     */
    private String batchId;

    /**
     * ssh Id
     */
    private String sshId;

    /**
     * @see Status
     */
    private Integer status;

    /**
     * 命令名称
     */
    private String commandName;

    /**
     * ssh 名称
     */
    private String sshName;

    /**
     * 参数
     */
    private String params;

    /**
     * 触发类型 {0，手动，1 自动触发}
     */
    private Integer triggerExecType;

    /**
     * 日志文件是否存在
     */
    @PropIgnore
    private Boolean hasLog;

    /**
     * 退出码
     */
    private Integer exitCode;

    public File logFile() {
        return FileUtil.file(CommandExecLogModel.logFileDir(this.getCommandId()), batchId, this.getId() + ".log");
    }

    /**
     * log 存储目录
     *
     * @param commandId 命令ID
     * @return 文件
     */
    public static File logFileDir(String commandId) {
        return FileUtil.file(Voyager1Application.getInstance().getDataPath(), "command_log", commandId);
    }

    @Getter
    public enum Status implements BaseEnum {
        /**
         *
         */
        ING(0, "执行中"),
        DONE(1, "执行结束"),
        ERROR(2, "执行错误"),
        SESSION_ERROR(3, "会话异常"),
        ;
        private final int code;
        private final String desc;

        Status(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }
}
