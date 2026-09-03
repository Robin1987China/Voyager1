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

import io.voyager1.util.ObjectUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 资产配置
 *
 * @since 23/12/25 025
 */
@ConfigurationProperties("voyager1.assets")
@Data
@Configuration
@EnableConfigurationProperties({AssetsConfig.SshConfig.class, AssetsConfig.DockerConfig.class})
public class AssetsConfig {
    /**
     * 监控线程池大小,小于等于0 为CPU核心数
     */
    private int monitorPoolSize = 0;

    /**
     * 监控任务等待数量，超过此数量将取消监控任务，值最小为 1
     */
    private int monitorPoolWaitQueue = 500;
    /**
     * ssh 资产配置
     */
    private SshConfig ssh;
    /**
     * docker 资产配置
     */
    private DockerConfig docker;

    public SshConfig getSsh() {
        if (this.ssh == null) {
            this.ssh = new SshConfig();
        }
        return this.ssh;
    }

    public DockerConfig getDocker() {
        if (this.docker == null) {
            this.docker = new DockerConfig();
        }
        return this.docker;
    }

    /**
     * ssh 配置
     */
    @Data
    @ConfigurationProperties("voyager1.assets.ssh")
    public static class SshConfig {

        /**
         * 监控频率
         */
        private String monitorCron;
        /**
         * 禁用监控的分组名 （如果想禁用所有配置 * 即可）
         */
        private List<String> disableMonitorGroupName;

    }

    /**
     * docker 配置
     */
    @Data
    @ConfigurationProperties("voyager1.assets.docker")
    public static class DockerConfig {

        /**
         * 监控频率
         */
        private String monitorCron;
    }
}
