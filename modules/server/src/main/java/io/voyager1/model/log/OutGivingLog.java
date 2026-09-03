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

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseWorkspaceModel;
import io.voyager1.model.outgiving.OutGivingModel;
import io.voyager1.model.outgiving.OutGivingNodeProject;

/**
 * 项目分发日志
 *
 * @since 2019/7/19
 **/
@EqualsAndHashCode(callSuper = true)
@TableName(value = "OPS_RELEASE_LOG",
    nameKey = "分发日志", parents = OutGivingModel.class, workspaceBind = 3)
@Data
public class OutGivingLog extends BaseWorkspaceModel {
    /**
     * 分发id
     */
    private String outGivingId;
    /**
     * 状态
     *
     * @see OutGivingNodeProject.Status
     */
    private Integer status;
    /**
     * 开始时间
     */
    private Long startTime;
    /**
     * 结束时间
     */
    private Long endTime;
    /**
     * 处理消息
     */
    private String result;
    /**
     * 节点id
     */
    private String nodeId;
    /**
     * 项目id
     */
    private String projectId;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 进度信息
     */
    private Long progressSize;
    /**
     * 分发方式
     * upload: "手动上传",
     * download: "远程下载",
     * "build-trigger": "构建触发",
     * "use-build": "构建产物",
     */
    private String mode;
    /**
     * 数据
     */
    private String modeData;
}
