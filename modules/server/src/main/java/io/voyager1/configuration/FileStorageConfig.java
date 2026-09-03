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

/**
 * 文件管理存储
 *
 * @since 23/12/25 025
 */
@Data
@ConfigurationProperties("voyager1.file-storage")
public class FileStorageConfig {

    /**
     * 文件中心存储路径
     */
    private String savePah;
    /**
     * 静态目录扫描周期
     * <p>
     * 0 0/1 * * *
     */
    private String scanStaticDirCron = "0 0/1 * * *";
    /**
     * 开启静态目录监听
     */
    private Boolean watchMonitorStaticDir = true;
    /**
     * 监听深度
     */
    private Integer watchMonitorMaxDepth = 1;
}
