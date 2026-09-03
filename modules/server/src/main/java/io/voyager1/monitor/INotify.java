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

package io.voyager1.monitor;

import io.voyager1.model.data.MonitorModel;

/**
 * 通知接口
 *
 * @since 2019/7/13
 */
public interface INotify {

    /**
     * 发送通知
     *
     * @param notify  通知方式
     * @param title   标题
     * @param context 内容
     */
    void send(MonitorModel.Notify notify, String title, String context) throws Exception;
}
