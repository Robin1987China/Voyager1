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

package io.voyager1.configuration;

import io.voyager1.util.StrUtil;
import lombok.Data;
import io.voyager1.common.Const;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @since 23/12/25 025
 */
@Data
@ConfigurationProperties("voyager1.cluster")
public class ClusterConfig {

    /**
     * 集群Id，默认为 default 不区分大小写，只能是字母或者数字，长度小于 20
     */
    private String id;
    /**
     * 检查节点心跳间隔时间,最小值 5 秒
     */
    private int heartSecond = 30;

    public int getHeartSecond() {
        return Math.max(this.heartSecond, 5);
    }

    public String getId() {
        return (this.id == null || this.id.isEmpty() ? Const.WORKSPACE_DEFAULT_ID : this.id).toUpperCase();
    }
}
