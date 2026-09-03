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
 * @since 2021-11-24
 */
public enum BackupTypeEnum implements BaseEnum {
    /**
     * 备份类型{0: 全量, 1: 部分}
     */
    ALL(0, "全量备份"),
    PART(1, "部分备份"),
    IMPORT(2, "导入备份"),
    AUTO(3, "自动备份"),
    TRIGGER(4, "触发器"),
    ;

    BackupTypeEnum(int code, String desc) {
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
