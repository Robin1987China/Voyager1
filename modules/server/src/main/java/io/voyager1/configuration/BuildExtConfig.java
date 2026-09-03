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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 构建相关配置
 *
 * @since 2022/7/7
 */
@Configuration
@ConfigurationProperties(prefix = "voyager1.build")
@Data
public class BuildExtConfig {

    /**
     * 构建最多保存多少份历史记录
     */
    private int maxHistoryCount = 1000;

    /**
     * 每一项构建最多保存的历史份数
     */
    private int itemMaxHistoryCount = 50;

    private boolean checkDeleteCommand = true;

    /**
     * 构建线程池大小,小于 1 则为不限制，默认大小为 5
     */
    private int poolSize = 5;

    /**
     * 构建任务等待数量，超过此数量将取消构建任务，值最小为 1
     */
    private int poolWaitQueue = 10;
    /**
     * 压缩折叠显示进度比例 范围 1-100
     */
    private int logReduceProgressRatio = 5;

    public void setLogReduceProgressRatio(int logReduceProgressRatio) {
        // 修正值
        this.logReduceProgressRatio = Math.min(Math.max(logReduceProgressRatio, 1), 100);
    }
}
