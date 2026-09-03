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

package io.voyager1.func.files.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseWorkspaceModel;

/**
 * @since 2023/3/18
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "OPS_FILE_RELEASE_LOG",
    nameKey = "文件发布任务记录", parents = FileStorageModel.class)
@Data
@NoArgsConstructor
public class FileReleaseTaskLogModel extends BaseWorkspaceModel {
    /**
     * 父级任务id
     */
    public static final String TASK_ROOT_ID = "task-root";

    /**
     * 任务名
     */
    private String name;
    /**
     * 任务id
     *
     * @see FileReleaseTaskLogModel#TASK_ROOT_ID
     */
    private String taskId;
    /**
     * 文件 id
     *
     * @see FileStorageModel#getId()
     */
    private String fileId;
    /**
     * 文件来源类型
     * 1 文件中心
     * 2 静态文件
     */
    private Integer fileType;
    /**
     * 任务类型 0 ssh 1 节点
     */
    private Integer taskType;
    /**
     * 发布路径
     */
    private String releasePath;
    /**
     * 任务关联的数据id
     */
    private String taskDataId;
    /**
     * 任务状态， 0 等待开始 1 进行中 2 任务结束 3 失败 4 取消任务
     */
    private Integer status;
    /**
     * 状态描述
     */
    private String statusMsg;
    /**
     * 发布之前的脚本
     */
    private String beforeScript;
    /**
     * 发布后的脚本
     */
    private String afterScript;
}
