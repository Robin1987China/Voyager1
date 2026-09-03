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
 * 版本生命周期状态
 *
 * @since 2026/8/7
 */
@Getter
public enum VersionStatus implements BaseEnum {
    /**
     * 开发中（CI+CD 可用）
     */
    Developing(0, "开发中"),
    /**
     * 已提测（冻结 CI，仅 CD）
     */
    Submitted(1, "已提测"),
    /**
     * 已发布
     */
    Released(2, "已发布"),
    /**
     * 已打回（回到开发中，解锁 CI）
     */
    Returned(3, "已打回"),
    ;

    private final int code;
    private final String desc;

    VersionStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
