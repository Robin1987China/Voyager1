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

import io.voyager1.util.ObjectUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseWorkspaceModel;
import io.voyager1.model.data.MonitorModel;

/**
 * 监控日志
 *
 * @since 2019/7/13
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "OPS_MONITOR_NOTIFY_LOG",
    nameKey = "监控通知", parents = MonitorModel.class)
@Data
public class MonitorNotifyLog extends BaseWorkspaceModel {


    private String nodeId;
    private String projectId;
    /**
     * 异常发生时间
     */
    private Long createTime;
    private String title;
    private String content;
    /**
     * 项目状态状态
     */
    private Boolean status;
    /**
     * 通知方式
     *
     * @see MonitorModel.NotifyType
     */
    private Integer notifyStyle;
    /**
     * 通知发送状态
     */
    private Boolean notifyStatus;
    /**
     * 监控id
     */
    private String monitorId;
    /**
     * 通知对象
     */
    private String notifyObject;
    /**
     * 通知异常消息
     */
    private String notifyError;

    public boolean status() {
        return (status != null ? status : false);
    }
}
