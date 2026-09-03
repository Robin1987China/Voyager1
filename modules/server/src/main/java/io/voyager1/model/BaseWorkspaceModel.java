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

import io.voyager1.util.ClassUtil;
import io.voyager1.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.common.Const;
import io.voyager1.common.ServerConst;
import io.voyager1.core.db.TableName;
import io.voyager1.model.data.WorkspaceModel;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工作空间 数据
 *
 * @since 2021/12/04
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseWorkspaceModel extends BaseUserModifyDbModel {

    /**
     * 工作空间ID
     *
     * @see WorkspaceModel
     * @see Const#WORKSPACE_ID_REQ_HEADER
     */
    private String workspaceId;

    @Override
    public String toString() {
        return super.toString();
    }

    public boolean global() {
        return java.util.Objects.equals(this.workspaceId, ServerConst.WORKSPACE_GLOBAL);
    }

    /**
     * 所有实现过的 class
     *
     * @return set
     */
    public static Set<Class<?>> allClass() {
        return ClassUtil.scanPackageBySuper("io.voyager1", BaseWorkspaceModel.class);
    }

    /**
     * 所有实现过的 class
     *
     * @return set
     */
    public static Set<Class<?>> allTableClass() {
        Set<Class<?>> classes1 = allClass();
        return classes1.stream()
            .filter(aClass -> aClass.isAnnotationPresent(TableName.class))
            .collect(Collectors.toSet());
    }

}
