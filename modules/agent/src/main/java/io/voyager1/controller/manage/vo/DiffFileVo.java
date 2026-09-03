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

package io.voyager1.controller.manage.vo;

import lombok.Data;

import java.util.List;

/**
 * @since 2021/12/16
 */
@Data
public class DiffFileVo {

    /**
     * 项目id
     */
    private String id;
    /**
     * 需要对比的数据
     */
    private List<DiffItem> data;
    /**
     * 需要对比的目录
     */
    private String dir;

    @Data
    public static class DiffItem {
        /**
         * 名称
         */
        private String name;
        /**
         * 文件签名
         */
        private String sha1;
    }
}
