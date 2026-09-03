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

package io.voyager1.model.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseUserModifyDbModel;
import io.voyager1.util.StringUtil;

import java.util.List;

/**
 * 系统参数
 *
 * @since 2021/12/2
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "SYS_PARAMETER",
    nameKey = "系统参数")
@Data
public class SystemParametersModel extends BaseUserModifyDbModel {

    /**
     * 参数值
     */
    private String value;
    /**
     * 参数描述
     */
    private String description;

    public <T> T jsonToBean(Class<T> cls) {
        return StringUtil.jsonConvert(this.getValue(), cls);
    }

    public <T> List<T> jsonToBeanList(Class<T> cls) {
        return StringUtil.jsonConvertArray(this.getValue(), cls);
    }
}
