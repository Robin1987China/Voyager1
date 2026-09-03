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

package io.voyager1.model.user;

import io.voyager1.util.PropIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseDbModel;
import io.voyager1.service.ITriggerToken;

/**
 * id 为 triggerToken
 *
 * @since 2022/7/22
 */
@TableName(value = "TRIGGER_TOKEN_LOG",
    nameKey = "触发器 token")
@Data
@EqualsAndHashCode(callSuper = true)
public class TriggerTokenLogBean extends BaseDbModel {
    /**
     * 为了兼容旧数据（因为旧数据字段长度大于 50 ）
     * <p>
     * 198fc5b944a3978b839506eb9d534c6f2b200dcda4e1d378fe30d2e8dbd7335bf4a
     */
    private String triggerToken;
    /**
     * 类型
     *
     * @see ITriggerToken#typeName()
     */
    private String type;

    /**
     * 关联数据ID
     */
    private String dataId;
    /**
     * 关联数据名称
     */
    @PropIgnore
    private String dataName;

    /**
     * 用户ID
     *
     * @see UserModel#getId()
     */
    private String userId;
    /**
     * 触发次数
     */
    private Integer triggerCount;
}
