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

package io.voyager1.permission;

import lombok.Getter;
import io.voyager1.common.i18n.I18nMessageUtil;

import java.util.function.Supplier;

/**
 * 功能方法
 *
 * @since 2019/8/13
 */
@Getter
public enum MethodFeature {
    /**
     * 没有
     */
    NULL(() -> ""),
    EDIT(() -> "修改、添加数据"),
    DEL(() -> "删除数据"),
    LIST(() -> "列表、查询"),
    DOWNLOAD(() -> "下载"),
    UPLOAD(() -> "上传"),
    EXECUTE(() -> "执行"),
    REMOTE_DOWNLOAD(() -> "下载远程文件"),
    ;

    private final Supplier<String> name;

    MethodFeature(Supplier<String> name) {
        this.name = name;
    }
}
