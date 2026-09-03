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

package io.voyager1.model.outgiving;

import io.voyager1.util.ObjectUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import io.voyager1.model.BaseEnum;

/**
 * 节点项目
 *
 * @since 2019/4/22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OutGivingNodeProject extends BaseNodeProject {

    /**
     * 排序值
     */
    private Integer sortValue;

    /**
     * 是否禁用
     */
    private Boolean disabled;

    public Boolean getDisabled() {
        return (disabled != null ? disabled : false);
    }

    /**
     * 状态
     */
    @Getter
    public enum Status implements BaseEnum {
        /**
         *
         */
        No(0, "未分发"),
        Ing(1, "分发中"),
        Ok(2, "分发成功"),
        Fail(3, "分发失败"),
        Cancel(4, "系统取消分发"),
        Prepare(5, "准备分发"),
        ArtificialCancel(6, "手动取消分发"),
        ;
        private final int code;
        private final String desc;

        Status(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }
}
