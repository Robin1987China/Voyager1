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
 * 带最后修改人字段 数据表实体
 *
 * @since 2021/8/24
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseUserModifyDbModel extends BaseDbModel {
    /**
     * 修改人
     */
    private String modifyUser;
    /**
     * 创建人
     */
    private String createUser;

    @Override
    public String toString() {
        return super.toString();
    }

    public void setCreateUser(String createUser) {
        if (this.hasCreateUser()) {
            this.createUser = createUser;
        }
    }

    /**
     * 是否开启创建人字段
     *
     * @return true 开启
     */
    protected boolean hasCreateUser() {
        return false;
    }
}
