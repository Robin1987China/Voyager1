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

package io.voyager1.model.enums;

import lombok.Getter;
import io.voyager1.model.BaseEnum;

/**
 * Pipeline 执行状态
 *
 * @since 2026/8/7
 */
@Getter
public enum PipelineExecuteStatus implements BaseEnum {
    /**
     * 等待执行
     */
    Wait(0, "等待中"),
    /**
     * 运行中
     */
    Running(1, "运行中"),
    /**
     * 成功
     */
    Success(2, "成功"),
    /**
     * 失败
     */
    Failed(3, "失败"),
    /**
     * 已取消
     */
    Cancel(4, "已取消"),
    /**
     * 等待审批
     */
    WaitApproval(5, "等待审批"),
    ;

    private final int code;
    private final String desc;

    PipelineExecuteStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
