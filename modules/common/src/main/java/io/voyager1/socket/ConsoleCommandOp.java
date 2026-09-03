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

package io.voyager1.socket;

import lombok.Getter;

/**
 * 控制台 socket 操作枚举
 *
 * @since 2019/4/16
 */
@Getter
public enum ConsoleCommandOp {
    /**
     * 启动
     */
    start(true),
    stop(true),
    restart(true),
    status,
    /**
     * 重载
     */
    reload(true),
    /**
     * 运行日志
     */
    showlog,
    /**
     * 心跳
     */
    heart,
    ;
    /**
     * 是否支持手动操作（执行）
     */
    private final boolean canOpt;

    ConsoleCommandOp() {
        this.canOpt = false;
    }

    ConsoleCommandOp(boolean canOpt) {
        this.canOpt = canOpt;
    }
}
