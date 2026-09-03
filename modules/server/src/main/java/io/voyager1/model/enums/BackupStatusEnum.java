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

import io.voyager1.model.BaseEnum;

/**
 * backup type
 *
 * @since 2021-11-27
 */
public enum BackupStatusEnum implements BaseEnum {
    /**
     * 状态{0: 处理中; 1: 成功; 2: 失败}
     */
    DEFAULT(0, "处理中"),
    SUCCESS(1, "备份成功"),
    FAILED(2, "备份失败"),
    ;

    BackupStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    final int code;
    final String desc;

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

}
