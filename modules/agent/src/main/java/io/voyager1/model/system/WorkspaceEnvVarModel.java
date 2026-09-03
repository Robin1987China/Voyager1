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

package io.voyager1.model.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.model.BaseModel;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 2022/3/8
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class WorkspaceEnvVarModel extends BaseModel {

    private Map<String, WorkspaceEnvVarItemModel> varData;

    /**
     * 更新变量
     *
     * @param name                 变量名称
     * @param workspaceEnvVarModel 变量信息
     */
    public void put(String name, WorkspaceEnvVarItemModel workspaceEnvVarModel) {
        if (varData == null) {
            varData = new HashMap<>(2);
        }
        varData.put(name, workspaceEnvVarModel);
    }

    /**
     * 删除 变量
     *
     * @param name 名称
     */
    public void remove(String name) {
        if (varData == null) {
            return;
        }
        varData.remove(name);
    }

    /**
     * @since 2022/3/8
     */
    @Data
    public static class WorkspaceEnvVarItemModel {

        private String name;

        private String value;

        private String description;

        /**
         * 隐私变量{1，隐私变量，0 非隐私变量（明文回显）}
         */
        private Integer privacy;
    }
}
