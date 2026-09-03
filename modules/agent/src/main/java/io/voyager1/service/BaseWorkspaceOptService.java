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

package io.voyager1.service;

import io.voyager1.util.DateUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.common.BaseAgentController;
import io.voyager1.model.data.BaseWorkspaceModel;

/**
 * @since 2022/1/17
 */
public abstract class BaseWorkspaceOptService<T extends BaseWorkspaceModel> extends BaseOperService<T> {

    public BaseWorkspaceOptService(String fileName) {
        super(fileName);
    }

    @Override
    public void addItem(T t) {
        t.setCreateTime(DateUtil.now().toString());
        String userName = BaseAgentController.getNowUserName();
        if (!"-".equals(userName)) {
            t.setCreateUser(userName);
            t.setModifyUser(userName);
        }
        super.addItem(t);
    }

    /**
     * 修改信息
     *
     * @param data 信息
     */
    @Override
    public void updateItem(T data) {
        data.setModifyTime(DateUtil.now().toString());
        String userName = BaseAgentController.getNowUserName();
        if (!"-".equals(userName)) {
            data.setModifyUser(userName);
        }
        super.updateItem(data);
    }

    @Override
    public void updateById(T updateData, String id) {
        updateData.setModifyTime(DateUtil.now().toString());
        String userName = BaseAgentController.getNowUserName();
        if (!"-".equals(userName)) {
            updateData.setModifyUser(userName);
        }
        super.updateById(updateData, id);
    }
}
