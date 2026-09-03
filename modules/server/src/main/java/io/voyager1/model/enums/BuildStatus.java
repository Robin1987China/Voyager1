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
 * @since 2021/8/27
 */
@Getter
public enum BuildStatus implements BaseEnum {
    /**
     *
     */
    No(0, "未构建"),
    Ing(1, "构建中", true),
    Success(2, "构建结束"),
    Error(3, "构建失败"),
    PubIng(4, "发布中", true),
    PubSuccess(5, "发布成功"),
    PubError(6, "发布失败"),
    Cancel(7, "取消构建"),
    Interrupt(8, "构建中断"),
    WaitExec(9, "队列等待", true),
    AbnormalShutdown(10, "异常关闭"),
    ;

    private final int code;
    private final String desc;
    private final boolean progress;

    BuildStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
        this.progress = false;
    }

    BuildStatus(int code, String desc, boolean progress) {
        this.code = code;
        this.desc = desc;
        this.progress = progress;
    }

}
