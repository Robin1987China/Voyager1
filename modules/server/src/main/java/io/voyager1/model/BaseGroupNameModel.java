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

package io.voyager1.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @since 2023/2/25
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseGroupNameModel extends BaseUserModifyDbModel {
    /**
     * 名称
     */
    private String name;
    /**
     * 分组名称
     */
    private String groupName;
}
