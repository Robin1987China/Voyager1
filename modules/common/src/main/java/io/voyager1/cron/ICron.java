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

package io.voyager1.cron;

import io.voyager1.model.BaseIdModel;

import java.util.List;

/**
 * 需要启动定时任务的 服务接口
 *
 * @since 2021/12/23
 */
public interface ICron<T extends BaseIdModel> {

    /**
     * 查询启动中的 所有定时任务 列表
     *
     * @return list
     */
    List<T> queryStartingList();

    /**
     * 启动所有的定时任务
     *
     * @return 启动成功的任务数
     */
    default int startCron() {
        List<T> startingList = this.queryStartingList();
        if (startingList == null) {
            return 0;
        }
        return (int) startingList.stream()
                .map(ICron.this::checkCron)
                .filter(aBoolean -> aBoolean)
                .count();
    }

    /**
     * 检查是否启动定时
     *
     * @param data bean
     * @return true 开启定时、false 关闭定时
     */
    boolean checkCron(T data);
}
