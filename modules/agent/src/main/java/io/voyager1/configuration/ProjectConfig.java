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

import java.util.Optional;

/**
 * @since 23/12/29 029
 */
@Data
@ConfigurationProperties("voyager1.project")
public class ProjectConfig {
    /**
     * 项目日志配置
     */
    private ProjectLogConfig log;
    /**
     * 停止项目等待的时长 单位秒，最小为1秒
     */
    private int statusWaitTime = 10;

    /**
     * 项目状态检测间隔时间 单位毫秒，最小为1毫秒
     */
    private int statusDetectionInterval = 500;

    /**
     * 项目文件备份保留个数,大于 1 才会备份
     */
    private int fileBackupCount;

    /**
     * 限制备份指定文件后缀（支持正则）
     * [ '.jar','.html','^.+\\.(?i)(txt)$' ]
     */
    private String[] fileBackupSuffix;

    public ProjectLogConfig getLog() {
        return Optional.ofNullable(this.log).orElseGet(() -> {
            this.log = new ProjectLogConfig();
            return this.log;
        });
    }
}
