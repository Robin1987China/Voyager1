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

import io.voyager1.util.CharsetUtil;
import io.voyager1.util.SystemUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.charset.Charset;
import java.util.Optional;
import java.nio.charset.StandardCharsets;

/**
 * @since 23/12/29 029
 */
@Data
@ConfigurationProperties("voyager1.project.log")
public class ProjectLogConfig {
    /**
     * 检测控制台日志周期，防止日志文件过大，目前暂只支持linux 不停服备份
     */
    private String autoBackupConsoleCron = "0 0/10 * * * ?";
    /**
     * 当文件多大时自动备份
     *
     * @see ch.qos.logback.core.util.FileSize
     */
    private DataSize autoBackupSize = DataSize.ofMegabytes(50);
    /**
     * 是否自动将控制台日志文件备份
     */
    private boolean autoBackupToFile = true;

    /**
     * 控制台日志保存时长单位天
     */
    private int saveDays = 7;

    public int getSaveDays() {
        return Math.max(saveDays, 0);
    }

    /**
     * 日志文件的编码格式
     */
    private Charset fileCharset;

    public Charset getFileCharset() {
        return Optional.ofNullable(this.fileCharset).orElseGet(() ->
            SystemUtil.getOsInfo().isWindows() ?
                java.nio.charset.Charset.forName("GBK") : StandardCharsets.UTF_8);
    }
}
