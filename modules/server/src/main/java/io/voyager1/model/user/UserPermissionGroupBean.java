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

package io.voyager1.model.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseUserModifyDbModel;

/**
 * @since 2022/8/3
 */
@TableName(value = "SYS_PERMISSION_GROUP",
    nameKey = "用户权限组")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPermissionGroupBean extends BaseUserModifyDbModel {

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 禁止执行时间段，优先判断禁止执行
     * <pre>
     * [{
     *     "startTime": 1,
     *     "endTime": 1,
     *     "reason": ""
     * }]
     * </pre>
     */
    private String prohibitExecute;

    /**
     * 允许执行的时间段
     * <pre>
     * [{
     *     "week": [1,2],
     *     "startTime":"08:00:00"
     *     "endTime": "18:00:00"
     * }]
     * </pre>
     */
    private String allowExecute;
}
