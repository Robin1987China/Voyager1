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

/**
 * 用户操作日志
 *
 * @since 2019/4/19
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "SYS_OPERATION_LOG",
    nameKey = "用户操作日志", workspaceBind = 2)
@Data
public class UserOperateLogV1 extends BaseWorkspaceModel {
    /**
     * 操作ip
     */
    private String ip;
    /**
     * 用户ip
     */
    private String userId;
    /**
     * 节点id
     */
    private String nodeId;
    /**
     * 操作时间
     */
    private Long optTime;
    /**
     * 操作状态,业务状态码
     */
    private Integer optStatus;
    /**
     * 完整消息
     */
    private String resultMsg;
    /**
     * 请求参数
     */
    private String reqData;
    /**
     * 数据id
     */
    private String dataId;
    /**
     * 数据名称
     */
    private String dataName;
    /**
     * 浏览器标识
     */
    private String userAgent;

    private String classFeature;
    private String methodFeature;
    /**
     * 工作空间名称
     */
    private String workspaceName;

    private String username;
}
